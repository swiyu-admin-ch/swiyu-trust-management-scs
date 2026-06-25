package ch.admin.bj.swiyu.trust.management.modules.dataimport.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport.*;

import ch.admin.bj.swiyu.trust.management.modules.common.security.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.*;
import ch.admin.bj.swiyu.trust.management.modules.management.service.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.context.annotation.*;
import org.springframework.security.core.context.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

@SuppressWarnings({ "java:S1192", "java:S5803", "java:S1854" })
@Component
@Profile("test-data-injection")
@RequiredArgsConstructor
@Slf4j
public class DemoDataImportService {

    private final TrustOnboardingTaskRepository trustOnboardingTaskRepository;
    private final DomainEventLogRepository domainEventLogRepository;
    private final DomainEventService domainEventService;

    public void setSystemSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(new SystemUserAuthentication());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteTrustOnboardingTasks() {
        domainEventLogRepository.deleteAllByTrustTaskPartnerId(CoreDemoData.CORE_ID_BP_DEFAULT);
        domainEventLogRepository.deleteAllByTrustTaskPartnerId(CoreDemoData.CORE_ID_BP_WANTS_TO_BE_TRUSTED);
        domainEventLogRepository.deleteAllByTrustTaskPartnerId(CoreDemoData.CORE_ID_BP_GOV);
        domainEventLogRepository.deleteAllByTrustTaskPartnerId(CoreDemoData.CORE_ID_BP_BASE_ONBOARDING_ONLY);
        trustOnboardingTaskRepository.deleteAllByPartnerId(CoreDemoData.CORE_ID_BP_DEFAULT);
        trustOnboardingTaskRepository.deleteAllByPartnerId(CoreDemoData.CORE_ID_BP_WANTS_TO_BE_TRUSTED);
        trustOnboardingTaskRepository.deleteAllByPartnerId(CoreDemoData.CORE_ID_BP_GOV);
        trustOnboardingTaskRepository.deleteAllByPartnerId(CoreDemoData.CORE_ID_BP_BASE_ONBOARDING_ONLY);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loadTrustOnboardingTasks() {
        List<TrustOnboardingTask> data = new ArrayList<>();
        TrustOnboardingTask task;

        task = new TrustOnboardingTask(
            TmsDemoData.TMS_ID_TOT_SUCCEEDED,
            CoreDemoData.CORE_ID_BP_DEFAULT,
            CoreDemoData.CORE_ID_BP_DEFAULT_NAMES,
            CoreDemoData.CORE_ID_TOS_SUCCEEDED,
            Instant.now().minus(5, ChronoUnit.DAYS),
            Instant.now().minus(35, ChronoUnit.DAYS)
        );
        task.changeStatus(TrustTaskStatus.ACCEPTED);
        data.add(task);

        task = new TrustOnboardingTask(
            TmsDemoData.TMS_ID_TOT_INFO_REQUESTED,
            CoreDemoData.CORE_ID_BP_GOV,
            CoreDemoData.CORE_ID_BP_GOV_NAMES,
            CoreDemoData.CORE_ID_TOS_INFO_REQUESTED,
            Instant.now().plus(29, ChronoUnit.DAYS),
            Instant.now().minus(1, ChronoUnit.DAYS)
        );
        task.changeStatus(TrustTaskStatus.INFORMATION_REQUESTED);
        data.add(task);

        task = new TrustOnboardingTask(
            TmsDemoData.TMS_ID_TOT_REJECTED,
            CoreDemoData.CORE_ID_BP_WANTS_TO_BE_TRUSTED,
            CoreDemoData.CORE_ID_BP_WANTS_TO_BE_TRUSTED_NAMES,
            CoreDemoData.CORE_ID_TOS_REJECTED,
            Instant.now().plus(1000, ChronoUnit.DAYS),
            Instant.now().minus(1, ChronoUnit.DAYS)
        );
        task.changeStatus(TrustTaskStatus.REJECTED);
        data.add(task);

        task = new TrustOnboardingTask(
            TmsDemoData.TMS_ID_TOT_SUBMITTED,
            CoreDemoData.CORE_ID_BP_DEFAULT,
            CoreDemoData.CORE_ID_BP_DEFAULT_NAMES,
            CoreDemoData.CORE_ID_TOS_SUBMITTED,
            Instant.now().plus(1000, ChronoUnit.DAYS),
            Instant.now().minus(1, ChronoUnit.DAYS)
        );
        data.add(task);
        for (var d : data) {
            var optDbEntity = trustOnboardingTaskRepository.findById(d.getId());
            if (optDbEntity.isPresent()) {
                var dbEntity = optDbEntity.get();
                dbEntity.overwriteFrom(d);
                trustOnboardingTaskRepository.saveAndFlush(dbEntity);
            } else {
                var newTask = trustOnboardingTaskRepository.save(d);
                domainEventService.trustOnboardingSubmissionReceived(newTask.getId(), getCurrentUserName());
            }
        }
    }
}
