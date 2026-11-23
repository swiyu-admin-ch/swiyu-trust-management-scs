package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditMapper.toAuditJson;
import static ch.admin.bj.swiyu.trust.management.modules.common.exception.BusinessPartnerDeletionError.OPEN_DELETION_REQUEST_EXISTS;
import static ch.admin.bj.swiyu.trust.management.modules.common.exception.BusinessPartnerDeletionError.PARTNER_NOT_CLEARED_FOR_DELETION;
import static ch.admin.bj.swiyu.trust.management.modules.common.exception.BusinessPartnerDeletionError.PARTNER_NOT_SYNCED;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.TaskActionsResolver.resolvePossibleActions;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.TaskActionsResolver.validateActionAllowed;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.TaskMapper.toDeleteBusinessPartnerTaskDto;

import ch.admin.bit.jeap.security.resource.properties.ResourceServerProperties;
import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.trust.management.modules.common.config.OffboardingProperties;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.BusinessPartnerNotDeletableException;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ResourceNotFoundException;
import ch.admin.bj.swiyu.trust.management.modules.common.features.FeaturesProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.CreateDeleteBusinessPartnerTaskRequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.DeleteBusinessPartnerTaskDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.RejectDeleteBusinessPartnerTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.TaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartner;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.command.TiHardDeleteBusinessPartnerCommandBuilder;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.OutboxEventPublisher;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.DeleteBusinessPartnerTask;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.DeleteBusinessPartnerTaskRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TaskStatus;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Creates and rejects delete-business-partner tasks, and approves them once they are due. Approval is not a manual
 * action: a job approves the task when {@code hardDeleteDueAt} is reached (EID-6994), which is what publishes the
 * command that makes the core business service delete the partner. Rejecting within the waiting period is the only
 * manual outcome and the only way to call the deletion off.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteBusinessPartnerTaskService {

    /**
     * A partner with a deletion request in one of these states must not get a second one.
     */
    private static final Set<TaskStatus> PENDING_STATUSES = Set.of(
        TaskStatus.OPENED,
        TaskStatus.INFORMATION_REQUESTED,
        TaskStatus.RESUBMITTED
    );

    private final DeleteBusinessPartnerTaskRepository deleteBusinessPartnerTaskRepository;
    private final BusinessPartnerRepository businessPartnerRepository;
    private final DomainEventService domainEventService;
    private final AuditPublisher auditPublisher;
    private final OutboxEventPublisher outboxEventPublisher;
    private final ResourceServerProperties resourceServerProperties;
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
     * Approves a deletion task that has become due and thereby orders the deletion of the partner in the core business
     * service. Called by {@code HardDeleteBusinessPartnerJob}; the only manual path is the devops endpoint, which
     * deliberately skips the waiting period (see {@link #approveWithoutDelay}).
     *
     * <p>Everything happens in one transaction: the status change, the command in the transactional outbox, the domain
     * event and the audit record. If any of them fails, nothing happened and the task is still open for the next run.
     *
     * <p>Two checks deliberately leave the task open instead of failing: a task that is not due yet (the job's filter
     * is an upper bound, so it can see tasks that only become due later) and a partner the core business service has
     * withdrawn the deletion clearance for. Both are retried on the next run, so a deletion that is called off by
     * disarming the safeguard resumes once the safeguard is armed again.
     */
    @Transactional
    public void approve(UUID taskId, String triggeredBy) {
        var task = findTask(taskId);
        if (task.getHardDeleteDueAt().isAfter(Instant.now())) {
            log.info("Deletion task {} is not due before {}, leaving it open", taskId, task.getHardDeleteDueAt());
            return;
        }
        if (!isHardDeleteStillAllowed(task.getPartnerId())) {
            log.warn(
                "Business partner {} of deletion task {} is no longer cleared for deletion, leaving the task open",
                task.getPartnerId(),
                taskId
            );
            return;
        }
        approveTask(task, triggeredBy);
    }

    /**
     * Approves the partner's latest deletion request without waiting for {@code hardDeleteDueAt}. Devops only, for
     * end-to-end tests and to unblock a deletion without waiting out the configured delay.
     *
     * <p>Only the waiting period is skipped. The clearance of the local projection still applies: a partner the core
     * business service does not allow to be deleted is refused here with an error instead of leaving the task open,
     * because unlike the job nobody would come back and retry it.
     *
     * @throws ResourceNotFoundException                                                                        when the partner has no deletion request at all
     * @throws ch.admin.bj.swiyu.trust.management.modules.common.exception.TaskStatusTransitionException        when its
     *                                                                                                          latest request was already approved or rejected - rejection is final, so there is nothing left to approve
     * @throws ch.admin.bj.swiyu.trust.management.modules.common.exception.BusinessPartnerNotDeletableException when
     *                                                                                                          the partner is no longer cleared for deletion
     */
    @Transactional
    public void approveWithoutDelay(UUID partnerId, String triggeredBy) {
        requireCompleteBpOffboardingEnabled();
        var task = deleteBusinessPartnerTaskRepository
            .findFirstByPartnerIdOrderBySubmittedAtDesc(partnerId)
            .orElseThrow(() ->
                new ResourceNotFoundException("Business partner %s has no deletion request".formatted(partnerId))
            );
        if (!isHardDeleteStillAllowed(partnerId)) {
            throw new BusinessPartnerNotDeletableException(
                PARTNER_NOT_CLEARED_FOR_DELETION,
                "Business partner %s is not cleared for deletion by the core business service.".formatted(partnerId)
            );
        }
        approveTask(task, triggeredBy);
    }

    private void approveTask(DeleteBusinessPartnerTask task, String triggeredBy) {
        // Not validateActionAllowed: the action resolver removes APPROVE from this task type on purpose, so that no
        // UI user can approve it. Only the job and the devops endpoint approve it, once the waiting period is over or
        // on explicit request.
        task.approve();
        // flush, so the audit record names the version of the accepted task and not the one before the status change
        task = deleteBusinessPartnerTaskRepository.saveAndFlush(task);

        outboxEventPublisher.publishHardDeleteBusinessPartnerCommand(
            TiHardDeleteBusinessPartnerCommandBuilder.create()
                .businessPartnerId(task.getPartnerId())
                // The person who requested the deletion is named in the command, not the job that runs it: the core
                // business service audits it as the originator of the deletion.
                .requester(task.getAudit().getCreatedBy(), authorizationServerIssuer())
                .build()
        );
        domainEventService.businessPartnerDeleteAccepted(task.getId(), triggeredBy);
        auditPublisher.taskApproved(
            task.getPartnerId(),
            task.getId(),
            task.getVersion(),
            task.getTaskType().toString(),
            toAuditJson(task)
        );
        log.info(
            "Deletion task {} approved by {}, hard deletion of partner {} ordered",
            task.getId(),
            triggeredBy,
            task.getPartnerId()
        );
    }

    /**
     * The core business service owns this decision; the local projection only mirrors it. Without the flag the partner
     * stays undeletable, so there is no point in ordering a deletion that would be refused.
     */
    private boolean isHardDeleteStillAllowed(UUID partnerId) {
        return businessPartnerRepository.findById(partnerId).map(BusinessPartner::isHardDeleteAllowed).orElse(false);
    }

    /**
     * The resource server accepts tokens from exactly one issuer, so the configured issuer is the {@code iss} claim of
     * every request. The task itself only stores the audit user id (see {@link
     * ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditorProvider}), so this fills in the second half of
     * the command's {@code AuditUser}.
     */
    private String authorizationServerIssuer() {
        return resourceServerProperties.getAuthorizationServer().getIssuer();
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
