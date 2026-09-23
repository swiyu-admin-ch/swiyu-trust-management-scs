package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.admin.bj.swiyu.trust.client.core.business.internal.api.IdentifierApi;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.IdentifierEntryDto;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.PageMetadataDto;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.PagedModelIdentifierEntryDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.NonCompliantActor;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.NonCompliantActorPublicationEntry;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.NonCompliantActorRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.NonCompliantReasonText;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;

class NonCompliantActorDidsResolverTest {

    private final IdentifierApi identifierApi = mock(IdentifierApi.class);
    private final NonCompliantActorRepository repository = mock(NonCompliantActorRepository.class);
    private final NonCompliantActorDidsResolver resolver = new NonCompliantActorDidsResolver(identifierApi, repository);

    @Test
    void resolvesActorFlaggedByDidDirectly() {
        var actor = new NonCompliantActor(
            UUID.randomUUID(),
            "did:tdw:direct",
            new NonCompliantReasonText(null, null, null, "Violation", null)
        );

        var result = resolver.resolve(List.of(actor));

        assertThat(result).extracting(NonCompliantActorPublicationEntry::did).containsExactly("did:tdw:direct");
        verifyNoInteractions(identifierApi);
    }

    @Test
    void resolvesActorFlaggedByBusinessPartnerIdToAllDids() {
        var businessPartnerId = UUID.randomUUID();
        var actor = new NonCompliantActor(
            UUID.randomUUID(),
            null,
            businessPartnerId,
            new NonCompliantReasonText(null, null, null, "Violation", null)
        );
        var page = new PagedModelIdentifierEntryDto()
            .content(List.of(new IdentifierEntryDto().did("did:tdw:one"), new IdentifierEntryDto().did("did:tdw:two")))
            .page(new PageMetadataDto().totalPages(1L));
        when(identifierApi.getAllIdentifierEntries(eq(businessPartnerId), eq(0), any(), any())).thenReturn(page);

        var result = resolver.resolve(List.of(actor));

        assertThat(result)
            .extracting(NonCompliantActorPublicationEntry::did)
            .containsExactly("did:tdw:one", "did:tdw:two");
        assertThat(result).allSatisfy(entry ->
            assertThat(entry.flaggedAsNonCompliantAt()).isEqualTo(actor.getFlaggedAsNonCompliantAt())
        );
    }

    @Test
    void fetchesAllPagesOfIdentifierEntries() {
        var businessPartnerId = UUID.randomUUID();
        var actor = new NonCompliantActor(
            UUID.randomUUID(),
            null,
            businessPartnerId,
            new NonCompliantReasonText(null, null, null, "Violation", null)
        );
        var page0 = new PagedModelIdentifierEntryDto()
            .content(List.of(new IdentifierEntryDto().did("did:tdw:p0")))
            .page(new PageMetadataDto().totalPages(2L));
        var page1 = new PagedModelIdentifierEntryDto()
            .content(List.of(new IdentifierEntryDto().did("did:tdw:p1")))
            .page(new PageMetadataDto().totalPages(2L));
        when(identifierApi.getAllIdentifierEntries(eq(businessPartnerId), eq(0), any(), any())).thenReturn(page0);
        when(identifierApi.getAllIdentifierEntries(eq(businessPartnerId), eq(1), any(), any())).thenReturn(page1);

        var result = resolver.resolve(List.of(actor));

        assertThat(result)
            .extracting(NonCompliantActorPublicationEntry::did)
            .containsExactly("did:tdw:p0", "did:tdw:p1");
    }

    @Test
    void removesActorFromDbWhenCbsReturns404() {
        var businessPartnerId = UUID.randomUUID();
        var actor = new NonCompliantActor(
            UUID.randomUUID(),
            null,
            businessPartnerId,
            new NonCompliantReasonText(null, null, null, "Violation", null)
        );
        when(identifierApi.getAllIdentifierEntries(eq(businessPartnerId), eq(0), any(), any())).thenThrow(
            new RestClientResponseException("not found", HttpStatus.NOT_FOUND.value(), "Not Found", null, null, null)
        );

        var result = resolver.resolve(List.of(actor));

        assertThat(result).isEmpty();
        verify(repository).delete(actor);
    }

    @Test
    void doesNotPropagatesNon404Errors() {
        var businessPartnerId = UUID.randomUUID();
        var actor = new NonCompliantActor(
            UUID.randomUUID(),
            null,
            businessPartnerId,
            new NonCompliantReasonText(null, null, null, "Violation", null)
        );
        var error = new RestClientResponseException(
            "internal error",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            null,
            null,
            null
        );
        when(identifierApi.getAllIdentifierEntries(eq(businessPartnerId), eq(0), any(), any())).thenThrow(error);

        Assertions.assertDoesNotThrow(() -> resolver.resolve(List.of(actor)));
    }
}
