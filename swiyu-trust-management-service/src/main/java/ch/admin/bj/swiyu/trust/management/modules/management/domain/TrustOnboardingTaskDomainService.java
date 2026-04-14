package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import ch.admin.bj.swiyu.trust.management.modules.common.exception.*;
import java.util.*;
import lombok.*;
import org.springframework.stereotype.*;

@Service
@RequiredArgsConstructor
public class TrustOnboardingTaskDomainService {

    private final TrustOnboardingTaskRepository taskRepository;

    public TrustOnboardingTask getTrustOnboardingTask(UUID taskId) {
        return taskRepository
            .findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task with id " + taskId + " not found"));
    }
}
