package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import static ch.admin.bj.swiyu.trust.management.modules.common.path.PathSupport.*;
import static ch.admin.bj.swiyu.trust.management.test.TrustStatementTestData.*;
import static org.junit.jupiter.api.Assertions.*;

import ch.admin.bj.swiyu.trust.management.modules.common.exception.*;
import ch.admin.bj.swiyu.trust.management.modules.common.registry.RegistryProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.registry.*;
import ch.admin.bj.swiyu.trust.management.test.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.autoconfigure.jdbc.*;
import org.springframework.boot.test.autoconfigure.orm.jpa.*;
import org.springframework.context.annotation.*;
import org.springframework.test.context.*;
import org.testcontainers.junit.jupiter.*;

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@DataJpaTest
@Import({ TrustStatementPartnerLinkValidator.class, DataJpaTestConfiguration.class })
@ActiveProfiles("test")
class TrustStatementPartnerLinkValidatorIT {

    @Autowired
    TrustStatementPartnerLinkRepository trustStatementPartnerLinkRepository;

    @Autowired
    RegistryProperties registryProperties;

    @Test
    void validateSubmission_IssuanceV1() {
        // GIVEN
        var validator = new TrustStatementPartnerLinkValidator(trustStatementPartnerLinkRepository);
        var canIssue1 = join(registryProperties.getVcSchemaBaseUrl(), "/test-vc-schema1");
        // same subject different canIssue
        var partnerLink = trustStatementPartnerLinkRepository.saveAndFlush(
            issuanceV1("did:known", canIssue1.toString())
        );
        partnerLink.persistReferencesAfterPublicationSucceeded(null, null, TrustStatementPartnerLinkStatus.ACTIVE);
        var canIssue2 = join(registryProperties.getVcSchemaBaseUrl(), "/test-vc-schema");
        var submission = issuanceV1("did:known", canIssue2.toString());
        // WHEN / THEN
        assertDoesNotThrow(() -> validator.validateSubmission(submission));
    }

    @Test
    @Disabled(
        "As of EID-5402 and in preparation for the upcoming VCT concept (https://confluence.bit.admin.ch/x/ZofVNQ) rework this check is deactivated."
    )
    void validateSubmission_IssuanceV1_invalid() {
        // GIVEN
        var validator = new TrustStatementPartnerLinkValidator(trustStatementPartnerLinkRepository);
        var canIssue = "https://some-random-registry.ch/api/v1/vc-schema/test-vc-schema";
        var partnerLink = issuanceV1("did:known", canIssue);
        // WHEN / THEN
        assertThrows(TrustStatementValidationFailedException.class, () -> validator.validateSubmission(partnerLink));
    }

    @Test
    void validateSubmission_VerificationV1() {
        // GIVEN
        var validator = new TrustStatementPartnerLinkValidator(trustStatementPartnerLinkRepository);
        var canVerify1 = registryProperties
            .getVcSchemaBaseUrl()
            .resolve("/api/v1/vc-schema/test-vc-schema-other")
            .normalize();
        var canVerify2 = registryProperties
            .getVcSchemaBaseUrl()
            .resolve("/api/v1/vc-schema/test-vc-schema")
            .normalize();
        var partnerLink = trustStatementPartnerLinkRepository.saveAndFlush(
            verificationV1("did:known", canVerify1.toString())
        );
        partnerLink.persistReferencesAfterPublicationSucceeded(null, null, TrustStatementPartnerLinkStatus.ACTIVE);

        var submission = verificationV1("did:known", canVerify2.toString());
        // WHEN / THEN
        assertDoesNotThrow(() -> validator.validateSubmission(partnerLink));
    }

    @Test
    @Disabled(
        "As of EID-5402 and in preparation for the upcoming VCT concept (https://confluence.bit.admin.ch/x/ZofVNQ) rework this check is deactivated."
    )
    void validateSubmission_VerificationV1_invalid() {
        // GIVEN
        var validator = new TrustStatementPartnerLinkValidator(trustStatementPartnerLinkRepository);
        var canVerify = "https://some-random-registry.ch/api/v1/vc-schema/test-vc-schema";
        var partnerLink = verificationV1("did:known", canVerify);
        // WHEN / THEN
        assertThrows(TrustStatementValidationFailedException.class, () -> validator.validateSubmission(partnerLink));
    }

    @Test
    void validateSubmission_VerificationV1_already_active_invalid() {
        // GIVEN
        var validator = new TrustStatementPartnerLinkValidator(trustStatementPartnerLinkRepository);
        var canVerify = registryProperties.getVcSchemaBaseUrl().resolve("/api/v1/vc-schema/test-vc-schema").normalize();
        var partnerLink = trustStatementPartnerLinkRepository.saveAndFlush(
            verificationV1("did:known", canVerify.toString())
        );
        partnerLink.persistReferencesAfterPublicationSucceeded(null, null, TrustStatementPartnerLinkStatus.ACTIVE);

        var submission = verificationV1("did:known", canVerify.toString());

        // WHEN / THEN
        assertThrows(TrustStatementValidationFailedException.class, () -> validator.validateSubmission(submission));
    }
}
