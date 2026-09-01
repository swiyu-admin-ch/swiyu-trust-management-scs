package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.TaskActionDto.*;
import static ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TaskStatus.*;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.TaskActionsResolver.resolvePossibleActions;
import static ch.admin.bj.swiyu.trust.management.test.TaskTestData.protectedVerificationRequestTask;
import static ch.admin.bj.swiyu.trust.management.test.TaskTestData.trustOnboardingTask;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.TaskActionDto;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TaskActionsResolverTest {

    private static final String CURRENT_USER_FULLNAME = "Timo Truster";

    @Test
    void resolvePossibleActions_Opened() {
        // GIVEN / WHEN
        var task = trustOnboardingTask(OPENED, null /* no assignee */);
        var actions = resolvePossibleActions(task, CURRENT_USER_FULLNAME);
        // THEN
        assertThat(actions).containsExactlyInAnyOrder(
            REJECT,
            APPROVE,
            REQUEST_MORE_INFORMATION,
            TaskActionDto.ADD_INTERNAL_NOTE,
            TaskActionDto.ASSIGN_SELF
        );
    }

    @Test
    void resolvePossibleActions_Rejected() {
        // GIVEN / WHEN
        var task = trustOnboardingTask(REJECTED, null /* no assignee */);
        var actions = resolvePossibleActions(task, CURRENT_USER_FULLNAME);
        // THEN
        assertThat(actions).containsExactlyInAnyOrder(ADD_INTERNAL_NOTE, ASSIGN_SELF);
    }

    @Test
    void resolvePossibleActions_ACCEPTED() {
        // GIVEN / WHEN
        var task = trustOnboardingTask(ACCEPTED, null /* no assignee */);
        var actions = resolvePossibleActions(task, CURRENT_USER_FULLNAME);
        // THEN
        assertThat(actions).containsExactlyInAnyOrder(ADD_INTERNAL_NOTE, ASSIGN_SELF);
    }

    @Test
    void resolvePossibleActions_InformationRequested() {
        // GIVEN / WHEN
        var task = trustOnboardingTask(INFORMATION_REQUESTED, null /* no assignee */);
        var actions = resolvePossibleActions(task, CURRENT_USER_FULLNAME);
        // THEN
        assertThat(actions).containsExactlyInAnyOrder(REJECT, APPROVE, ADD_INTERNAL_NOTE, ASSIGN_SELF);
    }

    @Test
    void resolvePossibleActions_Resubmitted() {
        // GIVEN / WHEN
        var task = trustOnboardingTask(RESUBMITTED, null /* no assignee */);
        var actions = resolvePossibleActions(task, CURRENT_USER_FULLNAME);
        // THEN
        assertThat(actions).containsExactlyInAnyOrder(
            REJECT,
            APPROVE,
            REQUEST_MORE_INFORMATION,
            ADD_INTERNAL_NOTE,
            ASSIGN_SELF
        );
    }

    @Test
    void resolvePossibleActions_Resubmitted_ResubmissionCapReached() {
        // GIVEN / WHEN
        var task = trustOnboardingTask(RESUBMITTED, null /* no assignee */);
        // resubmit again, so max amount is reached
        task.requestMoreInformation(Instant.now());
        task.resubmit(Instant.now());
        var actions = resolvePossibleActions(task, CURRENT_USER_FULLNAME);
        // THEN
        assertThat(actions).containsExactlyInAnyOrder(REJECT, APPROVE, ADD_INTERNAL_NOTE, ASSIGN_SELF);
    }

    @Test
    void resolvePossibleActions_Opened_ProtectedVerificationRequestTask_WithoutZas() {
        // GIVEN / WHEN - e.g. a protected verification request whose ZAS data hasn't been reviewed yet
        var task = protectedVerificationRequestTask();
        var actions = resolvePossibleActions(task, CURRENT_USER_FULLNAME);
        // THEN
        assertThat(actions).containsExactlyInAnyOrder(ADD_INTERNAL_NOTE, ASSIGN_SELF);
    }

    @Test
    void resolvePossibleActions_Opened_ProtectedVerificationRequestTask_WithZas() {
        // GIVEN / WHEN - e.g. a protected verification request whose ZAS data has already been reviewed, so
        // approve/reject are allowed regardless of which view (list or detail) triggers the action
        var task = protectedVerificationRequestTask();
        task.markZasDataOpened(Instant.now());
        var actions = resolvePossibleActions(task, CURRENT_USER_FULLNAME);
        // THEN
        assertThat(actions).containsExactlyInAnyOrder(REJECT, APPROVE, ADD_INTERNAL_NOTE, ASSIGN_SELF);
    }
}
