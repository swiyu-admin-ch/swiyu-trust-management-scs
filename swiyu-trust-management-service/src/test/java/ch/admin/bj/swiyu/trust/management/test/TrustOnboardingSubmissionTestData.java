package ch.admin.bj.swiyu.trust.management.test;

import ch.admin.bj.swiyu.trust.client.core.business.internal.model.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class TrustOnboardingSubmissionTestData {

    public static TrustOnboardingSubmissionDto trustOnboardingSubmission() {
        return trustOnboardingSubmission(entityName(), LanguageDto.DE);
    }

    public static TrustOnboardingSubmissionDto trustOnboardingSubmission(
        MultiLanguageTextDto entityName,
        LanguageDto correspondingLanguage
    ) {
        var submission = new TrustOnboardingSubmissionDto();
        submission.setId(UUID.randomUUID());
        submission.setAddress(address());
        submission.setEntityName(entityName);
        submission.setBusinessPartnerType(BusinessPartnerTypeDto.GOVERNMENTAL_INSTITUTION);
        submission.setRegistryIds(Map.of("uid", "123456"));
        submission.setCorrespondingLanguage(correspondingLanguage);
        submission.setContactPerson(contact());
        submission.setProofOfPossessions(proofOfPossessions());
        submission.setPartnerId(UUID.randomUUID());
        submission.setStatus(TrustOnboardingSubmissionStatusDto.UNSUBMITTED);
        submission.setEntityEmail("test@test.ch");
        submission.setSubmittedAt(LocalDateTime.of(2025, 8, 9, 10, 0, 0).toInstant(ZoneOffset.UTC));
        return submission;
    }

    private static List<ProofOfPossessionDto> proofOfPossessions() {
        var pop = new ProofOfPossessionDto();
        pop.setDid("did:123");
        pop.setNonce("123");
        pop.setStatus(ProofOfPossessionStatusDto.VALID);
        pop.setVerifiedAt(Instant.now());
        return List.of(pop);
    }

    private static @NotNull MultiLanguageTextDto entityName() {
        var entityName = new MultiLanguageTextDto();
        entityName.setDe("EntityDE");
        entityName.setFr("EntityFR");
        entityName.setIt("EntityIT");
        entityName.setEn("EntityEN");
        entityName.setRm("EntityRM");
        return entityName;
    }

    private static @NotNull ContactDto contact() {
        var contact = new ContactDto();
        contact.setAddress(address());
        contact.setEmail("test@test.ch");
        contact.setFirstName("John");
        contact.setLastName("Doe");
        contact.setPhone("12345");
        return contact;
    }

    private static @NotNull AddressDto address() {
        var address = new AddressDto();
        address.setCity("Bern");
        address.setCountry("Switzerland");
        address.setPostalCode("12345");
        return address;
    }
}
