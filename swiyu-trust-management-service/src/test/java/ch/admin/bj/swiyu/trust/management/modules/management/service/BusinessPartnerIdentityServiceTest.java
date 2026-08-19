package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.management.api.ProtectedVerificationAuthorizationV2RequestDto.AuthorizableFieldDto.AHV_NUMBER;
import static ch.admin.bj.swiyu.trust.management.test.BusinessPartnerIdentityTestData.BUSINESS_PARTNER_NAME;
import static java.time.Duration.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityActivatedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityDeactivatedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityUpdatedEvent;
import ch.admin.bj.swiyu.trust.management.modules.common.date.DateTimeHelper;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.BusinessPartnerIdentityBadRequestException;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ResourceNotFoundException;
import ch.admin.bj.swiyu.trust.management.modules.management.api.IdentityV1RequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.IdentityV2RequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.ProtectedVerificationAuthorizationV2RequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.config.DefaultIdentityProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.config.statements.DefaultStatementProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.OutboxEventPublisher;
import ch.admin.bj.swiyu.trust.management.test.BusinessPartnerIdentityTestData;
import java.time.Instant;
import java.time.Period;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BusinessPartnerIdentityServiceTest {

    @Mock
    private BusinessPartnerIdentityRepository businessPartnerIdentityRepository;

    @Mock
    private DefaultIdentityProperties defaultIdentityProperties;

    @Mock
    private DefaultStatementProperties defaultStatementProperties;

    @Mock
    private DomainEventService domainEventService;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @Mock
    private ProtectedVerificationRepository protectedVerificationRepository;

    @Mock
    private TrustStatementPartnerLinkRepository trustStatementPartnerLinkRepository;

    @Mock
    private TrustStatementService trustStatementService;

    @Mock
    private ProtectedIssuanceAuthorizationRepository protectedIssuanceAuthorizationRepository;

    @Mock
    private ProtectedIssuanceEntryRepository protectedIssuanceEntryRepository;

    private BusinessPartnerIdentityService businessPartnerIdentityService;

    @BeforeEach
    void setUp() {
        businessPartnerIdentityService = new BusinessPartnerIdentityService(
            businessPartnerIdentityRepository,
            defaultIdentityProperties,
            defaultStatementProperties,
            domainEventService,
            outboxEventPublisher,
            protectedVerificationRepository,
            trustStatementPartnerLinkRepository,
            trustStatementService,
            protectedIssuanceAuthorizationRepository,
            protectedIssuanceEntryRepository
        );
    }

    @Test
    void activate_shouldSendActivateEvent() {
        var bpi = BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(
            BusinessPartnerIdentityStatus.DEACTIVATED
        );
        bpi.setVersion(0);
        when(businessPartnerIdentityRepository.findById(bpi.getId())).thenReturn(Optional.of(bpi));
        when(defaultIdentityProperties.validity()).thenReturn(Period.ofYears(3));

        businessPartnerIdentityService.activate(bpi.getId());

        var captor = ArgumentCaptor.forClass(TiBusinessPartnerIdentityActivatedEvent.class);
        verify(outboxEventPublisher).publishBusinessPartnerIdentityActivatedEvent(captor.capture());

        var event = captor.getValue();
        assertThat(event.getIdentity()).isNotNull();
        assertThat(event.getType()).isNotNull();
        assertThat(event.getType().getName()).isEqualTo("TiBusinessPartnerIdentityActivatedEvent");
        assertThat(event.getPublisher().getService()).isEqualTo("swiyu-trust-management-service");

        var payload = event.getPayload();
        assertThat(payload).isNotNull();
        assertThat(payload.getBusinessPartnerIdentityId()).isEqualTo(bpi.getId());
        assertThat(payload.getValidUntil()).isEqualTo(DateTimeHelper.today().plusYears(3).toInstant());
        assertThat(payload.getBusinessPartnerIdentityId()).isEqualTo(bpi.getId());
        assertThat(payload.getValidUntil()).isCloseTo(
            DateTimeHelper.today().plusYears(3).toInstant(),
            within(ofDays(1))
        );
        assertThat(payload.getStatus()).isEqualTo(
            ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerIdentityStatus.ACTIVE
        );
        assertThat(payload.getLastActivated()).isCloseTo(Instant.now(), within(ofSeconds(1)));
        assertThat(payload.getUid()).isEqualTo("CHE-123-456-789");
        assertThat(payload.getEntityName()).containsAllEntriesOf(BUSINESS_PARTNER_NAME);
    }

    @Test
    void activate_updateStatus() {
        var bpi = BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(
            BusinessPartnerIdentityStatus.DEACTIVATED
        );
        bpi.setVersion(0);
        when(businessPartnerIdentityRepository.findById(bpi.getId())).thenReturn(Optional.of(bpi));
        when(defaultIdentityProperties.validity()).thenReturn(Period.ofYears(3));

        businessPartnerIdentityService.activate(bpi.getId());

        assertThat(bpi.getStatus()).isEqualTo(BusinessPartnerIdentityStatus.ACTIVE);
    }

    @Test
    void activate_updateValidUntil() {
        var bpi = BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(
            BusinessPartnerIdentityStatus.DEACTIVATED
        );
        bpi.setVersion(0);
        when(businessPartnerIdentityRepository.findById(bpi.getId())).thenReturn(Optional.of(bpi));
        when(defaultIdentityProperties.validity()).thenReturn(Period.ofMonths(1));

        businessPartnerIdentityService.activate(bpi.getId());

        assertThat(bpi.getValidUntil()).isCloseTo(DateTimeHelper.today().plusMonths(1).toInstant(), within(ofDays(1)));
    }

    @Test
    void activate_updateLastActivated() {
        var bpi = BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(
            BusinessPartnerIdentityStatus.DEACTIVATED
        );
        bpi.setVersion(0);
        when(businessPartnerIdentityRepository.findById(bpi.getId())).thenReturn(Optional.of(bpi));
        when(defaultIdentityProperties.validity()).thenReturn(Period.ofYears(3));

        businessPartnerIdentityService.activate(bpi.getId());

        assertThat(bpi.getLastActivated()).isCloseTo(Instant.now(), within(ofSeconds(1)));
    }

    @Test
    void deactivate_shouldUpdateStatus() {
        var bpi = BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(
            BusinessPartnerIdentityStatus.ACTIVE
        );
        bpi.setVersion(0);
        when(businessPartnerIdentityRepository.findById(bpi.getId())).thenReturn(Optional.of(bpi));

        businessPartnerIdentityService.deactivate(bpi.getId());

        assertThat(bpi.getStatus()).isEqualTo(BusinessPartnerIdentityStatus.DEACTIVATED);
    }

    @Test
    void deactivate_shouldSendDeactivatedEvent() {
        var bpi = BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(
            BusinessPartnerIdentityStatus.ACTIVE
        );
        bpi.setVersion(0);
        when(businessPartnerIdentityRepository.findById(bpi.getId())).thenReturn(Optional.of(bpi));

        businessPartnerIdentityService.deactivate(bpi.getId());

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
        assertThat(payload.getBusinessPartnerIdentityId()).isEqualTo(bpi.getId());
        assertThat(payload.getStatus()).isEqualTo(
            ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerIdentityStatus.DEACTIVATED
        );
    }

    @Test
    void deactivateTrustStatementWith2TrustedIdentifier_shouldDeactivateOnlyThose2() {
        var bpi = BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(
            BusinessPartnerIdentityStatus.ACTIVE
        );
        bpi.getTrustedIdentifier().addAll(List.of("subject1V1", "Some-did"));
        when(businessPartnerIdentityRepository.findById(bpi.getId())).thenReturn(Optional.of(bpi));

        when(
            trustStatementPartnerLinkRepository.findAllByPartnerIdAndStatus(
                bpi.getId(),
                TrustStatementPartnerLinkStatus.ACTIVE
            )
        ).thenReturn(
            List.of(
                BusinessPartnerIdentityTestData.partnerLinkIdentityV1("subject1V1"),
                BusinessPartnerIdentityTestData.partnerLinkIdentityV1("subject2V1"),
                BusinessPartnerIdentityTestData.partnerLinkIdentityV2("Some-did")
            )
        );

        businessPartnerIdentityService.deactivateTrustStatements(bpi.getId(), "time to be deactivated");

        verify(trustStatementService, times(3)).deactivateTrustStatement(any(), any());
    }

    @Test
    void issueTrustStatement_shouldFailIfBpiIsNotActive() {
        var bpi = BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(
            BusinessPartnerIdentityStatus.DEACTIVATED
        );
        var bpiId = bpi.getId();
        when(businessPartnerIdentityRepository.findById(bpiId)).thenReturn(Optional.of(bpi));

        assertThatThrownBy(() -> businessPartnerIdentityService.issueTrustStatements(bpiId))
            .isInstanceOf(BusinessPartnerIdentityBadRequestException.class)
            .hasMessageMatching("Business partner identity for id '%s' is not active".formatted(bpiId));
    }

    @Test
    void issueTrustStatement_shouldSetLastIssuanceToNow() {
        var bpi = BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(
            BusinessPartnerIdentityStatus.ACTIVE
        );
        when(businessPartnerIdentityRepository.findById(bpi.getId())).thenReturn(Optional.of(bpi));

        businessPartnerIdentityService.issueTrustStatements(bpi.getId());

        assertThat(bpi.getLastIssuanceAt()).isCloseTo(Instant.now(), within(ofSeconds(1)));
    }

    @Test
    void issueTrustStatement_shouldCallIssueAndPublishIdTSV1WithTheRightValues() {
        var bpi = BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(
            BusinessPartnerIdentityStatus.ACTIVE
        );
        when(businessPartnerIdentityRepository.findById(bpi.getId())).thenReturn(Optional.of(bpi));

        when(defaultStatementProperties.timeToLive()).thenReturn(Period.ofMonths(6));

        businessPartnerIdentityService.issueTrustStatements(bpi.getId());

        var captor = ArgumentCaptor.forClass(IdentityV1RequestDto.class);
        verify(trustStatementService).issueAndPublishIdentityV1TrustStatement(eq(bpi.getId()), captor.capture());
        var request = captor.getValue();

        assertThat(request.getEntityName()).containsExactlyEntriesOf(BUSINESS_PARTNER_NAME);
        assertThat(request.getIsStateActor()).isFalse();
        assertThat(request.getRegistryIds()).hasSize(1);
        assertThat(request.getRegistryIds().getFirst().type()).isEqualTo("UID");
        assertThat(request.getRegistryIds().getFirst().value()).isEqualTo("CHE-123-456-789");
        assertThat(request.getValidFrom()).isCloseTo(Instant.now(), within(ofSeconds(1)));
        assertThat(request.getValidUntil()).isCloseTo(
            DateTimeHelper.today().plusMonths(6).toInstant(),
            within(ofDays(1))
        );
    }

    @Test
    void issueTrustStatementWithBpiValidUntil1MonthLater_shouldIssueAndPublishIdTSV1WithStatementValidUntil1MonthLater() {
        var bpi = BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(
            BusinessPartnerIdentityStatus.ACTIVE
        );
        bpi.activate(Period.ofMonths(1));
        when(businessPartnerIdentityRepository.findById(bpi.getId())).thenReturn(Optional.of(bpi));

        when(defaultStatementProperties.timeToLive()).thenReturn(Period.ofMonths(6));

        businessPartnerIdentityService.issueTrustStatements(bpi.getId());

        var captor = ArgumentCaptor.forClass(IdentityV1RequestDto.class);
        verify(trustStatementService).issueAndPublishIdentityV1TrustStatement(any(), captor.capture());
        var request = captor.getValue();

        assertThat(request.getValidUntil()).isCloseTo(
            DateTimeHelper.today().plusMonths(1).toInstant(),
            within(ofDays(1))
        );
    }

    @Test
    void issueTrustStatement_shouldCallIssueAndPublishIdTSV2WithTheRightValues() {
        var bpi = BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(
            BusinessPartnerIdentityStatus.ACTIVE
        );
        when(businessPartnerIdentityRepository.findById(bpi.getId())).thenReturn(Optional.of(bpi));

        when(defaultStatementProperties.timeToLive()).thenReturn(Period.ofMonths(6));

        businessPartnerIdentityService.issueTrustStatements(bpi.getId());

        var captor = ArgumentCaptor.forClass(IdentityV2RequestDto.class);
        verify(trustStatementService).issueAndPublishIdentityV2TrustStatement(captor.capture());
        var request = captor.getValue();

        assertThat(request.getEntityName()).containsExactlyEntriesOf(BUSINESS_PARTNER_NAME);
        assertThat(request.getIsStateActor()).isFalse();
        assertThat(request.getRegistryIds()).hasSize(1);
        assertThat(request.getRegistryIds().getFirst().type()).isEqualTo("UID");
        assertThat(request.getRegistryIds().getFirst().value()).isEqualTo("CHE-123-456-789");
        assertThat(request.getValidFrom()).isCloseTo(Instant.now(), within(ofMinutes(1)));
        assertThat(request.getValidUntil()).isCloseTo(
            DateTimeHelper.today().plusMonths(6).toInstant(),
            within(ofHours(24))
        );
    }

    @Test
    void issueTrustStatementWithBpiValidUntil1DayLater_shouldIssueAndPublishIdTSV2WithStatementValidUntil1DayLater() {
        var bpi = BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(
            BusinessPartnerIdentityStatus.ACTIVE
        );
        bpi.activate(Period.ofMonths(1));
        when(businessPartnerIdentityRepository.findById(bpi.getId())).thenReturn(Optional.of(bpi));

        when(defaultStatementProperties.timeToLive()).thenReturn(Period.ofMonths(6));

        businessPartnerIdentityService.issueTrustStatements(bpi.getId());

        var captor = ArgumentCaptor.forClass(IdentityV2RequestDto.class);
        verify(trustStatementService).issueAndPublishIdentityV2TrustStatement(captor.capture());
        var request = captor.getValue();

        assertThat(request.getValidUntil()).isCloseTo(
            DateTimeHelper.today().plusMonths(1).toInstant(),
            within(ofDays(1))
        );
    }

    @Test
    void issueTrustStatement_shouldCallIssueAndPublishPvaSV2WithTheRightValues() {
        var bpi = BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(
            BusinessPartnerIdentityStatus.ACTIVE
        );
        when(businessPartnerIdentityRepository.findById(bpi.getId())).thenReturn(Optional.of(bpi));
        when(protectedVerificationRepository.findAllByBusinessPartnerIdentityId(bpi.getId())).thenReturn(
            List.of(
                new ProtectedVerificationAuthorization(
                    UUID.randomUUID(),
                    bpi.getId(),
                    ProtectedVerificationField.AHV_NUMBER
                )
            )
        );

        when(defaultStatementProperties.timeToLive()).thenReturn(Period.ofMonths(6));

        businessPartnerIdentityService.issueTrustStatements(bpi.getId());

        var captor = ArgumentCaptor.forClass(ProtectedVerificationAuthorizationV2RequestDto.class);
        verify(trustStatementService).issueAndPublishProtectedVerificationAuthorizationV2TrustStatement(
            captor.capture()
        );
        var request = captor.getValue();

        assertThat(request.getSubject()).isEqualTo("Some-did");
        assertThat(request.getAuthorizedFields()).containsOnly(AHV_NUMBER);
        assertThat(request.getValidFrom()).isCloseTo(Instant.now(), within(ofMinutes(1)));
        assertThat(request.getValidUntil()).isCloseTo(
            DateTimeHelper.today().plusMonths(6).toInstant(),
            within(ofDays(1))
        );
    }

    @Test
    void renewTrustStatementsDeactivated_shouldThrow() {
        var bpi = BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(
            BusinessPartnerIdentityStatus.DEACTIVATED
        );
        var bpiId = bpi.getId();
        when(businessPartnerIdentityRepository.findById(bpi.getId())).thenReturn(Optional.of(bpi));

        assertThatThrownBy(() -> businessPartnerIdentityService.renewTrustStatements(bpiId))
            .isInstanceOf(BusinessPartnerIdentityBadRequestException.class)
            .hasMessageMatching("Business partner identity for id '%s' is not active".formatted(bpi.getId()));
    }

    @Test
    void renewTrustStatements_shouldSendBusinessPartnerIdentityUpdatedEvent() {
        var bpi = BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(
            BusinessPartnerIdentityStatus.ACTIVE
        );
        bpi.setVersion(0);
        when(businessPartnerIdentityRepository.findById(bpi.getId())).thenReturn(Optional.of(bpi));

        businessPartnerIdentityService.renewTrustStatements(bpi.getId());

        var captor = ArgumentCaptor.forClass(TiBusinessPartnerIdentityUpdatedEvent.class);
        verify(outboxEventPublisher).publishBusinessPartnerIdentityUpdatedEvent(captor.capture());

        var event = captor.getValue();

        assertThat(event.getIdentity()).isNotNull();
        assertThat(event.getType()).isNotNull();
        assertThat(event.getType().getName()).isEqualTo("TiBusinessPartnerIdentityUpdatedEvent");
        assertThat(event.getPublisher().getService()).isEqualTo("swiyu-trust-management-service");

        var payload = event.getPayload();
        assertThat(payload).isNotNull();
        assertThat(payload.getBusinessPartnerIdentityId()).isEqualTo(bpi.getId());
        assertThat(payload.getValidUntil()).isCloseTo(
            DateTimeHelper.today().plusYears(3).toInstant(),
            within(ofDays(1))
        );
        assertThat(payload.getStatus()).isEqualTo(
            ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerIdentityStatus.ACTIVE
        );
        assertThat(payload.getLastActivated()).isCloseTo(Instant.now(), within(ofMinutes(1)));
        assertThat(payload.getUid()).isEqualTo("CHE-123-456-789");
        assertThat(payload.getEntityName()).containsAllEntriesOf(BUSINESS_PARTNER_NAME);
    }

    @Test
    void syncWithInvalidBpiId_shouldThrow() {
        var invalidId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        assertThatThrownBy(() -> businessPartnerIdentityService.sync(invalidId))
            .isInstanceOfAny(ResourceNotFoundException.class)
            .hasMessageMatching("No business partner identity found for id 00000000-0000-0000-0000-000000000000");
    }

    @Test
    void sync_shouldEmitBpiUpdateEvent() {
        var bpi = BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(
            BusinessPartnerIdentityStatus.ACTIVE
        );
        bpi.setVersion(0);
        when(businessPartnerIdentityRepository.findById(bpi.getId())).thenReturn(Optional.of(bpi));

        businessPartnerIdentityService.sync(bpi.getId());

        var captor = ArgumentCaptor.forClass(TiBusinessPartnerIdentityUpdatedEvent.class);
        verify(outboxEventPublisher).publishBusinessPartnerIdentityUpdatedEvent(captor.capture());
        var event = captor.getValue();

        assertThat(event.getIdentity()).isNotNull();
        assertThat(event.getType()).isNotNull();
        assertThat(event.getType().getName()).isEqualTo("TiBusinessPartnerIdentityUpdatedEvent");
        assertThat(event.getPublisher().getService()).isEqualTo("swiyu-trust-management-service");

        var payload = event.getPayload();
        assertThat(payload).isNotNull();
        assertThat(payload.getBusinessPartnerIdentityId()).isEqualTo(bpi.getId());
        assertThat(payload.getValidUntil()).isCloseTo(
            DateTimeHelper.today().plusYears(3).toInstant(),
            within(ofDays(1))
        );
        assertThat(payload.getStatus()).isEqualTo(
            ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerIdentityStatus.ACTIVE
        );
        assertThat(payload.getLastActivated()).isCloseTo(Instant.now(), within(ofSeconds(1)));
        assertThat(payload.getUid()).isEqualTo("CHE-123-456-789");
        assertThat(payload.getEntityName()).containsAllEntriesOf(BUSINESS_PARTNER_NAME);
    }

    @Test
    void syncAllWith2Bpi_shouldEmit2Events() {
        var bpi1 = BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(
            BusinessPartnerIdentityStatus.ACTIVE
        );
        bpi1.setVersion(0);
        var bpi2 = BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(
            BusinessPartnerIdentityStatus.ACTIVE
        );
        bpi2.setVersion(0);
        when(businessPartnerIdentityRepository.findAll()).thenReturn(List.of(bpi1, bpi2));

        businessPartnerIdentityService.syncAll();

        var captor = ArgumentCaptor.forClass(TiBusinessPartnerIdentityUpdatedEvent.class);
        verify(outboxEventPublisher, times(2)).publishBusinessPartnerIdentityUpdatedEvent(captor.capture());
        var events = captor.getAllValues();
        assertThat(events).hasSize(2);
        assertThat(events)
            .extracting(event -> event.getPayload().getBusinessPartnerIdentityId())
            .containsExactlyInAnyOrder(bpi1.getId(), bpi2.getId());
    }
}
