package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.admin.bit.jeap.messaging.avro.security.AvroClassSecurity;
import ch.admin.bj.swiyu.messagetype.ti.TiProtectedVerificationSubmissionApprovedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiProtectedVerificationSubmissionRejectedEvent;
import ch.admin.bj.swiyu.trust.client.core.business.internal.api.ProtectedVerificationSubmissionInternalApi;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.ProtectedVerificationCategoryDto;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.ProtectedVerificationSubmissionDto;
import ch.admin.bj.swiyu.trust.client.zas.sbn.api.UsnApi;
import ch.admin.bj.swiyu.trust.client.zas.sbn.model.OrganisationDto;
import ch.admin.bj.swiyu.trust.client.zas.sbn.model.StatusDto;
import ch.admin.bj.swiyu.trust.client.zas.sbn.model.UsnDto;
import ch.admin.bj.swiyu.trust.client.zas.sbn.model.UsnPageDto;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ResourceNotFoundException;
import ch.admin.bj.swiyu.trust.management.modules.management.api.AuthorizableFieldDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.ProtectedVerificationAuthorizationRequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.taskaction.ApproveProtectedVerificationRequestTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.taskaction.RejectProtectedVerificationRequestTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.config.ProtectedVerificationTaskProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.ProtectedVerificationRequestTask;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.ProtectedVerificationRequestTaskRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustTaskStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.OutboxEventPublisher;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProtectedVerificationRequestTaskServiceTest {

    private static final UUID PARTNER_ID = UUID.randomUUID();
    private static final UUID SUBMISSION_ID = UUID.randomUUID();
    private static final UUID SBN_ID = UUID.randomUUID();

    @Mock
    private ProtectedVerificationRequestTaskRepository taskRepository;

    @Mock
    private ProtectedVerificationSubmissionInternalApi protectedVerificationSubmissionApi;

    @Mock
    private UsnApi usnApi;

    @Mock
    private BusinessPartnerIdentityService businessPartnerIdentityService;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @Mock
    private DomainEventService domainEventService;

    @Mock
    private ProtectedVerificationTaskProperties protectedVerificationTaskProperties;

    private ProtectedVerificationRequestTaskService service;

    private ProtectedVerificationRequestTask newTask() {
        return new ProtectedVerificationRequestTask(
            PARTNER_ID,
            Map.of("default", "Acme AG"),
            SUBMISSION_ID,
            Instant.now().plusSeconds(3600),
            Instant.now()
        );
    }

    private ProtectedVerificationSubmissionDto submissionDto() {
        var submission = mock(ProtectedVerificationSubmissionDto.class);
        when(submission.getSbnId()).thenReturn(SBN_ID);
        return submission;
    }

    private ProtectedVerificationSubmissionDto submissionDtoForCreateTask() {
        var submission = mock(ProtectedVerificationSubmissionDto.class);
        when(submission.getPartnerId()).thenReturn(PARTNER_ID);
        when(submission.getEntityName()).thenReturn("Acme AG");
        when(submission.getSubmittedAt()).thenReturn(Instant.now());
        return submission;
    }

    private UsnPageDto usnPageWith(UsnDto usn) {
        return new UsnPageDto().content(List.of(usn));
    }

    private UsnDto usnDto() {
        return new UsnDto()
            .businessId(SBN_ID)
            .organisation(new OrganisationDto().name("Acme AG").locality("Bern"))
            .status(new StatusDto().code("ACTIVE"))
            .activityDomain(null);
    }

    @BeforeAll
    static void installAvroClassWhitelist() {
        AvroClassSecurity.installDefaultIfMissing();
    }

    @BeforeEach
    void setUp() {
        service = new ProtectedVerificationRequestTaskService(
            taskRepository,
            protectedVerificationSubmissionApi,
            usnApi,
            businessPartnerIdentityService,
            outboxEventPublisher,
            domainEventService,
            protectedVerificationTaskProperties
        );
    }

    @Test
    void getZasData_returnsDataWithoutMarkingTaskAsOpened() {
        var task = newTask();
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        var submission = submissionDto();

        when(protectedVerificationSubmissionApi.getProtectedVerificationSubmission(SUBMISSION_ID)).thenReturn(
            submission
        );
        when(
            usnApi.searchUsns(
                eq(SBN_ID),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(usnPageWith(usnDto()));

        var zasData = service.getZasData(task.getId());

        assertThat(zasData.businessId()).isEqualTo(SBN_ID);
        assertThat(zasData.organisation().name()).isEqualTo("Acme AG");
        // Reading the data is a side-effect-free GET - markZasDataReviewed is the separate action that records
        // the review.
        assertThat(task.hasOpenedZasData()).isFalse();
        verify(taskRepository, never()).save(any());
    }

    @Test
    void getZasData_noContent_throwsNotFound() {
        var task = newTask();
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        var submission = submissionDto();

        when(protectedVerificationSubmissionApi.getProtectedVerificationSubmission(SUBMISSION_ID)).thenReturn(
            submission
        );
        when(
            usnApi.searchUsns(
                eq(SBN_ID),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(new UsnPageDto().content(List.of()));

        var taskId = task.getId();
        assertThatThrownBy(() -> service.getZasData(taskId)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(task.hasOpenedZasData()).isFalse();
    }

    @Test
    void markZasDataReviewed_marksTaskAsOpened() {
        var task = newTask();
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        service.markZasDataReviewed(task.getId());

        assertThat(task.hasOpenedZasData()).isTrue();
        verify(taskRepository).save(task);
    }

    @Test
    void approve_blockedWhenZasDataNotOpened() {
        var task = newTask();
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        var taskId = task.getId();
        var request = new ApproveProtectedVerificationRequestTaskActionDto(null);

        assertThatThrownBy(() -> service.approve(taskId, request, "tester")).isInstanceOf(
            IllegalArgumentException.class
        );

        verify(outboxEventPublisher, never()).publishProtectedVerificationSubmissionApprovedEvent(any());
    }

    @Test
    void reject_blockedWhenZasDataNotOpened() {
        var task = newTask();
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        var taskId = task.getId();
        var request = new RejectProtectedVerificationRequestTaskActionDto("not valid", null);

        assertThatThrownBy(() -> service.reject(taskId, request, "tester")).isInstanceOf(
            IllegalArgumentException.class
        );

        verify(outboxEventPublisher, never()).publishProtectedVerificationSubmissionRejectedEvent(any());
    }

    @Test
    void approve_addsProtectedVerificationAuthorizationAndPublishesEvent() {
        var task = newTask();
        task.markZasDataOpened(Instant.now());
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        var submission = mock(ProtectedVerificationSubmissionDto.class);
        when(submission.getCategory()).thenReturn(ProtectedVerificationCategoryDto.PERSONAL_ADMINISTRATIVE_NUMBER);

        when(protectedVerificationSubmissionApi.getProtectedVerificationSubmission(SUBMISSION_ID)).thenReturn(
            submission
        );

        service.approve(task.getId(), new ApproveProtectedVerificationRequestTaskActionDto("ok"), "tester");

        assertThat(task.getStatus()).isEqualTo(TrustTaskStatus.ACCEPTED);

        var requestCaptor = ArgumentCaptor.forClass(ProtectedVerificationAuthorizationRequestDto.class);
        verify(businessPartnerIdentityService).addProtectedVerificationAuthorization(requestCaptor.capture());
        assertThat(requestCaptor.getValue().businessPartnerIdentityId()).isEqualTo(PARTNER_ID);
        assertThat(requestCaptor.getValue().protectedField()).isEqualTo(AuthorizableFieldDto.AHV_NUMBER);

        var eventCaptor = ArgumentCaptor.forClass(TiProtectedVerificationSubmissionApprovedEvent.class);
        verify(outboxEventPublisher).publishProtectedVerificationSubmissionApprovedEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getPayload().getProtectedVerificationSubmissionId()).isEqualTo(SUBMISSION_ID);

        verify(domainEventService).protectedVerificationRequestApproved(task.getId(), "tester", "ok");
    }

    @Test
    void reject_publishesRejectedEventWithReason() {
        var task = newTask();
        task.markZasDataOpened(Instant.now());
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        service.reject(
            task.getId(),
            new RejectProtectedVerificationRequestTaskActionDto("Not eligible", "internal"),
            "tester"
        );

        assertThat(task.getStatus()).isEqualTo(TrustTaskStatus.REJECTED);
        var eventCaptor = ArgumentCaptor.forClass(TiProtectedVerificationSubmissionRejectedEvent.class);
        verify(outboxEventPublisher).publishProtectedVerificationSubmissionRejectedEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getPayload().getRejectReason()).isEqualTo("Not eligible");
        verify(domainEventService).protectedVerificationRequestRejected(
            task.getId(),
            "tester",
            "Not eligible",
            "internal"
        );
        verify(businessPartnerIdentityService, never()).addProtectedVerificationAuthorization(any());
    }

    @Test
    void createTask_trustedPartner_createsTaskWithoutRejecting() {
        var submission = submissionDtoForCreateTask();
        when(protectedVerificationSubmissionApi.getProtectedVerificationSubmission(SUBMISSION_ID)).thenReturn(
            submission
        );
        when(taskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(businessPartnerIdentityService.businessPartnerIdentityExists(PARTNER_ID)).thenReturn(true);
        when(protectedVerificationTaskProperties.dueDatePeriod()).thenReturn(Duration.ofDays(30));

        var taskId = service.createTask(SUBMISSION_ID, "tester");

        verify(domainEventService).protectedVerificationRequestReceived(taskId, "tester");
        verify(outboxEventPublisher, never()).publishProtectedVerificationSubmissionRejectedEvent(any());
        verify(domainEventService, never()).protectedVerificationRequestRejected(any(), any(), any(), any());
    }

    @Test
    void createTask_setsDueAtBasedOnConfiguredDueDatePeriod() {
        var submission = submissionDtoForCreateTask();
        when(protectedVerificationSubmissionApi.getProtectedVerificationSubmission(SUBMISSION_ID)).thenReturn(
            submission
        );
        when(taskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(businessPartnerIdentityService.businessPartnerIdentityExists(PARTNER_ID)).thenReturn(true);
        when(protectedVerificationTaskProperties.dueDatePeriod()).thenReturn(Duration.ofDays(14));

        service.createTask(SUBMISSION_ID, "tester");

        var taskCaptor = ArgumentCaptor.forClass(ProtectedVerificationRequestTask.class);
        verify(taskRepository).save(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getDueAt()).isEqualTo(submission.getSubmittedAt().plus(Duration.ofDays(14)));
    }

    @Test
    void createTask_untrustedPartner_createsTaskThenAutoRejects() {
        var submission = submissionDtoForCreateTask();
        when(protectedVerificationSubmissionApi.getProtectedVerificationSubmission(SUBMISSION_ID)).thenReturn(
            submission
        );
        when(taskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(businessPartnerIdentityService.businessPartnerIdentityExists(PARTNER_ID)).thenReturn(false);
        when(protectedVerificationTaskProperties.dueDatePeriod()).thenReturn(Duration.ofDays(30));

        var taskId = service.createTask(SUBMISSION_ID, "tester");

        verify(domainEventService).protectedVerificationRequestReceived(taskId, "tester");
        verify(outboxEventPublisher).publishProtectedVerificationSubmissionRejectedEvent(any());
        verify(domainEventService).protectedVerificationRequestRejected(
            taskId,
            "System User",
            "Your organization is not yet onboarded to the trust registry. Please complete onboarding and resubmit this protected verification request.",
            null
        );
    }
}
