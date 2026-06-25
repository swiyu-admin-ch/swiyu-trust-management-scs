package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.map;

import ch.admin.bj.swiyu.trust.management.modules.management.config.issuer.IssuerJwtConfig;
import ch.admin.bj.swiyu.trust.management.modules.management.config.issuer.IssuerJwtProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.config.statements.DefaultStatementProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness.IssuerTrustRootProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.ProtectedVerificationAuthorizationV2Details;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.JsonJwtDeserializer;
import ch.admin.bj.swiyu.trust.management.test.*;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@Import(
    {
        IssuerJwtConfig.class,
        JwtStatementDomainService.class,
        JsonJwtDeserializer.class,
        DataJpaTestConfiguration.class,
    }
)
@ActiveProfiles("test")
@EnableConfigurationProperties(
    { IssuerJwtProperties.class, DefaultStatementProperties.class, IssuerTrustRootProperties.class }
)
class JwtStatementDomainServiceIT {

    @Autowired
    JwtStatementDomainService jwtStatementDomainService;

    @Test
    void pvaTS_structure() throws ParseException {
        // GIVEN
        var statusListMetadataId = UUID.randomUUID();
        var partnerLink = TrustStatementPartnerLink.createProtectedVerificationAuthorizationV2(
            UUID.randomUUID(),
            "did:example",
            Instant.now().minusSeconds(1),
            Instant.now().plusSeconds(1),
            List.of(ProtectedVerificationAuthorizationV2Details.AuthorizableField.AHV_NUMBER),
            new StatusListEntry(statusListMetadataId, 0)
        );
        // WHEN
        var serializedStatement = jwtStatementDomainService.generateProtectedVerificationAuthorizationTrustStatement(
            partnerLink,
            new StatusListMetadata(statusListMetadataId, "https://statusListMetadata", 1)
        );

        // THEN
        var statement = SignedJWT.parse(serializedStatement);
        var claims = statement.getJWTClaimsSet();
        assertThat(claims.getClaim("authorized_fields"))
            .isNotNull()
            .asInstanceOf(list(String.class))
            .containsExactly("personal_administrative_number");
        assertThat(claims.getClaim("sub")).isNotNull().asString().isEqualTo("did:example");
        assertThat(claims.getClaim("status"))
            .isNotNull()
            .asInstanceOf(map(String.class, Object.class))
            .extractingByKey("status_list", map(String.class, Object.class))
            .containsEntry("idx", 0L)
            .containsEntry("uri", "https://statusListMetadata");
    }
}
