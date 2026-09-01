package ch.admin.bj.swiyu.trust.management.modules.ui.infrastructure.web.controller;

import static ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRole.Expressions.HAS_ROLE_EDITOR;
import static ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRole.Expressions.HAS_ROLE_EDITOR_OR_READER;
import static ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport.getCurrentUserFullName;

import ch.admin.bj.swiyu.trust.management.modules.management.api.ZasDataDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.*;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.AddInternalNoteTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.ApproveProtectedVerificationRequestTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.ApproveTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.RejectProtectedVerificationRequestTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.RejectTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.RequestMoreInformationTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.service.ProtectedVerificationRequestTaskService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TaskService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TrustAddDidTaskService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TrustOnboardingTaskService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.SortDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@AllArgsConstructor
@Tag(name = "Task")
@RestController
@RequestMapping("/ui-api/tasks")
@PreAuthorize("isAuthenticated()")
public class TaskController {

    private final TaskService taskService;
    private final TrustOnboardingTaskService trustOnboardingTaskService;
    private final TrustAddDidTaskService trustAddDidTaskService;
    private final ProtectedVerificationRequestTaskService protectedVerificationRequestTaskService;

    @GetMapping("/")
    @PreAuthorize(HAS_ROLE_EDITOR_OR_READER)
    @PageableAsQueryParam
    public PagedModel<TaskListItemDto> getTasks(
        @ParameterObject TaskFilterDto filter,
        @SortDefault(sort = "submittedAt", direction = Sort.Direction.DESC) @Parameter(
            hidden = true
        ) final Pageable pageable
    ) {
        return new PagedModel<>(taskService.getTasks(pageable, filter, getCurrentUserFullName()));
    }

    @GetMapping("/{taskId}/trust-onboarding")
    @PreAuthorize(HAS_ROLE_EDITOR_OR_READER)
    public TrustOnboardingTaskDto getTrustOnboardingTask(@PathVariable UUID taskId) {
        return this.trustOnboardingTaskService.getTask(taskId, getCurrentUserFullName());
    }

    @GetMapping("/{taskId}/add-did")
    @PreAuthorize(HAS_ROLE_EDITOR_OR_READER)
    public TrustAddDidTaskDto getAddDidTask(@PathVariable UUID taskId) {
        return this.trustAddDidTaskService.getTask(taskId);
    }

    @GetMapping("/{taskId}/protected-verification-request")
    @PreAuthorize(HAS_ROLE_EDITOR_OR_READER)
    public ProtectedVerificationRequestTaskDto getProtectedVerificationRequestTask(@PathVariable UUID taskId) {
        return this.protectedVerificationRequestTaskService.getTask(taskId, getCurrentUserFullName());
    }

    @GetMapping("/{taskId}/protected-verification-request/zas-data")
    @PreAuthorize(HAS_ROLE_EDITOR_OR_READER)
    public ZasDataDto getProtectedVerificationRequestTaskZasData(@PathVariable UUID taskId) {
        return this.protectedVerificationRequestTaskService.getZasData(taskId);
    }

    @PostMapping("/{taskId}/protected-verification-request/zas-data/review")
    @PreAuthorize(HAS_ROLE_EDITOR_OR_READER)
    public void markProtectedVerificationRequestTaskZasDataReviewed(@PathVariable UUID taskId) {
        this.protectedVerificationRequestTaskService.markZasDataReviewed(taskId);
    }

    @PostMapping("/{taskId}/protected-verification-request/approve")
    @PreAuthorize(HAS_ROLE_EDITOR)
    public void approveProtectedVerificationRequest(
        @PathVariable UUID taskId,
        @NotNull ApproveProtectedVerificationRequestTaskActionDto request
    ) {
        this.protectedVerificationRequestTaskService.approve(taskId, request, getCurrentUserFullName());
    }

    @PostMapping("/{taskId}/protected-verification-request/reject")
    @PreAuthorize(HAS_ROLE_EDITOR)
    public void rejectProtectedVerificationRequest(
        @PathVariable UUID taskId,
        @NotNull RejectProtectedVerificationRequestTaskActionDto request
    ) {
        this.protectedVerificationRequestTaskService.reject(taskId, request, getCurrentUserFullName());
    }

    @PostMapping("/{taskId}/trust-onboarding/approve")
    @PreAuthorize(HAS_ROLE_EDITOR)
    public void approveTrustOnboarding(@PathVariable UUID taskId, @NotNull ApproveTaskActionDto request) {
        this.trustOnboardingTaskService.approve(taskId, request, getCurrentUserFullName());
    }

    @PostMapping("/{taskId}/trust-onboarding/reject")
    @PreAuthorize(HAS_ROLE_EDITOR)
    public void rejectTrustOnboarding(@PathVariable UUID taskId, @NotNull RejectTaskActionDto request) {
        this.trustOnboardingTaskService.reject(taskId, request, getCurrentUserFullName());
    }

    @PostMapping("/{taskId}/trust-onboarding/request-more-information")
    @PreAuthorize(HAS_ROLE_EDITOR)
    public void requestMoreInformationForTrustOnboarding(
        @PathVariable UUID taskId,
        @NotNull RequestMoreInformationTaskActionDto request
    ) {
        this.trustOnboardingTaskService.requestMoreInformation(taskId, request, getCurrentUserFullName());
    }

    @PostMapping("/{taskId}/add-internal-note")
    @PreAuthorize(HAS_ROLE_EDITOR)
    public void addInternalNote(@PathVariable @NotNull UUID taskId, @NotNull AddInternalNoteTaskActionDto request) {
        this.taskService.addInternalNote(taskId, request.internalNote(), getCurrentUserFullName());
    }

    @PostMapping("/{taskId}/assign/self")
    @PreAuthorize(HAS_ROLE_EDITOR)
    public void assignSelf(@PathVariable @NotNull UUID taskId) {
        var user = getCurrentUserFullName();
        this.taskService.assign(taskId, user, user);
    }
}
