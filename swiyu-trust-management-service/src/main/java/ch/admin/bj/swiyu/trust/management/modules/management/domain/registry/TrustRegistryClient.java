package ch.admin.bj.swiyu.trust.management.modules.management.domain.registry;

import ch.admin.bj.swiyu.trust.management.modules.common.registry.VcSchemaUrlValidator;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

@Slf4j
@RequiredArgsConstructor
public class TrustRegistryClient {

    private final RestClient restClient;
    private final VcSchemaUrlValidator vcSchemaUrlValidator;

    public boolean checkVcSchemaExists(@NotNull String absoluteVcSchemaUrl) {
        vcSchemaUrlValidator.validateIsValidVcSchemaUrl(absoluteVcSchemaUrl);
        var status = restClient.get().uri(absoluteVcSchemaUrl).retrieve().toEntity(Void.class).getStatusCode();
        return status.isSameCodeAs(HttpStatus.OK);
    }
}
