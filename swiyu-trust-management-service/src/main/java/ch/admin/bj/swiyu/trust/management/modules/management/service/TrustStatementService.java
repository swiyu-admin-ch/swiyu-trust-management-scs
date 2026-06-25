package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.client.issuer.management.model.CredentialStatusTypeDto.REVOKED;
import static ch.admin.bj.swiyu.trust.management.modules.common.date.DateTimeHelper.today;
import static ch.admin.bj.swiyu.trust.management.modules.common.persistence.TransactionManagerNames.MANAGEMENT_TRANSACTION_MANAGER;
import static ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLink.*;
import static ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkStatus.ACTIVE;
import static ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementPartnerLinkType.*;
import static ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer.IssuerClient.TrustStatementIssuanceResult;
import static ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer.IssuerCredentialRequestFactory.createIssuerCredentialRequest;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.TrustStatementMapper.*;

import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditMapper;
import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.IssuanceException;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ResourceNotFoundException;
import ch.admin.bj.swiyu.trust.management.modules.common.i18n.ValidLocalizedMapValidator;
import ch.admin.bj.swiyu.trust.management.modules.management.api.*;
import ch.admin.bj.swiyu.trust.management.modules.management.config.statements.DefaultStatementProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness.IssuerTrustRootProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementPartnerLinkType;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer.IssuerClient;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer.IssuerVcStatus;
import ch.admin.bj.swiyu.trust.management.modules.registry.api.DatastoreDto;
import ch.admin.bj.swiyu.trust.management.modules.registry.api.DatastoreStatusDto;
import ch.admin.bj.swiyu.trust.management.modules.registry.api.StatementTypeDto;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.TrustRegistryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.BooleanBuilder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrustStatementService {

    private final ObjectMapper objectMapper;
    private final IssuerClient issuerClient;
    private final TrustRegistryService trustRegistryService;
    private final TrustStatementPartnerLinkRepository trustStatementPartnerLinkRepository;
    private final TrustStatementPartnerLinkValidator trustStatementPartnerLinkValidator;
    private final JwtStatementDomainService jwtStatementService;
    private final StatusListDomainService statusListDomainService;
    private final StatusListMetadataRepository statusListMetadataRepository;
    private final IssuerTrustRootProperties issuerTrustRootProperties;
    private final DefaultStatementProperties defaultStatementProperties;
    private final NonCompliantActorRepository nonCompliantActorRepository;
    private final ProtectedIssuanceEntryRepository protectedIssuanceEntryRepository;
    private final AuditPublisher auditPublisher;

    private final String singletonStatementReason = "Deactivate statement due to re-issuing of singleton statement.";

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
    public TrustStatementPartnerLinkDto deactivateTrustStatement(UUID partnerLinkId, DeactivationRequestDto request) {
        var partnerLink = trustStatementPartnerLinkRepository
            .findById(partnerLinkId)
            .orElseThrow(partnerLinkNotFound(partnerLinkId));
        return deactivateTrustStatement(partnerLink, request);
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER, propagation = Propagation.REQUIRES_NEW)
    public TrustStatementPartnerLinkDto issueAndPublishIdentityV1TrustStatement(
        UUID partnerId,
        @Valid IdentityV1RequestDto request
    ) {
        log.debug("Creating identity v1 trust statement partner link");
        var result = this.issueAndPublishTrustStatement(
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
        return findPartnerLinkById(result.partnerLink().getId());
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER, propagation = Propagation.REQUIRES_NEW)
    public TrustStatementPartnerLinkDto issueAndPublishIdentityV2TrustStatement(@Valid IdentityV2RequestDto request) {
        log.debug("Creating identity v2 trust statement partner link");
        var result = this.issueAndPublishTrustStatement(
            createIdentityV2(
                request.getBusinessPartnerId(),
                request.getSubject(),
                request.getValidFrom(),
                request.getValidUntil(),
                toIdentityV2LanguageMap(request.getEntityName()),
                toIdentityV2RegistryIds(request.getRegistryIds()),
                request.getIsStateActor(),
                statusListDomainService.getNewStatusListEntry()
            )
        );
        return findPartnerLinkById(result.partnerLink().getId());
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER, propagation = Propagation.REQUIRES_NEW)
    public VerificationQueryPublicationResult issueAndPublishVerificationQueryV2TrustStatement(
        @Valid VerificationQueryV2RequestDto request
    ) {
        ValidLocalizedMapValidator.validateLocalizedMap(request.getPurposeName());
        ValidLocalizedMapValidator.validateLocalizedMap(request.getPurposeDescription());
        log.debug("Creating VerificationQuery trust statement partner link");
        var result = this.issueAndPublishTrustStatement(
            createVerificationQueryV2(
                request.getBusinessPartnerId(),
                request.getSubject(),
                request.getValidFrom(),
                request.getValidUntil(),
                toVerificationQueryV2LanguageMap(request.getPurposeName()),
                toVerificationQueryV2LanguageMap(request.getPurposeDescription()),
                toVerificationQueryV2VerificationRequestObject(request.getRequest())
            )
        );
        return new VerificationQueryPublicationResult(
            findPartnerLinkById(result.partnerLink().getId()),
            result.encodedVc()
        );
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public TrustStatementPartnerLinkDto issueAndPublishNonComplianceV2TrustListStatement() {
        // 1/3: deactivate previous active non-compliance-list statements since
        // we re-publish based on current setting of non-compliant actors
        log.debug("Deactivate old TrustListStatementNonComplianceV2 statements since a re-publication is happening...");

        // 2/3: issue and publish new ones
        log.debug("Issue and publish NonComplianceV2TrustListStatement trust statement partner link");
        var nonCompliantActors = this.nonCompliantActorRepository.findAll();
        var result = this.issueAndPublishTrustStatement(
            createNonComplianceV2(
                issuerTrustRootProperties.businessPartnerId(),
                today().toInstant(),
                today().plus(defaultStatementProperties.nonComplianceTrustListStatement().timeToLive()).toInstant(),
                nonCompliantActors,
                statusListDomainService.getNewStatusListEntry()
            )
        );

        // 3/3: since we disabled some statements, we need to re-publish the statuslist the statements are attached to
        deactivateAllStatementsOfTypeExcept(
            TRUST_LIST_STATEMENT_NON_COMPLIANCE_V2,
            singletonStatementReason,
            result.partnerLink().getId()
        );
        return toTrustStatementPartnerLinkDto(result.partnerLink());
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER, propagation = Propagation.REQUIRES_NEW)
    public TrustStatementPartnerLinkDto issueAndPublishProtectedVerificationAuthorizationV2TrustStatement(
        @Valid ProtectedVerificationAuthorizationV2RequestDto request
    ) {
        log.debug("Creating ProtectedVerificationAuthorizationV2 trust statement partner link");
        var result = this.issueAndPublishTrustStatement(
            createProtectedVerificationAuthorizationV2(
                request.getBusinessPartnerId(),
                request.getSubject(),
                request.getValidFrom(),
                request.getValidUntil(),
                TrustStatementMapper.toProtectedVerificationAuthorizationV2AuthorizableField(
                    request.getAuthorizedFields()
                ),
                statusListDomainService.getNewStatusListEntry()
            )
        );
        return findPartnerLinkById(result.partnerLink().getId());
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER, propagation = Propagation.REQUIRES_NEW)
    public TrustStatementPartnerLinkDto issueAndPublishProtectedIssuanceAuthorizationV2TrustStatement(
        @Valid ProtectedIssuanceAuthorizationV2RequestDto request
    ) {
        log.debug("Creating ProtectedVIssuanceAuthorizationV2 trust statement partner link");
        var result = this.issueAndPublishTrustStatement(
            createProtectedIssuanceAuthorizationV2(
                request.getBusinessPartnerId(),
                request.getSubject(),
                request.getValidFrom(),
                request.getValidUntil(),
                TrustStatementMapper.toProtectedIssuanceAuthorizationDto(request.getCanIssue()),
                statusListDomainService.getNewStatusListEntry()
            )
        );
        return findPartnerLinkById(result.partnerLink().getId());
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public TrustStatementPartnerLinkDto issueAndPublishIssuanceV1TrustStatement(
        @NotNull UUID partnerId,
        @Valid IssuanceV1RequestDto request
    ) {
        log.debug("Creating issuance v1 trust statement partner link");
        var result = this.issueAndPublishTrustStatement(
            createIssuanceV1(
                partnerId,
                request.getSubject(),
                request.getValidFrom(),
                request.getValidUntil(),
                request.getCanIssue()
            )
        );
        return findPartnerLinkById(result.partnerLink().getId());
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public TrustStatementPartnerLinkDto issueAndPublishVerificationV1TrustStatement(
        @NotNull UUID partnerId,
        @Valid VerificationV1RequestDto request
    ) {
        log.debug("Creating verification v1 trust statement partner link");
        var result = this.issueAndPublishTrustStatement(
            createVerificationV1(
                partnerId,
                request.getSubject(),
                request.getValidFrom(),
                request.getValidUntil(),
                request.getCanVerify()
            )
        );
        return findPartnerLinkById(result.partnerLink().getId());
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public TrustStatementPartnerLinkDto issueAndPublishProtectedIssuanceV2TrustListStatement() {
        // 1/3: delete old statements that become inactive
        log.debug("Deactivate old ProtectedIssuanceV2TrustListStatement partner link");
        var protectedElements = this.protectedIssuanceEntryRepository.findAll();

        // 2/3: issue and publish new statements
        log.debug("Creating ProtectedIssuanceV2TrustListStatement trust statement partner link");
        var result = this.issueAndPublishTrustStatement(
            createProtectedIssuanceV2(
                issuerTrustRootProperties.businessPartnerId(),
                today().toInstant(),
                today().plus(defaultStatementProperties.protectedIssuanceTrustListStatement().timeToLive()).toInstant(),
                ProtectedIssuanceEntryMapper.toProtectedIssuanceV2Details(protectedElements),
                statusListDomainService.getNewStatusListEntry()
            )
        );
        // 3/3: since we disabled some statements, we need to re-publish the statuslist the statements are attached to
        log.debug("Publish deactivation of old ProtectedIssuanceV2TrustListStatements");
        deactivateAllStatementsOfTypeExcept(
            TRUST_LIST_STATEMENT_PROTECTED_ISSUANCE_V2,
            singletonStatementReason,
            result.partnerLink().getId()
        );
        return toTrustStatementPartnerLinkDto(result.partnerLink());
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public void deactivateAllStatementsOfTypeAndSubjectExcept(
        TrustStatementTypeDto statementType,
        String reason,
        UUID partnerLinkIdToPreserve,
        String subject
    ) {
        var oldStatements = this.trustStatementPartnerLinkRepository.findAllBySubjectAndTypeInAndStatus(
                subject,
                List.of(TrustStatementMapper.toTrustStatementType(statementType)),
                ACTIVE
            )
            .stream()
            .filter(a -> a.getId().compareTo(partnerLinkIdToPreserve) != 0)
            .toList();
        log.debug("deactivating {} statements of type {}", oldStatements.size(), statementType);
        for (var activeStatement : oldStatements) {
            deactivateTrustStatement(activeStatement, new DeactivationRequestDto(reason));
        }
    }

    private static Supplier<ResourceNotFoundException> partnerLinkNotFound(UUID id) {
        return () -> new ResourceNotFoundException("No partner link found for id %s".formatted(id));
    }

    private static String auditBusinessPartnerId(TrustStatementPartnerLink partnerLink) {
        return partnerLink.getPartnerId() != null ? partnerLink.getPartnerId().toString() : "";
    }

    private TrustStatementPartnerLinkDto deactivateTrustStatement(
        TrustStatementPartnerLink partnerLink,
        DeactivationRequestDto request
    ) {
        switch (partnerLink.getType()) {
            case
                TRUST_STATEMENT_IDENTITY_V1,
                TRUST_STATEMENT_ISSUANCE_V1,
                TRUST_STATEMENT_VERIFICATION_V1 -> deactivateTrustStatementV1(partnerLink, request.getReason());
            default -> deactivateTrustStatementV2(partnerLink, request.getReason());
        }

        // Only audit jwt for v2 statements
        var serializedJwt = "";
        if (partnerLink.isV2PartnerLink()) {
            serializedJwt = trustRegistryService.getStatement(partnerLink.getTrustRegistryEntryId()).serialized();
        }

        auditPublisher.deactivateTrustStatement(
            partnerLink.getId().toString(),
            auditBusinessPartnerId(partnerLink),
            partnerLink.getType().name(),
            0L, // partnerLink has no version field
            AuditMapper.toAuditJson(partnerLink),
            serializedJwt
        );
        return toTrustStatementPartnerLinkDto(partnerLink);
    }

    @NotNull
    private List<TrustStatementPartnerLink> deactivateAllStatementsOfTypeExcept(
        TrustStatementPartnerLinkType statementType,
        String reason,
        UUID partnerLinkId
    ) {
        var oldStatements = this.trustStatementPartnerLinkRepository.findAllByTypeAndStatus(statementType, ACTIVE)
            .stream()
            .filter(a -> a.getId().compareTo(partnerLinkId) != 0)
            .toList();
        log.debug("deactivating {} statements of type {}", oldStatements.size(), statementType);
        for (var activeStatement : oldStatements) {
            deactivateTrustStatement(activeStatement, new DeactivationRequestDto(reason));
        }
        return oldStatements;
    }

    private TrustStatementPartnerLinkDto findPartnerLinkById(UUID partnerLinkId) {
        var statement = trustStatementPartnerLinkRepository
            .findById(partnerLinkId)
            .orElseThrow(partnerLinkNotFound(partnerLinkId));
        return toTrustStatementPartnerLinkDto(statement);
    }

    private PartnerLinkIssuanceResult issueAndPublishTrustStatement(TrustStatementPartnerLink partnerLink) {
        // save first, since we need an id for validation and publication
        partnerLink = trustStatementPartnerLinkRepository.save(partnerLink);
        log.debug("issue and publish trust statement {} of type {}", partnerLink.getId(), partnerLink.getType());
        log.debug("{}", partnerLink.getDetails());
        trustStatementPartnerLinkValidator.validateSubmission(partnerLink);
        // 1/2: create vc from metadata at gov trust issuer
        var issuanceResult = issueTrustStatement(partnerLink);
        // 2/2: publish vc to registry
        publishTrustStatement(issuanceResult, partnerLink);
        partnerLink = trustStatementPartnerLinkRepository.saveAndFlush(partnerLink);

        auditPublisher.publishTrustStatement(
            partnerLink.getId().toString(),
            auditBusinessPartnerId(partnerLink),
            partnerLink.getType().name(),
            0L, // partnerLink has no version field
            AuditMapper.toAuditJson(partnerLink),
            issuanceResult.encodedVc()
        );
        return new PartnerLinkIssuanceResult(partnerLink, issuanceResult.encodedVc());
    }

    private TrustStatementIssuanceResult issueTrustStatement(TrustStatementPartnerLink statement) {
        return switch (statement.getType()) {
            case TRUST_STATEMENT_IDENTITY_V2 -> {
                var jwt = jwtStatementService.generateIdentityTrustStatement(
                    statement,
                    statusListMetadataRepository
                        .findById(statement.getStatusListMetadataId())
                        .orElseThrow(IllegalStateException::new)
                );
                yield new TrustStatementIssuanceResult(null, jwt);
            }
            case TRUST_STATEMENT_PROTECTED_VERIFICATION_AUTHORIZATION_V2 -> {
                var jwt = jwtStatementService.generateProtectedVerificationAuthorizationTrustStatement(
                    statement,
                    statusListMetadataRepository
                        .findById(statement.getStatusListMetadataId())
                        .orElseThrow(IllegalStateException::new)
                );
                yield new TrustStatementIssuanceResult(null, jwt);
            }
            case TRUST_STATEMENT_PROTECTED_ISSUANCE_AUTHORIZATION_V2 -> {
                var jwt = jwtStatementService.generateProtectedIssuanceAuthorizationTrustStatement(
                    statement,
                    statusListMetadataRepository
                        .findById(statement.getStatusListMetadataId())
                        .orElseThrow(IllegalStateException::new)
                );
                yield new TrustStatementIssuanceResult(null, jwt);
            }
            case TRUST_LIST_STATEMENT_NON_COMPLIANCE_V2 -> {
                var jwt = jwtStatementService.generateNonComplianceTrustListStatement(
                    statement,
                    statusListMetadataRepository
                        .findById(statement.getStatusListMetadataId())
                        .orElseThrow(IllegalStateException::new)
                );
                yield new TrustStatementIssuanceResult(null, jwt);
            }
            case TRUST_LIST_STATEMENT_PROTECTED_ISSUANCE_V2 -> {
                var jwt = jwtStatementService.generateProtectedIssuanceTrustListStatement(
                    statement,
                    statusListMetadataRepository
                        .findById(statement.getStatusListMetadataId())
                        .orElseThrow(IllegalStateException::new)
                );
                yield new TrustStatementIssuanceResult(null, jwt);
            }
            case PUBLIC_STATEMENT_VERIFICATION_QUERY_V2 -> {
                var jwt = jwtStatementService.generateVerificationQueryPublicStatement(statement);
                yield new TrustStatementIssuanceResult(null, jwt);
            }
            case
                TRUST_STATEMENT_IDENTITY_V1,
                TRUST_STATEMENT_ISSUANCE_V1,
                TRUST_STATEMENT_VERIFICATION_V1 -> issueAndPublishTrustStatementV1(statement);
        };
    }

    private TrustStatementIssuanceResult issueAndPublishTrustStatementV1(TrustStatementPartnerLink statement) {
        try {
            var request = createIssuerCredentialRequest(statement, List.of(issuerClient.getStatusListUri()));
            log.info("Issuing trust statement {}", statement.getId());
            return issuerClient.issueTrustStatement(request);
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to issue trust statement %s at gov trust issuer".formatted(statement.getId()),
                e
            );
        }
    }

    private UUID publishTrustStatementV2(
        TrustStatementIssuanceResult issuanceResult,
        TrustStatementPartnerLink partnerLink
    ) {
        StatementTypeDto type = switch (partnerLink.getType()) {
            case
                TRUST_STATEMENT_IDENTITY_V1,
                TRUST_STATEMENT_ISSUANCE_V1,
                TRUST_STATEMENT_VERIFICATION_V1 -> throw new IllegalStateException(
                "Trust Protocol V1 statements cannot be issued by V2 standards."
            );
            case TRUST_STATEMENT_IDENTITY_V2 -> StatementTypeDto.TRUST_STATEMENT_IDENTITY_V2;
            case TRUST_STATEMENT_PROTECTED_VERIFICATION_AUTHORIZATION_V2 -> StatementTypeDto.TRUST_STATEMENT_VERIFICATION_AUTHORIZATION_V2;
            case TRUST_STATEMENT_PROTECTED_ISSUANCE_AUTHORIZATION_V2 -> StatementTypeDto.TRUST_STATEMENT_ISSUANCE_AUTHORIZATION_V2;
            case TRUST_LIST_STATEMENT_NON_COMPLIANCE_V2 -> StatementTypeDto.TRUST_LIST_STATEMENT_NON_COMPLIANCE_V2;
            case TRUST_LIST_STATEMENT_PROTECTED_ISSUANCE_V2 -> StatementTypeDto.TRUST_LIST_STATEMENT_PROTECTED_ISSUANCE_V2;
            case PUBLIC_STATEMENT_VERIFICATION_QUERY_V2 -> StatementTypeDto.PUBLIC_STATEMENT_VERIFICATION_QUERY_V2;
        };
        var statement = trustRegistryService.createStatement(type, partnerLink.getId(), issuanceResult.encodedVc());
        return statement.id();
    }

    private UUID publishTrustStatementV1(
        TrustStatementIssuanceResult issuanceResult,
        TrustStatementPartnerLink partnerLink
    ) {
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
        return registryEntry.id();
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
            log.info("Publishing trust statement {}", partnerLink.getId());

            // 1/2: publish the trust statement vc to the trust registry
            UUID registryEntryId = switch (partnerLink.getType()) {
                case
                    TRUST_STATEMENT_IDENTITY_V2,
                    TRUST_STATEMENT_PROTECTED_VERIFICATION_AUTHORIZATION_V2,
                    TRUST_STATEMENT_PROTECTED_ISSUANCE_AUTHORIZATION_V2,
                    TRUST_LIST_STATEMENT_NON_COMPLIANCE_V2,
                    TRUST_LIST_STATEMENT_PROTECTED_ISSUANCE_V2,
                    PUBLIC_STATEMENT_VERIFICATION_QUERY_V2 -> publishTrustStatementV2(issuanceResult, partnerLink);
                case
                    TRUST_STATEMENT_IDENTITY_V1,
                    TRUST_STATEMENT_ISSUANCE_V1,
                    TRUST_STATEMENT_VERIFICATION_V1 -> publishTrustStatementV1(issuanceResult, partnerLink);
            };

            // 2/2: update the trust statement as issued and published
            var status = ACTIVE;
            var issuerVcStatus = lookupIssuerVcStatus(partnerLink);
            if (issuerVcStatus.isInvalid()) {
                status = TrustStatementPartnerLinkStatus.INACTIVE;
            }
            partnerLink.persistReferencesAfterPublicationSucceeded(
                issuanceResult.managementId(),
                registryEntryId,
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

    private void deactivateTrustStatementV2(TrustStatementPartnerLink partnerLink, String reason) {
        switch (partnerLink.getType()) {
            case
                TRUST_STATEMENT_IDENTITY_V2,
                TRUST_STATEMENT_PROTECTED_VERIFICATION_AUTHORIZATION_V2,
                TRUST_STATEMENT_PROTECTED_ISSUANCE_AUTHORIZATION_V2,
                TRUST_LIST_STATEMENT_NON_COMPLIANCE_V2,
                TRUST_LIST_STATEMENT_PROTECTED_ISSUANCE_V2,
                PUBLIC_STATEMENT_VERIFICATION_QUERY_V2 -> {
                /*Nothing to do*/
            }
            default -> throw new IllegalArgumentException(
                "Statements of type %s cannot be deactivated by this method.".formatted(partnerLink.getType())
            );
        }

        // 1/3: deactivate vc on trust registry side
        trustRegistryService.markAsNonActiveInStatuslist(partnerLink.getTrustRegistryEntryId());

        // 2/3: mark trust statement as inactive in management db
        partnerLink.markAsInactive();
        trustStatementPartnerLinkRepository.saveAndFlush(partnerLink);

        // 3/3: refresh statuslist if one is set
        if (partnerLink.getStatusListMetadataId() != null) {
            var statusListMetadata = statusListMetadataRepository
                .findById(partnerLink.getStatusListMetadataId())
                .orElseThrow(IllegalStateException::new);
            statusListDomainService.triggerPublication(statusListMetadata);
        }
    }

    private void deactivateTrustStatementV1(TrustStatementPartnerLink partnerLink, String reason) {
        // 1/3: set vc status to revoked on issuer side
        var issuerVcStatus = lookupIssuerVcStatus(partnerLink);
        if (!issuerVcStatus.isInvalid()) {
            switch (partnerLink.getType()) {
                case TRUST_STATEMENT_IDENTITY_V1, TRUST_STATEMENT_ISSUANCE_V1, TRUST_STATEMENT_VERIFICATION_V1 -> {
                    issuerClient.updateCredentialStatus(partnerLink.getTrustIssuerCredentialId(), REVOKED);
                }
                default -> throw new IllegalArgumentException(
                    "Statements of type %s cannot be deactivated by this method.".formatted(partnerLink.getType())
                );
            }
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
        // As long as we have no real VC validator library we use this approximation
        // If now < validUntil then the VC should be expired
        if (Instant.now().compareTo(statement.getValidUntil()) > 0) {
            return IssuerVcStatus.INVALID;
        }

        return switch (statement.getType()) {
            // If not expired, a vqPS is always valid
            case PUBLIC_STATEMENT_VERIFICATION_QUERY_V2 -> IssuerVcStatus.VALID;
            case
                TRUST_STATEMENT_IDENTITY_V2,
                TRUST_STATEMENT_PROTECTED_VERIFICATION_AUTHORIZATION_V2,
                TRUST_STATEMENT_PROTECTED_ISSUANCE_AUTHORIZATION_V2,
                TRUST_LIST_STATEMENT_NON_COMPLIANCE_V2,
                TRUST_LIST_STATEMENT_PROTECTED_ISSUANCE_V2 -> statement.getStatus() ==
            TrustStatementPartnerLinkStatus.INACTIVE
                ? IssuerVcStatus.INVALID
                : IssuerVcStatus.VALID;
            case TRUST_STATEMENT_IDENTITY_V1, TRUST_STATEMENT_ISSUANCE_V1, TRUST_STATEMENT_VERIFICATION_V1 -> {
                if (statement.getTrustIssuerCredentialId() == null) {
                    yield IssuerVcStatus.UNKNOWN;
                } else {
                    yield lookupIssuerVcStatusOnRemoteIssuer(statement);
                }
            }
        };
    }

    private IssuerVcStatus lookupIssuerVcStatusOnRemoteIssuer(TrustStatementPartnerLink statement) {
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

        TrustRegistryStatusDto trustRegistryStatus = switch (partnerLink.getType()) {
            case
                TRUST_STATEMENT_IDENTITY_V2,
                TRUST_STATEMENT_PROTECTED_VERIFICATION_AUTHORIZATION_V2,
                TRUST_STATEMENT_PROTECTED_ISSUANCE_AUTHORIZATION_V2,
                TRUST_LIST_STATEMENT_NON_COMPLIANCE_V2,
                TRUST_LIST_STATEMENT_PROTECTED_ISSUANCE_V2,
                PUBLIC_STATEMENT_VERIFICATION_QUERY_V2 -> toTrustRegistryStatusDto(
                trustRegistryService.getStatement(partnerLink.getId())
            );
            case
                TRUST_STATEMENT_IDENTITY_V1,
                TRUST_STATEMENT_ISSUANCE_V1,
                TRUST_STATEMENT_VERIFICATION_V1 -> toTrustRegistryStatusDto(lookupTrustRegistryStatus(partnerLink));
        };

        return TrustStatementMapper.toTrustStatementPartnerLinkDto(
            objectMapper,
            partnerLink,
            issuerVcStatus,
            trustRegistryStatus
        );
    }

    public record VerificationQueryPublicationResult(TrustStatementPartnerLinkDto partnerLinkDto, String encodedVqps) {}

    private record PartnerLinkIssuanceResult(TrustStatementPartnerLink partnerLink, String encodedVc) {}
}
