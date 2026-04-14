package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport.*;
import static ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustTaskStatus.*;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.TrustOnboardingTaskActionsResolver.*;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.TrustOnboardingTaskMapper.*;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.TrustStatementMapper.*;

import ch.admin.bj.swiyu.trust.client.core.business.api.*;
import ch.admin.bj.swiyu.trust.client.core.business.model.*;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.event.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.*;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.*;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.taskaction.*;
import com.querydsl.core.*;
import java.time.*;
import java.time.temporal.*;
import java.util.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrustOnboardingTaskService {

    private final TrustOnboardingTaskRepository trustOnboardingTaskRepository;
    private final TrustOnboardingSubmissionApi trustOnboardingSubmissionApi;
    private final OutboxEventPublisher outboxEventPublisher;
    private final DomainEventService domainEventService;
    private final TrustOnboardingTaskRepository taskRepository;
    private final TrustTaskRepository trustTaskRepository;
    private final TrustStatementService trustStatementService;
    private final TrustOnboardingTaskDomainService taskDomainService;

    /**
     * Creates a new TrustOnboardingTask based on the provided TrustOnboardingSubmission
     * @param trustOnboardingSubmission the submission based on which the task should be created
     * @param currentUserName the name of the user triggering the creation, used for event publishing
     * @return the id of the created task
     */
    @Transactional
    public UUID createTaskByTrustOnboardingSubmission(
        TrustOnboardingSubmissionDto trustOnboardingSubmission,
        String currentUserName
    ) {
        var task = new TrustOnboardingTask(
            trustOnboardingSubmission.getPartnerId(),
            new PartnerName(
                trustOnboardingSubmission.getEntityName().getDe(),
                trustOnboardingSubmission.getEntityName().getFr(),
                trustOnboardingSubmission.getEntityName().getIt(),
                trustOnboardingSubmission.getEntityName().getEn(),
                trustOnboardingSubmission.getEntityName().getRm()
            ),
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
        var task = taskDomainService.getTrustOnboardingTask(taskId);
        var submissionId = task.getTrustOnboardingSubmissionId();
        task.changeStatus(ACCEPTED);
        taskRepository.save(task);

        var trustOnboardingSubmissionDto = trustOnboardingSubmissionApi.getTrustOnboardingSubmission(submissionId);
        // map onboardingSubmissionDto to trustStatementRequest
        var trustStatementPartnerLinkRequestList = toTrustStatementPartnerLinkIdentityV1RequestDtoList(
            trustOnboardingSubmissionDto,
            Instant.now(),
            Instant.now().plus(Duration.ofDays(365))
        );
        // issue truststatements for each did
        trustStatementPartnerLinkRequestList.forEach(r ->
            trustStatementService.issueAndPublishIdentityTrustStatement(task.getPartnerId(), r)
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
    }

    @Transactional
    public void reject(UUID taskId, RejectTaskActionDto request, String triggeredBy) {
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

    private Instant calculateDueAt(TrustOnboardingSubmissionDto trustOnboardingSubmission) {
        var referenceInstant = Optional.ofNullable(trustOnboardingSubmission.getSubmittedAt()).orElseThrow();
        return referenceInstant.plus(30, ChronoUnit.DAYS);
    }
}
