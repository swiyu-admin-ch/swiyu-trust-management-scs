package ch.admin.bj.swiyu.trust.management.modules.ui.infrastructure.web.config;

import jakarta.validation.*;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.*;

@Validated
@ConfigurationProperties(prefix = "cors")
public record CorsProperties(@Valid @NotNull @NotEmpty List<String> allowedOrigins) {}
