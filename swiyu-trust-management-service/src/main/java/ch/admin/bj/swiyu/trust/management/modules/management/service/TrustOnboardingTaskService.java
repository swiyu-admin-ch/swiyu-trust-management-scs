package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport.getCurrentUserFullName;
import static ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustTaskStatus.*;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.TrustOnboardingTaskActionsResolver.resolvePossibleActions;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.TrustOnboardingTaskMapper.toTrustOnboardingTaskDto;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.TrustStatementMapper.toTrustStatementPartnerLinkIdentityV1RequestDtoList;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.TrustStatementMapper.toTrustStatementPartnerLinkIdentityV2RequestDtoList;

import ch.admin.bj.swiyu.trust.client.core.business.internal.api.TrustOnboardingSubmissionApi;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.TrustOnboardingSubmissionDto;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ExternalSystem;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ExternalSystemException;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustOnboardingTaskDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustOnboardingTaskListItemDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustStatementTypeDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.taskaction.ApproveTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.taskaction.RejectTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.taskaction.RequestMoreInformationTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.event.TiTrustOnboardingInformationRequestedEventBuilder;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.event.TiTrustOnboardingRejectedEventBuilder;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.event.TiTrustOnboardingSucceededEventBuilder;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.OutboxEventPublisher;
import com.querydsl.core.BooleanBuilder;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrustOnboardingTaskService {

    private final TrustOnboardingTaskRepository trustOnboardingTaskRepository;
    private final TrustOnboardingSubmissionApi trustOnboardingSubmissionApi;
    private final TrustTaskRepository trustTaskRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final DomainEventService domainEventService;
    private final TrustOnboardingTaskRepository taskRepository;
    private final TrustStatementService trustStatementService;
    private final TrustOnboardingTaskDomainService taskDomainService;
    private final EntityManager entityManager;

    /**
     * Creates a new TrustOnboardingTask based on the provided TrustOnboardingSubmission
     *
     * @param trustOnboardingSubmission the submission based on which the task should be created
     * @param currentUserName           the name of the user triggering the creation, used for event publishing
     * @return the id of the created task
     */
    @Transactional
    public UUID createTaskByTrustOnboardingSubmission(
        TrustOnboardingSubmissionDto trustOnboardingSubmission,
        String currentUserName
    ) {
        var task = new TrustOnboardingTask(
            trustOnboardingSubmission.getPartnerId(),
            Map.copyOf(trustOnboardingSubmission.getName()),
            trustOnboardingSubmission.getId(),
            calculateDueAt(trustOnboardingSubmission),
            trustOnboardingSubmission.getSubmittedAt()
        );
        task = this.trustOnboardingTaskRepository.save(task);
        domainEventService.trustOnboardingSubmissionReceived(task.getId(), currentUserName);
        return task.getId();
    }

    @Transactional(readOnly = true)
    public TrustOnboardingTaskDto getTask(UUID taskId) {
        var task = taskDomainService.getTrustOnboardingTask(taskId);
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

        return toTrustOnboardingTaskDto(
            resolvePossibleActions(task.getStatus(), task.getAssignee(), getCurrentUserFullName()),
            task,
            submission
        );
    }

    @Transactional(readOnly = true)
    public Page<TrustOnboardingTaskListItemDto> getTasks(
        Pageable pageable,
        LocalDate submissionStartDate,
        LocalDate submissionEndDate,
        LocalDate dueStartDate,
        LocalDate dueEndDate,
        List<String> states,
        String assignee
    ) {
        QTrustTask q = QTrustTask.trustTask;

        BooleanBuilder where = new BooleanBuilder();
        if (submissionStartDate != null) {
            where.and(q.submittedAt.goe(submissionStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }
        if (submissionEndDate != null) {
            where.and(
                q.submittedAt.loe(
                    submissionEndDate
                        .atTime(LocalTime.MAX) // 23:59:59.999999999
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                )
            );
        }
        if (dueStartDate != null) {
            where.and(q.dueAt.goe(Instant.from(dueStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant())));
        }
        if (dueEndDate != null) {
            where.and(
                q.dueAt.loe(
                    dueEndDate
                        .atTime(LocalTime.MAX) // 23:59:59.999999999
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                )
            );
        }
        if (states != null && !states.isEmpty()) {
            List<TrustTaskStatus> statusEnums = states.stream().map(TrustTaskStatus::valueOf).toList();
            where.and(q.status.in(statusEnums));
        }
        if (assignee != null && !assignee.isBlank()) {
            where.and(q.assignee.equalsIgnoreCase(assignee));
        }
        return this.trustTaskRepository.findAll(where, pageable).map(task ->
            TrustOnboardingTaskMapper.toTaskListItemDto(
                task,
                resolvePossibleActions(task.getStatus(), task.getAssignee(), getCurrentUserFullName())
            )
        );
    }

    @Transactional
    public void approve(UUID taskId, ApproveTaskActionDto request, String triggeredBy) {
        log.info("Task {} is approved by {}", taskId.toString(), triggeredBy);
        var task = taskDomainService.getTrustOnboardingTask(taskId);
        var submissionId = task.getTrustOnboardingSubmissionId();
        task.changeStatus(ACCEPTED);
        taskRepository.save(task);

        var trustOnboardingSubmissionDto = trustOnboardingSubmissionApi.getTrustOnboardingSubmission(submissionId);
        issueAndPublishIdentityTrustStatements(trustOnboardingSubmissionDto);

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
    }

    @Transactional
    public void reject(UUID taskId, RejectTaskActionDto request, String triggeredBy) {
        log.info("Task {} is rejected by {}", taskId.toString(), triggeredBy);
        var task = taskDomainService.getTrustOnboardingTask(taskId);
        var submissionId = task.getTrustOnboardingSubmissionId();
        task.changeStatus(REJECTED);
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
    }

    @Transactional
    public void requestMoreInformation(UUID taskId, RequestMoreInformationTaskActionDto request, String triggeredBy) {
        log.info("Task {} is send back to user by {}", taskId.toString(), triggeredBy);
        var task = taskDomainService.getTrustOnboardingTask(taskId);
        var submissionId = task.getTrustOnboardingSubmissionId();
        task.changeStatus(INFORMATION_REQUESTED);
        taskRepository.save(task);
        outboxEventPublisher.publishTrustOnboardingInformationRequestedEvent(
            TiTrustOnboardingInformationRequestedEventBuilder.create()
                .trustOnboardingSubmissionId(submissionId)
                .partnerNote(request.partnerNote())
                .declineReasonType(request.declineReason().toString())
                .build()
        );
        domainEventService.trustOnboardingSubmissionMoreInformationRequested(
            task.getId(),
            triggeredBy,
            request.partnerNote(),
            request.internalNote()
        );
    }

    @Transactional
    public void addInternalNote(UUID taskId, String internalNote, String triggeredBy) {
        var task = taskDomainService.getTrustOnboardingTask(taskId);
        domainEventService.trustOnboardingSubmissionTaskNoteAdded(task.getId(), triggeredBy, internalNote);
    }

    @Transactional
    public void assign(UUID taskId, String assignee, String triggeredBy) {
        var task = taskDomainService.getTrustOnboardingTask(taskId);
        task.assignTo(assignee);
        taskRepository.save(task);
        domainEventService.trustOnboardingSubmissionAssigned(task.getId(), triggeredBy);
    }

    private void issueAndPublishIdentityTrustStatements(TrustOnboardingSubmissionDto trustOnboardingSubmissionDto) {
        var businessPartnerIdentity = BusinessPartnerIdentityMapper.toBusinessPartnerIdentity(
            trustOnboardingSubmissionDto
        );

        // map onboardingSubmissionDto to trustStatementRequestV1
        var trustStatementPartnerLinkRequestList = toTrustStatementPartnerLinkIdentityV1RequestDtoList(
            trustOnboardingSubmissionDto,
            businessPartnerIdentity
        );
        trustStatementPartnerLinkRequestList.forEach(r -> {
            // issue trust statements of TP1.0 for each did
            var newStatement = trustStatementService.issueAndPublishIdentityV1TrustStatement(
                trustOnboardingSubmissionDto.getPartnerId(),
                r
            );
            // deactivate old trust statements
            trustStatementService.deactivateAllStatementsOfTypeAndSubjectExcept(
                TrustStatementTypeDto.IDENTITY_V1,
                "Renewal through TrustOnboarding Submission",
                newStatement.getId(),
                newStatement.getSubject()
            );
        });

        // map onboardingSubmissionDto to trustStatementRequestV2
        var trustStatementV2PartnerLinkRequestList = toTrustStatementPartnerLinkIdentityV2RequestDtoList(
            trustOnboardingSubmissionDto,
            businessPartnerIdentity
        );

        trustStatementV2PartnerLinkRequestList.forEach(r -> {
            // issue trust statements of TP2.0 for each did
            var newStatement = trustStatementService.issueAndPublishIdentityV2TrustStatement(r);
            // deactivate old trust statements
            trustStatementService.deactivateAllStatementsOfTypeAndSubjectExcept(
                TrustStatementTypeDto.IDENTITY_V2,
                "Renewal through TrustOnboarding Submission",
                newStatement.getId(),
                newStatement.getSubject()
            );
        });
    }

    private Instant calculateDueAt(TrustOnboardingSubmissionDto trustOnboardingSubmission) {
        var referenceInstant = Optional.ofNullable(trustOnboardingSubmission.getSubmittedAt()).orElseThrow();
        return referenceInstant.plus(30, ChronoUnit.DAYS);
    }
}
