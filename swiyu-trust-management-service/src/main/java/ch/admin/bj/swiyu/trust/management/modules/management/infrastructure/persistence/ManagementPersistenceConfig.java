package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.persistence;

import static ch.admin.bj.swiyu.trust.management.modules.common.persistence.TransactionManagerNames.MANAGEMENT_TRANSACTION_MANAGER;
import static ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.persistence.ManagementPersistenceConfig.MANAGEMENT_TRANSACTION_MANAGER_FACTORY;
import static ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.persistence.ManagementPersistenceConfig.PACKAGES_TO_SCAN;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * This configuration initializes the connection and flyway migration to the trust management database (non-public data).
 * <p>
 * Note: As of now the trust management scs connects to 2 databases. Once the replication is handled properly
 * everything will be merged into the management database with dedicated schemas.
 * <p>
 * 1. management database: stores the trust statement requests/metadata
 * 2. registry database: stores everything what is public (trust statement vc, vc-schema)
 */
@Slf4j
@RequiredArgsConstructor
@Configuration
@EnableJpaRepositories(
    basePackages = { PACKAGES_TO_SCAN },
    entityManagerFactoryRef = MANAGEMENT_TRANSACTION_MANAGER_FACTORY,
    transactionManagerRef = MANAGEMENT_TRANSACTION_MANAGER
)
public class ManagementPersistenceConfig {

    protected static final String PACKAGES_TO_SCAN = "ch.admin.bj.swiyu.trust.management.modules.management";
    protected static final String MANAGEMENT_TRANSACTION_MANAGER_FACTORY = "managementEntityManagerFactory";
    private static final String FLYWAY_LOCATION = "classpath:db/migration/management";
    private final FlywayMigrationStrategy flywayMigrationStrategy;
    private final JpaProperties jpaProperties;

    @Value("${spring.flyway.locations:}")
    private String additionalFlywayLocations;

    @Bean
    @ConfigurationProperties("spring.datasource.management-db")
    public DataSourceProperties managementDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "managementDataSource")
    public DataSource managementDataSource(DataSourceProperties managementDataSourceProperties) {
        var config = new HikariConfig();
        config.setJdbcUrl(managementDataSourceProperties.getUrl());
        config.setUsername(managementDataSourceProperties.getUsername());
        config.setPassword(managementDataSourceProperties.getPassword());
        config.setDriverClassName(managementDataSourceProperties.getDriverClassName());
        config.setSchema(jpaProperties.getProperties().get("hibernate.default_schema"));
        config.setPoolName("connection-pool-management-db");
        return new HikariDataSource(config);
    }

    @Primary // only needed for tests since no solution found to run it without this annotation
    @Bean(
        name = {
            MANAGEMENT_TRANSACTION_MANAGER_FACTORY, "entityManagerFactory", // Required by jeap messaging dependencies
        }
    )
    @DependsOn("managementFlyway")
    public LocalContainerEntityManagerFactoryBean managementEntityManagerFactory(DataSource managementDataSource) {
        var em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(managementDataSource);
        em.setPackagesToScan(PACKAGES_TO_SCAN, "ch.admin.bit.jeap.messaging");
        var vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaPropertyMap(jpaProperties.getProperties());
        return em;
    }

    @Bean(
        name = {
            MANAGEMENT_TRANSACTION_MANAGER, "transactionManager", // Required by jeap messaging dependencies
        }
    )
    @Primary // Required by transaction outbox because it relies on a default management transaction manager
    public PlatformTransactionManager managementTransactionManager(
        @Qualifier("managementDataSource") DataSource managementDataSource,
        @Qualifier(MANAGEMENT_TRANSACTION_MANAGER_FACTORY) LocalContainerEntityManagerFactoryBean factory
    ) {
        var manager = new JpaTransactionManager();
        manager.setJpaPropertyMap(jpaProperties.getProperties());
        manager.setDataSource(managementDataSource);
        manager.setEntityManagerFactory(factory.getObject());
        return manager;
    }

    @Bean
    public Flyway managementFlyway(@Qualifier("managementDataSource") DataSource managementDataSource) {
        log.debug("Migrating database {} ...", FLYWAY_LOCATION);
        var locations = new ArrayList<>(List.of(FLYWAY_LOCATION));
        if (additionalFlywayLocations != null && !additionalFlywayLocations.isBlank()) {
            var extraLocations = List.of(additionalFlywayLocations.split(","));
            log.debug("Adding additional flyway locations: {}", extraLocations);
            locations.addAll(extraLocations);
        }
        var flyway = Flyway.configure()
            .dataSource(managementDataSource)
            .defaultSchema(jpaProperties.getProperties().get("hibernate.default_schema"))
            .locations(locations.toArray(new String[0]))
            .load();
        flywayMigrationStrategy.migrate(flyway);
        return flyway;
    }

    @Primary // Require because managementEntityManagerFactory is annotated @Primary
    @Bean
    public LockProvider lockProvider(@Qualifier("managementDataSource") DataSource dataSource) {
        return new JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .usingDbTime()
                .build()
        );
    }

    @Bean
    public LockingTaskExecutor lockingTaskExecutor(LockProvider lockProvider) {
        return new DefaultLockingTaskExecutor(lockProvider);
    }
}
