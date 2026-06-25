package ch.admin.bj.swiyu.trust.management.test;

import static org.mockito.Mockito.mock;

import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditPublisher;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class MockAuditPublisherTestConfiguration {

    @Bean
    @Primary
    public AuditPublisher auditPublisher() {
        return mock(AuditPublisher.class);
    }
}
