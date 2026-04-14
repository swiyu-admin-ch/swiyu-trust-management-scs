package ch.admin.bj.swiyu.trust.management.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.admin.bit.jeap.starter.db.config.FlywayMigrationConfiguration;
import ch.admin.bj.swiyu.trust.client.issuer.management.api.CredentialApi;
import ch.admin.bj.swiyu.trust.client.issuer.management.model.CredentialStatusTypeDto;
import ch.admin.bj.swiyu.trust.client.issuer.oid4vci.api.IssuerOid4VciApi;
import ch.admin.bj.swiyu.trust.management.modules.common.registry.RegistryProperties;
import ch.admin.bj.swiyu.trust.management.modules.common.registry.VcSchemaUrlValidator;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer.DefaultIssuerClient;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer.IssuerClient;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer.IssuerProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.registry.TrustRegistryClient;
import ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.persistence.ManagementPersistenceConfig;
import ch.admin.bj.swiyu.trust.management.modules.registry.infrastructure.persistence.RegistryPersistenceConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

/**
 * Holds all necessary configurations for sliced @DataJpaTest integration tests which are common to all.
 */
@EnableJpaAuditing
@AutoConfigureJson
@Import(
    {
        FlywayMigrationConfiguration.class,
        RegistryPersistenceConfig.class,
        ManagementPersistenceConfig.class,
        VcSchemaUrlValidator.class,
    }
)
@EnableConfigurationProperties({ RegistryProperties.class, IssuerProperties.class })
public class DataJpaTestConfiguration {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of("TestUser");
    }

    @Bean
    public IssuerClient issuerClient(
        CredentialApi issuerManagementApi,
        IssuerOid4VciApi issuerOid4vciApi,
        ObjectMapper objectMapper,
        IssuerProperties issuerProperties
    ) {
        return new DefaultIssuerClient(issuerManagementApi, issuerOid4vciApi, objectMapper, issuerProperties);
    }

    @Bean
    public CredentialApi issuerManagementApi() {
        var mock = mock(CredentialApi.class);
        when(mock.createCredential(any())).thenReturn(IssuerTestData.credentialWithDeeplinkResponse());
        when(mock.getCredentialStatus(any())).thenReturn(IssuerTestData.statusResponse_Issued());
        when(mock.updateCredentialStatus(any(), any())).thenAnswer(invocation ->
            IssuerTestData.updateStatusResponse(
                invocation.getArgument(0, UUID.class),
                invocation.getArgument(1, CredentialStatusTypeDto.class)
            )
        );
        return mock;
    }

    @Bean
    public IssuerOid4VciApi issuerOid4vciApi() {
        var mock = mock(IssuerOid4VciApi.class);
        when(mock.oauthAccessToken(any(), any())).thenReturn(IssuerTestData.oAuthToken());
        when(mock.createCredential(any(), any())).thenReturn(IssuerTestData.credentialResponseAsString());
        return mock;
    }

    @Bean
    public TrustRegistryClient trustRegistryClient(VcSchemaUrlValidator vcSchemaUrlValidator) {
        return new TrustRegistryClient(mock(RestClient.class), vcSchemaUrlValidator);
    }

    @Bean
    public TransactionTemplate transactionTemplate(
        @Qualifier("managementTransactionManager") PlatformTransactionManager transactionManager
    ) {
        return new TransactionTemplate(transactionManager);
    }
}
