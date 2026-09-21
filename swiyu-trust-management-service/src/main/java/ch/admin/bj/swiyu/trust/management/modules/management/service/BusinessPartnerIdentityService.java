package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.date.DateTimeHelper.today;
import static ch.admin.bj.swiyu.trust.management.modules.common.persistence.TransactionManagerNames.MANAGEMENT_TRANSACTION_MANAGER;
import static ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport.getCurrentUserFullName;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.BusinessPartnerIdentityMapper.*;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.ProtectedVerificationAuthorizationMapper.mapPageableWithValidSortProperties;

import ch.admin.bj.swiyu.trust.client.core.business.internal.model.TrustOnboardingSubmissionDto;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.BusinessPartnerIdentityNotActiveException;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ResourceNotFoundException;
import ch.admin.bj.swiyu.trust.management.modules.management.api.*;
import ch.admin.bj.swiyu.trust.management.modules.management.config.statements.DefaultStatementProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.event.TiBusinessPartnerIdentityActivatedEventBuilder;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.event.TiBusinessPartnerIdentityDeactivatedEventBuilder;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.event.TiBusinessPartnerIdentityUpdatedEventBuilder;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.OutboxEventPublisher;
import com.querydsl.core.BooleanBuilder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessPartnerIdentityService {

    private final BusinessPartnerIdentityDomainService businessPartnerIdentityDomainService;
    private final BusinessPartnerIdentityRepository businessPartnerIdentityRepository;
    private final DefaultStatementProperties defaultStatementProperties;
    private final DomainEventService domainEventService;
    private final OutboxEventPublisher outboxEventPublisher;
    private final ProtectedVerificationAuthorizationRepository protectedVerificationAuthorizationRepository;
    private final TrustStatementPartnerLinkRepository partnerLinkRepository;
    private final TrustStatementService trustStatementService;
    private final ProtectedIssuanceAuthorizationRepository protectedIssuanceAuthorizationRepository;
    private final ProtectedIssuanceEntryRepository protectedIssuanceEntryRepository;

    /**
     * Whether a {@link BusinessPartnerIdentity} exists for the partner at all, regardless of its status - an
     * authorization can be added even for an untrusted (non-ACTIVE) BPI, but not if no BPI exists.
     */
    @Transactional(readOnly = true, transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public boolean businessPartnerIdentityExists(UUID businessPartnerId) {
        return businessPartnerIdentityRepository.existsById(businessPartnerId);
    }

    public void activate(UUID businessPartnerId) {
        var bpi = businessPartnerIdentityDomainService.activate(businessPartnerId);
        var event = TiBusinessPartnerIdentityActivatedEventBuilder.create().businessPartnerIdentity(bpi).build();

        outboxEventPublisher.publishBusinessPartnerIdentityActivatedEvent(event);
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public void deactivate(UUID businessPartnerId) {
        var bpi = businessPartnerIdentityDomainService.deactivate(businessPartnerId);

        var event = TiBusinessPartnerIdentityDeactivatedEventBuilder.create().businessPartnerIdentity(bpi).build();
        outboxEventPublisher.publishBusinessPartnerIdentityDeactivatedEvent(event);
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public List<UUID> findAllBusinessPartnerIdentityIdsToDeactivate() {
        return businessPartnerIdentityRepository
            .findAllByStatusAndValidUntilBefore(BusinessPartnerIdentityStatus.ACTIVE, today().toInstant())
            .stream()
            .map(BusinessPartnerIdentity::getId)
            .toList();
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public void addTrustedIdentifiers(UUID businessPartnerId, Set<String> trustedIdentifiers) {
        var bpi = businessPartnerIdentityRepository
            .findById(businessPartnerId)
            .orElseThrow(businessPartnerIdentityNotFound(businessPartnerId));
        bpi.getTrustedIdentifier().addAll(trustedIdentifiers);
        businessPartnerIdentityRepository.save(bpi);
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public void deactivateTrustStatements(UUID businessPartnerId, String reason) {
        var bpi = businessPartnerIdentityRepository
            .findById(businessPartnerId)
            .orElseThrow(businessPartnerIdentityNotFound(businessPartnerId));

        List<TrustStatementPartnerLink> partnerLinks = partnerLinkRepository.findAllByPartnerIdAndStatus(
            bpi.getId(),
            TrustStatementPartnerLinkStatus.ACTIVE
        );

        for (var partnerLink : partnerLinks) {
            var req = new DeactivationRequestDto(reason);
            trustStatementService.deactivateTrustStatement(partnerLink.getId(), req);
        }
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public void issueTrustStatements(UUID businessPartnerId) {
        var bpi = businessPartnerIdentityRepository
            .findById(businessPartnerId)
            .orElseThrow(businessPartnerIdentityNotFound(businessPartnerId));

        if (bpi.getStatus() != BusinessPartnerIdentityStatus.ACTIVE) {
            throw new BusinessPartnerIdentityNotActiveException(
                "Business partner identity for id '%s' is not active".formatted(businessPartnerId)
            );
        }

        log.debug(
            "Issuing trust statements for business partner identity: {}, trustedIdentifier ({}): {} ",
            businessPartnerId,
            bpi.getTrustedIdentifier().size(),
            bpi.getTrustedIdentifier()
        );
        issueAllIdTSForTrustedIdentifiers(bpi);
        issueAllPvaTSForTrustedIdentifiers(bpi);
        issueAllPiaTSForTrustedIdentifiers(bpi);

        bpi.updateLastIssuance();
        businessPartnerIdentityRepository.save(bpi);
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public void renewTrustStatements(UUID businessPartnerId) {
        var bpi = businessPartnerIdentityRepository
            .findById(businessPartnerId)
            .orElseThrow(businessPartnerIdentityNotFound(businessPartnerId));

        if (bpi.getStatus() != BusinessPartnerIdentityStatus.ACTIVE) {
            throw new BusinessPartnerIdentityNotActiveException(
                "Business partner identity for id '%s' is not active".formatted(businessPartnerId)
            );
        }

        outboxEventPublisher.publishBusinessPartnerIdentityUpdatedEvent(
            TiBusinessPartnerIdentityUpdatedEventBuilder.create().businessPartnerIdentity(bpi).build()
        );
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public List<UUID> findAllBusinessPartnerIdentityIdsWithTrustStatementToRenew() {
        var lastIssuanceLimit = today().minus(defaultStatementProperties.refreshPeriod()).toInstant();
        return businessPartnerIdentityRepository
            .findAllByStatusAndLastIssuanceAtLessThanEqual(BusinessPartnerIdentityStatus.ACTIVE, lastIssuanceLimit)
            .stream()
            .map(BusinessPartnerIdentity::getId)
            .toList();
    }

    @Transactional(readOnly = true, transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public Page<BusinessPartnerIdentityDto> getBusinessPartnerIdentities(
        BusinessPartnerIdentityFilterDto filters,
        Pageable pageable
    ) {
        var q = QBusinessPartnerIdentity.businessPartnerIdentity;
        var where = new BooleanBuilder();
        if (filters.lastModifiedBy() != null) {
            where.and(q.audit.lastModifiedBy.like(filters.lastModifiedBy()));
        }
        if (filters.createdBy() != null) {
            where.and(q.audit.createdBy.like(filters.createdBy()));
        }
        return businessPartnerIdentityRepository
            .findAll(where, mapBusinessPartnerIdentityPageableWithValidSortProperties(pageable))
            .map(BusinessPartnerIdentityMapper::toBusinessPartnerIdentityDto);
    }

    @Transactional(readOnly = true, transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public BusinessPartnerIdentity getBusinessPartnerIdentity(@Valid @NotNull UUID businessPartnerIdentityId) {
        return businessPartnerIdentityRepository
            .findById(businessPartnerIdentityId)
            .orElseThrow(businessPartnerIdentityNotFound(businessPartnerIdentityId));
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public UUID handleTrustOnboardingApproval(TrustOnboardingSubmissionDto submission) {
        BusinessPartnerIdentity bpi;
        var relevantInformationUpdate = BusinessPartnerIdentityRelevantInformationUpdate.of(submission);
        var bpiOpt = businessPartnerIdentityRepository.findById(submission.getPartnerId());

        switch (submission.getType()) {
            case REGISTRATION -> {
                if (bpiOpt.isEmpty()) {
                    bpi = new BusinessPartnerIdentity(
                        submission.getPartnerId(),
                        relevantInformationUpdate.entityName(),
                        null,
                        relevantInformationUpdate.uid(),
                        relevantInformationUpdate.isRegisteredInCommercialRegister(),
                        relevantInformationUpdate.correspondingLanguage(),
                        BusinessPartnerIdentityStatus.DEACTIVATED,
                        relevantInformationUpdate.isStateActor(),
                        null,
                        null,
                        relevantInformationUpdate.trustedIdentifier()
                    );
                } else {
                    bpi = bpiOpt.get();
                    bpi.applyRelevantInformationUpdate(relevantInformationUpdate);
                }
            }
            case PROFILE_CHANGE, RENEWAL -> {
                if (bpiOpt.isEmpty()) {
                    throw new IllegalStateException(
                        "Business partner identity for id '%s' must exist for a %s state change".formatted(
                            submission.getPartnerId(),
                            submission.getType()
                        )
                    );
                }
                bpi = bpiOpt.get();
                bpi.applyRelevantInformationUpdate(relevantInformationUpdate);
            }
            default -> throw new IllegalStateException("Unexpected value: " + submission.getType());
        }
        businessPartnerIdentityRepository.save(bpi);
        return bpi.getId();
    }

    @Transactional(readOnly = true, transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public BusinessPartnerIdentityDto getBusinessPartnerIdentityDto(@Valid @NotNull UUID businessPartnerIdentityId) {
        var businessPartnerIdentity = businessPartnerIdentityRepository
            .findById(businessPartnerIdentityId)
            .orElseThrow(businessPartnerIdentityNotFound(businessPartnerIdentityId));
        return toBusinessPartnerIdentityDto(businessPartnerIdentity);
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public void sync(UUID businessPartnerIdentityId) {
        var bpi = businessPartnerIdentityRepository
            .findById(businessPartnerIdentityId)
            .orElseThrow(businessPartnerIdentityNotFound(businessPartnerIdentityId));
        log.debug("Invoking Sync / BusinessPartnerIdentityUpdatedEvent for {}", businessPartnerIdentityId);
        outboxEventPublisher.publishBusinessPartnerIdentityUpdatedEvent(
            TiBusinessPartnerIdentityUpdatedEventBuilder.create().businessPartnerIdentity(bpi).build()
        );
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public void syncAll() {
        var bpis = businessPartnerIdentityRepository.findAll();

        for (var bpi : bpis) {
            log.debug("Invoking Sync All / BusinessPartnerIdentityUpdatedEvent for {}", bpi.getId());
            outboxEventPublisher.publishBusinessPartnerIdentityUpdatedEvent(
                TiBusinessPartnerIdentityUpdatedEventBuilder.create().businessPartnerIdentity(bpi).build()
            );
        }
    }

    @Transactional(readOnly = true, transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public Page<ProtectedVerificationAuthorizationDto> getProtectedVerificationAuthorizationsPaged(
        ProtectedVerificationAuthorizationFilterDto filters,
        Pageable pageable
    ) {
        var q = QProtectedVerificationAuthorization.protectedVerificationAuthorization;
        var where = new BooleanBuilder();
        if (filters.businessPartnerIdentityId() != null) {
            where.and(q.businessPartnerIdentityId.eq(filters.businessPartnerIdentityId()));
        }
        if (filters.createdBy() != null) {
            where.and(q.audit.createdBy.like(filters.createdBy()));
        }
        if (filters.lastModifiedBy() != null) {
            where.and(q.audit.lastModifiedBy.like(filters.lastModifiedBy()));
        }
        return protectedVerificationAuthorizationRepository
            .findAll(where, mapPageableWithValidSortProperties(pageable))
            .map(BusinessPartnerIdentityMapper::toProtectedVerificationAuthorizationDto);
    }

    @Transactional(readOnly = true, transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public ProtectedVerificationAuthorizationDto getProtectedVerificationAuthorizationDto(
        @Valid @NotNull UUID protectedVerificationAuthorizationId
    ) {
        var pva = protectedVerificationAuthorizationRepository
            .findById(protectedVerificationAuthorizationId)
            .orElseThrow(protectedVerificationAuthorizationNotFound(protectedVerificationAuthorizationId));
        return toProtectedVerificationAuthorizationDto(pva);
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    @SuppressWarnings("java:S6809") // bypass SonarQube Method with Spring proxy should not be called via "this"
    public ProtectedVerificationAuthorizationDto addProtectedVerificationAuthorization(
        @Valid @NotNull ProtectedVerificationAuthorizationRequestDto request
    ) {
        var field = toProtectedVerificationField(request.protectedField());

        // Uniqueness is enforced at the DB level (unique DID), but we also check here to provide a
        // friendly 400 error instead of a low-level constraint violation.
        var existingEntry =
            protectedVerificationAuthorizationRepository.findAllByBusinessPartnerIdentityIdAndProtectedVerificationField(
                request.businessPartnerIdentityId(),
                field
            );
        if (!existingEntry.isEmpty()) {
            throw new IllegalArgumentException(
                "Validation failed: protected verification authorization already exists for field '%s' and business partner identity id '%s'.".formatted(
                    field,
                    request.businessPartnerIdentityId()
                )
            );
        }

        var bpi = getBusinessPartnerIdentity(request.businessPartnerIdentityId());

        var pva = protectedVerificationAuthorizationRepository.save(
            new ProtectedVerificationAuthorization(UUID.randomUUID(), bpi.getId(), field)
        );

        this.domainEventService.protectedVerificationAuthorizationAdded(pva.getId(), getCurrentUserFullName());

        outboxEventPublisher.publishBusinessPartnerIdentityUpdatedEvent(
            TiBusinessPartnerIdentityUpdatedEventBuilder.create().businessPartnerIdentity(bpi).build()
        );

        return toProtectedVerificationAuthorizationDto(pva);
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    @SuppressWarnings("java:S6809") // bypass SonarQube Method with Spring proxy should not be called via "this"
    public void removeProtectedVerificationAuthorization(@Valid @NotNull UUID protectedVerificationAuthorizationId) {
        var pva = protectedVerificationAuthorizationRepository
            .findById(protectedVerificationAuthorizationId)
            .orElseThrow(protectedVerificationAuthorizationNotFound(protectedVerificationAuthorizationId));
        var bpi = getBusinessPartnerIdentity(pva.getBusinessPartnerIdentityId());

        protectedVerificationAuthorizationRepository.delete(pva);

        outboxEventPublisher.publishBusinessPartnerIdentityUpdatedEvent(
            TiBusinessPartnerIdentityUpdatedEventBuilder.create().businessPartnerIdentity(bpi).build()
        );
    }

    @NotNull
    public BusinessPartnerIdentityDto getBusinessPartnerIdentityByTrustedIdentifier(String identifier) {
        var businessPartnerIdentity = businessPartnerIdentityRepository
            .findByTrustedIdentifier(identifier)
            .orElseThrow(businessPartnerIdentityNotFound());
        return toBusinessPartnerIdentityDto(businessPartnerIdentity);
    }

    private static Supplier<ResourceNotFoundException> businessPartnerIdentityNotFound(UUID id) {
        return () -> new ResourceNotFoundException("No business partner identity found for id %s".formatted(id));
    }

    private static Supplier<ResourceNotFoundException> businessPartnerIdentityNotFound() {
        return () -> new ResourceNotFoundException("No matching business partner identity found");
    }

    private static Supplier<ResourceNotFoundException> protectedVerificationAuthorizationNotFound(UUID id) {
        return () ->
            new ResourceNotFoundException("Protected Verification Authorization found for id %s".formatted(id));
    }

    private void issueAllIdTSForTrustedIdentifiers(BusinessPartnerIdentity bpi) {
        for (var trustedDid : bpi.getTrustedIdentifier()) {
            var statementValidUntil = calculateValidUntilForStatement(
                bpi.getValidUntil(),
                defaultStatementProperties.timeToLive()
            );

            var reqV1 = new IdentityV1RequestDto(
                trustedDid,
                Instant.now(),
                statementValidUntil,
                bpi.getEntityName(),
                bpi.getIsStateActor(),
                toRegistryIdDtoV1List(bpi.getUid())
            );
            trustStatementService.issueAndPublishIdentityV1TrustStatement(bpi.getId(), reqV1);

            var reqV2 = new IdentityV2RequestDto(
                bpi.getId(),
                trustedDid,
                Instant.now(),
                statementValidUntil,
                bpi.getEntityName(),
                bpi.getIsStateActor(),
                toRegistryIdDtoV2List(bpi.getUid())
            );
            trustStatementService.issueAndPublishIdentityV2TrustStatement(reqV2);
        }
    }

    private void issueAllPvaTSForTrustedIdentifiers(BusinessPartnerIdentity bpi) {
        var pvas = protectedVerificationAuthorizationRepository.findAllByBusinessPartnerIdentityId(bpi.getId());

        for (var trustedDid : bpi.getTrustedIdentifier()) {
            var statementValidUntil = calculateValidUntilForStatement(
                bpi.getValidUntil(),
                defaultStatementProperties.timeToLive()
            );

            for (var pva : pvas) {
                var req = new ProtectedVerificationAuthorizationV2RequestDto(
                    bpi.getId(),
                    trustedDid,
                    Instant.now(),
                    statementValidUntil,
                    List.of(
                        toProtectedVerificationAuthorizationV2AuthorizedFieldDto(pva.getProtectedVerificationField())
                    )
                );
                trustStatementService.issueAndPublishProtectedVerificationAuthorizationV2TrustStatement(req);
            }
        }
    }

    private Instant calculateValidUntilFromNow(Period statementValidity) {
        return ZonedDateTime.now().plus(statementValidity).toInstant();
    }

    private Instant calculateValidUntilForStatement(Instant bpiValidUntil, Period statementValidity) {
        var statementValidUntil = calculateValidUntilFromNow(statementValidity);
        return statementValidUntil.isBefore(bpiValidUntil) ? statementValidUntil : bpiValidUntil;
    }

    private void issueAllPiaTSForTrustedIdentifiers(BusinessPartnerIdentity bpi) {
        var authorizations = protectedIssuanceAuthorizationRepository.findAllByBusinessPartnerIdentityId(bpi.getId());
        if (authorizations.isEmpty()) {
            return;
        }

        var statementValidUntil = calculateValidUntilForStatement(
            bpi.getValidUntil(),
            defaultStatementProperties.timeToLive()
        );

        for (var authorization : authorizations) {
            var entry = protectedIssuanceEntryRepository
                .findById(authorization.getProtectedIssuanceEntryId())
                .orElseThrow(() ->
                    new IllegalStateException(
                        "No ProtectedIssuanceEntry found for id %s".formatted(
                            authorization.getProtectedIssuanceEntryId()
                        )
                    )
                );

            var canIssue = new ProtectedIssuanceAuthorizationV2RequestDto.ProtectedIssuanceAuthorizationDto(
                entry.getVct(),
                entry.getName(),
                authorization.getReason()
            );
            for (var trustedDid : bpi.getTrustedIdentifier()) {
                var req = new ProtectedIssuanceAuthorizationV2RequestDto(
                    bpi.getId(),
                    trustedDid,
                    Instant.now(),
                    statementValidUntil,
                    canIssue
                );
                trustStatementService.issueAndPublishProtectedIssuanceAuthorizationV2TrustStatement(
                    req,
                    authorization.getId()
                );
            }
        }
    }
}
