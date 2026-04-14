package ch.admin.bj.swiyu.trust.management.modules.ui.infrastructure.web.controller;

import ch.admin.bj.swiyu.trust.management.modules.ui.api.*;
import ch.admin.bj.swiyu.trust.management.modules.ui.infrastructure.web.config.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.tags.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.boot.info.*;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "FrontendConfiguration",
    description = "Serves configuration information for the frontend as specified by Quadrel-Services/QdAuth.",
    externalDocs = @ExternalDocumentation(
        url = "https://confluence.bit.admin.ch/display/DAZUIC/Integration+der+Qd-Auth+Library"
    )
)
@RestController
@RequestMapping("/ui-api/configuration")
@RequiredArgsConstructor
@Slf4j
public class FrontendConfigurationController {

    private final BuildProperties buildProperties;

    private final FrontendProperties frontendProperties;

    @GetMapping("/")
    public @NonNull AppConfigDto getConfiguration() {
        var authProperties = frontendProperties.auth();

        return new AppConfigDto(
            frontendProperties.environment(),
            new AuthConfigDto(
                authProperties.issuer(),
                authProperties.useSilentRefresh(),
                authProperties.clientId(),
                authProperties.scope(),
                authProperties.responseType(),
                authProperties.requireHttps()
            ),
            buildProperties.getVersion()
        );
    }
}
