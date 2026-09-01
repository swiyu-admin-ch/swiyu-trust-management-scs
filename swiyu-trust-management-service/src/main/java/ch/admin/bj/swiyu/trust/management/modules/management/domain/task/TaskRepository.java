package ch.admin.bj.swiyu.trust.management.modules.management.domain.task;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.querydsl.*;

public interface TaskRepository extends JpaRepository<Task, UUID>, QuerydslPredicateExecutor<Task> {}
