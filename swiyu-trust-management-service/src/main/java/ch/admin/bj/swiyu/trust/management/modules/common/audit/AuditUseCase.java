package ch.admin.bj.swiyu.trust.management.modules.common.audit;

import ch.admin.bit.jeap.audit.record.create.AuditEventType;
import java.util.Objects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuditUseCase {
    NON_COMPLIANT_ACTOR_ADDED(
        "NON_COMPLIANT_ACTOR_ADDED",
        Category.TRUST_REGISTRY,
        "NON_COMPLIANT_ACTOR",
        "NON_COMPLIANT_ACTOR_DATA",
        null,
        AuditEventType.CREATED,
        false
    ),
    NON_COMPLIANT_ACTOR_DELETED(
        "NON_COMPLIANT_ACTOR_DELETED",
        Category.TRUST_REGISTRY,
        "NON_COMPLIANT_ACTOR",
        "NON_COMPLIANT_ACTOR_DATA",
        null,
        AuditEventType.DELETED,
        false
    ),
    TRUST_STATEMENT_PUBLISHED(
        "TRUST_STATEMENT_PUBLISHED",
        Category.TRUST_REGISTRY,
        null,
        "TRUST_STATEMENT_META",
        "TRUST_STATEMENT_JWT",
        AuditEventType.CREATED,
        true
    ),
    TRUST_STATEMENT_DEACTIVATED(
        "TRUST_STATEMENT_DEACTIVATED",
        Category.TRUST_REGISTRY,
        null,
        "TRUST_STATEMENT_META",
        "TRUST_STATEMENT_JWT",
        AuditEventType.DELETED,
        true
    ),
    VC_SCHEMA_PUBLISHED(
        "VC_SCHEMA_PUBLISHED",
        Category.TRUST_REGISTRY,
        "VC_SCHEMA",
        "VC_SCHEMA_META",
        "VC_SCHEMA_FILE",
        AuditEventType.CREATED,
        true
    );

    private final String name;
    private final String category;
    private final String auditObjectType;
    private final String metaFieldName;
    private final String dataFieldName;
    private final AuditEventType eventType;
    private final boolean includesBusinessPartnerId;

    public boolean hasDataField() {
        return dataFieldName != null;
    }

    public String resolveAuditObjectType(String auditObjectType) {
        return Objects.requireNonNullElseGet(this.auditObjectType, () ->
            Objects.requireNonNull(auditObjectType, "Use case %s requires a runtime audit object type".formatted(name))
        );
    }

    private static class Category {

        public static final String TRUST_REGISTRY = "TRUST_REGISTRY";
    }
}
