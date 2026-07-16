package ch.admin.bj.swiyu.trust.management.modules.ui.infrastructure.web.controller;

import static ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport.getCurrentUserFullName;

import ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRoles;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustAddDidTaskDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustOnboardingTaskDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustOnboardingTaskListItemDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.taskaction.AddInternalNoteTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.taskaction.ApproveTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.taskaction.RejectTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.taskaction.RequestMoreInformationTaskActionDto;
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
@Tag(name = "TrustOnboardingTask")
@RestController
@RequestMapping("/ui-api/tasks")
@PreAuthorize("isAuthenticated()")
public class TrustOnboardingTaskController {

    private final TrustOnboardingTaskService trustOnboardingTaskService;
    private final TrustAddDidTaskService trustAddDidTaskService;

    @GetMapping("/")
    @PreAuthorize("hasAnyRole('" + UserRoles.READER + "', '" + UserRoles.EDITOR + "')")
    @PageableAsQueryParam
    public PagedModel<TrustOnboardingTaskListItemDto> getTasks(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate submissionStartDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate submissionEndDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueStartDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueEndDate,
        @RequestParam(required = false) List<String> state,
        @RequestParam(required = false) String assignee,
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
                assignee
            )
        );
    }

    @GetMapping("/{taskId}")
    @PreAuthorize("hasAnyRole('" + UserRoles.READER + "', '" + UserRoles.EDITOR + "')")
    public TrustOnboardingTaskDto getTask(@PathVariable UUID taskId) {
        return this.trustOnboardingTaskService.getTask(taskId);
    }

    @GetMapping("/{taskId}/add-did")
    @PreAuthorize("hasAnyRole('" + UserRoles.READER + "', '" + UserRoles.EDITOR + "')")
    public TrustAddDidTaskDto getAddDidTask(@PathVariable UUID taskId) {
        return this.trustAddDidTaskService.getTask(taskId);
    }

    @PostMapping("/{taskId}/approve")
    @PreAuthorize("hasRole('" + UserRoles.EDITOR + "')")
    public void approve(@PathVariable UUID taskId, @NotNull ApproveTaskActionDto request) {
        this.trustOnboardingTaskService.approve(taskId, request, getCurrentUserFullName());
    }

    @PostMapping("/{taskId}/reject")
    @PreAuthorize("hasRole('" + UserRoles.EDITOR + "')")
    public void reject(@PathVariable UUID taskId, @NotNull RejectTaskActionDto request) {
        this.trustOnboardingTaskService.reject(taskId, request, getCurrentUserFullName());
    }

    @PostMapping("/{taskId}/request-more-information")
    @PreAuthorize("hasRole('" + UserRoles.EDITOR + "')")
    public void requestMoreInformation(
        @PathVariable UUID taskId,
        @NotNull RequestMoreInformationTaskActionDto request
    ) {
        this.trustOnboardingTaskService.requestMoreInformation(taskId, request, getCurrentUserFullName());
    }

    @PostMapping("/{taskId}/add-internal-note")
    @PreAuthorize("hasRole('" + UserRoles.EDITOR + "')")
    public void addInternalNote(@PathVariable @NotNull UUID taskId, @NotNull AddInternalNoteTaskActionDto request) {
        this.trustOnboardingTaskService.addInternalNote(taskId, request.internalNote(), getCurrentUserFullName());
    }

    @PostMapping("/{taskId}/assign/self")
    @PreAuthorize("hasRole('" + UserRoles.EDITOR + "')")
    public void assignSelf(@PathVariable @NotNull UUID taskId) {
        var user = getCurrentUserFullName();
        this.trustOnboardingTaskService.assign(taskId, user, user);
    }
}
