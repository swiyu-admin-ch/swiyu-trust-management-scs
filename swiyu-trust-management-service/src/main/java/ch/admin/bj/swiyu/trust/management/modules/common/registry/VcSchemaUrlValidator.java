package ch.admin.bj.swiyu.trust.management.modules.common.registry;

import ch.admin.bj.swiyu.trust.management.modules.common.exception.VcSchemaUrlValidationFailedException;
import jakarta.validation.constraints.NotNull;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class VcSchemaUrlValidator {

    private final RegistryProperties registryProperties;
    private final Pattern vcSchemaUrlPattern;
    private static final String VC_SCHEMA_URL_REGEX = "/([a-zA-Z0-9/\\-_]*[a-zA-Z0-9])$";

    public VcSchemaUrlValidator(RegistryProperties registryProperties) {
        this.registryProperties = registryProperties;
        this.vcSchemaUrlPattern = Pattern.compile(
            Pattern.quote(registryProperties.getVcSchemaBaseUrl().toString()) + VC_SCHEMA_URL_REGEX
        );
    }

    public boolean isInvalidVcSchemaUrl(@NotNull String absoluteVcSchemaUrl) {
        try {
            validateIsValidVcSchemaUrl(absoluteVcSchemaUrl);
            return false;
        } catch (VcSchemaUrlValidationFailedException e) {
            return true;
        }
    }

    public void validateIsValidVcSchemaUrl(String absoluteVcSchemaUrl) {
        if (absoluteVcSchemaUrl == null) {
            throw new VcSchemaUrlValidationFailedException("The vc schema url must not be null.");
        }
        if (!absoluteVcSchemaUrl.startsWith(registryProperties.getVcSchemaBaseUrl().toString())) {
            throw new VcSchemaUrlValidationFailedException(
                "The vc schema url must start with " + registryProperties.getVcSchemaBaseUrl()
            );
        }
        getVctResourcePath(absoluteVcSchemaUrl); // This will throw an exception if the URL is invalid
    }

    /*
     * Extracts the user provided last path segment of the vct URL which served as unique identifier
     */
    public String getVctResourcePath(String vct) throws VcSchemaUrlValidationFailedException {
        var matcher = vcSchemaUrlPattern.matcher(vct);
        if (!matcher.find()) {
            throw new VcSchemaUrlValidationFailedException(
                "Vct does not match expected pattern for user provided last path segment: " + vcSchemaUrlPattern
            );
        }
        return matcher.group(1);
    }
}
