package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditMapper.toAuditJson;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.TaskActionsResolver.validateActionAllowed;

import ch.admin.bj.swiyu.messagetype.ti.RejectReason;
import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ResourceNotFoundException;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.TrustAddDidTaskDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.TaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.event.TiTrustAddDidSubmissionAcceptedEventBuilder;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.event.TiTrustAddDidSubmissionRejectedEventBuilder;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.OutboxEventPublisher;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TrustAddDidTask;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TrustAddDidTaskRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrustAddDidTaskService {

    private final TrustAddDidTaskRepository trustAddDidTaskRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final DomainEventService domainEventService;
    private final AuditPublisher auditPublisher;

    @Transactional(readOnly = true)
    public TrustAddDidTaskDto getTask(UUID taskId) {
        var task = trustAddDidTaskRepository
            .findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task with id %s not found".formatted(taskId)));
        return new TrustAddDidTaskDto(
            task.getId(),
            task.getAssignee(),
            task.getSubmittedAt(),
            task.getDueAt(),
            TaskMapper.toTaskStatusDto(task.getStatus()),
            task.getPartnerName(),
            task.getPermissionDid(),
            task.getTrustAddDidSubmissionId()
        );
    }

    @Transactional
    public UUID createTask(
        UUID partnerId,
        Map<String, String> partnerName,
        UUID trustAddDidSubmissionId,
        String permissionDid,
        Instant submittedAt,
        String currentUserName
    ) {
        var dueAt = submittedAt.plus(30, ChronoUnit.DAYS);
        var task = new TrustAddDidTask(
            partnerId,
            partnerName,
            trustAddDidSubmissionId,
            permissionDid,
            dueAt,
            submittedAt
        );
        task = trustAddDidTaskRepository.save(task);
        domainEventService.trustAddDidSubmissionReceived(task.getId(), currentUserName);
        return task.getId();
    }

    @Transactional
    public void approve(UUID taskId, String triggeredBy) {
        var task = trustAddDidTaskRepository
            .findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task with id " + taskId + " not found"));
        validateActionAllowed(task, triggeredBy, TaskActionDto.APPROVE);
        task.approve();
        trustAddDidTaskRepository.save(task);
        outboxEventPublisher.publishTrustAddDidSubmissionAcceptedEvent(
            TiTrustAddDidSubmissionAcceptedEventBuilder.create()
                .trustAddDidSubmissionId(task.getTrustAddDidSubmissionId())
                .build()
        );
        domainEventService.trustAddDidSubmissionSucceeded(task.getId(), triggeredBy);
        auditPublisher.taskApproved(
            task.getPartnerId(),
            task.getId(),
            task.getVersion(),
            task.getTaskType().toString(),
            toAuditJson(task)
        );
    }

    @Transactional
    public void reject(UUID taskId, RejectReason reason, String triggeredBy) {
        var task = trustAddDidTaskRepository
            .findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task with id " + taskId + " not found"));
        validateActionAllowed(task, triggeredBy, TaskActionDto.REJECT);
        task.reject();
        trustAddDidTaskRepository.save(task);
        outboxEventPublisher.publishTrustAddDidSubmissionRejectedEvent(
            TiTrustAddDidSubmissionRejectedEventBuilder.create()
                .trustAddDidSubmissionId(task.getTrustAddDidSubmissionId())
                .rejectReason(reason)
                .build()
        );
        domainEventService.trustAddDidSubmissionRejected(task.getId(), triggeredBy, reason.name());
        auditPublisher.taskRejected(
            task.getPartnerId(),
            task.getId(),
            task.getVersion(),
            task.getTaskType().toString(),
            toAuditJson(task)
        );
    }
}
