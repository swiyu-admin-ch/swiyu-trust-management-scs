package ch.admin.bj.swiyu.trust.management.modules.management.service;

import ch.admin.bj.swiyu.trust.client.core.business.internal.api.IdentifierApi;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.PageMetadataDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.NonCompliantActor;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.NonCompliantActorPublicationEntry;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.NonCompliantActorRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

/**
 * Resolves {@link NonCompliantActor}s flagged by business partner id into concrete DIDs before they
 * are published in the non-compliance list.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NonCompliantActorDidsResolver {

    private static final int PAGE_SIZE = 1000;

    private final IdentifierApi identifierApi;
    private final NonCompliantActorRepository nonCompliantActorRepository;

    public List<NonCompliantActorPublicationEntry> resolve(List<NonCompliantActor> nonCompliantActors) {
        var resolved = new ArrayList<NonCompliantActorPublicationEntry>();
        for (var actor : nonCompliantActors) {
            if (actor.getBusinessPartnerId() != null) {
                resolved.addAll(resolveByBusinessPartnerId(actor));
            } else {
                resolved.add(
                    new NonCompliantActorPublicationEntry(
                        actor.getDid(),
                        actor.getFlaggedAsNonCompliantAt(),
                        actor.getReason()
                    )
                );
            }
        }
        return resolved;
    }

    private List<NonCompliantActorPublicationEntry> resolveByBusinessPartnerId(NonCompliantActor actor) {
        List<String> dids = List.of();
        try {
            dids = collectDidsOfBusinessPartner(actor.getBusinessPartnerId());
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                log.info(
                    "CBS returned 404 for business partner {} of non-compliant actor {}, removing it",
                    actor.getBusinessPartnerId(),
                    actor.getId()
                );
                nonCompliantActorRepository.delete(actor);
                return List.of();
            } else {
                log.error("Error during identifier resolution of business partner", e);
                // Drop error as the resolution should not impact the token generation
            }
        } catch (Exception e) {
            log.error("Error during identifier resolution of business partner", e);
            // Drop error as the resolution should not impact the token generation
        }
        return dids
            .stream()
            .map(did ->
                new NonCompliantActorPublicationEntry(did, actor.getFlaggedAsNonCompliantAt(), actor.getReason())
            )
            .toList();
    }

    private List<String> collectDidsOfBusinessPartner(UUID businessPartnerId) {
        var dids = new ArrayList<String>();
        var page = 0;
        PageMetadataDto metadata = null;
        do {
            var result = identifierApi.getAllIdentifierEntries(
                businessPartnerId,
                page,
                PAGE_SIZE,
                List.of("created_at")
            );
            if (result.getContent() != null) {
                for (var entry : result.getContent()) {
                    if (entry.getDid() != null && !entry.getDid().isBlank()) {
                        dids.add(entry.getDid());
                    }
                }
            }
            metadata = result.getPage();

            page++;
        } while (metadata != null && metadata.getTotalPages() != null && page < metadata.getTotalPages());
        return dids;
    }
}
