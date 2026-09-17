package ch.admin.bj.swiyu.trust.management.test;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventIdentity;
import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventPublisher;
import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventType;
import ch.admin.bj.swiyu.messagetype.ti.*;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentity;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TrustAddDidTask;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TrustOnboardingTestData {

    public static final ZoneId ZONE_ID_ZURICH = ZoneId.of("Europe/Zurich");

    public static TrustOnboardingSubmissionDto trustOnboardingSubmissionDto() {
        return trustOnboardingSubmissionDto(UUID.randomUUID());
    }

    public static TrustOnboardingSubmissionDto trustOnboardingSubmissionDto(UUID id) {
        var pop1 = new ProofOfPossessionDto();
        pop1.did("did1");
        pop1.nonce("nonce");
        pop1.status(ProofOfPossessionStatusDto.VALID);

        var pop2 = new ProofOfPossessionDto();
        pop2.did("did2");
        pop2.nonce("nonce");
        pop2.status(ProofOfPossessionStatusDto.VALID);

        return new TrustOnboardingSubmissionDto()
            .id(id)
            .version(1L)
            .partnerId(UUID.randomUUID())
            .proofOfPossessions(List.of(pop1, pop2))
            .businessPartnerType(BusinessPartnerTypeDto.GOVERNMENTAL_INSTITUTION)
            .type(TrustOnboardingSubmissionTypeDto.REGISTRATION)
            .submittedAt(LocalDate.of(2025, 8, 9).atStartOfDay(ZONE_ID_ZURICH).toInstant())
            .updatedAt(LocalDate.of(2025, 8, 9).atStartOfDay(ZONE_ID_ZURICH).toInstant())
            .createdAt(LocalDate.of(2025, 8, 9).atStartOfDay(ZONE_ID_ZURICH).toInstant())
            .name(
                Map.of(
                    "default",
                    "Migros",
                    "de-CH",
                    "Migros(de)",
                    "fr-CH",
                    "Migros(fr)",
                    "it-CH",
                    "Migros(it)",
                    "en",
                    "Migros(en)",
                    "rm-CH",
                    "Migros(rm)"
                )
            );
    }

    public static TrustAddDidTask trustAddDidTask(BusinessPartnerIdentity bpi) {
        var submittedAt = LocalDate.of(2025, 8, 9).atStartOfDay(ZONE_ID_ZURICH).toInstant();
        return new TrustAddDidTask(
            bpi.getId(),
            bpi.getEntityName(),
            UUID.randomUUID(),
            "did:example:permission123",
            submittedAt.plus(30, ChronoUnit.DAYS),
            submittedAt
        );
    }

    public static TrustAdditionalDidsSubmissionInternalDtoDto trustAddDidSubmissionDto() {
        return trustAddDidSubmissionDto(UUID.randomUUID());
    }

    public static TrustAdditionalDidsSubmissionInternalDtoDto trustAddDidSubmissionDto(UUID id) {
        var permissionDid = new ProofOfPossessionDto();
        permissionDid.did("did:example:permission123");
        permissionDid.nonce("nonce");
        permissionDid.status(ProofOfPossessionStatusDto.VALID);

        var newDid = new ProofOfPossessionDto();
        newDid.did("did:example:new456");
        newDid.nonce("nonce2");
        newDid.status(ProofOfPossessionStatusDto.VALID);

        return new TrustAdditionalDidsSubmissionInternalDtoDto()
            .id(id)
            .status(TrustAdditionalDidsSubmissionInternalDtoDto.StatusEnum.SUBMITTED)
            .permissionDid(permissionDid)
            .didsToAdd(List.of(newDid))
            .updatedAt(LocalDate.of(2025, 8, 9).atStartOfDay(ZONE_ID_ZURICH).toInstant());
    }

    public static TiTrustAddDidSubmissionSubmittedEvent tiTrustAddDidSubmissionSubmittedEvent() {
        return tiTrustAddDidSubmissionSubmittedEvent(UUID.randomUUID());
    }

    public static TiTrustAddDidSubmissionSubmittedEvent tiTrustAddDidSubmissionSubmittedEvent(UUID submissionId) {
        var identity = AvroDomainEventIdentity.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setIdempotenceId(UUID.randomUUID().toString())
            .setCreated(Instant.now())
            .build();

        var type = AvroDomainEventType.newBuilder()
            .setName("TiTrustAddDidSubmissionSubmittedEvent")
            .setVersion("1.0.0")
            .build();

        var payload = TrustAddDidSubmissionSubmittedPayload.newBuilder()
            .setTrustAddDidSubmissionId(submissionId)
            .build();

        return TiTrustAddDidSubmissionSubmittedEvent.newBuilder()
            .setIdentity(identity)
            .setType(type)
            .setPublisher(new AvroDomainEventPublisher())
            .setPayload(payload)
            .setDomainEventVersion("1.0.0")
            .build();
    }

    public static TiTrustOnboardingSubmissionAcceptedEvent tiTrustOnboardingSubmissionAcceptedEvent() {
        var identity = AvroDomainEventIdentity.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setIdempotenceId(UUID.randomUUID().toString())
            .setCreated(Instant.now())
            .build();

        var type = AvroDomainEventType.newBuilder()
            .setName("TiTrustOnboardingSubmissionAcceptedEvent")
            .setVersion("2.0.0")
            .build();

        var payload = TrustOnboardingSubmissionAcceptedPayload.newBuilder()
            .setTrustOnboardingSubmissionId(UUID.randomUUID())
            .setPartnerId(UUID.randomUUID())
            .build();

        return TiTrustOnboardingSubmissionAcceptedEvent.newBuilder()
            .setIdentity(identity)
            .setType(type)
            .setPublisher(new AvroDomainEventPublisher())
            .setReferences(TrustOnboardingSubmissionAcceptedReferences.newBuilder().build())
            .setPayload(payload)
            .setDomainEventVersion("2.0.0")
            .build();
    }

    public static TiProtectedVerificationSubmissionAcceptedEvent tiProtectedVerificationSubmissionAcceptedEvent() {
        return tiProtectedVerificationSubmissionAcceptedEvent(UUID.randomUUID());
    }

    public static TiProtectedVerificationSubmissionAcceptedEvent tiProtectedVerificationSubmissionAcceptedEvent(
        UUID submissionId
    ) {
        var identity = AvroDomainEventIdentity.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setIdempotenceId(UUID.randomUUID().toString())
            .setCreated(Instant.now())
            .build();

        var type = AvroDomainEventType.newBuilder()
            .setName("TiProtectedVerificationSubmissionAcceptedEvent")
            .setVersion("1.0.0")
            .build();

        var payload = ProtectedVerificationSubmissionAcceptedPayload.newBuilder()
            .setProtectedVerificationSubmissionId(submissionId)
            .build();

        return TiProtectedVerificationSubmissionAcceptedEvent.newBuilder()
            .setIdentity(identity)
            .setType(type)
            .setPublisher(new AvroDomainEventPublisher())
            .setPayload(payload)
            .setDomainEventVersion("1.0.0")
            .build();
    }
}
