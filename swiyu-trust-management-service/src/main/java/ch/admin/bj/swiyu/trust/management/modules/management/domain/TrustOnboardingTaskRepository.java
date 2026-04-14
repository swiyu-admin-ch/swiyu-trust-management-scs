package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.querydsl.*;

public interface TrustOnboardingTaskRepository
    extends JpaRepository<TrustOnboardingTask, UUID>, QuerydslPredicateExecutor<TrustOnboardingTask>
{
    TrustOnboardingTask getTrustOnboardingTaskByTrustOnboardingSubmissionId(UUID id);

    void deleteAllByPartnerId(UUID partnerId);
}
