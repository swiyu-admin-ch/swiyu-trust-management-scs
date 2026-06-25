package ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import ch.admin.bj.swiyu.didresolveradapter.DidResolverAdapter;
import ch.admin.bj.swiyu.trust.management.modules.management.config.issuer.IssuerJwtConfig;
import ch.admin.bj.swiyu.trust.management.modules.management.config.issuer.IssuerJwtProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.config.issuer.SignerContext;
import ch.admin.bj.swiyu.trust.management.modules.management.config.statements.DefaultStatementProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.JwtStatementDomainService;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.PublicTransparencyIssuerHealthConfig;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustIssuerHealthConfig;
import ch.admin.bj.swiyu.trust.management.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.DidServiceTestConfig;
import ch.admin.bj.swiyu.trust.management.test.IssuerJwtTestConfig;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles({ "test", "test-software-key" })
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@DataJpaTest
@Import(
    {
        JwtStatementDomainService.class,
        IssuerJwtTestConfig.class,
        IssuerJwtConfig.class,
        DataJpaTestConfiguration.class,
        PublicTransparencyIssuerHealthConfig.class,
        TrustIssuerHealthConfig.class,
        DidServiceTestConfig.class,
    }
)
@EnableAutoConfiguration
@EnableConfigurationProperties({ IssuerJwtProperties.class, DefaultStatementProperties.class })
class IssuerHealthIT {

    @MockitoBean
    DidResolverAdapter didResolverAdapter;

    @Autowired
    SignerContext trustIssuer;

    @Autowired
    SignerContext publicTransparencyIssuer;

    @Autowired
    PublicTransparencyIssuerHealthConfig publicTransparencyIssuerHealthConfig;

    @Autowired
    TrustIssuerHealthConfig trustIssuerHealthConfig;

    @Value("${app.issuer.jwt.trust-issuer.software.public-key}")
    String trustIssuerPublicKey;

    @Value("${app.issuer.jwt.public-transparency-issuer.software.public-key}")
    String publicTransparencyIssuerPublicKey;

    @Test
    void test_softwareKeysGetsLoaded() {
        assertThat(trustIssuer).isNotNull();
        assertThat(publicTransparencyIssuer).isNotNull();
    }

    @Test
    void test_SoftwareKey_success() throws JOSEException {
        // WHEN
        when(
            didResolverAdapter.resolveKey(
                eq("did:tdw:QmbBoyVLWetfXMKwsrtZcejKVKhMY5nVy138R7F9bQwxtw:localhost#key-01"),
                any()
            )
        ).thenReturn(ECKey.parseFromPEMEncodedObjects(trustIssuerPublicKey));
        when(
            didResolverAdapter.resolveKey(
                eq("did:tdw:QmUQ8AZp7hYrm5XEA6XPUYT6Nr3jvRnbthbE83PLxuT7u7:localhost#key-01"),
                any()
            )
        ).thenReturn(ECKey.parseFromPEMEncodedObjects(publicTransparencyIssuerPublicKey));

        // THEN
        assertThat(publicTransparencyIssuerHealthConfig.checkPublicTransparencyKeyValid()).isEqualTo(
            PublicTransparencyIssuerHealthConfig.KeyState.OK
        );
        assertThat(trustIssuerHealthConfig.checkTrustIssuerKeyValid()).isEqualTo(TrustIssuerHealthConfig.KeyState.OK);
    }

    @Test
    void test_SoftwareKey_failsOnWrongKey() throws JOSEException {
        // WHEN
        when(
            didResolverAdapter.resolveKey(
                eq("did:tdw:QmbBoyVLWetfXMKwsrtZcejKVKhMY5nVy138R7F9bQwxtw:localhost#key-01"),
                any()
            )
        ).thenReturn(ECKey.parseFromPEMEncodedObjects(publicTransparencyIssuerPublicKey));
        when(
            didResolverAdapter.resolveKey(
                eq("did:tdw:QmUQ8AZp7hYrm5XEA6XPUYT6Nr3jvRnbthbE83PLxuT7u7:localhost#key-01"),
                any()
            )
        ).thenReturn(ECKey.parseFromPEMEncodedObjects(trustIssuerPublicKey));

        // THEN
        assertThat(publicTransparencyIssuerHealthConfig.checkPublicTransparencyKeyValid()).isEqualTo(
            PublicTransparencyIssuerHealthConfig.KeyState.ERROR_KEY_MISMATCH
        );
        assertThat(trustIssuerHealthConfig.checkTrustIssuerKeyValid()).isEqualTo(
            TrustIssuerHealthConfig.KeyState.ERROR_KEY_MISMATCH
        );
    }
}
