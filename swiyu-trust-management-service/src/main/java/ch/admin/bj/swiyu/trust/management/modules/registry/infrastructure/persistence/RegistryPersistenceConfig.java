package ch.admin.bj.swiyu.trust.management.modules.registry.infrastructure.persistence;

import static ch.admin.bj.swiyu.trust.management.modules.common.persistence.TransactionManagerNames.REGISTRY_TRANSACTION_MANAGER;
import static ch.admin.bj.swiyu.trust.management.modules.registry.infrastructure.persistence.RegistryPersistenceConfig.PACKAGES_TO_SCAN;
import static ch.admin.bj.swiyu.trust.management.modules.registry.infrastructure.persistence.RegistryPersistenceConfig.REGISTRY_TRANSACTION_MANAGER_FACTORY;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * This configuration initializes the connection and flyway migration to the public trust registry database.
 * <p>
 * Note: As of now the trust management service connects to 2 databases. Once the replication is handled properly
 * everything will be merged into the management database with dedicated schemas.
 * <p>
 * 1. management database: stores the trust statement requests/metadata
 * 2. registry database: stores everything what is public (trust statement vc, vc-schema)
 */
@Slf4j
@RequiredArgsConstructor
@Configuration
@EnableJpaRepositories(
    basePackages = PACKAGES_TO_SCAN,
    entityManagerFactoryRef = REGISTRY_TRANSACTION_MANAGER_FACTORY,
    transactionManagerRef = REGISTRY_TRANSACTION_MANAGER
)
public class RegistryPersistenceConfig {

    protected static final String PACKAGES_TO_SCAN = "ch.admin.bj.swiyu.trust.management.modules.registry";
    protected static final String REGISTRY_TRANSACTION_MANAGER_FACTORY = "registryEntityManagerFactory";
    private static final String FLYWAY_LOCATION = "classpath:db/migration/registry";
    private final FlywayMigrationStrategy flywayMigrationStrategy;
    private final JpaProperties jpaProperties;

    @Value("${spring.flyway.locations:}")
    private String additionalFlywayLocations;

    @Bean
    @ConfigurationProperties("spring.datasource.registry-db")
    public DataSourceProperties registryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "registryDataSource")
    public DataSource registryDataSource(DataSourceProperties registryDataSourceProperties) {
        var config = new HikariConfig();
        config.setJdbcUrl(registryDataSourceProperties.getUrl());
        config.setUsername(registryDataSourceProperties.getUsername());
        config.setPassword(registryDataSourceProperties.getPassword());
        config.setDriverClassName(registryDataSourceProperties.getDriverClassName());
        config.setSchema(jpaProperties.getProperties().get("hibernate.default_schema"));
        config.setPoolName("connection-pool-registry-db");
        return new HikariDataSource(config);
    }

    @Bean("registryEntityManagerFactory")
    @DependsOn("registryFlyway")
    public LocalContainerEntityManagerFactoryBean registryEntityManagerFactory(DataSource registryDataSource) {
        var em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(registryDataSource);
        em.setPackagesToScan(PACKAGES_TO_SCAN);
        var vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaPropertyMap(jpaProperties.getProperties());
        return em;
    }

    @Bean(REGISTRY_TRANSACTION_MANAGER)
    public PlatformTransactionManager registryTransactionManager(
        @Qualifier("registryDataSource") DataSource registryDataSource,
        @Qualifier(REGISTRY_TRANSACTION_MANAGER_FACTORY) LocalContainerEntityManagerFactoryBean factory
    ) {
        var manager = new JpaTransactionManager();
        manager.setJpaPropertyMap(jpaProperties.getProperties());
        manager.setDataSource(registryDataSource);
        manager.setEntityManagerFactory(factory.getObject());
        return manager;
    }

    @Bean
    public Flyway registryFlyway(@Qualifier("registryDataSource") DataSource registryDataSource) {
        log.debug("Migrating database {} ...", FLYWAY_LOCATION);
        var locations = new ArrayList<>(List.of(FLYWAY_LOCATION));
        if (additionalFlywayLocations != null && !additionalFlywayLocations.isBlank()) {
            var extraLocations = List.of(additionalFlywayLocations.split(","));
            log.debug("Adding additional flyway locations: {}", extraLocations);
            locations.addAll(extraLocations);
        }
        var flyway = Flyway.configure()
            .dataSource(registryDataSource)
            .defaultSchema(jpaProperties.getProperties().get("hibernate.default_schema"))
            .locations(locations.toArray(new String[0]))
            .load();
        flywayMigrationStrategy.migrate(flyway);
        return flyway;
    }
}
