package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.management.api.TrustOnboardingTaskActionDto.*;
import static ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustTaskStatus.*;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.TrustOnboardingTaskActionsResolver.resolvePossibleActions;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustOnboardingTaskActionDto;
import org.junit.jupiter.api.Test;

class TrustOnboardingTaskActionsResolverTest {

    private static final String ASSIGNEE = null;
    private static final String CURRENT_USER_FULLNAME = "Timo Truster";

    @Test
    void resolvePossibleActions_Opened() {
        // GIVEN / WHEN
        var actions = resolvePossibleActions(OPENED, ASSIGNEE, CURRENT_USER_FULLNAME);
        // THEN
        assertThat(actions).containsExactlyInAnyOrder(
            REJECT,
            APPROVE,
            REQUEST_MORE_INFORMATION,
            TrustOnboardingTaskActionDto.ADD_INTERNAL_NOTE,
            TrustOnboardingTaskActionDto.ASSIGN_SELF
        );
    }

    @Test
    void resolvePossibleActions_Rejected() {
        // GIVEN / WHEN
        var actions = resolvePossibleActions(REJECTED, ASSIGNEE, CURRENT_USER_FULLNAME);
        // THEN
        assertThat(actions).containsExactlyInAnyOrder(ADD_INTERNAL_NOTE, ASSIGN_SELF);
    }

    @Test
    void resolvePossibleActions_ACCEPTED() {
        // GIVEN / WHEN
        var actions = resolvePossibleActions(ACCEPTED, ASSIGNEE, CURRENT_USER_FULLNAME);
        // THEN
        assertThat(actions).containsExactlyInAnyOrder(ADD_INTERNAL_NOTE, ASSIGN_SELF);
    }

    @Test
    void resolvePossibleActions_InformationRequested() {
        // GIVEN / WHEN
        var actions = resolvePossibleActions(INFORMATION_REQUESTED, ASSIGNEE, CURRENT_USER_FULLNAME);
        // THEN
        assertThat(actions).containsExactlyInAnyOrder(REJECT, APPROVE, ADD_INTERNAL_NOTE, ASSIGN_SELF);
    }

    @Test
    void resolvePossibleActions_Resubmitted() {
        // GIVEN / WHEN
        var actions = resolvePossibleActions(RESUBMITTED, ASSIGNEE, CURRENT_USER_FULLNAME);
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
        var actions = resolvePossibleActions(RESUBMITTED, ASSIGNEE, CURRENT_USER_FULLNAME, false);
        // THEN
        assertThat(actions).containsExactlyInAnyOrder(REJECT, APPROVE, ADD_INTERNAL_NOTE, ASSIGN_SELF);
    }

    @Test
    void resolvePossibleActions_Opened_ReviewPreconditionsNotMet_ExcludesApproveAndReject() {
        // GIVEN / WHEN - e.g. a protected verification request whose ZAS data hasn't been reviewed yet
        var actions = resolvePossibleActions(OPENED, ASSIGNEE, CURRENT_USER_FULLNAME, false, false);
        // THEN
        assertThat(actions).containsExactlyInAnyOrder(ADD_INTERNAL_NOTE, ASSIGN_SELF);
    }

    @Test
    void resolvePossibleActions_Opened_ReviewPreconditionsMet_IncludesApproveAndReject() {
        // GIVEN / WHEN - e.g. a protected verification request whose ZAS data has already been reviewed, so
        // approve/reject are allowed regardless of which view (list or detail) triggers the action
        var actions = resolvePossibleActions(OPENED, ASSIGNEE, CURRENT_USER_FULLNAME, false, true);
        // THEN
        assertThat(actions).containsExactlyInAnyOrder(REJECT, APPROVE, ADD_INTERNAL_NOTE, ASSIGN_SELF);
    }
}
