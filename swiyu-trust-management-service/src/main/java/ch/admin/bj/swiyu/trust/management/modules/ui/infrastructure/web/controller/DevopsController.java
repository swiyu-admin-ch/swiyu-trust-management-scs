package ch.admin.bj.swiyu.trust.management.modules.ui.infrastructure.web.controller;

import static ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRole.Expressions.HAS_ROLE_EDITOR;
import static ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport.getCurrentUserFullName;

import ch.admin.bj.swiyu.trust.management.modules.management.service.BusinessPartnerIdentityService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.DeleteBusinessPartnerTaskService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@AllArgsConstructor
@Tag(name = "Devops")
@RestController
@RequestMapping("api/v2/devops")
@PreAuthorize("isAuthenticated()")
public class DevopsController {

    private final BusinessPartnerIdentityService businessPartnerIdentityService;
    private final DeleteBusinessPartnerTaskService deleteBusinessPartnerTaskService;

    @PostMapping("sync")
    @PreAuthorize(HAS_ROLE_EDITOR)
    public void syncAll() {
        businessPartnerIdentityService.syncAll();
    }

    @PostMapping("sync/{businessPartnerIdentityId}")
    @PreAuthorize(HAS_ROLE_EDITOR)
    public void sync(@PathVariable UUID businessPartnerIdentityId) {
        businessPartnerIdentityService.sync(businessPartnerIdentityId);
    }

    /**
     * Approves the pending deletion request of the partner without waiting for its {@code hardDeleteDueAt}. Under
     * normal circumstances the {@code HardDeleteBusinessPartnerJob} approves it once the configured delay has passed;
     * this endpoint exists for end-to-end tests and to unblock a deletion without waiting days.
     *
     * <p>Only the waiting period is skipped: a partner the core business service does not allow to be deleted is still
     * refused ({@code 400 PARTNER_NOT_CLEARED_FOR_DELETION}). An already approved or rejected request answers
     * {@code 409 Conflict}.
     */
    @PostMapping("business-partners/{partnerId}/approve-deletion")
    @PreAuthorize(HAS_ROLE_EDITOR)
    public void approveDeletion(@PathVariable UUID partnerId) {
        log.info("Deletion of business partner {} is approved by {}", partnerId, getCurrentUserFullName());
        deleteBusinessPartnerTaskService.approveWithoutDelay(partnerId, getCurrentUserFullName());
    }
}
