package ch.admin.bj.swiyu.trust.management.modules.management.service;

import ch.admin.bj.swiyu.messagetype.ti.RejectReason;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ResourceNotFoundException;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustAddDidTask;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustAddDidTaskRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustTaskStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.event.TiTrustAddDidSubmissionAcceptedEventBuilder;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.event.TiTrustAddDidSubmissionRejectedEventBuilder;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.OutboxEventPublisher;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.TrustAddDidTaskDto;
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
            TrustOnboardingTaskMapper.toTrustOnboardingTaskStatusDto(task.getStatus()),
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
    public void accept(UUID taskId) {
        var task = trustAddDidTaskRepository
            .findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task with id " + taskId + " not found"));
        task.changeStatus(TrustTaskStatus.ACCEPTED);
        trustAddDidTaskRepository.save(task);
        outboxEventPublisher.publishTrustAddDidSubmissionAcceptedEvent(
            TiTrustAddDidSubmissionAcceptedEventBuilder.create()
                .trustAddDidSubmissionId(task.getTrustAddDidSubmissionId())
                .build()
        );
        domainEventService.trustAddDidSubmissionSucceeded(task.getId(), "System User");
    }

    @Transactional
    public void reject(UUID taskId, RejectReason reason) {
        var task = trustAddDidTaskRepository
            .findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task with id " + taskId + " not found"));
        task.changeStatus(TrustTaskStatus.REJECTED);
        trustAddDidTaskRepository.save(task);
        outboxEventPublisher.publishTrustAddDidSubmissionRejectedEvent(
            TiTrustAddDidSubmissionRejectedEventBuilder.create()
                .trustAddDidSubmissionId(task.getTrustAddDidSubmissionId())
                .rejectReason(reason)
                .build()
        );
        domainEventService.trustAddDidSubmissionRejected(task.getId(), "System User", reason.name());
    }
}
