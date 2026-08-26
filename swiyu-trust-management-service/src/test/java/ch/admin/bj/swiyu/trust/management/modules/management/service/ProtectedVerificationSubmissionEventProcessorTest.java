package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.test.TrustOnboardingTestData.tiProtectedVerificationSubmissionAcceptedEvent;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProtectedVerificationSubmissionEventProcessorTest {

    private static final UUID SUBMISSION_ID = UUID.randomUUID();

    @Mock
    private ProtectedVerificationRequestTaskService taskService;

    private ProtectedVerificationSubmissionEventProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new ProtectedVerificationSubmissionEventProcessor(taskService);
    }

    @Test
    void delegatesTaskCreationToTaskService() {
        processor.processTiProtectedVerificationSubmissionAcceptedEvent(
            tiProtectedVerificationSubmissionAcceptedEvent(SUBMISSION_ID)
        );

        verify(taskService).createTask(eq(SUBMISSION_ID), any());
    }
}
