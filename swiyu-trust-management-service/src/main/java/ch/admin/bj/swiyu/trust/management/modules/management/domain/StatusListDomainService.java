package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import ch.admin.bj.swiyu.trust.client.core.business.b2b.api.StatusB2BApi;
import ch.admin.bj.swiyu.trust.client.core.business.b2b.model.StatusListEntryCreationDtoDto;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ExternalSystem;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ExternalSystemException;
import ch.admin.bj.swiyu.trust.management.modules.management.config.statements.DefaultStatementProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness.IssuerTrustRootProperties;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.ClientAuthorizationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatusListDomainService {

    private final StatusListMetadataRepository statusListMetadataRepository;
    private final TrustStatementPartnerLinkRepository trustStatementPartnerLinkRepository;
    private final JwtStatementDomainService jwtStatementDomainService;
    private final StatusB2BApi statusB2bApi;
    private final IssuerTrustRootProperties issuerTrustRootProperties;
    private final DefaultStatementProperties defaultStatementProperties;

    @Transactional
    public void triggerPublications() {
        log.debug("Triggering status list publications ...");
        var statusLists = statusListMetadataRepository.findAllByStatusIn(
            List.of(StatusListMetadataStatus.FULL, StatusListMetadataStatus.ACTIVE)
        );
        for (StatusListMetadata statusListMetadata : statusLists) {
            try {
                issueAndPublishStatusList(statusListMetadata);
            } catch (Exception e) {
                log.error("Error during StatusList refresh ({})", statusListMetadata.getId(), e);
            }
        }
    }

    @Transactional
    public void triggerPublication(@NotNull StatusListMetadata statusListMetadata) {
        log.debug(
            "Triggering status list publication for status list {} with url {}...",
            statusListMetadata.getId(),
            statusListMetadata.getStatusRegistryUrl()
        );
        issueAndPublishStatusList(statusListMetadata);
    }

    /**
     * Returns the next free index of a statuslist, may create a new statuslist is none is free anymore
     *
     * @return A free statuslist id and index combination.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StatusListEntry getNewStatusListEntry() {
        Optional<StatusListEntry> possibleIndex;

        // Create a new statuslist if one could not present an unused index
        var emergencyBreak = 4;
        do {
            possibleIndex = statusListMetadataRepository.incrementAndGetIndex();

            if (possibleIndex.isEmpty()) {
                createStatusList();
            }

            if (emergencyBreak-- <= 0) {
                throw new IllegalStateException("New statuslists do not present a free index.");
            }
        } while (possibleIndex.isEmpty());

        // Set statuslist to full if this was the last free entry
        var statuslist = statusListMetadataRepository
            .findById(possibleIndex.get().statusListMetadataId())
            .orElseThrow(IllegalStateException::new);

        if (possibleIndex.get().allocatedIndex() >= statuslist.getMaxSize() - 1) {
            log.info("Set statuslist to full {}", statuslist.getId());
            statuslist.markAsFull();
            statusListMetadataRepository.save(statuslist);
        }

        return possibleIndex.get();
    }

    private void issueAndPublishStatusList(StatusListMetadata statusListMetadata) {
        log.info("Publishing status list {}", statusListMetadata.getStatusRegistryUrl());
        log.debug("Generating status list {} token", statusListMetadata.getStatusRegistryUrl());
        var revokedStatusListEntries = trustStatementPartnerLinkRepository.findAllByStatusListMetadataIdAndStatus(
            statusListMetadata.getId(),
            TrustStatementPartnerLinkStatus.INACTIVE
        );
        var tokenStatusList = jwtStatementDomainService.generateTokenStatusList(
            statusListMetadata,
            revokedStatusListEntries
        );
        log.debug("Publishing status list {} to api", statusListMetadata.getStatusRegistryUrl());
        try {
            statusB2bApi.updateStatusListEntry(
                issuerTrustRootProperties.businessPartnerId(),
                statusListMetadata.getId(),
                tokenStatusList.serialize()
            );
        } catch (ClientAuthorizationException e) {
            throw new ExternalSystemException(
                "Could not refresh token to access B2B api.",
                ExternalSystem.CORE_BUSINESS_SERVICE_AUTH_SYSTEM,
                HttpStatus.INTERNAL_SERVER_ERROR,
                e
            );
        } catch (RestClientResponseException e) {
            throw new ExternalSystemException(
                "Could not update StatusList",
                ExternalSystem.CORE_BUSINESS_SERVICE,
                HttpStatus.INTERNAL_SERVER_ERROR,
                e
            );
        }
        statusListMetadata.updateLastPublication();
        statusListMetadataRepository.save(statusListMetadata);
        log.debug("Publishing of status list {} done", statusListMetadata.getStatusRegistryUrl());
    }

    private void createStatusList() {
        log.debug("Creating status list");
        StatusListEntryCreationDtoDto entry;
        try {
            entry = statusB2bApi.createStatusListEntry(issuerTrustRootProperties.businessPartnerId());
        } catch (ClientAuthorizationException e) {
            throw new ExternalSystemException(
                "Could not refresh token to access B2B api.",
                ExternalSystem.CORE_BUSINESS_SERVICE_AUTH_SYSTEM,
                HttpStatus.INTERNAL_SERVER_ERROR,
                e
            );
        } catch (RestClientResponseException e) {
            throw new ExternalSystemException(
                "Could not create new StatusList",
                ExternalSystem.CORE_BUSINESS_SERVICE,
                HttpStatus.INTERNAL_SERVER_ERROR,
                e
            );
        }
        log.debug("Created statuslist {}", entry.getId());
        var statusListMetadata = new StatusListMetadata(
            entry.getId(),
            entry.getStatusRegistryUrl(),
            defaultStatementProperties.statuslist().size()
        );
        this.statusListMetadataRepository.save(statusListMetadata);
        issueAndPublishStatusList(statusListMetadata);
    }
}
