package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditMapper.toAuditJson;
import static ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport.getCurrentUserName;
import static ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.TaskActionDto.*;
import static ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TaskStatus.*;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.TaskActionsResolver.resolvePossibleActions;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.TaskActionsResolver.validateActionAllowed;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.TaskMapper.toTrustOnboardingTaskDto;

import ch.admin.bj.swiyu.trust.client.core.business.internal.api.TrustOnboardingSubmissionApi;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.TrustOnboardingSubmissionDto;
import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ExternalSystem;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ExternalSystemException;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ResourceNotFoundException;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustOnboardingRejectReasonDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.TrustOnboardingTaskDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.ApproveTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.RejectTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.RequestMoreInformationTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.config.TrustOnboardingTaskProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.event.TiTrustOnboardingInformationRequestedEventBuilder;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.event.TiTrustOnboardingRejectedEventBuilder;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.event.TiTrustOnboardingSucceededEventBuilder;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.OutboxEventPublisher;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.*;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrustOnboardingTaskService {

    private final TrustOnboardingTaskRepository taskRepository;
    private final TrustOnboardingSubmissionApi trustOnboardingSubmissionApi;
    private final OutboxEventPublisher outboxEventPublisher;
    private final DomainEventService domainEventService;
    private final TrustOnboardingTaskProperties trustOnboardingTaskProperties;
    private final AuditPublisher auditPublisher;
    private final BusinessPartnerIdentityService businessPartnerIdentityService;

    /**
     * Creates a new TrustOnboardingTask based on the provided TrustOnboardingSubmission
     *
     * @param trustOnboardingSubmission the submission based on which the task should be created
     * @param currentUserName           the name of the user triggering the creation, used for event publishing
     * @return the id of the created task
     */
    @Transactional
    public UUID createTask(TrustOnboardingSubmissionDto trustOnboardingSubmission, String currentUserName) {
        var dueAt = calculateDueAt(trustOnboardingSubmission);
        var task = switch (trustOnboardingSubmission.getType()) {
            case REGISTRATION -> TrustOnboardingTask.createRegistrationTask(
                UUID.randomUUID(),
                trustOnboardingSubmission.getPartnerId(),
                Map.copyOf(trustOnboardingSubmission.getName()),
                trustOnboardingSubmission.getId(),
                dueAt,
                trustOnboardingSubmission.getSubmittedAt()
            );
            case PROFILE_CHANGE -> TrustOnboardingTask.createProfileChangeTask(
                UUID.randomUUID(),
                trustOnboardingSubmission.getPartnerId(),
                Map.copyOf(trustOnboardingSubmission.getName()),
                trustOnboardingSubmission.getId(),
                dueAt,
                trustOnboardingSubmission.getSubmittedAt()
            );
            case RENEWAL -> TrustOnboardingTask.createRenewalTask(
                UUID.randomUUID(),
                trustOnboardingSubmission.getPartnerId(),
                Map.copyOf(trustOnboardingSubmission.getName()),
                trustOnboardingSubmission.getId(),
                dueAt,
                trustOnboardingSubmission.getSubmittedAt()
            );
        };
        task = this.taskRepository.save(task);
        domainEventService.trustOnboardingSubmissionReceived(task.getId(), currentUserName);
        return task.getId();
    }

    /**
     * Creates a new TrustOnboardingTask for the given submission, or, if a task for this submission already
     * exists (i.e. the partner resubmitted after being asked for more information), marks it as resubmitted.
     *
     * @param trustOnboardingSubmission the submission that was accepted
     * @param currentUserName           the name of the user triggering the creation, used for event publishing
     * @return the id of the created or resubmitted task
     */
    @Transactional
    public UUID createOrResubmitTask(TrustOnboardingSubmissionDto trustOnboardingSubmission, String currentUserName) {
        var existingTask = taskRepository.getTrustOnboardingTaskByTrustOnboardingSubmissionId(
            trustOnboardingSubmission.getId()
        );
        if (existingTask == null) {
            return createTask(trustOnboardingSubmission, currentUserName);
        }
        existingTask.resubmit(calculateDueAt(trustOnboardingSubmission));
        taskRepository.save(existingTask);
        domainEventService.trustOnboardingSubmissionResubmitted(existingTask.getId(), currentUserName);
        return existingTask.getId();
    }

    @Transactional(readOnly = true)
    public TrustOnboardingTaskDto getTask(UUID taskId, String currentUserFullName) {
        var task = getTrustOnboardingTask(taskId);
        TrustOnboardingSubmissionDto submission;
        try {
            submission = this.trustOnboardingSubmissionApi.getTrustOnboardingSubmission(
                task.getTrustOnboardingSubmissionId()
            );
        } catch (Exception e) {
            throw new ExternalSystemException(
                "Could not read submission data.",
                ExternalSystem.CORE_BUSINESS_SERVICE,
                HttpStatus.INTERNAL_SERVER_ERROR,
                e
            );
        }

        return toTrustOnboardingTaskDto(resolvePossibleActions(task, currentUserFullName), task, submission);
    }

    @Transactional
    public void approve(UUID taskId, ApproveTaskActionDto request, String triggeredBy) {
        log.info("Task {} is approved by {}", taskId.toString(), triggeredBy);
        var task = getTrustOnboardingTask(taskId);
        validateActionAllowed(task, triggeredBy, APPROVE);
        var submissionId = task.getTrustOnboardingSubmissionId();
        task.approve();
        taskRepository.save(task);

        var trustOnboardingSubmissionDto = trustOnboardingSubmissionApi.getTrustOnboardingSubmission(submissionId);
        businessPartnerIdentityService.activate(
            businessPartnerIdentityService.handleTrustOnboardingApproval(trustOnboardingSubmissionDto)
        );

        outboxEventPublisher.publishTrustOnboardingSucceededEvent(
            TiTrustOnboardingSucceededEventBuilder.create()
                .trustOnboardingSubmissionId(submissionId)
                .partnerNote((request.partnerNote()))
                .build()
        );
        domainEventService.trustOnboardingSubmissionSucceeded(
            task.getId(),
            triggeredBy,
            request.partnerNote(),
            request.internalNote()
        );
        auditPublisher.taskApproved(
            task.getPartnerId(),
            task.getId(),
            task.getVersion(),
            task.getTaskType().toString(),
            toAuditJson(task)
        );
    }

    @Transactional
    public void reject(UUID taskId, RejectTaskActionDto request, String triggeredBy) {
        log.info("Task {} is rejected by {}", taskId.toString(), triggeredBy);
        var task = getTrustOnboardingTask(taskId);
        var submissionId = task.getTrustOnboardingSubmissionId();
        validateActionAllowed(task, triggeredBy, REJECT);
        task.reject();
        taskRepository.save(task);
        outboxEventPublisher.publishTrustOnboardingRejectedEvent(
            TiTrustOnboardingRejectedEventBuilder.create()
                .trustOnboardingSubmissionId(submissionId)
                .partnerNote(request.partnerNote())
                .rejectReason(request.rejectReason().toString())
                .build()
        );
        domainEventService.trustOnboardingSubmissionRejected(
            task.getId(),
            triggeredBy,
            request.partnerNote(),
            request.internalNote()
        );
        auditPublisher.taskRejected(
            task.getPartnerId(),
            task.getId(),
            task.getVersion(),
            task.getTaskType().toString(),
            toAuditJson(task)
        );
    }

    @Transactional
    public void requestMoreInformation(UUID taskId, RequestMoreInformationTaskActionDto request, String triggeredBy) {
        log.info("Task {} is send back to user by {}", taskId.toString(), triggeredBy);
        var task = getTrustOnboardingTask(taskId);
        validateActionAllowed(task, triggeredBy, REQUEST_MORE_INFORMATION);
        var submissionId = task.getTrustOnboardingSubmissionId();
        var resubmitRequiredUntil = Instant.now().plus(trustOnboardingTaskProperties.rejectionEnforcementPeriod());
        task.requestMoreInformation(resubmitRequiredUntil);
        taskRepository.save(task);
        outboxEventPublisher.publishTrustOnboardingInformationRequestedEvent(
            TiTrustOnboardingInformationRequestedEventBuilder.create()
                .trustOnboardingSubmissionId(submissionId)
                .partnerNote(request.partnerNote())
                .resubmitRequiredUntil(resubmitRequiredUntil)
                .build()
        );
        domainEventService.trustOnboardingSubmissionMoreInformationRequested(
            task.getId(),
            triggeredBy,
            request.partnerNote(),
            request.internalNote()
        );
        auditPublisher.taskMoreInformationRequested(
            task.getPartnerId(),
            task.getId(),
            task.getVersion(),
            task.getTaskType().toString(),
            toAuditJson(task)
        );
    }

    /**
     * Automatically rejects trust onboarding tasks that are awaiting partner resubmission past the
     * rejection-enforcement deadline.
     */
    @Transactional
    public void rejectTasksPastRejectionEnforcementDeadline() {
        var now = Instant.now();
        QTrustOnboardingTask q = QTrustOnboardingTask.trustOnboardingTask;
        var overdueForResubmission = q.status.eq(INFORMATION_REQUESTED).and(q.rejectionEnforcedAt.lt(now));

        var triggeredBy = getCurrentUserName();
        for (var task : taskRepository.findAll(overdueForResubmission)) {
            log.info(
                "Task {} is automatically rejected due to exceeding its rejection-enforcement deadline",
                task.getId()
            );
            reject(
                task.getId(),
                new RejectTaskActionDto(
                    TrustOnboardingRejectReasonDto.NO_RESPONSE_FROM_APPLICANT,
                    "Automatically rejected by system due to exceeded rejection-enforcement deadline",
                    "Automatic rejection by system - rejection-enforcement deadline passed"
                ),
                triggeredBy
            );
        }
    }

    public TrustOnboardingTask getTrustOnboardingTask(UUID taskId) {
        return taskRepository
            .findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task with id " + taskId + " not found"));
    }

    private Instant calculateDueAt(TrustOnboardingSubmissionDto trustOnboardingSubmission) {
        var referenceInstant = Optional.ofNullable(trustOnboardingSubmission.getSubmittedAt()).orElseThrow();
        return referenceInstant.plus(trustOnboardingTaskProperties.dueDatePeriod());
    }
}
