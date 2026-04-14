package ch.admin.bj.swiyu.trust.management.modules.management.domain.details;

import static ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementType.TRUST_STATEMENT_METADATA_V1;
import static org.springframework.util.StringUtils.hasText;

import com.fasterxml.jackson.annotation.*;
import java.util.Arrays;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents the details of a Trust Statement of type MetadataV1.
 * This class contains metadata such as preferred language, organization name,
 * and logo URI in multiple languages.
 * See <a href="https://github.com/admin-ch-ssi/specifications-to-publish/blob/42fe685cabdadac7b171afb1bc7c05799e1fc9a9/trust-protocol-v0.1.md">spec</a>
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Getter
public final class MetadataV1Details extends TrustStatementDetails {

    private Language preferredLanguage;
    private Map<Language, String> orgName;
    private Map<Language, String> logoUri;

    MetadataV1Details() {
        super(TRUST_STATEMENT_METADATA_V1);
    }

    public MetadataV1Details(Language preferredLanguage, Map<Language, String> orgName, Map<Language, String> logoUri) {
        this();
        this.preferredLanguage = preferredLanguage;
        this.orgName = orgName;
        this.logoUri = logoUri;
    }

    @JsonIgnore
    public boolean hasAnyLogoUri() {
        return Arrays.stream(Language.values()).anyMatch(this::hasLogoUri);
    }

    @JsonIgnore
    public boolean hasLogoUri(Language lang) {
        if (logoUri == null) {
            return false;
        }
        return switch (lang) {
            case EN -> hasText(logoUri.get(Language.EN));
            case DE_CH -> hasText(logoUri.get(Language.DE_CH));
            case FR_CH -> hasText(logoUri.get(Language.FR_CH));
            case IT_CH -> hasText(logoUri.get(Language.IT_CH));
            case RM_CH -> hasText(logoUri.get(Language.RM_CH));
        };
    }

    public boolean hasOrgName(Language lang) {
        if (orgName == null) {
            return false;
        }
        return switch (lang) {
            case EN -> hasText(orgName.get(Language.EN));
            case DE_CH -> hasText(orgName.get(Language.DE_CH));
            case FR_CH -> hasText(orgName.get(Language.FR_CH));
            case IT_CH -> hasText(orgName.get(Language.IT_CH));
            case RM_CH -> hasText(orgName.get(Language.RM_CH));
        };
    }

    @AllArgsConstructor
    public enum Language {
        EN("en"),
        DE_CH("de-CH"),
        FR_CH("fr-CH"),
        IT_CH("it-CH"),
        RM_CH("rm-CH");

        private final String jsonValue;

        @JsonValue
        public String getJsonValue() {
            return jsonValue;
        }

        @JsonCreator
        public static Language fromJsonValue(String value) {
            for (Language language : values()) {
                if (language.jsonValue.equals(value)) {
                    return language;
                }
            }
            throw new IllegalArgumentException("Unknown language: " + value);
        }
    }
}
