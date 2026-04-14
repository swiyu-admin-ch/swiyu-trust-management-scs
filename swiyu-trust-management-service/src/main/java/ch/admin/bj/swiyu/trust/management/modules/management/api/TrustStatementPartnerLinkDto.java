package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Data;

@Data
@Schema(name = "TrustStatementPartnerLink")
public class TrustStatementPartnerLinkDto {

    UUID id;
    String subject;
    TrustStatementTypeDto type;
    TrustStatementPartnerLinkStatusDto status;
    IssuerVcStatusDto vcStatus;
    TrustRegistryStatusDto trustRegistryStatus;
    Instant validFrom;
    Instant validUntil;

    // AuditMetadata
    Instant lastModifiedAt;
    String lastModifiedBy;
    Instant createdAt;
    String createdBy;

    /**
     * Statement type specific properties.
     */
    Map<String, Object> details = new HashMap<>();
}
