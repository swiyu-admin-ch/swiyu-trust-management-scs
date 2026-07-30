package ch.admin.bj.swiyu.trust.management.modules.ui.infrastructure.web.controller;

import static ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRole.Expressions.HAS_ROLE_EDITOR;

import ch.admin.bj.swiyu.trust.management.modules.management.service.BusinessPartnerIdentityService;
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
}
