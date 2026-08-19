package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.persistence.TransactionManagerNames.MANAGEMENT_TRANSACTION_MANAGER;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.ProtectedVerificationAuthorizationMapper.mapPageableWithValidSortProperties;

import ch.admin.bj.swiyu.trust.management.modules.common.exception.ResourceNotFoundException;
import ch.admin.bj.swiyu.trust.management.modules.management.api.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.event.TiBusinessPartnerIdentityUpdatedEventBuilder;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.OutboxEventPublisher;
import com.querydsl.core.BooleanBuilder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
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
public class ProtectedIssuanceService {

    private final ProtectedIssuanceEntryRepository protectedIssuanceEntryRepository;
    private final ProtectedIssuanceAuthorizationRepository protectedIssuanceAuthorizationRepository;
    private final BusinessPartnerIdentityRepository businessPartnerIdentityRepository;
    private final DomainEventService domainEventService;
    private final OutboxEventPublisher outboxEventPublisher;
    private final TrustStatementPartnerLinkRepository trustStatementPartnerLinkRepository;
    private final TrustStatementService trustStatementService;

    // ------------------------------------------------------------------
    // ProtectedIssuanceEntry operations
    // ------------------------------------------------------------------

    @Transactional(readOnly = true, transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public ProtectedIssuanceEntryDto getProtectedIssuanceEntry(@NotNull UUID id) {
        var protectedIssuanceEntry = this.protectedIssuanceEntryRepository.findById(id).orElseThrow(
            protectedIssuanceEntryNotFound(id)
        );
        return ProtectedIssuanceEntryMapper.toProtectedIssuanceEntryDto(protectedIssuanceEntry);
    }

    @Transactional(readOnly = true, transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public Page<ProtectedIssuanceEntryDto> listProtectedIssuanceEntries(
        @Valid @NotNull ProtectedIssuanceEntryFilterDto filters,
        @Valid @NotNull Pageable pageable
    ) {
        var q = QProtectedIssuanceEntry.protectedIssuanceEntry;
        BooleanBuilder where = new BooleanBuilder();
        if (filters.vct() != null && !filters.vct().isBlank()) {
            where.and(q.vct.containsIgnoreCase(filters.vct()));
        }
        return this.protectedIssuanceEntryRepository.findAll(where, pageable).map(
            ProtectedIssuanceEntryMapper::toProtectedIssuanceEntryDto
        );
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public ProtectedIssuanceEntryDto createProtectedIssuanceEntry(
        @Valid @NotNull ProtectedIssuanceEntryCreateRequestDto request,
        String currentUserFullName
    ) {
        var preExistingEntry = this.protectedIssuanceEntryRepository.findByVct(request.vct());
        if (preExistingEntry.isPresent()) {
            throw new IllegalArgumentException(
                "Validation failed: ProtectedIssuanceEntry with given vct '%s' already exists on id %s.".formatted(
                    request.vct(),
                    preExistingEntry.get().getId()
                )
            );
        }
        var protectedIssuanceEntry = new ProtectedIssuanceEntry(
            UUID.randomUUID(),
            request.vct(),
            Instant.now(),
            request.name()
        );
        var saved = this.protectedIssuanceEntryRepository.save(protectedIssuanceEntry);
        this.domainEventService.protectedIssuanceEntryAdded(saved.getId(), currentUserFullName);
        return ProtectedIssuanceEntryMapper.toProtectedIssuanceEntryDto(saved);
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public void deleteProtectedIssuanceEntry(@Valid @NotNull UUID id, String currentUserFullName) {
        var authorizations = protectedIssuanceAuthorizationRepository.findAllByProtectedIssuanceEntryId(id);
        for (var authorization : authorizations) {
            removeAuthorization(authorization.getId(), currentUserFullName);
        }
        this.protectedIssuanceEntryRepository.deleteById(id);
        this.domainEventService.protectedIssuanceEntryRemoved(id, currentUserFullName);
    }

    @Transactional(readOnly = true, transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public Page<ProtectedIssuanceAuthorizationDto> getProtectedIssuanceAuthorizationsPaged(
        ProtectedIssuanceAuthorizationFilterDto filters,
        Pageable pageable
    ) {
        var q = QProtectedIssuanceAuthorization.protectedIssuanceAuthorization;
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
        return protectedIssuanceAuthorizationRepository
            .findAll(where, mapPageableWithValidSortProperties(pageable))
            .map(auth -> {
                var entry = protectedIssuanceEntryRepository.findById(auth.getProtectedIssuanceEntryId()).orElse(null);
                return ProtectedIssuanceEntryMapper.toProtectedIssuanceAuthorizationDto(auth, entry);
            });
    }

    @Transactional(readOnly = true, transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public ProtectedIssuanceAuthorizationDto getAuthorization(@NotNull UUID protectedIssuanceAuthorizationId) {
        var authorization = protectedIssuanceAuthorizationRepository
            .findById(protectedIssuanceAuthorizationId)
            .orElseThrow(protectedIssuanceAuthorizationNotFound(protectedIssuanceAuthorizationId));
        var entry = protectedIssuanceEntryRepository.findById(authorization.getProtectedIssuanceEntryId()).orElse(null);
        return ProtectedIssuanceEntryMapper.toProtectedIssuanceAuthorizationDto(authorization, entry);
    }

    // ------------------------------------------------------------------
    // ProtectedIssuanceAuthorization operations
    // ------------------------------------------------------------------

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public ProtectedIssuanceAuthorizationDto addAuthorization(
        @NotNull ProtectedIssuanceAuthorizationCreateRequestDto request,
        String currentUserFullName
    ) {
        var protectedIssuanceEntryId = request.protectedIssuanceEntryId();
        var businessPartnerIdentityId = request.businessPartnerIdentityId();

        var entry = protectedIssuanceEntryRepository
            .findById(protectedIssuanceEntryId)
            .orElseThrow(protectedIssuanceEntryNotFound(protectedIssuanceEntryId));
        var bpi = businessPartnerIdentityRepository
            .findById(businessPartnerIdentityId)
            .orElseThrow(businessPartnerIdentityNotFound(businessPartnerIdentityId));

        var authorization = new ProtectedIssuanceAuthorization(
            UUID.randomUUID(),
            businessPartnerIdentityId,
            protectedIssuanceEntryId,
            request.reason()
        );
        var saved = protectedIssuanceAuthorizationRepository.save(authorization);

        domainEventService.protectedIssuanceAuthorizationAdded(
            saved.getId(),
            businessPartnerIdentityId,
            currentUserFullName
        );

        var event = TiBusinessPartnerIdentityUpdatedEventBuilder.create().businessPartnerIdentity(bpi).build();
        outboxEventPublisher.publishBusinessPartnerIdentityUpdatedEvent(event);

        log.info(
            "Added ProtectedIssuanceAuthorization {} for BPI {} on entry {}",
            saved.getId(),
            businessPartnerIdentityId,
            protectedIssuanceEntryId
        );
        return ProtectedIssuanceEntryMapper.toProtectedIssuanceAuthorizationDto(saved, entry);
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public void removeAuthorization(@NotNull UUID protectedIssuanceAuthorizationId, String currentUserFullName) {
        var authorization = protectedIssuanceAuthorizationRepository
            .findById(protectedIssuanceAuthorizationId)
            .orElseThrow(protectedIssuanceAuthorizationNotFound(protectedIssuanceAuthorizationId));

        var businessPartnerIdentityId = authorization.getBusinessPartnerIdentityId();

        domainEventService.protectedIssuanceAuthorizationRemoved(
            protectedIssuanceAuthorizationId,
            businessPartnerIdentityId,
            currentUserFullName
        );

        // Revoke all active piaTS linked to this authorization immediately before deletion
        var activePiaTS = trustStatementPartnerLinkRepository.findAllByProtectedIssuanceAuthorizationIdAndStatus(
            protectedIssuanceAuthorizationId,
            TrustStatementPartnerLinkStatus.ACTIVE
        );
        for (var partnerLink : activePiaTS) {
            trustStatementService.deactivateTrustStatement(
                partnerLink.getId(),
                new DeactivationRequestDto("ProtectedIssuanceAuthorization removed")
            );
        }

        protectedIssuanceAuthorizationRepository.deleteById(protectedIssuanceAuthorizationId);

        var bpi = businessPartnerIdentityRepository
            .findById(businessPartnerIdentityId)
            .orElseThrow(businessPartnerIdentityNotFound(businessPartnerIdentityId));
        var event = TiBusinessPartnerIdentityUpdatedEventBuilder.create().businessPartnerIdentity(bpi).build();
        outboxEventPublisher.publishBusinessPartnerIdentityUpdatedEvent(event);

        log.info(
            "Removed ProtectedIssuanceAuthorization {} for BPI {}",
            protectedIssuanceAuthorizationId,
            businessPartnerIdentityId
        );
    }

    private static Supplier<ResourceNotFoundException> businessPartnerIdentityNotFound(UUID id) {
        return () -> new ResourceNotFoundException("No BusinessPartnerIdentity found for id %s".formatted(id));
    }

    private static Supplier<ResourceNotFoundException> protectedIssuanceEntryNotFound(UUID id) {
        return () -> new ResourceNotFoundException("No ProtectedIssuanceEntry found for id %s".formatted(id));
    }

    private static Supplier<ResourceNotFoundException> protectedIssuanceAuthorizationNotFound(UUID id) {
        return () -> new ResourceNotFoundException("No ProtectedIssuanceAuthorization found for id %s".formatted(id));
    }
}
