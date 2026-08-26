package ch.admin.bj.swiyu.trust.management.modules.ui.infrastructure.web.controller;

import static ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRole.Expressions.HAS_ROLE_EDITOR;
import static ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRole.Expressions.HAS_ROLE_EDITOR_OR_READER;
import static ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport.getCurrentUserFullName;

import ch.admin.bj.swiyu.trust.management.modules.management.api.ProtectedVerificationRequestTaskDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustAddDidTaskDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustOnboardingTaskDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustOnboardingTaskListItemDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.ZasDataDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.taskaction.AddInternalNoteTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.taskaction.ApproveProtectedVerificationRequestTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.taskaction.ApproveTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.taskaction.RejectProtectedVerificationRequestTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.taskaction.RejectTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.taskaction.RequestMoreInformationTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.service.ProtectedVerificationRequestTaskService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TrustAddDidTaskService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TrustOnboardingTaskService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.SortDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@AllArgsConstructor
@Tag(name = "Task")
@RestController
@RequestMapping("/ui-api/tasks")
@PreAuthorize("isAuthenticated()")
public class TaskController {

    private final TrustOnboardingTaskService trustOnboardingTaskService;
    private final TrustAddDidTaskService trustAddDidTaskService;
    private final ProtectedVerificationRequestTaskService protectedVerificationRequestTaskService;

    @GetMapping("/")
    @PreAuthorize(HAS_ROLE_EDITOR_OR_READER)
    @PageableAsQueryParam
    public PagedModel<TrustOnboardingTaskListItemDto> getTasks(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate submissionStartDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate submissionEndDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueStartDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueEndDate,
        @RequestParam(required = false) List<String> state,
        @RequestParam(required = false) String assignee,
        @RequestParam(required = false) List<String> taskType,
        @SortDefault(sort = "submittedAt", direction = Sort.Direction.DESC) @Parameter(
            hidden = true
        ) final Pageable pageable
    ) {
        return new PagedModel<>(
            trustOnboardingTaskService.getTasks(
                pageable,
                submissionStartDate,
                submissionEndDate,
                dueStartDate,
                dueEndDate,
                state,
                assignee,
                taskType
            )
        );
    }

    @GetMapping("/{taskId}")
    @PreAuthorize(HAS_ROLE_EDITOR_OR_READER)
    public TrustOnboardingTaskDto getTask(@PathVariable UUID taskId) {
        return this.trustOnboardingTaskService.getTask(taskId);
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

    @PostMapping("/{taskId}/approve")
    @PreAuthorize(HAS_ROLE_EDITOR)
    public void approve(@PathVariable UUID taskId, @NotNull ApproveTaskActionDto request) {
        this.trustOnboardingTaskService.approve(taskId, request, getCurrentUserFullName());
    }

    @PostMapping("/{taskId}/reject")
    @PreAuthorize(HAS_ROLE_EDITOR)
    public void reject(@PathVariable UUID taskId, @NotNull RejectTaskActionDto request) {
        this.trustOnboardingTaskService.reject(taskId, request, getCurrentUserFullName());
    }

    @PostMapping("/{taskId}/request-more-information")
    @PreAuthorize(HAS_ROLE_EDITOR)
    public void requestMoreInformation(
        @PathVariable UUID taskId,
        @NotNull RequestMoreInformationTaskActionDto request
    ) {
        this.trustOnboardingTaskService.requestMoreInformation(taskId, request, getCurrentUserFullName());
    }

    @PostMapping("/{taskId}/add-internal-note")
    @PreAuthorize(HAS_ROLE_EDITOR)
    public void addInternalNote(@PathVariable @NotNull UUID taskId, @NotNull AddInternalNoteTaskActionDto request) {
        this.trustOnboardingTaskService.addInternalNote(taskId, request.internalNote(), getCurrentUserFullName());
    }

    @PostMapping("/{taskId}/assign/self")
    @PreAuthorize(HAS_ROLE_EDITOR)
    public void assignSelf(@PathVariable @NotNull UUID taskId) {
        var user = getCurrentUserFullName();
        this.trustOnboardingTaskService.assign(taskId, user, user);
    }
}
