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
}
