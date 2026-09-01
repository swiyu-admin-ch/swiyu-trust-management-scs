package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.management.service.TaskActionsResolver.resolvePossibleActions;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.TaskMapper.toTaskListItemDto;

import ch.admin.bj.swiyu.trust.management.modules.common.exception.ResourceNotFoundException;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.TaskFilterDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.TaskListItemDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.*;
import com.querydsl.core.BooleanBuilder;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final DomainEventService domainEventService;

    @Transactional(readOnly = true)
    public Page<TaskListItemDto> getTasks(Pageable pageable, TaskFilterDto filter, String currentUserFullName) {
        QTask q = QTask.task;

        BooleanBuilder where = new BooleanBuilder();
        if (filter.submissionStartDate() != null) {
            where.and(q.submittedAt.goe(filter.submissionStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }
        if (filter.submissionEndDate() != null) {
            where.and(
                q.submittedAt.loe(
                    filter
                        .submissionEndDate()
                        .atTime(LocalTime.MAX) // 23:59:59.999999999
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                )
            );
        }
        if (filter.dueStartDate() != null) {
            where.and(
                q.dueAt.goe(Instant.from(filter.dueStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant()))
            );
        }
        if (filter.dueEndDate() != null) {
            where.and(
                q.dueAt.loe(
                    filter
                        .dueEndDate()
                        .atTime(LocalTime.MAX) // 23:59:59.999999999
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                )
            );
        }
        if (filter.state() != null && !filter.state().isEmpty()) {
            List<TaskStatus> statusEnums = filter.state().stream().map(TaskStatus::valueOf).toList();
            where.and(q.status.in(statusEnums));
        }
        if (filter.assignee() != null && !filter.assignee().isBlank()) {
            where.and(q.assignee.equalsIgnoreCase(filter.assignee()));
        }
        if (filter.taskType() != null && !filter.taskType().isEmpty()) {
            List<TaskType> taskTypeEnums = filter.taskType().stream().map(TaskType::valueOf).toList();
            where.and(q.taskType.in(taskTypeEnums));
        }
        return this.taskRepository.findAll(where, pageable).map(task ->
            toTaskListItemDto(task, resolvePossibleActions(task, currentUserFullName))
        );
    }

    @Transactional
    public void assign(UUID taskId, String assignee, String triggeredBy) {
        var task = getTask(taskId);
        task.assignTo(assignee);
        taskRepository.save(task);
        domainEventService.taskAssigned(task.getId(), triggeredBy);
    }

    @Transactional
    public void addInternalNote(UUID taskId, String internalNote, String triggeredBy) {
        var task = getTask(taskId);
        domainEventService.taskNoteAdded(task.getId(), triggeredBy, internalNote);
    }

    private Task getTask(UUID taskId) {
        return taskRepository
            .findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task with id " + taskId + " not found"));
    }
}
