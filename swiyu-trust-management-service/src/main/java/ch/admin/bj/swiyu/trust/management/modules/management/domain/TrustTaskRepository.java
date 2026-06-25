package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.querydsl.*;

public interface TrustTaskRepository extends JpaRepository<TrustTask, UUID>, QuerydslPredicateExecutor<TrustTask> {}
