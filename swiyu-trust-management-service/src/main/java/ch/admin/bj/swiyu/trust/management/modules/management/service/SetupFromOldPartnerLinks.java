package ch.admin.bj.swiyu.trust.management.modules.management.service;

import ch.admin.bj.swiyu.trust.client.core.business.internal.model.*;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.trust.management.modules.common.async.AsyncService;
import ch.admin.bj.swiyu.trust.management.modules.common.async.Lock;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ResourceNotFoundException;
import ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport;
import ch.admin.bj.swiyu.trust.management.modules.management.api.*;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@SuppressWarnings({ "java:S1141", "java:S1192", "java:S3776", "java:S3655" })
@Slf4j
@Component
@RequiredArgsConstructor
public class SetupFromOldPartnerLinks {

    private final BusinessPartnerIdentityService businessPartnerIdentityService;
    private final TrustStatementService trustStatementService;
    private final ProtectedIssuanceService protectedIssuanceService;
    private final LockingTaskExecutor lockingTaskExecutor;

    private final AsyncService async;

    @EventListener(ApplicationReadyEvent.class)
    public void runTransformation() {
        async.run(() -> {
            var lockAtMostUntil = java.time.Instant.now().plus(java.time.Duration.ofMinutes(15));
            lockingTaskExecutor.executeWithLock(
                (Runnable) () -> {
                    SecurityContextSupport.setSystemUserAuthentication();

                    log.info("Create/Update BusinessPartnerIdentities from old trust statements");
                    var businessPartnerIdentityIds = createBusinessPartnerIdentitiesAndAddTrustedIdentifiers();
                    log.info("Create/Update Protected Issuance status from old trust statements");
                    createProtectedIssuanceEntriesAndAuthorizations();
                    log.info("Create/Update Protected Verification status from old trust statements");
                    createProtectedVerificationAuthorizations();
                    log.info("Completed BPI transformation input from old trust statements ");
                    activateBusinessPartnerIdentities(businessPartnerIdentityIds);
                },
                new LockConfiguration(
                    lockAtMostUntil,
                    Lock.JAVA_MIGRATION,
                    java.time.Duration.ofMinutes(15),
                    java.time.Duration.ofMinutes(1)
                )
            );
        });
    }

    private void activateBusinessPartnerIdentities(Set<UUID> businessPartnerIdentityId) {
        for (var bpiId : businessPartnerIdentityId) {
            businessPartnerIdentityService.activate(bpiId);
        }
    }

    private void createProtectedVerificationAuthorizations() {
        var pvaTSList = trustStatementService.getPartnerLinks(
            new TrustStatementPartnerLinkFilterDto(
                null,
                true,
                null,
                TrustStatementTypeDto.PROTECTED_VERIFICATION_AUTHORIZATION_V2,
                TrustStatementPartnerLinkStatusDto.ACTIVE,
                null,
                null
            ),
            Pageable.unpaged()
        );

        for (var pvaTS : pvaTSList) {
            try {
                var bpi = businessPartnerIdentityService.getBusinessPartnerIdentityByTrustedIdentifier(pvaTS.subject());
                businessPartnerIdentityService.addProtectedVerificationAuthorization(
                    new ProtectedVerificationAuthorizationRequestDto(bpi.id(), AuthorizableFieldDto.AHV_NUMBER)
                );
            } catch (ResourceNotFoundException e) {
                log.debug("Nothing to do as no bpi exists for partnerLink / pvaTS {}", pvaTS.id());
            } catch (IllegalArgumentException e) {
                log.debug("Nothing to do as Authorization is already set for partnerLink / pvaTS {}", pvaTS.id());
            }
        }
    }

    private void createProtectedIssuanceEntriesAndAuthorizations() {
        var piaTSList = trustStatementService.getPartnerLinks(
            new TrustStatementPartnerLinkFilterDto(
                null,
                true,
                null,
                TrustStatementTypeDto.PROTECTED_ISSUANCE_AUTHORIZATION_V2,
                TrustStatementPartnerLinkStatusDto.ACTIVE,
                null,
                null
            ),
            Pageable.unpaged()
        );

        for (var piaTS : piaTSList) {
            try {
                var partnerLink = trustStatementService.getPartnerLink(piaTS.id());
                var bpi = businessPartnerIdentityService.getBusinessPartnerIdentityByTrustedIdentifier(piaTS.subject());
                Map<?, ?> canIssue = (Map<?, ?>) partnerLink.getDetails().get("canIssue");
                var vct = canIssue.get("vct").toString();
                Map<String, String> vctName = (Map<String, String>) canIssue.get("vctName");
                Map<String, String> reason = (Map<String, String>) canIssue.get("reason");

                // Make sure the ProtectedIssuanceEntry exists
                try {
                    protectedIssuanceService.createProtectedIssuanceEntry(
                        new ProtectedIssuanceEntryCreateRequestDto(vct, vctName),
                        SecurityContextSupport.getCurrentUserFullName()
                    );
                } catch (IllegalArgumentException e) {
                    log.debug("vct {} already has a ProtectedIssuanceEntry", vct);
                }
                var pie = protectedIssuanceService.getProtectedIssuanceEntry(vct);

                protectedIssuanceService.addAuthorization(
                    new ProtectedIssuanceAuthorizationCreateRequestDto(pie.id(), bpi.id(), reason),
                    SecurityContextSupport.getCurrentUserFullName()
                );
            } catch (ResourceNotFoundException e) {
                log.debug("Nothing to do as no bpi exists for partnerLink / piaTS {}", piaTS.id());
            } catch (IllegalArgumentException e) {
                log.debug("Nothing to do as Authorization is already set for partnerLink / piaTS {}", piaTS.id());
            }
        }
    }

    private Set<UUID> createBusinessPartnerIdentitiesAndAddTrustedIdentifiers() {
        Set<UUID> createdBusinessPartnerIdentityIds = new HashSet<>();
        var idTSList = trustStatementService.getPartnerLinks(
            new TrustStatementPartnerLinkFilterDto(
                null,
                true,
                null,
                TrustStatementTypeDto.IDENTITY_V2,
                TrustStatementPartnerLinkStatusDto.ACTIVE,
                null,
                null
            ),
            Pageable.unpaged()
        );
        for (var idTS : idTSList) {
            if (idTS.partnerId() != null) {
                if (!businessPartnerIdentityService.businessPartnerIdentityExists(idTS.partnerId())) {
                    createBusinessPartnerIdentity(idTS);
                    createdBusinessPartnerIdentityIds.add(idTS.partnerId());
                }
                businessPartnerIdentityService.addTrustedIdentifiers(idTS.partnerId(), Set.of(idTS.subject()));
            }
        }
        return createdBusinessPartnerIdentityIds;
    }

    private void createBusinessPartnerIdentity(TrustStatementPartnerLinkListItemDto idTSv2) {
        // If idTsv2 does have a BP set and no BPI exists: Create a BPI
        var partnerLink = trustStatementService.getPartnerLink(idTSv2.id());
        var submission = new TrustOnboardingSubmissionDto();
        submission.setId(UUID.randomUUID());
        submission.setPartnerId(idTSv2.partnerId());
        submission.setBusinessPartnerType(
            partnerLink.getDetails().getOrDefault("isStateActor", "false") == "true"
                ? BusinessPartnerTypeDto.GOVERNMENTAL_INSTITUTION
                : BusinessPartnerTypeDto.BUSINESS
        );
        submission.setCorrespondingLanguage(LanguageDto.DE);
        Object registryIdsObj = partnerLink.getDetails().get("registryIds");
        var registeredInCommercialRegister =
            registryIdsObj instanceof Map<?, ?> registryIds &&
            (registryIds.containsKey("uid") || registryIds.containsKey("UID"));
        submission.setIsRegisteredInCommercialRegister(registeredInCommercialRegister);

        Object entityNameSource = partnerLink.getDetails().get("entityName");
        if (entityNameSource instanceof Map<?, ?> tsEntityNameSource) {
            var entityNameTarget = new MultiLanguageTextDto();
            var nameTarget = new HashMap<String, String>();

            for (var lang : List.of("de", "de-CH")) {
                if (tsEntityNameSource.containsKey(lang)) {
                    entityNameTarget.setDe(tsEntityNameSource.get(lang).toString());
                    nameTarget.put(lang, tsEntityNameSource.get(lang).toString());
                }
            }
            for (var lang : List.of("en", "en-CH")) {
                if (tsEntityNameSource.containsKey(lang)) {
                    entityNameTarget.setEn(tsEntityNameSource.get(lang).toString());
                    nameTarget.put(lang, tsEntityNameSource.get(lang).toString());
                }
            }
            for (var lang : List.of("fr", "fr-CH")) {
                if (tsEntityNameSource.containsKey(lang)) {
                    entityNameTarget.setFr(tsEntityNameSource.get(lang).toString());
                    nameTarget.put(lang, tsEntityNameSource.get(lang).toString());
                }
            }
            for (var lang : List.of("it", "it-CH")) {
                if (tsEntityNameSource.containsKey(lang)) {
                    entityNameTarget.setIt(tsEntityNameSource.get(lang).toString());
                    nameTarget.put(lang, tsEntityNameSource.get(lang).toString());
                }
            }
            for (var lang : List.of("rm", "rm-CH")) {
                if (tsEntityNameSource.containsKey(lang)) {
                    entityNameTarget.setRm(tsEntityNameSource.get(lang).toString());
                    nameTarget.put(lang, tsEntityNameSource.get(lang).toString());
                }
            }
            // set default name
            if (tsEntityNameSource.containsKey("default")) {
                nameTarget.put("default", tsEntityNameSource.get("default").toString());
            } else if (tsEntityNameSource.containsKey("de")) {
                nameTarget.put("default", tsEntityNameSource.get("de").toString());
            } else if (tsEntityNameSource.containsKey("de-CH")) {
                nameTarget.put("default", tsEntityNameSource.get("de-CH").toString());
            } else if (tsEntityNameSource.containsKey("en")) {
                nameTarget.put("default", tsEntityNameSource.get("en").toString());
            } else {
                nameTarget.put("default", tsEntityNameSource.values().stream().findFirst().get().toString());
            }

            submission.setName(nameTarget);
            submission.setEntityName(entityNameTarget);
        }
        var pop = new ProofOfPossessionDto();
        pop.setDid(idTSv2.subject());
        pop.setNonce("NOTUSED");
        pop.setStatus(ProofOfPossessionStatusDto.NOT_SUPPLIED);
        pop.setVerifiedAt(idTSv2.createdAt());
        submission.setProofOfPossessions(List.of(pop));
        submission.setSubmittedAt(Instant.now());
        submission.setType(TrustOnboardingSubmissionTypeDto.REGISTRATION);

        businessPartnerIdentityService.handleTrustOnboardingApproval(submission);
    }
}
