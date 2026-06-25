package ch.admin.bj.swiyu.trust.management.modules.common.audit;

import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditorProvider.getCurrentAuditor;

import ch.admin.bj.swiyu.trust.management.modules.common.security.*;
import java.util.*;
import lombok.extern.slf4j.*;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.config.*;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableJpaAuditing
@Slf4j
public class AuditConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            var auditor = getCurrentAuditor(SecurityContextHolder.getContext().getAuthentication());
            if (auditor.isSystem()) {
                log.trace("Data is being mutated in system context.");
            } else if (auditor.isAnonymous()) {
                log.error("Data is being mutated by an anonymous user. This should not happen.");
            }
            return Optional.of(auditor.auditUserId());
        };
    }
}
