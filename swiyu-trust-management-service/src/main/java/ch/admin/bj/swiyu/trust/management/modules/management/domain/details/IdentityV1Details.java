package ch.admin.bj.swiyu.trust.management.modules.management.domain.details;

import static ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementPartnerLinkType.TRUST_STATEMENT_IDENTITY_V1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.Map;
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

    private Map<String, String> entityName;
    private Boolean isStateActor;
    private List<RegistryId> registryIds;

    IdentityV1Details() {
        super(TRUST_STATEMENT_IDENTITY_V1);
    }

    public IdentityV1Details(Map<String, String> entityName, Boolean isStateActor, List<RegistryId> registryIds) {
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

    public record RegistryId(String type, String value) {}
}
