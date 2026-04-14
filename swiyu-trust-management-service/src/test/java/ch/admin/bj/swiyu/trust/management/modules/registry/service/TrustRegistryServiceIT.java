package ch.admin.bj.swiyu.trust.management.modules.registry.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.persistence.TransactionManagerNames.REGISTRY_TRANSACTION_MANAGER;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.trust.management.modules.registry.api.DatastoreStatusDto;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.DatastoreEntityRepository;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.VcEntityRepository;
import ch.admin.bj.swiyu.trust.management.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.IssuerTestData;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@DataJpaTest
@Import({ TrustRegistryService.class, DataJpaTestConfiguration.class })
@ActiveProfiles("test")
class TrustRegistryServiceIT {

    @Autowired
    VcEntityRepository vcEntityRepository;

    @Autowired
    DatastoreEntityRepository datastoreEntityRepository;

    @Autowired
    TrustRegistryService trustRegistryService;

    @Transactional(transactionManager = REGISTRY_TRANSACTION_MANAGER)
    @BeforeEach
    void setUp() {
        vcEntityRepository.deleteAllInBatch();
        datastoreEntityRepository.deleteAllInBatch();
    }

    @Test
    void createTrustStatementVc() {
        // GIVEN
        var rawVc = IssuerTestData.sdjwt();
        // WHEN
        var id = trustRegistryService.createTrustStatementVc(rawVc).id();
        // THEN
        var status = trustRegistryService.getStatus(id);
        assertThat(status).isEqualTo(DatastoreStatusDto.ACTIVE);
    }
}
