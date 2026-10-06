package ch.admin.bj.swiyu.trust.management.modules.management.service;

import ch.admin.bj.swiyu.trust.management.modules.management.domain.StatusListDomainService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class StatusListService {

    private final StatusListDomainService statusListDomainService;

    @Transactional(readOnly = true)
    public long count() {
        return statusListDomainService.count();
    }

    @Transactional
    public void triggerPublications() {
        statusListDomainService.triggerPublications();
    }
}
