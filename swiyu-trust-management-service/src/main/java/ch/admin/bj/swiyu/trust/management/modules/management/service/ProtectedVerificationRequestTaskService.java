package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.management.service.TaskActionsResolver.resolvePossibleActions;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.TaskMapper.*;

import ch.admin.bj.swiyu.trust.client.core.business.internal.api.ProtectedVerificationSubmissionInternalApi;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.ProtectedVerificationSubmissionDto;
import ch.admin.bj.swiyu.trust.client.zas.sbn.api.UsnApi;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ExternalSystem;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ExternalSystemException;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ResourceNotFoundException;
import ch.admin.bj.swiyu.trust.management.modules.common.i18n.LocalizedMapUtil;
import ch.admin.bj.swiyu.trust.management.modules.management.api.ProtectedVerificationAuthorizationRequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.ZasDataDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.ProtectedVerificationRequestTaskDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.ApproveProtectedVerificationRequestTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.RejectProtectedVerificationRequestTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.TaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.config.ProtectedVerificationTaskProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.event.TiProtectedVerificationSubmissionApprovedEventBuilder;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.event.TiProtectedVerificationSubmissionRejectedEventBuilder;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.OutboxEventPublisher;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.ProtectedVerificationRequestTask;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.ProtectedVerificationRequestTaskRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.Task;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProtectedVerificationRequestTaskService {

    private static final String SYSTEM_USER = "System User";
    private static final String NO_BUSINESS_PARTNER_IDENTITY_REJECT_REASON =
        "Your organization is not yet onboarded to the trust registry. Please complete onboarding and resubmit this protected verification request.";

    private final ProtectedVerificationRequestTaskRepository taskRepository;
    private final ProtectedVerificationSubmissionInternalApi protectedVerificationSubmissionApi;
    private final UsnApi usnApi;
    private final BusinessPartnerIdentityService businessPartnerIdentityService;
    private final OutboxEventPublisher outboxEventPublisher;
    private final DomainEventService domainEventService;
    private final ProtectedVerificationTaskProperties protectedVerificationTaskProperties;

    @Transactional
    public UUID createTask(UUID protectedVerificationSubmissionId, String triggeredBy) {
        var submission = fetchSubmission(protectedVerificationSubmissionId);
        var submittedAt = submission.getSubmittedAt();
        var dueAt = submittedAt.plus(protectedVerificationTaskProperties.dueDatePeriod());
        var task = new ProtectedVerificationRequestTask(
            submission.getPartnerId(),
            LocalizedMapUtil.fromSingleName(submission.getEntityName()),
            protectedVerificationSubmissionId,
            dueAt,
            submittedAt
        );
        task = taskRepository.save(task);
        domainEventService.protectedVerificationRequestReceived(task.getId(), triggeredBy);

        var businessPartnerIdentityExists = businessPartnerIdentityService.businessPartnerIdentityExists(
            submission.getPartnerId()
        );
        if (!businessPartnerIdentityExists) {
            log.warn(
                "Partner {} of protected verification submission {} has no BusinessPartnerIdentity. Auto-rejecting task {}.",
                submission.getPartnerId(),
                protectedVerificationSubmissionId,
                task.getId()
            );
            // No ZAS data snapshot to audit: the task was auto-rejected right at creation, so it was never shown
            // to a BJ user in the first place.
            rejectTask(task, SYSTEM_USER, NO_BUSINESS_PARTNER_IDENTITY_REJECT_REASON, null);
        }
        return task.getId();
    }

    @Transactional(readOnly = true)
    public ProtectedVerificationRequestTaskDto getTask(UUID taskId, String currentUserFullName) {
        var task = getTaskOrThrow(taskId);
        var submission = fetchSubmission(task.getProtectedVerificationSubmissionId());
        var allowedActions = resolvePossibleActions(task, currentUserFullName);
        return toProtectedVerificationRequestTaskDto(task, submission, allowedActions);
    }

    @Transactional(readOnly = true)
    public ZasDataDto getZasData(UUID taskId) {
        var task = getTaskOrThrow(taskId);
        var submission = fetchSubmission(task.getProtectedVerificationSubmissionId());
        return fetchZasData(submission);
    }

    /**
     * The UI calls this right after it has fetched and displayed the ZAS data on the task detail page, so a
     * successful call here is taken as the signal that the data has been reviewed and gates approve/reject
     * (see {@link #requireActionAllowed}). Kept separate from {@link #getZasData} so that reading the data stays a
     * side-effect-free GET - safe for prefetching, monitoring, etc. - while this explicit action is what actually
     * records the review.
     */
    @Transactional
    public void markZasDataReviewed(UUID taskId) {
        var task = getTaskOrThrow(taskId);
        task.markZasDataOpened(Instant.now());
        taskRepository.save(task);
    }

    @Transactional
    public void approve(UUID taskId, ApproveProtectedVerificationRequestTaskActionDto request, String triggeredBy) {
        log.info("Task {} is approved by {}", taskId, triggeredBy);
        var task = getTaskOrThrow(taskId);
        requireActionAllowed(task, triggeredBy, TaskActionDto.APPROVE);
        var submission = fetchSubmission(task.getProtectedVerificationSubmissionId());

        task.approve();
        taskRepository.save(task);

        businessPartnerIdentityService.addProtectedVerificationAuthorization(
            new ProtectedVerificationAuthorizationRequestDto(
                task.getPartnerId(),
                toAuthorizableField(submission.getCategory())
            )
        );

        outboxEventPublisher.publishProtectedVerificationSubmissionApprovedEvent(
            TiProtectedVerificationSubmissionApprovedEventBuilder.create()
                .protectedVerificationSubmissionId(task.getProtectedVerificationSubmissionId())
                .build()
        );

        domainEventService.protectedVerificationRequestApproved(task.getId(), triggeredBy, request.internalNote());
    }

    @Transactional
    public void reject(UUID taskId, RejectProtectedVerificationRequestTaskActionDto request, String triggeredBy) {
        log.info("Task {} is rejected by {}", taskId, triggeredBy);
        var task = getTaskOrThrow(taskId);
        requireActionAllowed(task, triggeredBy, TaskActionDto.REJECT);
        rejectTask(task, triggeredBy, request.rejectReason(), request.internalNote());
    }

    private void rejectTask(
        ProtectedVerificationRequestTask task,
        String triggeredBy,
        String rejectReason,
        String internalNote
    ) {
        task.reject();
        taskRepository.save(task);

        outboxEventPublisher.publishProtectedVerificationSubmissionRejectedEvent(
            TiProtectedVerificationSubmissionRejectedEventBuilder.create()
                .protectedVerificationSubmissionId(task.getProtectedVerificationSubmissionId())
                .rejectReason(rejectReason)
                .build()
        );

        domainEventService.protectedVerificationRequestRejected(task.getId(), triggeredBy, rejectReason, internalNote);
    }

    private ZasDataDto fetchZasData(ProtectedVerificationSubmissionDto submission) {
        try {
            var page = usnApi.searchUsns(
                submission.getSbnId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            );
            var usn = page
                .getContent()
                .stream()
                .findFirst()
                .orElseThrow(() ->
                    new ResourceNotFoundException("No ZAS data found for SBN-ID %s".formatted(submission.getSbnId()))
                );
            return toZasDataDto(usn);
        } catch (RestClientResponseException exception) {
            throw new ExternalSystemException(
                exception.getMessage(),
                ExternalSystem.ZAS_SBN,
                exception.getStatusCode()
            );
        }
    }

    /**
     * Validates the requested action against the same {@link TaskActionsResolver#resolvePossibleActions(Task, String)}
     * result the UI uses to decide which actions to show, so the two never diverge.
     */
    private void requireActionAllowed(ProtectedVerificationRequestTask task, String triggeredBy, TaskActionDto action) {
        var allowedActions = resolvePossibleActions(task, triggeredBy);
        if (!allowedActions.contains(action)) {
            throw new IllegalArgumentException(
                "Action %s is not allowed for task %s in its current state".formatted(action, task.getId())
            );
        }
    }

    private ProtectedVerificationSubmissionDto fetchSubmission(UUID protectedVerificationSubmissionId) {
        try {
            return protectedVerificationSubmissionApi.getProtectedVerificationSubmission(
                protectedVerificationSubmissionId
            );
        } catch (RestClientResponseException exception) {
            throw new ExternalSystemException(
                exception.getMessage(),
                ExternalSystem.CORE_BUSINESS_SERVICE,
                exception.getStatusCode()
            );
        }
    }

    private ProtectedVerificationRequestTask getTaskOrThrow(UUID taskId) {
        return taskRepository
            .findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task with id %s not found".formatted(taskId)));
    }
}
