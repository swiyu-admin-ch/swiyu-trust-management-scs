package ch.admin.bj.swiyu.trust.management.modules.jobs.service;

import ch.admin.bj.swiyu.trust.management.modules.management.api.BusinessPartnerIdentityStatusDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustStatementTypeDto;
import ch.admin.bj.swiyu.trust.management.modules.management.service.BusinessPartnerIdentityService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.StatusListService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TrustStatementService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exports the total number of business partner identities, status lists and partner links as Prometheus
 * metrics, broken down by business partner identity status and partner link type. The values are
 * (re-)evaluated by a scheduler according to {@code app.jobs.statistics.export-cron}. Set the cron
 * expression to {@code -} to disable scheduled updates; the initial update still runs at startup.
 */
@Configuration
@ConfigurationPropertiesScan
@RequiredArgsConstructor
public class TrustStatisticsMetricsExporterJob {

    private final BusinessPartnerIdentityService businessPartnerIdentityService;
    private final StatusListService statusListService;
    private final TrustStatementService trustStatementService;
    private final MeterRegistry meterRegistry;

    private final AtomicLong businessPartnerIdentityCount = new AtomicLong();
    private final AtomicLong statusListCount = new AtomicLong();
    private final AtomicLong partnerLinkCount = new AtomicLong();

    private final Map<BusinessPartnerIdentityStatusDto, AtomicLong> businessPartnerIdentityCountByStatus =
        new EnumMap<>(BusinessPartnerIdentityStatusDto.class);
    private final Map<TrustStatementTypeDto, AtomicLong> partnerLinkCountByType = new EnumMap<>(
        TrustStatementTypeDto.class
    );

    @PostConstruct
    public void setup() {
        Gauge.builder("swiyu_tms_business_partner_identity", businessPartnerIdentityCount, AtomicLong::get)
            .description("Total number of business partner identities")
            .register(meterRegistry);
        Gauge.builder("swiyu_tms_status_list", statusListCount, AtomicLong::get)
            .description("Total number of status lists")
            .register(meterRegistry);
        Gauge.builder("swiyu_tms_partner_link", partnerLinkCount, AtomicLong::get)
            .description("Total number of partner links")
            .register(meterRegistry);

        for (BusinessPartnerIdentityStatusDto status : BusinessPartnerIdentityStatusDto.values()) {
            var count = new AtomicLong();
            businessPartnerIdentityCountByStatus.put(status, count);
            Gauge.builder("swiyu_tms_business_partner_identity_by_status", count, AtomicLong::get)
                .description("Number of business partner identities")
                .tag("status", status.name())
                .register(meterRegistry);
        }

        for (TrustStatementTypeDto type : TrustStatementTypeDto.values()) {
            var count = new AtomicLong();
            partnerLinkCountByType.put(type, count);
            Gauge.builder("swiyu_tms_partner_link_by_type", count, AtomicLong::get)
                .description("Number of partner links")
                .tag("type", type.name())
                .register(meterRegistry);
        }
        // initial update
        refreshStatistics();
    }

    @Scheduled(cron = "${app.jobs.trust-statistics-metrics-exporter-cron}")
    @Transactional(readOnly = true)
    public void refreshStatisticsJob() {
        refreshStatistics();
    }

    void refreshStatistics() {
        businessPartnerIdentityCount.set(businessPartnerIdentityService.count());
        statusListCount.set(statusListService.count());
        partnerLinkCount.set(trustStatementService.count());

        for (var entry : businessPartnerIdentityCountByStatus.entrySet()) {
            entry.getValue().set(businessPartnerIdentityService.countByStatus(entry.getKey()));
        }

        for (var entry : partnerLinkCountByType.entrySet()) {
            entry.getValue().set(trustStatementService.countByType(entry.getKey()));
        }
    }
}
