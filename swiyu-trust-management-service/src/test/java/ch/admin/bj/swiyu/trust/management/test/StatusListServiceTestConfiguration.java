package ch.admin.bj.swiyu.trust.management.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.admin.bj.swiyu.trust.client.core.business.b2b.api.StatusB2BApi;
import ch.admin.bj.swiyu.trust.client.core.business.b2b.model.StatusListEntryCreationDtoDto;
import ch.admin.bj.swiyu.trust.management.modules.management.config.statements.DefaultStatementProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.StatusListDomainService;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness.CoreBusinessProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness.StatusRegistryProperties;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@TestConfiguration
@Import({ StatusListDomainService.class })
@EnableConfigurationProperties(
    { CoreBusinessProperties.class, DefaultStatementProperties.class, StatusRegistryProperties.class }
)
public class StatusListServiceTestConfiguration {

    @Bean
    @Primary
    public StatusB2BApi statusB2BApi() {
        var api = mock(StatusB2BApi.class);
        when(api.createStatusListEntry(any())).thenAnswer(_ -> {
            var id = UUID.randomUUID();
            return new StatusListEntryCreationDtoDto().statusRegistryUrl(("https://status.list.mock/" + id)).id(id);
        });
        return api;
    }
}
