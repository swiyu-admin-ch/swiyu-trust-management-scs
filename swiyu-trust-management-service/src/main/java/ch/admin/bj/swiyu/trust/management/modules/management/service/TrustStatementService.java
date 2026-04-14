package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.client.issuer.management.model.CredentialStatusTypeDto.REVOKED;
import static ch.admin.bj.swiyu.trust.management.modules.common.persistence.TransactionManagerNames.MANAGEMENT_TRANSACTION_MANAGER;
import static ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLink.*;
import static ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer.IssuerClient.TrustStatementIssuanceResult;
import static ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer.IssuerCredentialRequestFactory.createIssuerCredentialRequest;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.TrustStatementMapper.*;

import ch.admin.bj.swiyu.trust.management.modules.common.exception.ResourceNotFoundException;
import ch.admin.bj.swiyu.trust.management.modules.management.api.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer.IssuanceException;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer.IssuerClient;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer.IssuerVcStatus;
import ch.admin.bj.swiyu.trust.management.modules.registry.api.DatastoreDto;
import ch.admin.bj.swiyu.trust.management.modules.registry.api.DatastoreStatusDto;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.TrustRegistryService;
import com.querydsl.core.BooleanBuilder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class TrustStatementService {

    private final IssuerClient issuerClient;
    private final TrustRegistryService trustRegistryService;
    private final TrustStatementPartnerLinkRepository trustStatementPartnerLinkRepository;
    private final TrustStatementPartnerLinkValidator trustStatementPartnerLinkValidator;

    public TrustStatementService(
        IssuerClient issuerClient,
        TrustRegistryService trustRegistryService,
        TrustStatementPartnerLinkRepository trustStatementPartnerLinkRepository,
        TrustStatementPartnerLinkValidator trustStatementPartnerLinkValidator
    ) {
        this.issuerClient = issuerClient;
        this.trustRegistryService = trustRegistryService;
        this.trustStatementPartnerLinkRepository = trustStatementPartnerLinkRepository;
        this.trustStatementPartnerLinkValidator = trustStatementPartnerLinkValidator;
    }

    @Transactional(readOnly = true, transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public Page<TrustStatementPartnerLinkListItemDto> getPartnerLinks(
        TrustStatementPartnerLinkFilterDto filter,
        Pageable pageable
    ) {
        var q = QTrustStatementPartnerLink.trustStatementPartnerLink;
        var where = new BooleanBuilder();
        if (filter.subject() != null) {
            where.and(q.subject.like(filter.subject()));
        }
        if (filter.type() != null) {
            where.and(q.type.eq(toTrustStatementType(filter.type())));
        }
        if (filter.status() != null) {
            where.and(q.status.eq(toTrustStatementPartnerLinkStatus(filter.status())));
        }
        if (filter.createdBy() != null) {
            where.and(q.audit.createdBy.like(filter.createdBy()));
        }
        if (filter.lastModifiedBy() != null) {
            where.and(q.audit.lastModifiedBy.like(filter.lastModifiedBy()));
        }
        return trustStatementPartnerLinkRepository
            .findAll(where, mapPageableWithValidSortProperties(pageable))
            .map(TrustStatementMapper::toTrustStatementPartnerLinkListItemDto);
    }

    @Transactional(readOnly = true, transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public TrustStatementPartnerLinkDto getPartnerLink(UUID submissionId) {
        var partnerLink = trustStatementPartnerLinkRepository
            .findById(submissionId)
            .orElseThrow(partnerLinkNotFound(submissionId));
        return toTrustStatementPartnerLinkDto(partnerLink);
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public TrustStatementPartnerLinkDto deactivateTrustStatement(
        UUID partnerLinkId,
        TrustStatementPartnerLinkDeactivationRequestDto request
    ) {
        var partnerLink = trustStatementPartnerLinkRepository
            .findById(partnerLinkId)
            .orElseThrow(partnerLinkNotFound(partnerLinkId));
        deactivateTrustStatement(partnerLink, request.getReason());
        return toTrustStatementPartnerLinkDto(partnerLink);
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public TrustStatementPartnerLinkDto issueAndPublishMetadataTrustStatement(
        TrustStatementPartnerLinkMetadataV1RequestDto request
    ) {
        log.debug("Creating metadata v1 trust statement partnerLink");
        var partnerLink = this.issueAndPublishTrustStatement(
            createMetadataV1(
                request.getSubject(),
                request.getValidFrom(),
                request.getValidUntil(),
                toMetadataV1Language(request.getPreferredLanguage()),
                toMetadataV1LanguageMap(request.getOrgName()),
                toMetadataV1LanguageMap(request.getLogoUri())
            )
        );
        return findPartnerLinkById(partnerLink.getId());
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER, propagation = Propagation.REQUIRES_NEW)
    public TrustStatementPartnerLinkDto issueAndPublishIdentityTrustStatement(
        UUID partnerId,
        @Valid TrustStatementPartnerLinkIdentityV1RequestDto request
    ) {
        log.debug("Creating identity v1 trust statement partner link");
        var partnerLink = this.issueAndPublishTrustStatement(
            createIdentityV1(
                partnerId,
                request.getSubject(),
                request.getValidFrom(),
                request.getValidUntil(),
                toIdentityV1LanguageMap(request.getEntityName()),
                toIdentityV1RegistryIds(request.getRegistryIds()),
                request.getIsStateActor()
            )
        );
        return findPartnerLinkById(partnerLink.getId());
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public TrustStatementPartnerLinkDto issueAndPublishIssuanceTrustStatement(
        @NotNull UUID partnerId,
        @Valid TrustStatementPartnerLinkIssuanceV1RequestDto request
    ) {
        log.debug("Creating issuance v1 trust statement partner link");
        var partnerLink = this.issueAndPublishTrustStatement(
            createIssuanceV1(
                partnerId,
                request.getSubject(),
                request.getValidFrom(),
                request.getValidUntil(),
                request.getCanIssue()
            )
        );
        return findPartnerLinkById(partnerLink.getId());
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public TrustStatementPartnerLinkDto issueAndPublishVerificationTrustStatement(
        @NotNull UUID partnerId,
        @Valid TrustStatementPartnerLinkVerificationV1RequestDto request
    ) {
        log.debug("Creating verification v1 trust statement partner link");
        var partnerLink = this.issueAndPublishTrustStatement(
            createVerificationV1(
                partnerId,
                request.getSubject(),
                request.getValidFrom(),
                request.getValidUntil(),
                request.getCanVerify()
            )
        );
        return findPartnerLinkById(partnerLink.getId());
    }

    private TrustStatementPartnerLinkDto findPartnerLinkById(UUID partnerLinkId) {
        var statement = trustStatementPartnerLinkRepository
            .findById(partnerLinkId)
            .orElseThrow(partnerLinkNotFound(partnerLinkId));
        return toTrustStatementPartnerLinkDto(statement);
    }

    private TrustStatementPartnerLink issueAndPublishTrustStatement(TrustStatementPartnerLink partnerLink) {
        // save first, since we need an id for validation and publication
        partnerLink = trustStatementPartnerLinkRepository.save(partnerLink);
        trustStatementPartnerLinkValidator.validateSubmission(partnerLink);
        // 1/2: create vc from metadata at gov trust issuer
        var issuanceResult = issueTrustStatement(partnerLink);
        // 2/2: publish vc to registry
        publishTrustStatement(issuanceResult, partnerLink);
        return trustStatementPartnerLinkRepository.saveAndFlush(partnerLink);
    }

    private TrustStatementIssuanceResult issueTrustStatement(TrustStatementPartnerLink statement) {
        try {
            var request = createIssuerCredentialRequest(statement, List.of(issuerClient.getStatusListUri()));
            log.info("Issuing metadata v1 trust statement {}", statement.getId());
            return issuerClient.issueTrustStatement(request);
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to issue trust statement %s at gov trust issuer".formatted(statement.getId()),
                e
            );
        }
    }

    /**
     * Issues and publishes the trust statement to the trust registry, updates the entity with the relevant ids/status
     * DOES NOT validate the request.
     */
    private void publishTrustStatement(
        TrustStatementIssuanceResult issuanceResult,
        TrustStatementPartnerLink partnerLink
    ) {
        try {
            log.info("Processing metadata trust statement {}", partnerLink.getId());

            // 1/2: publish the trust statement vc to the trust registry
            DatastoreDto registryEntry;
            if (partnerLink.getTrustRegistryEntryId() == null) {
                registryEntry = trustRegistryService.createTrustStatementVc(issuanceResult.encodedVc());
            } else {
                registryEntry = trustRegistryService.updateTrustStatementVc(
                    partnerLink.getTrustRegistryEntryId(),
                    issuanceResult.encodedVc()
                );
            }
            if (!registryEntry.isActive()) {
                throw new IssuanceException(
                    "Failed to publish Trust statement. Registry did not confirm upload. Status: %s".formatted(
                        registryEntry.status()
                    )
                );
            }

            // 2/2: update the trust statement as issued and published
            var status = TrustStatementPartnerLinkStatus.ACTIVE;
            var issuerVcStatus = lookupIssuerVcStatus(partnerLink);
            if (issuerVcStatus.isInvalid()) {
                status = TrustStatementPartnerLinkStatus.INACTIVE;
            }
            partnerLink.persistReferencesAfterPublicationSucceeded(
                issuanceResult.managementId(),
                registryEntry.id(),
                status
            );
            log.info("Processing metadata trust statement {} done.", partnerLink.getId());
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to publish already issued Trust statement with id %s and issuer management id %s".formatted(
                    partnerLink.getId(),
                    issuanceResult.managementId()
                ),
                e
            );
        }
    }

    private void deactivateTrustStatement(TrustStatementPartnerLink partnerLink, String reason) {
        // 1/3: set vc status to revoked on issuer side
        var issuerVcStatus = lookupIssuerVcStatus(partnerLink);
        if (!issuerVcStatus.isInvalid()) {
            issuerClient.updateCredentialStatus(partnerLink.getTrustIssuerCredentialId(), REVOKED);
        }

        // 2/3: deactivate vc on trust registry side
        var trustRegistryStatus = trustRegistryService.getStatus(partnerLink.getTrustRegistryEntryId());
        if (!trustRegistryStatus.isDeactivated()) {
            trustRegistryService.deactivate(partnerLink.getTrustRegistryEntryId());
        }

        // 3/3: mark trust statement as inactive
        partnerLink.markAsInactive();
        trustStatementPartnerLinkRepository.saveAndFlush(partnerLink);
    }

    private IssuerVcStatus lookupIssuerVcStatus(TrustStatementPartnerLink statement) {
        if (statement.getTrustIssuerCredentialId() == null) {
            return IssuerVcStatus.UNKNOWN;
        }

        // As long as we have no real VC validator library we use this approximation
        // If now < validUntil then the VC should be expired
        if (Instant.now().compareTo(statement.getValidUntil()) > 0) {
            return IssuerVcStatus.INVALID;
        }

        var status = issuerClient.getCredentialStatus(statement.getTrustIssuerCredentialId());
        return switch (status) {
            case ISSUED -> IssuerVcStatus.VALID;
            case null -> IssuerVcStatus.UNKNOWN;
            default -> IssuerVcStatus.INVALID;
        };
    }

    private DatastoreStatusDto lookupTrustRegistryStatus(TrustStatementPartnerLink partnerLink) {
        if (partnerLink.getTrustRegistryEntryId() == null) {
            // since it is possible to save a trust statement that is not published in registry (when in status
            // PREPARATION) the id can be null. When this happens there is no need to look up the status.
            return null;
        }
        return trustRegistryService.getStatus(partnerLink.getTrustRegistryEntryId());
    }

    private TrustStatementPartnerLinkDto toTrustStatementPartnerLinkDto(TrustStatementPartnerLink partnerLink) {
        var issuerVcStatus = lookupIssuerVcStatus(partnerLink);
        var trustRegistryStatus = lookupTrustRegistryStatus(partnerLink);
        return TrustStatementMapper.toTrustStatementPartnerLinkDto(partnerLink, issuerVcStatus, trustRegistryStatus);
    }

    private static Supplier<ResourceNotFoundException> partnerLinkNotFound(UUID id) {
        return () -> new ResourceNotFoundException("No partner link found for id %s".formatted(id));
    }
}
