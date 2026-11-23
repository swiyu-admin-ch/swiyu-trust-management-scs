package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.VcSchemaPartnerLink;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.VcSchemaPartnerLinkRepository;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.VcSchema;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.VcSchemaRepository;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.VcSchemaService;
import ch.admin.bj.swiyu.trust.management.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.MockAuditPublisherTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@DataJpaTest
@Import(
    {
        VcSchemaPartnerLinkService.class,
        VcSchemaAuditService.class,
        VcSchemaService.class,
        DataJpaTestConfiguration.class,
        MockAuditPublisherTestConfiguration.class,
    }
)
@ActiveProfiles("test")
class VcSchemaPartnerLinkServiceIT {

    private static final String VC_SCHEMA_FILE =
        "{\"vct\":\"https://trust-reg.trust-infra.swiyu.admin.ch/api/v1/vc-schema/mySchema\"}";

    @Autowired
    VcSchemaPartnerLinkService vcSchemaPartnerLinkService;

    @Autowired
    VcSchemaPartnerLinkRepository vcSchemaPartnerLinkRepository;

    @Autowired
    VcSchemaRepository vcSchemaRepository;

    @Autowired
    AuditPublisher auditPublisher;

    private UUID partnerId;
    private UUID otherPartnerId;

    @BeforeEach
    void setUp() {
        reset(auditPublisher);
        vcSchemaPartnerLinkRepository.deleteAllInBatch();
        vcSchemaRepository.deleteAllInBatch();
        partnerId = UUID.randomUUID();
        otherPartnerId = UUID.randomUUID();
    }

    @Test
    void hardDeleteByPartnerId_deletesTheSchemasAndLinksOfThatPartnerWithAudit() {
        var vcSchema = vcSchemaRepository.saveAndFlush(new VcSchema("mySchema", VC_SCHEMA_FILE));
        vcSchemaPartnerLinkRepository.saveAndFlush(
            new VcSchemaPartnerLink(vcSchema.getId(), UUID.randomUUID(), partnerId)
        );
        var otherSchema = vcSchemaRepository.saveAndFlush(new VcSchema("otherSchema", VC_SCHEMA_FILE));
        vcSchemaPartnerLinkRepository.saveAndFlush(
            new VcSchemaPartnerLink(otherSchema.getId(), UUID.randomUUID(), otherPartnerId)
        );

        vcSchemaPartnerLinkService.hardDeleteByPartnerId(partnerId);

        assertThat(vcSchemaRepository.findById(vcSchema.getId())).isEmpty();
        assertThat(vcSchemaPartnerLinkRepository.findAll())
            .extracting(VcSchemaPartnerLink::getPartnerId)
            .containsOnly(otherPartnerId);
        assertThat(vcSchemaRepository.findById(otherSchema.getId())).isPresent();
        verify(auditPublisher).vcSchemaDeleted(
            eq(vcSchema.getId().toString()),
            eq(partnerId.toString()),
            eq(0L),
            anyString(),
            eq(VC_SCHEMA_FILE)
        );

        vcSchemaPartnerLinkService.hardDeleteByPartnerId(partnerId);
        verify(auditPublisher, times(1)).vcSchemaDeleted(anyString(), anyString(), eq(0L), anyString(), anyString());
    }

    @Test
    void hardDeleteByPartnerId_toleratesAnAlreadyDeletedSchemaAndSkipsTheAudit() {
        vcSchemaPartnerLinkRepository.saveAndFlush(
            new VcSchemaPartnerLink(UUID.randomUUID(), UUID.randomUUID(), partnerId)
        );

        vcSchemaPartnerLinkService.hardDeleteByPartnerId(partnerId);

        assertThat(vcSchemaPartnerLinkRepository.findAll()).isEmpty();
        verify(auditPublisher, never()).vcSchemaDeleted(anyString(), anyString(), anyLong(), anyString(), anyString());
    }
}
