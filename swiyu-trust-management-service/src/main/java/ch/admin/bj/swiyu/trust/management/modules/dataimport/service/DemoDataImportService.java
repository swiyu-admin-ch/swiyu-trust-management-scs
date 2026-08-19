package ch.admin.bj.swiyu.trust.management.modules.dataimport.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport.getCurrentUserName;

import ch.admin.bj.swiyu.trust.management.modules.common.security.SystemUserAuthentication;
import ch.admin.bj.swiyu.trust.management.modules.dataimport.domain.DemoData;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.DomainEventLogRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.service.DomainEventService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings({ "java:S1192", "java:S5803", "java:S1854" })
@Component
@Profile("test-data-injection")
@RequiredArgsConstructor
@Slf4j
public class DemoDataImportService {

    private final BusinessPartnerIdentityRepository businessPartnerIdentityRepository;
    private final DomainEventLogRepository domainEventLogRepository;
    private final DomainEventService domainEventService;
    private final ProtectedVerificationRepository protectedVerificationRepository;
    private final TrustOnboardingTaskRepository trustOnboardingTaskRepository;
    private final ProtectedIssuanceEntryRepository protectedIssuanceEntryRepository;
    private final ProtectedIssuanceAuthorizationRepository protectedIssuanceAuthorizationRepository;

    public void setSystemSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(new SystemUserAuthentication());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteTrustOnboardingTasks() {
        log.debug("Delete demo business partner onboarding tasks ...");
        for (var demoCase : DemoData.DemoCase.values()) {
            domainEventLogRepository.deleteAllByTrustTaskPartnerId(demoCase.bp.id());
            trustOnboardingTaskRepository.deleteAllByPartnerId(demoCase.bp.id());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteBusinessPartnerIdentities() {
        log.debug("Delete demo business partner identity entries ...");
        for (var demoCase : DemoData.DemoCase.values()) {
            businessPartnerIdentityRepository.deleteById(demoCase.bp.id());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteProtectedVerificationAuthorizations() {
        log.debug("Delete demo business partner protected verification authorization entries ...");
        for (var demoCase : DemoData.DemoCase.values()) {
            protectedVerificationRepository.deleteByBusinessPartnerIdentityId(demoCase.bp.id());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteProtectedIssuanceEntriesAndAuthorizations() {
        // Delete all Authorizations for all Demo BPs
        for (var demoCase : DemoData.DemoCase.values()) {
            protectedIssuanceAuthorizationRepository.deleteByBusinessPartnerIdentityId(demoCase.bp.id());
        }
        // Delete all Entries
        Arrays.stream(DemoData.DemoCase.values())
            .filter(d -> d.bp.bpi() != null)
            .forEach(d ->
                d.bp
                    .bpi()
                    .protectedIssuanceAuthorizations()
                    .forEach(pia -> {
                        // Delete all Authorizations for BPs connected to demo entries
                        protectedIssuanceAuthorizationRepository.deleteByProtectedIssuanceEntryId(
                            pia.protectedVctEntryId()
                        );
                        // Delete all demo entries
                        protectedIssuanceEntryRepository.deleteById(pia.protectedVctEntryId());
                    })
            );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loadBusinessPartnerIdentities() {
        log.debug("Importing demo business partner identities entries ...");
        var bpis = Arrays.stream(DemoData.DemoCase.values())
            .filter(demoCase -> demoCase.bp.bpi() != null)
            .map(demoCase -> DemoDataMapper.toBusinessPartnerIdentity(demoCase.bp))
            .toList();

        for (var bpi : bpis) {
            var dbEntity = businessPartnerIdentityRepository.findById(bpi.getId()).orElseGet(() -> bpi);
            dbEntity.overrideFrom(bpi);
            businessPartnerIdentityRepository.saveAndFlush(dbEntity);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loadProtectedVerificationAuthorizations() {
        log.debug("Importing demo business partner identities protected verification authorizations ...");
        List<ProtectedVerificationAuthorization> pvas = new ArrayList<>();

        Arrays.stream(DemoData.DemoCase.values())
            .filter(demoCase -> demoCase.bp.bpi() != null)
            .filter(demoCase -> !demoCase.bp.bpi().protectedVerificationAuthorizations().isEmpty())
            .forEach(demoCase ->
                demoCase.bp
                    .bpi()
                    .protectedVerificationAuthorizations()
                    .forEach(demoPva ->
                        pvas.add(DemoDataMapper.toProtectedVerificationAuthorization(demoCase.bp, demoPva))
                    )
            );

        for (var pva : pvas) {
            var optDbEntity = protectedVerificationRepository.findById(pva.getId());
            if (optDbEntity.isPresent()) {
                var dbEntity = optDbEntity.get();
                dbEntity.overrideFrom(pva);
                protectedVerificationRepository.saveAndFlush(dbEntity);
            } else {
                protectedVerificationRepository.save(pva);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loadProtectedIssuanceDemoData() {
        Arrays.stream(DemoData.DemoCase.values())
            .filter(d -> d.bp.bpi() != null)
            .forEach(d ->
                d.bp
                    .bpi()
                    .protectedIssuanceAuthorizations()
                    .forEach(pia -> {
                        // Add demo entry if not present
                        protectedIssuanceEntryRepository
                            .findById(pia.protectedVctEntryId())
                            .orElseGet(() ->
                                protectedIssuanceEntryRepository.save(
                                    new ProtectedIssuanceEntry(
                                        pia.protectedVctEntryId(),
                                        pia.vct(),
                                        java.time.Instant.now(),
                                        pia.vctName()
                                    )
                                )
                            );
                        // Add Authorization
                        protectedIssuanceAuthorizationRepository
                            .findById(pia.protectedVctEntryId())
                            .orElseGet(() ->
                                protectedIssuanceAuthorizationRepository.save(
                                    new ProtectedIssuanceAuthorization(
                                        pia.id(),
                                        d.bp.id(),
                                        pia.protectedVctEntryId(),
                                        pia.reason()
                                    )
                                )
                            );
                    })
            );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loadTrustOnboardingTasks() {
        log.debug("Importing demo business partner onboarding tasks ...");
        List<TrustOnboardingTask> data = new ArrayList<>();

        Arrays.stream(DemoData.DemoCase.values()).forEach(demoCase ->
            demoCase.bp
                .trustOnboardings()
                .forEach(onboarding -> {
                    if (onboarding.task() != null) {
                        var task = DemoDataMapper.toTrustOnboardingTask(demoCase.bp, onboarding);
                        if (
                            onboarding.task().status() !=
                            DemoData.DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingTask.DemoTrustTaskStatus.OPENED
                        ) {
                            task.changeStatus(DemoDataMapper.toTrustTaskStatus(onboarding.task().status()));
                        }
                        data.add(task);
                    }
                })
        );

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
