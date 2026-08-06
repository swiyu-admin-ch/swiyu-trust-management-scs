package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerIdentityStatus;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityActivatedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityDeactivatedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityUpdatedEvent;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.BusinessPartnerIdentityBadRequestException;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ResourceNotFoundException;
import ch.admin.bj.swiyu.trust.management.modules.management.api.IdentityV1RequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.IdentityV2RequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.config.DefaultIdentityProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.config.statements.DefaultStatementProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.OutboxEventPublisher;
import jakarta.validation.constraints.NotBlank;
import java.time.*;
import java.util.*;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BusinessPartnerIdentityServiceTest {

    private static final Instant INSTANT_NOW = Instant.parse("2026-12-31T15:00:00Z");
    private static final Instant INSTANT_IN_1_DAY = Instant.parse("2027-01-01T15:00:00Z");
    private static final Instant INSTANT_IN_3_YEARS = Instant.parse("2029-12-31T15:00:00Z");
    private static final Instant INSTANT_IN_6_MONTHS = Instant.parse("2027-06-30T15:00:00Z");
    private static final UUID DEFAULT_BPI_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final UUID DEFAULT_BPI_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private BusinessPartnerIdentityRepository businessPartnerIdentityRepository;

    private Clock clock;

    @Mock
    private DefaultIdentityProperties defaultIdentityProperties;

    @Mock
    private DefaultStatementProperties defaultStatementProperties;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @Mock
    private TrustStatementPartnerLinkRepository trustStatementPartnerLinkRepository;

    @Mock
    private TrustStatementService trustStatementService;

    private BusinessPartnerIdentityService businessPartnerIdentityService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(INSTANT_NOW, ZoneOffset.systemDefault());

        businessPartnerIdentityService = new BusinessPartnerIdentityService(
            businessPartnerIdentityRepository,
            clock,
            defaultIdentityProperties,
            defaultStatementProperties,
            outboxEventPublisher,
            trustStatementPartnerLinkRepository,
            trustStatementService
        );
    }

    @Test
    void activate_shouldSendActivateEvent() {
        when(businessPartnerIdentityRepository.findById(DEFAULT_BPI_ID)).thenReturn(
            Optional.of(defaultPartnerIdentity(BusinessPartnerIdentityStatus.DEACTIVATED))
        );
        when(defaultIdentityProperties.validity()).thenReturn(Period.ofYears(3));

        businessPartnerIdentityService.activate(DEFAULT_BPI_ID);

        var captor = ArgumentCaptor.forClass(TiBusinessPartnerIdentityActivatedEvent.class);
        verify(outboxEventPublisher).publishBusinessPartnerIdentityActivatedEvent(captor.capture());

        var event = captor.getValue();
        assertThat(event.getIdentity()).isNotNull();
        assertThat(event.getType()).isNotNull();
        assertThat(event.getType().getName()).isEqualTo("TiBusinessPartnerIdentityActivatedEvent");
        assertThat(event.getType().getVersion()).isEqualTo("2.0.0");
        assertThat(event.getPublisher().getService()).isEqualTo("swiyu-trust-management-service");

        var payload = event.getPayload();
        assertThat(payload).isNotNull();
        assertThat(payload.getBusinessPartnerIdentityId()).isEqualTo(DEFAULT_BPI_ID);
        assertThat(payload.getValidUntil()).isEqualTo(INSTANT_IN_3_YEARS);
        assertThat(payload.getStatus()).isEqualTo(BusinessPartnerIdentityStatus.ACTIVE);
        assertThat(payload.getLastActivated()).isEqualTo(INSTANT_NOW);
        assertThat(payload.getUid()).isEqualTo("CHE-123-456-789");
        assertThat(payload.getEntityName()).containsAllEntriesOf(defaultEntityName());
    }

    @Test
    void activate_updateStatus() {
        var bpiId = DEFAULT_BPI_ID;
        var bpi = defaultPartnerIdentity(BusinessPartnerIdentityStatus.DEACTIVATED);
        when(businessPartnerIdentityRepository.findById(bpiId)).thenReturn(Optional.of(bpi));
        when(defaultIdentityProperties.validity()).thenReturn(Period.ofYears(3));

        businessPartnerIdentityService.activate(bpiId);

        assertThat(bpi.getStatus()).isEqualTo(BusinessPartnerIdentityStatus.ACTIVE);
    }

    @Test
    void activate_updateValidUntil() {
        var bpiId = DEFAULT_BPI_ID;
        var bpi = defaultPartnerIdentity(BusinessPartnerIdentityStatus.DEACTIVATED);
        when(businessPartnerIdentityRepository.findById(bpiId)).thenReturn(Optional.of(bpi));
        when(defaultIdentityProperties.validity()).thenReturn(Period.ofDays(1));

        businessPartnerIdentityService.activate(bpiId);

        assertThat(bpi.getValidUntil()).isEqualTo(Instant.parse("2027-01-01T15:00:00Z"));
    }

    @Test
    void activate_updateLastActivated() {
        var bpiId = DEFAULT_BPI_ID;
        var bpi = defaultPartnerIdentity(BusinessPartnerIdentityStatus.DEACTIVATED);
        when(businessPartnerIdentityRepository.findById(bpiId)).thenReturn(Optional.of(bpi));
        when(defaultIdentityProperties.validity()).thenReturn(Period.ofYears(3));

        businessPartnerIdentityService.activate(bpiId);

        assertThat(bpi.getLastActivated()).isEqualTo(INSTANT_NOW);
    }

    @Test
    void deactivate_shouldUpdateStatus() {
        var bpiId = DEFAULT_BPI_ID;
        var bpi = defaultPartnerIdentity(BusinessPartnerIdentityStatus.ACTIVE);
        when(businessPartnerIdentityRepository.findById(bpiId)).thenReturn(Optional.of(bpi));

        businessPartnerIdentityService.deactivate(bpiId);

        assertThat(bpi.getStatus()).isEqualTo(BusinessPartnerIdentityStatus.DEACTIVATED);
    }

    @Test
    void deactivate_shouldSendDeactivatedEvent() {
        var bpi = defaultPartnerIdentity(BusinessPartnerIdentityStatus.ACTIVE);
        when(businessPartnerIdentityRepository.findById(DEFAULT_BPI_ID)).thenReturn(Optional.of(bpi));

        businessPartnerIdentityService.deactivate(DEFAULT_BPI_ID);

        var captor = ArgumentCaptor.forClass(TiBusinessPartnerIdentityDeactivatedEvent.class);
        verify(outboxEventPublisher).publishBusinessPartnerIdentityDeactivatedEvent(captor.capture());

        var event = captor.getValue();
        assertThat(event.getIdentity()).isNotNull();
        assertThat(event.getType()).isNotNull();
        assertThat(event.getType().getName()).isEqualTo("TiBusinessPartnerIdentityDeactivatedEvent");
        assertThat(event.getType().getVersion()).isEqualTo("2.0.0");
        assertThat(event.getPublisher().getService()).isEqualTo("swiyu-trust-management-service");

        var payload = event.getPayload();
        assertThat(payload).isNotNull();
        assertThat(payload.getBusinessPartnerIdentityId()).isEqualTo(DEFAULT_BPI_ID);
        assertThat(payload.getStatus()).isEqualTo(BusinessPartnerIdentityStatus.DEACTIVATED);
    }

    @Test
    void deactivateTrustStatementWith2TrustedIdentifier_shouldDeactivateOnlyThose2() {
        var bpi = defaultPartnerIdentity(BusinessPartnerIdentityStatus.ACTIVE);
        bpi.getTrustedIdentifier().addAll(List.of("subject1V1", "subject1V2"));
        when(businessPartnerIdentityRepository.findById(DEFAULT_BPI_ID)).thenReturn(Optional.of(bpi));

        when(
            trustStatementPartnerLinkRepository.findAllByPartnerIdAndStatus(
                DEFAULT_BPI_ID,
                TrustStatementPartnerLinkStatus.ACTIVE
            )
        ).thenReturn(
            List.of(
                partnerLinkIdentityV1("subject1V1"),
                partnerLinkIdentityV1("subject2V1"),
                partnerLinkIdentityV2("subject1V2")
            )
        );

        businessPartnerIdentityService.deactivateTrustStatement(DEFAULT_BPI_ID, "time to be deactivated");

        verify(trustStatementService, times(3)).deactivateTrustStatement(any(), any());
    }

    @Test
    void issueTrustStatement_shouldFailIfBpiIsNotActive() {
        var bpi = defaultPartnerIdentity(BusinessPartnerIdentityStatus.DEACTIVATED);
        when(businessPartnerIdentityRepository.findById(DEFAULT_BPI_ID)).thenReturn(Optional.of(bpi));

        assertThatThrownBy(() -> businessPartnerIdentityService.issueTrustStatements(DEFAULT_BPI_ID))
            .isInstanceOf(BusinessPartnerIdentityBadRequestException.class)
            .hasMessageMatching(
                "Business partner identity for id '00000000-0000-0000-0000-000000000000' is not active"
            );
    }

    @Test
    void issueTrustStatement_shouldSetLastIssuanceToNow() {
        var bpi = defaultPartnerIdentity(BusinessPartnerIdentityStatus.ACTIVE);
        when(businessPartnerIdentityRepository.findById(DEFAULT_BPI_ID)).thenReturn(Optional.of(bpi));

        businessPartnerIdentityService.issueTrustStatements(DEFAULT_BPI_ID);

        assertThat(bpi.getLastIssuanceAt()).isEqualTo(INSTANT_NOW);
    }

    @Test
    void issueTrustStatement_shouldCallIssueAndPublishIdTSV1WithTheRightValues() {
        var bpi = defaultPartnerIdentity(BusinessPartnerIdentityStatus.ACTIVE);
        bpi.getTrustedIdentifier().add("subject1V1");
        when(businessPartnerIdentityRepository.findById(DEFAULT_BPI_ID)).thenReturn(Optional.of(bpi));

        when(defaultStatementProperties.timeToLive()).thenReturn(Period.ofMonths(6));

        businessPartnerIdentityService.issueTrustStatements(DEFAULT_BPI_ID);

        var captor = ArgumentCaptor.forClass(IdentityV1RequestDto.class);
        verify(trustStatementService).issueAndPublishIdentityV1TrustStatement(any(), captor.capture());
        var request = captor.getValue();

        assertThat(request.getEntityName()).containsExactlyEntriesOf(defaultEntityName());
        assertThat(request.getIsStateActor()).isFalse();
        assertThat(request.getRegistryIds()).hasSize(1);
        assertThat(request.getRegistryIds().getFirst().type()).isEqualTo("UID");
        assertThat(request.getRegistryIds().getFirst().value()).isEqualTo("CHE-123-456-789");
        assertThat(request.getValidFrom()).isEqualTo(INSTANT_NOW);
        assertThat(request.getValidUntil()).isCloseTo(INSTANT_IN_6_MONTHS, within(Duration.ofHours(24)));
    }

    @Test
    void issueTrustStatementWithBpiValidUntil1DayLater_shouldIssueAndPublishIdTSV1WithStatementValidUntil1DayLater() {
        var bpi = defaultPartnerIdentity(BusinessPartnerIdentityStatus.ACTIVE);
        bpi.setValidUntil(INSTANT_IN_1_DAY);
        bpi.getTrustedIdentifier().add("subject1V1");
        when(businessPartnerIdentityRepository.findById(DEFAULT_BPI_ID)).thenReturn(Optional.of(bpi));

        when(defaultStatementProperties.timeToLive()).thenReturn(Period.ofMonths(6));

        businessPartnerIdentityService.issueTrustStatements(DEFAULT_BPI_ID);

        var captor = ArgumentCaptor.forClass(IdentityV1RequestDto.class);
        verify(trustStatementService).issueAndPublishIdentityV1TrustStatement(any(), captor.capture());
        var request = captor.getValue();

        assertThat(request.getValidUntil()).isCloseTo(INSTANT_IN_1_DAY, within(Duration.ofMinutes(1)));
    }

    @Test
    void issueTrustStatement_shouldCallIssueAndPublishIdTSV2WithTheRightValues() {
        var bpi = defaultPartnerIdentity(BusinessPartnerIdentityStatus.ACTIVE);
        bpi.getTrustedIdentifier().add("subject1V2");
        when(businessPartnerIdentityRepository.findById(DEFAULT_BPI_ID)).thenReturn(Optional.of(bpi));

        when(defaultStatementProperties.timeToLive()).thenReturn(Period.ofMonths(6));

        businessPartnerIdentityService.issueTrustStatements(DEFAULT_BPI_ID);

        var captor = ArgumentCaptor.forClass(IdentityV2RequestDto.class);
        verify(trustStatementService).issueAndPublishIdentityV2TrustStatement(captor.capture());
        var request = captor.getValue();

        assertThat(request.getEntityName()).containsExactlyEntriesOf(defaultEntityName());
        assertThat(request.getIsStateActor()).isFalse();
        assertThat(request.getRegistryIds()).hasSize(1);
        assertThat(request.getRegistryIds().getFirst().type()).isEqualTo("UID");
        assertThat(request.getRegistryIds().getFirst().value()).isEqualTo("CHE-123-456-789");
        assertThat(request.getValidFrom()).isEqualTo(INSTANT_NOW);
        assertThat(request.getValidUntil()).isCloseTo(INSTANT_IN_6_MONTHS, within(Duration.ofHours(24)));
    }

    @Test
    void issueTrustStatementWithBpiValidUntil1DayLater_shouldIssueAndPublishIdTSV2WithStatementValidUntil1DayLater() {
        var bpi = defaultPartnerIdentity(BusinessPartnerIdentityStatus.ACTIVE);
        bpi.setValidUntil(INSTANT_IN_1_DAY);
        bpi.getTrustedIdentifier().add("subject1V2");
        when(businessPartnerIdentityRepository.findById(DEFAULT_BPI_ID)).thenReturn(Optional.of(bpi));

        when(defaultStatementProperties.timeToLive()).thenReturn(Period.ofMonths(6));

        businessPartnerIdentityService.issueTrustStatements(DEFAULT_BPI_ID);

        var captor = ArgumentCaptor.forClass(IdentityV2RequestDto.class);
        verify(trustStatementService).issueAndPublishIdentityV2TrustStatement(captor.capture());
        var request = captor.getValue();

        assertThat(request.getValidUntil()).isCloseTo(INSTANT_IN_1_DAY, within(Duration.ofMinutes(1)));
    }

    @Test
    void renewTrustStatementsDeactivated_shouldThrow() {
        when(businessPartnerIdentityRepository.findById(DEFAULT_BPI_ID)).thenReturn(
            Optional.of(defaultPartnerIdentity(BusinessPartnerIdentityStatus.DEACTIVATED))
        );

        assertThatThrownBy(() -> businessPartnerIdentityService.renewTrustStatements(DEFAULT_BPI_ID))
            .isInstanceOf(BusinessPartnerIdentityBadRequestException.class)
            .hasMessageMatching(
                "Business partner identity for id '00000000-0000-0000-0000-000000000000' is not active"
            );
    }

    @Test
    void renewTrustStatements_shouldSendBusinessPartnerIdentityUpdatedEvent() {
        var bpi = defaultPartnerIdentity(BusinessPartnerIdentityStatus.ACTIVE);
        when(businessPartnerIdentityRepository.findById(DEFAULT_BPI_ID)).thenReturn(Optional.of(bpi));

        businessPartnerIdentityService.renewTrustStatements(DEFAULT_BPI_ID);

        var captor = ArgumentCaptor.forClass(TiBusinessPartnerIdentityUpdatedEvent.class);
        verify(outboxEventPublisher).publishBusinessPartnerIdentityUpdatedEvent(captor.capture());

        var event = captor.getValue();

        assertThat(event.getIdentity()).isNotNull();
        assertThat(event.getType()).isNotNull();
        assertThat(event.getType().getName()).isEqualTo("TiBusinessPartnerIdentityUpdatedEvent");
        assertThat(event.getType().getVersion()).isEqualTo("2.0.0");
        assertThat(event.getPublisher().getService()).isEqualTo("swiyu-trust-management-service");

        var payload = event.getPayload();
        assertThat(payload).isNotNull();
        assertThat(payload.getBusinessPartnerIdentityId()).isEqualTo(DEFAULT_BPI_ID);
        assertThat(payload.getValidUntil()).isEqualTo(INSTANT_IN_3_YEARS);
        assertThat(payload.getStatus()).isEqualTo(BusinessPartnerIdentityStatus.ACTIVE);
        assertThat(payload.getLastActivated()).isEqualTo(INSTANT_NOW);
        assertThat(payload.getUid()).isEqualTo("CHE-123-456-789");
        assertThat(payload.getEntityName()).containsAllEntriesOf(defaultEntityName());
    }

    @Test
    void syncWithInvalidBpiId_shouldThrow() {
        assertThatThrownBy(() -> businessPartnerIdentityService.sync(DEFAULT_BPI_ID))
            .isInstanceOfAny(ResourceNotFoundException.class)
            .hasMessageMatching("No business partner identity found for id 00000000-0000-0000-0000-000000000000");
    }

    @Test
    void sync_shouldEmitBpiUpdateEvent() {
        var bpi = defaultPartnerIdentity(BusinessPartnerIdentityStatus.ACTIVE);
        when(businessPartnerIdentityRepository.findById(DEFAULT_BPI_ID)).thenReturn(Optional.of(bpi));

        businessPartnerIdentityService.sync(DEFAULT_BPI_ID);

        var captor = ArgumentCaptor.forClass(TiBusinessPartnerIdentityUpdatedEvent.class);
        verify(outboxEventPublisher).publishBusinessPartnerIdentityUpdatedEvent(captor.capture());
        var event = captor.getValue();

        assertThat(event.getIdentity()).isNotNull();
        assertThat(event.getType()).isNotNull();
        assertThat(event.getType().getName()).isEqualTo("TiBusinessPartnerIdentityUpdatedEvent");
        assertThat(event.getType().getVersion()).isEqualTo("2.0.0");
        assertThat(event.getPublisher().getService()).isEqualTo("swiyu-trust-management-service");

        var payload = event.getPayload();
        assertThat(payload).isNotNull();
        assertThat(payload.getBusinessPartnerIdentityId()).isEqualTo(DEFAULT_BPI_ID);
        assertThat(payload.getValidUntil()).isEqualTo(INSTANT_IN_3_YEARS);
        assertThat(payload.getStatus()).isEqualTo(BusinessPartnerIdentityStatus.ACTIVE);
        assertThat(payload.getLastActivated()).isEqualTo(INSTANT_NOW);
        assertThat(payload.getUid()).isEqualTo("CHE-123-456-789");
        assertThat(payload.getEntityName()).containsAllEntriesOf(defaultEntityName());
    }

    @Test
    void syncAllWith2Bpi_shouldEmit2Events() {
        var bpi1 = defaultPartnerIdentity(BusinessPartnerIdentityStatus.ACTIVE);
        var bpi2 = defaultPartnerIdentity(BusinessPartnerIdentityStatus.ACTIVE);
        bpi2.setId(DEFAULT_BPI_ID_2);
        when(businessPartnerIdentityRepository.findAll()).thenReturn(List.of(bpi1, bpi2));

        businessPartnerIdentityService.syncAll();

        var captor = ArgumentCaptor.forClass(TiBusinessPartnerIdentityUpdatedEvent.class);
        verify(outboxEventPublisher, times(2)).publishBusinessPartnerIdentityUpdatedEvent(captor.capture());
        var events = captor.getAllValues();
        assertThat(events).hasSize(2);
        assertThat(events)
            .extracting(event -> event.getPayload().getBusinessPartnerIdentityId())
            .containsExactlyInAnyOrder(DEFAULT_BPI_ID, DEFAULT_BPI_ID_2);
    }

    private @NonNull BusinessPartnerIdentity defaultPartnerIdentity(BusinessPartnerIdentityStatus status) {
        var bpi = new BusinessPartnerIdentity(
            DEFAULT_BPI_ID,
            defaultEntityName(),
            null,
            "CHE-123-456-789",
            false,
            "de-CH",
            status,
            false,
            INSTANT_IN_3_YEARS, // 10h in future
            null
        );
        bpi.setVersion(0L);
        bpi.setLastActivated(status == BusinessPartnerIdentityStatus.ACTIVE ? INSTANT_NOW : null);
        return bpi;
    }

    private static @NonNull Map<String, @NotBlank String> defaultEntityName() {
        return Map.of("de-CH", "Compagny-de", "fr-CH", "Company-fr", "it-CH", "Company-it", "rm-CH", "Company-rm");
    }

    private @NonNull TrustStatementPartnerLink partnerLinkIdentityV1(String subject) {
        var partnerLlink = TrustStatementPartnerLink.createIdentityV1(
            DEFAULT_BPI_ID,
            subject,
            Instant.now(clock),
            Instant.parse("2027-03-31T15:00:00Z"),
            defaultEntityName(),
            Collections.emptyList(),
            false
        );
        // set status to ACTIVE
        partnerLlink.persistReferencesAfterPublicationSucceeded(
            UUID.randomUUID(),
            UUID.randomUUID(),
            TrustStatementPartnerLinkStatus.ACTIVE
        );
        return partnerLlink;
    }

    private @NonNull TrustStatementPartnerLink partnerLinkIdentityV2(String subject) {
        var partnerLink = TrustStatementPartnerLink.createIdentityV2(
            DEFAULT_BPI_ID,
            subject,
            Instant.now(clock),
            Instant.parse("2027-03-31T15:00:00Z"),
            defaultEntityName(),
            Collections.emptyList(),
            false,
            null
        );
        // set status to ACTIVE
        partnerLink.persistReferencesAfterPublicationSucceeded(
            UUID.randomUUID(),
            UUID.randomUUID(),
            TrustStatementPartnerLinkStatus.ACTIVE
        );
        return partnerLink;
    }
}
