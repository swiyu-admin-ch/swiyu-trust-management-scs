package ch.admin.bj.swiyu.trust.management.modules.common.openapi;

import ch.admin.bj.swiyu.trust.management.*;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.*;
import lombok.*;
import org.springdoc.core.models.*;
import org.springframework.boot.info.*;
import org.springframework.context.annotation.*;

@Configuration
@RequiredArgsConstructor
public class OpenApiConfig {

    private final BuildProperties buildProperties;
    private static final String rootPackage = "ch.admin.bj.swiyu.trust.management";

    @Bean
    public OpenAPI openApi() {
        var openApi = new OpenAPI()
            .info(
                new Info()
                    .title("SWIYU Trust Management SCS API")
                    .description("IF-012 - APIs for the SWIYU Trust Management SCS")
                    .version(buildProperties.getVersion())
            )
            .addSecurityItem(new SecurityRequirement().addList("OIDC")) // OIDC schema is provided by jeap's SwaggerOauthConfiguration
            .components(new Components());
        addBearerAuthAsWorkaroundForOpenApiCodegen(openApi);
        return openApi;
    }

    /**
     * Workaround for <a href="https://github.com/OpenAPITools/openapi-generator/issues/19168">Github issue 19168</a>
     * when using generated api-doc with openapi codegen.
     */
    private void addBearerAuthAsWorkaroundForOpenApiCodegen(OpenAPI openApi) {
        var name = "bearer-jwt";
        openApi.getSecurity().getFirst().addList(name);
        openApi
            .getComponents()
            .addSecuritySchemes(
                name,
                new SecurityScheme()
                    .name("bearerAuth")
                    .description("JWT authentication")
                    .bearerFormat("jwt")
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .in(SecurityScheme.In.HEADER)
            );
    }

    @Bean
    GroupedOpenApi api() {
        return GroupedOpenApi.builder().group("B2B_API").pathsToMatch("/api/**").packagesToScan(rootPackage).build();
    }

    @Bean
    GroupedOpenApi uiApi() {
        return GroupedOpenApi.builder().group("UI_API").pathsToMatch("/ui-api/**").packagesToScan(rootPackage).build();
    }
}
