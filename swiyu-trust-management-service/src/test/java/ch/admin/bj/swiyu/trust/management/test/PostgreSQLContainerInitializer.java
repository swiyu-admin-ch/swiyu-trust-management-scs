package ch.admin.bj.swiyu.trust.management.test;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Slf4j
public class PostgreSQLContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static PostgreSQLContainer<?> managementDatabase;
    private static PostgreSQLContainer<?> registryDatabase;

    private static PostgreSQLContainer<?> getManagementDatabase() {
        if (managementDatabase == null) {
            managementDatabase = new PostgreSQLContainer<>(
                DockerImageName.parse("docker-hub.nexus.bit.admin.ch/postgres:17.8").asCompatibleSubstituteFor(
                    "postgres:17.8"
                )
            );
            managementDatabase.start();
        }
        return managementDatabase;
    }

    private static PostgreSQLContainer<?> getRegistryDatabase() {
        if (registryDatabase == null) {
            registryDatabase = new PostgreSQLContainer<>(
                DockerImageName.parse("docker-hub.nexus.bit.admin.ch/postgres:17.8").asCompatibleSubstituteFor(
                    "postgres:17.8"
                )
            );
            registryDatabase.start();
        }
        return registryDatabase;
    }

    @Override
    public void initialize(@NotNull ConfigurableApplicationContext configurableApplicationContext) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            configurableApplicationContext,
            "spring.datasource.management-db.url=" + getManagementDatabase().getJdbcUrl(),
            "spring.datasource.management-db.username=" + getManagementDatabase().getUsername(),
            "spring.datasource.management-db.password=" + getManagementDatabase().getPassword()
        );
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            configurableApplicationContext,
            "spring.datasource.registry-db.url=" + getRegistryDatabase().getJdbcUrl(),
            "spring.datasource.registry-db.username=" + getRegistryDatabase().getUsername(),
            "spring.datasource.registry-db.password=" + getRegistryDatabase().getPassword()
        );
    }
}
