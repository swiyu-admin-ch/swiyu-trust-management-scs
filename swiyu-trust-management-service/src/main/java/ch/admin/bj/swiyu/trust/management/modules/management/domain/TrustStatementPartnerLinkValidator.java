package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import ch.admin.bj.swiyu.trust.management.modules.common.exception.*;
import ch.admin.bj.swiyu.trust.management.modules.common.registry.RegistryProperties;
import ch.admin.bj.swiyu.trust.management.modules.common.registry.VcSchemaUrlValidator;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.registry.*;
import java.util.*;
import lombok.*;
import org.springframework.stereotype.*;
import org.springframework.validation.*;

@Component
@RequiredArgsConstructor
public class TrustStatementPartnerLinkValidator {

    private final TrustStatementPartnerLinkRepository trustStatementPartnerLinkRepository;
    private final TrustRegistryClient trustRegistryClient;
    private final VcSchemaUrlValidator vcSchemaUrlValidator;
    private final RegistryProperties registryProperties;

    private static void validateTimeRangeIsInOrder(SimpleErrors issues, TrustStatementPartnerLink statement) {
        if (statement.getValidUntil().compareTo(statement.getValidFrom()) < 0) issues.reject(
            "trust-statement.create.errors.timeframe-invalid",
            List.of(statement.getValidFrom(), statement.getValidUntil()).toArray(),
            "Field validUntil needs to be greater than validFrom."
        );
    }

    /**
     * Validates the generic trust statement validations.
     * <p>
     * Validations performed:
     * - validUntil < validFrom
     * - That exactly one trust statement type can be valid for a given subject at a given time.
     */
    @SuppressWarnings("java:S1301") // Suppress warning about missing default in switch
    public void validateSubmission(TrustStatementPartnerLink partnerLink) {
        var issues = new SimpleErrors(partnerLink);
        validateTimeRangeIsInOrder(issues, partnerLink);
        if (
            partnerLink.getDetails() instanceof MetadataV1Details ||
            partnerLink.getDetails() instanceof IdentityV1Details
        ) {
            validateNoOtherSubmissionWithSameSubjectIsActive(issues, partnerLink);
        }
        if (
            partnerLink.getDetails() instanceof IssuanceV1Details ||
            partnerLink.getDetails() instanceof VerificationV1Details
        ) {
            validateNoOtherSubmissionWithSameSubjectAndCanIssueOrCanVerifyIsActive(issues, partnerLink);
        }
        switch (partnerLink.getDetails()) {
            case MetadataV1Details details -> validateMetadataV1(issues, details);
            case IdentityV1Details details -> validateIdentityV1(issues, details);
            case IssuanceV1Details details -> validateIssuanceV1(issues, details);
            case VerificationV1Details details -> validateVerificationV1(issues, details);
            default -> throw new IllegalArgumentException(
                // This should never happen due to sealed class
                "Unsupported trust statement details type: " + partnerLink.getDetails().getClass().getName()
            );
        }
        if (issues.hasErrors()) {
            throw new TrustStatementValidationFailedException(issues);
        }
    }

    private void validateMetadataV1(SimpleErrors issues, MetadataV1Details data) {
        var preferredLanguage = data.getPreferredLanguage();

        var hasAnyLogoUri = data.hasAnyLogoUri();
        var hasLogoUriInPreferredLanguage = data.hasLogoUri(preferredLanguage);
        var hasOrgNameInPreferredLanguage = data.hasOrgName(preferredLanguage);

        if (hasAnyLogoUri && !hasLogoUriInPreferredLanguage) {
            issues.reject(
                "missing.logoUri.for.preferredLanguage",
                "The field orgName must contain a value for the preferred language"
            );
        }
        if (!hasOrgNameInPreferredLanguage) {
            issues.reject(
                "missing.orgName.for.preferredLanguage",
                "The field orgName must contain a value for the preferred language"
            );
        }
    }

    private void validateIdentityV1(SimpleErrors issues, IdentityV1Details data) {
        if (!data.hasEntityNameInAnyLanguage()) {
            issues.reject("missing.entityName", "The field entityName must contain a value for at least one language.");
        }
    }

    private void validateIssuanceV1(SimpleErrors issues, IssuanceV1Details details) {
        // note:
        // - since EIDARTFE-754 is not yet ready, fetching the schema from the trust registry for validation is not possible yet
        // - instead we just check if the URL is valid for the governmental trust registry
        /*
        As of EID-5402 and in preparation for the upcoming VCT concept (https://confluence.bit.admin.ch/x/ZofVNQ) rework this check is deactivated.
        if (vcSchemaUrlValidator.isInvalidVcSchemaUrl(details.getCanIssue())) {
            issues.reject(
                "trust-statement.create.errors.can-issue-invalid",
                List.of(details.getCanIssue()).toArray(),
                "The field canIssue must be a valid VC Schema URL from the govermental trust registry starting with " +
                registryProperties.getVcSchemaBaseUrl()
            );
        }
         */
    }

    private void validateVerificationV1(SimpleErrors issues, VerificationV1Details details) {
        // note:
        // - since EIDARTFE-754 is not yet ready, fetching the schema from the trust registry for validation is not possible yet
        // - instead we just check if the URL is valid for the governmental trust registry
        /*
        As of EID-5402 and in preparation for the upcoming VCT concept (https://confluence.bit.admin.ch/x/ZofVNQ) rework this check is deactivated.
        if (vcSchemaUrlValidator.isInvalidVcSchemaUrl(details.getCanVerify())) {
            issues.reject(
                    "trust-statement.create.errors.can-verify-invalid",
                    List.of(details.getCanVerify()).toArray(),
                    "The field canVerify must be a valid VC Schema URL from the govermental trust registry starting with " +
                            registryProperties.getVcSchemaBaseUrl()
            );
        }
         */
    }

    private void validateNoOtherSubmissionWithSameSubjectIsActive(
        SimpleErrors issues,
        TrustStatementPartnerLink partnerLink
    ) {
        trustStatementPartnerLinkRepository
            .findAllBySubjectAndTypeAndStatusIsInAndValidFromBetweenAndValidUntilBetween(
                partnerLink.getSubject(),
                partnerLink.getType(),
                List.of(TrustStatementPartnerLinkStatus.ACTIVE),
                partnerLink.getValidFrom(),
                partnerLink.getValidUntil(),
                partnerLink.getValidFrom(),
                partnerLink.getValidUntil()
            )
            // do not include the entity itself
            .filter(concurringSubmission -> !concurringSubmission.getId().equals(partnerLink.getId()))
            .forEach(concurringSubmission ->
                issues.reject(
                    "trust-statement.create.errors.concurrent-trust-statement",
                    List.of(
                        concurringSubmission.getId(),
                        partnerLink.getValidFrom(),
                        partnerLink.getValidUntil()
                    ).toArray(),
                    "There is already a valid statement partnerLink (id:%s) between %s to %s.".formatted(
                        concurringSubmission.getId(),
                        partnerLink.getValidFrom(),
                        partnerLink.getValidUntil()
                    )
                )
            );
    }

    private void validateNoOtherSubmissionWithSameSubjectAndCanIssueOrCanVerifyIsActive(
        SimpleErrors issues,
        TrustStatementPartnerLink partnerLink
    ) {
        trustStatementPartnerLinkRepository
            .findAllBySubjectAndTypeAndStatusIsInAndValidFromBetweenAndValidUntilBetween(
                partnerLink.getSubject(),
                partnerLink.getType(),
                List.of(TrustStatementPartnerLinkStatus.ACTIVE),
                partnerLink.getValidFrom(),
                partnerLink.getValidUntil(),
                partnerLink.getValidFrom(),
                partnerLink.getValidUntil()
            )
            .filter(concurringSubmission -> !concurringSubmission.getId().equals(partnerLink.getId()))
            .filter(concurringSubmission -> hasSameCanIssueOrSameCanVerify(concurringSubmission, partnerLink))
            .forEach(concurringSubmission ->
                issues.reject(
                    "trust-statement.create.errors.concurrent-trust-statement",
                    List.of(
                        concurringSubmission.getId(),
                        partnerLink.getValidFrom(),
                        partnerLink.getValidUntil()
                    ).toArray(),
                    "There is already a valid statement submission (id:%s) between %s to %s.".formatted(
                        concurringSubmission.getId(),
                        partnerLink.getValidFrom(),
                        partnerLink.getValidUntil()
                    )
                )
            );
    }

    private boolean hasSameCanIssueOrSameCanVerify(
        TrustStatementPartnerLink submission1,
        TrustStatementPartnerLink submission2
    ) {
        if (
            submission2.getDetails() instanceof IssuanceV1Details &&
            submission1.getDetails() instanceof IssuanceV1Details
        ) {
            return ((IssuanceV1Details) submission1.getDetails()).getCanIssue().equals(
                ((IssuanceV1Details) submission2.getDetails()).getCanIssue()
            );
        } else if (
            submission2.getDetails() instanceof VerificationV1Details &&
            submission1.getDetails() instanceof VerificationV1Details
        ) {
            return ((VerificationV1Details) submission1.getDetails()).getCanVerify().equals(
                ((VerificationV1Details) submission2.getDetails()).getCanVerify()
            );
        }
        return false;
    }
}
