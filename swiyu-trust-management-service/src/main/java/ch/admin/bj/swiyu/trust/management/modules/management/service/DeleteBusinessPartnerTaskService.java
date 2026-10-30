package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditMapper.toAuditJson;
import static ch.admin.bj.swiyu.trust.management.modules.common.exception.BusinessPartnerDeletionError.OPEN_DELETION_REQUEST_EXISTS;
import static ch.admin.bj.swiyu.trust.management.modules.common.exception.BusinessPartnerDeletionError.PARTNER_NOT_CLEARED_FOR_DELETION;
import static ch.admin.bj.swiyu.trust.management.modules.common.exception.BusinessPartnerDeletionError.PARTNER_NOT_SYNCED;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.TaskActionsResolver.resolvePossibleActions;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.TaskActionsResolver.validateActionAllowed;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.TaskMapper.toDeleteBusinessPartnerTaskDto;

import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.trust.management.modules.common.config.OffboardingProperties;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.BusinessPartnerNotDeletableException;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ResourceNotFoundException;
import ch.admin.bj.swiyu.trust.management.modules.common.features.FeaturesProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.CreateDeleteBusinessPartnerTaskRequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.DeleteBusinessPartnerTaskDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.RejectDeleteBusinessPartnerTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.TaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.DeleteBusinessPartnerTask;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.DeleteBusinessPartnerTaskRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TaskStatus;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Creates and rejects delete-business-partner tasks. There is deliberately no approve path: the task is auto-approved
 * by a job once {@code hardDeleteDueAt} is reached (EID-6994), so rejecting is the only manual outcome.
 */
@Service
@RequiredArgsConstructor
public class DeleteBusinessPartnerTaskService {

    /** A partner with a deletion request in one of these states must not get a second one. */
    private static final Set<TaskStatus> PENDING_STATUSES = Set.of(
        TaskStatus.OPENED,
        TaskStatus.INFORMATION_REQUESTED,
        TaskStatus.RESUBMITTED
    );

    private final DeleteBusinessPartnerTaskRepository deleteBusinessPartnerTaskRepository;
    private final BusinessPartnerRepository businessPartnerRepository;
    private final DomainEventService domainEventService;
    private final AuditPublisher auditPublisher;
    private final OffboardingProperties offboardingProperties;
    private final FeaturesProperties featuresProperties;

    @Transactional(readOnly = true)
    public DeleteBusinessPartnerTaskDto getTask(UUID taskId, String currentUserFullName) {
        var task = findTask(taskId);
        return toDeleteBusinessPartnerTaskDto(task, resolvePossibleActions(task, currentUserFullName));
    }

    @Transactional
    public UUID createTask(CreateDeleteBusinessPartnerTaskRequestDto request, String triggeredBy) {
        requireCompleteBpOffboardingEnabled();

        var partnerId = request.businessPartnerId();
        var partner = businessPartnerRepository
            .findById(partnerId)
            .orElseThrow(() ->
                new BusinessPartnerNotDeletableException(
                    PARTNER_NOT_SYNCED,
                    "Business partner %s has not been synced from the core business service yet, so it cannot be deleted.".formatted(
                        partnerId
                    )
                )
            );

        // The core business service owns this decision; see EID-6989. Without the flag the partner stays undeletable.
        if (!partner.isHardDeleteAllowed()) {
            throw new BusinessPartnerNotDeletableException(
                PARTNER_NOT_CLEARED_FOR_DELETION,
                "Business partner %s is not cleared for deletion by the core business service.".formatted(partnerId)
            );
        }
        if (deleteBusinessPartnerTaskRepository.existsByPartnerIdAndStatusIn(partnerId, PENDING_STATUSES)) {
            throw new BusinessPartnerNotDeletableException(
                OPEN_DELETION_REQUEST_EXISTS,
                "Business partner %s already has an open deletion request.".formatted(partnerId)
            );
        }

        var submittedAt = Instant.now();
        var task = new DeleteBusinessPartnerTask(
            partnerId,
            partner.getName(),
            submittedAt,
            submittedAt.plus(offboardingProperties.hardDeleteDelay())
        );
        task = saveSingleOpenTask(task, partnerId);

        // The reason is not a column of its own: it lives on as the internal note of the task.
        domainEventService.businessPartnerDeleteRequested(task.getId(), triggeredBy, request.reason());
        auditPublisher.taskCreated(
            partnerId,
            task.getId(),
            task.getVersion(),
            task.getTaskType().toString(),
            toAuditJson(task)
        );
        return task.getId();
    }

    @Transactional
    public void reject(UUID taskId, RejectDeleteBusinessPartnerTaskActionDto request, String triggeredBy) {
        var task = findTask(taskId);
        validateActionAllowed(task, triggeredBy, TaskActionDto.REJECT);
        task.reject();
        deleteBusinessPartnerTaskRepository.save(task);

        domainEventService.businessPartnerDeleteRejected(task.getId(), triggeredBy, request.internalNote());
        auditPublisher.taskRejected(
            task.getPartnerId(),
            task.getId(),
            task.getVersion(),
            task.getTaskType().toString(),
            toAuditJson(task)
        );
    }

    /**
     * A disabled feature must not be reachable just because someone knows the url, so this is enforced server-side
     * and not only by hiding the entry in the UI. Guarded here rather than in the controller, so no caller can reach
     * the flow with the toggle off.
     */
    private void requireCompleteBpOffboardingEnabled() {
        if (Boolean.FALSE.equals(featuresProperties.getEidartfe1571CompleteBpOffboarding())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Feature EIDARTFE_1571 is not enabled");
        }
    }

    /**
     * The duplicate check above reads before this write, so two editors requesting a deletion at the same time can
     * both pass it. The partial unique index on {@code task} is what actually guarantees a single pending task;
     * flushing here turns its violation into the same error the check would have produced.
     */
    private DeleteBusinessPartnerTask saveSingleOpenTask(DeleteBusinessPartnerTask task, UUID partnerId) {
        try {
            return deleteBusinessPartnerTaskRepository.saveAndFlush(task);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessPartnerNotDeletableException(
                OPEN_DELETION_REQUEST_EXISTS,
                "Business partner %s already has an open deletion request.".formatted(partnerId)
            );
        }
    }

    private DeleteBusinessPartnerTask findTask(UUID taskId) {
        return deleteBusinessPartnerTaskRepository
            .findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task with id %s not found".formatted(taskId)));
    }
}
