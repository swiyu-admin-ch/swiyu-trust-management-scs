package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.persistence.TransactionManagerNames.MANAGEMENT_TRANSACTION_MANAGER;
import static ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport.getCurrentUserFullName;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.BusinessPartnerIdentityMapper.*;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.ProtectedVerificationAuthorizationMapper.mapPageableWithValidSortProperties;

import ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerIdentityStatus;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.BusinessPartnerIdentityBadRequestException;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ResourceNotFoundException;
import ch.admin.bj.swiyu.trust.management.modules.management.api.*;
import ch.admin.bj.swiyu.trust.management.modules.management.config.DefaultIdentityProperties;
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

    private final BusinessPartnerIdentityRepository businessPartnerIdentityRepository;
    private final DefaultIdentityProperties defaultIdentityProperties;
    private final DefaultStatementProperties defaultStatementProperties;
    private final DomainEventService domainEventService;
    private final OutboxEventPublisher outboxEventPublisher;
    private final ProtectedVerificationRepository protectedVerificationRepository;
    private final TrustStatementPartnerLinkRepository partnerLinkRepository;
    private final TrustStatementService trustStatementService;

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public void activate(UUID businessPartnerId) {
        var bpi = businessPartnerIdentityRepository
            .findById(businessPartnerId)
            .orElseThrow(businessPartnerIdentityNotFound(businessPartnerId));

        bpi.activate(defaultIdentityProperties.validity());

        var event = TiBusinessPartnerIdentityActivatedEventBuilder.create().businessPartnerIdentity(bpi).build();

        outboxEventPublisher.publishBusinessPartnerIdentityActivatedEvent(event);
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public void deactivate(UUID businessPartnerId) {
        var bpi = businessPartnerIdentityRepository
            .findById(businessPartnerId)
            .orElseThrow(businessPartnerIdentityNotFound(businessPartnerId));

        bpi.deactivate();

        var event = TiBusinessPartnerIdentityDeactivatedEventBuilder.create().businessPartnerIdentity(bpi).build();
        outboxEventPublisher.publishBusinessPartnerIdentityDeactivatedEvent(event);
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
            throw new BusinessPartnerIdentityBadRequestException(
                "Business partner identity for id '%s' is not active".formatted(businessPartnerId)
            );
        }

        issueAllIdTSForTrustedIdentifiers(bpi);
        issueAllPvaTSForTrustedIdentifiers(bpi);

        bpi.updateLastIssuance();
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public void renewTrustStatements(UUID businessPartnerId) {
        var bpi = businessPartnerIdentityRepository
            .findById(businessPartnerId)
            .orElseThrow(businessPartnerIdentityNotFound(businessPartnerId));

        if (bpi.getStatus() != BusinessPartnerIdentityStatus.ACTIVE) {
            throw new BusinessPartnerIdentityBadRequestException(
                "Business partner identity for id '%s' is not active".formatted(businessPartnerId)
            );
        }

        outboxEventPublisher.publishBusinessPartnerIdentityUpdatedEvent(
            TiBusinessPartnerIdentityUpdatedEventBuilder.create().businessPartnerIdentity(bpi).build()
        );
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

        outboxEventPublisher.publishBusinessPartnerIdentityUpdatedEvent(
            TiBusinessPartnerIdentityUpdatedEventBuilder.create().businessPartnerIdentity(bpi).build()
        );
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public void syncAll() {
        var bpis = businessPartnerIdentityRepository.findAll();

        for (var bpi : bpis) {
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
        return protectedVerificationRepository
            .findAll(where, mapPageableWithValidSortProperties(pageable))
            .map(BusinessPartnerIdentityMapper::toProtectedVerificationAuthorizationDto);
    }

    @Transactional(readOnly = true, transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public ProtectedVerificationAuthorizationDto getProtectedVerificationAuthorizationDto(
        @Valid @NotNull UUID protectedVerificationAuthorizationId
    ) {
        var pva = protectedVerificationRepository
            .findById(protectedVerificationAuthorizationId)
            .orElseThrow(protectedVerificationAuthorizationNotFound(protectedVerificationAuthorizationId));
        return toProtectedVerificationAuthorizationDto(pva);
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public ProtectedVerificationAuthorizationDto addProtectedVerificationAuthorization(
        @Valid @NotNull ProtectedVerificationAuthorizationRequestDto request
    ) {
        var field = BusinessPartnerIdentityMapper.toProtectedVerificationField(request.protectedField());

        // Uniqueness is enforced at the DB level (unique DID), but we also check here to provide a
        // friendly 400 error instead of a low-level constraint violation.
        var existingEntry =
            protectedVerificationRepository.findAllByBusinessPartnerIdentityIdAndProtectedVerificationField(
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

        var pva = protectedVerificationRepository.save(
            new ProtectedVerificationAuthorization(UUID.randomUUID(), bpi.getId(), field)
        );

        this.domainEventService.protectedVerificationAuthorizationAdded(pva.getId(), getCurrentUserFullName());

        outboxEventPublisher.publishBusinessPartnerIdentityUpdatedEvent(
            TiBusinessPartnerIdentityUpdatedEventBuilder.create().businessPartnerIdentity(bpi).build()
        );

        return toProtectedVerificationAuthorizationDto(pva);
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public void removeProtectedVerificationAuthorization(@Valid @NotNull UUID protectedVerificationAuthorizationId) {
        var pva = protectedVerificationRepository
            .findById(protectedVerificationAuthorizationId)
            .orElseThrow(protectedVerificationAuthorizationNotFound(protectedVerificationAuthorizationId));
        var bpi = getBusinessPartnerIdentity(pva.getBusinessPartnerIdentityId());

        protectedVerificationRepository.delete(pva);

        outboxEventPublisher.publishBusinessPartnerIdentityUpdatedEvent(
            TiBusinessPartnerIdentityUpdatedEventBuilder.create().businessPartnerIdentity(bpi).build()
        );
    }

    private static Supplier<ResourceNotFoundException> businessPartnerIdentityNotFound(UUID id) {
        return () -> new ResourceNotFoundException("No business partner identity found for id %s".formatted(id));
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
        var pvas = protectedVerificationRepository.findAllByBusinessPartnerIdentityId(bpi.getId());

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
                        BusinessPartnerIdentityMapper.toProtectedVerificationAuthorizationV2AuthorizedFieldDto(
                            pva.getProtectedVerificationField()
                        )
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
}
