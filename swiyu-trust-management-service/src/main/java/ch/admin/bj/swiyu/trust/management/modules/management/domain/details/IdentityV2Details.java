package ch.admin.bj.swiyu.trust.management.modules.management.domain.details;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.util.StringUtils;

/**
 * Represents the details of a trust statement of type TrustStatementIdentityV2.
 * See <a href="https://confluence.bit.admin.ch/spaces/EIDTEAM/pages/1374138607/Trust+Protocol+2.0#TrustProtocol2.0-IdentityTrustStatement(idTS)Identity-Trust-Statement">spec</a>
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Getter
public final class IdentityV2Details extends TrustStatementDetails {

    private Map<Language, String> entityName;
    private Boolean isStateActor;
    private List<RegistryId> registryIds;

    IdentityV2Details() {
        super(TrustStatementPartnerLinkType.TRUST_STATEMENT_IDENTITY_V2);
    }

    public IdentityV2Details(Map<Language, String> entityName, Boolean isStateActor, List<RegistryId> registryIds) {
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
        EN("en"),
        DE_CH("de-CH"),
        FR_CH("fr-CH"),
        IT_CH("it-CH"),
        RM_CH("rm-CH");

        @Getter
        private final String languageCode;
    }

    public record RegistryId(String type, String value) {}
}
