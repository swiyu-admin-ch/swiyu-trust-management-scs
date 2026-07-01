package ch.admin.bj.swiyu.trust.management.modules.management.domain.details;

import static ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementPartnerLinkType.TRUST_STATEMENT_IDENTITY_V1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.util.StringUtils;

/**
 * Represents the details of a trust statement of type TrustStatementIdentityV1.
 * See <a href="https://github.com/admin-ch-ssi/specifications-to-publish/blob/feat/EID-4989/trust-protocol-v1.0.md">spec</a>
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Getter
public final class IdentityV1Details extends TrustStatementDetails {

    private Map<Language, String> entityName;
    private Boolean isStateActor;
    private List<RegistryId> registryIds;

    IdentityV1Details() {
        super(TRUST_STATEMENT_IDENTITY_V1);
    }

    public IdentityV1Details(Map<Language, String> entityName, Boolean isStateActor, List<RegistryId> registryIds) {
        this();
        this.entityName = entityName;
        this.isStateActor = isStateActor;
        this.registryIds = registryIds;
    }

    @JsonIgnore
    public boolean hasEntityNameInAnyLanguage() {
        return (
            entityName != null && !entityName.isEmpty() && entityName.values().stream().anyMatch(StringUtils::hasText)
        );
    }

    @AllArgsConstructor
    public enum Language {
        DEFAULT(""),
        @Deprecated(forRemoval = true, since = "3.29.1") // Remove in EID-6303
        EN("en"),
        EN_CH("en-CH"),
        DE_CH("de-CH"),
        FR_CH("fr-CH"),
        IT_CH("it-CH"),
        RM_CH("rm-CH");

        private final String jsonValue;

        @JsonCreator
        public static Language fromJsonValue(String value) {
            for (Language language : values()) {
                if (language.jsonValue.equals(value)) {
                    return language;
                }
            }
            throw new IllegalArgumentException("Unknown language: " + value);
        }

        @JsonValue
        public String getJsonValue() {
            return jsonValue;
        }
    }

    public record RegistryId(String type, String value) {}
}
