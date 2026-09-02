package ch.admin.bj.swiyu.trust.management.modules.common.audit;

import ch.admin.bit.jeap.audit.record.create.AuditEventType;
import java.util.Objects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

/**
 * See <a href="https://confluence.bit.admin.ch/x/gPVCTg">https://confluence.bit.admin.ch/x/gPVCTg</a> for all defined
 * UseCases.
 */
@Getter
@RequiredArgsConstructor
public enum AuditUseCase {
    NON_COMPLIANT_ACTOR_ADDED(
        Category.TRUST_REGISTRY,
        ObjectType.NON_COMPLIANT_ACTOR,
        DataJsonFieldName.NON_COMPLIANT_ACTOR_DATA,
        null, // no non-json value sent
        AuditEventType.CREATED,
        false
    ),
    NON_COMPLIANT_ACTOR_DELETED(
        Category.TRUST_REGISTRY,
        ObjectType.NON_COMPLIANT_ACTOR,
        DataJsonFieldName.NON_COMPLIANT_ACTOR_DATA,
        null, // no non-json value sent
        AuditEventType.DELETED,
        false
    ),
    TRUST_STATEMENT_PUBLISHED(
        Category.TRUST_REGISTRY,
        null, // not hardcoded, this is the trust statement type which is dynamic
        DataJsonFieldName.TRUST_STATEMENT_META,
        DataValueFieldName.TRUST_STATEMENT_JWT,
        AuditEventType.CREATED,
        true
    ),
    TRUST_STATEMENT_DEACTIVATED(
        Category.TRUST_REGISTRY,
        null, // not hardcoded, this is the trust statement type which is dynamic
        DataJsonFieldName.TRUST_STATEMENT_META,
        DataValueFieldName.TRUST_STATEMENT_JWT,
        AuditEventType.DELETED,
        true
    ),
    VC_SCHEMA_PUBLISHED(
        Category.TRUST_REGISTRY,
        ObjectType.VC_SCHEMA,
        DataJsonFieldName.VC_SCHEMA_META,
        DataValueFieldName.VC_SCHEMA_FILE,
        AuditEventType.CREATED,
        true
    ),
    TASK_APPROVED(
        Category.TASK,
        null, // not hardcoded, this is the dynamic task type
        DataJsonFieldName.TASK_DATA,
        null,
        AuditEventType.CREATED,
        true
    ),
    TASK_REJECTED(
        Category.TASK,
        null, // not hardcoded, this is the dynamic task type
        DataJsonFieldName.TASK_DATA,
        null, // no non-json value sent
        AuditEventType.CREATED,
        true
    ),
    TASK_MORE_INFORMATION_REQUESTED(
        Category.TASK,
        null, // not hardcoded, this is the dynamic task type
        DataJsonFieldName.TASK_DATA,
        null, // no non-json value sent
        AuditEventType.CREATED,
        true
    );

    private final String category;
    private final String auditObjectType;
    private final String dataJsonFieldName;
    private final String dataValueFieldName;
    private final AuditEventType eventType;
    private final boolean includesBusinessPartnerId;

    public boolean hasDataField() {
        return dataValueFieldName != null;
    }

    public String resolveAuditObjectType(String auditObjectType) {
        return Objects.requireNonNullElseGet(this.auditObjectType, () ->
            Objects.requireNonNull(
                auditObjectType,
                "Use case %s requires a runtime audit object type".formatted(this.name())
            )
        );
    }

    @UtilityClass
    private static class Category {

        public static final String TASK = "TASK";
        public static final String TRUST_REGISTRY = "TRUST_REGISTRY";
    }

    @UtilityClass
    private static final class ObjectType {

        public static final String NON_COMPLIANT_ACTOR = "NON_COMPLIANT_ACTOR";
        public static final String VC_SCHEMA = "VC_SCHEMA";
    }

    @UtilityClass
    private static final class DataJsonFieldName {

        public static final String TRUST_STATEMENT_META = "TRUST_STATEMENT_META";
        public static final String VC_SCHEMA_META = "VC_SCHEMA_META";
        public static final String NON_COMPLIANT_ACTOR_DATA = "NON_COMPLIANT_ACTOR_DATA";
        public static final String TASK_DATA = "TASK_DATA";
    }

    @UtilityClass
    private static final class DataValueFieldName {

        public static final String TRUST_STATEMENT_JWT = "TRUST_STATEMENT_JWT";
        public static final String VC_SCHEMA_FILE = "VC_SCHEMA_FILE";
    }
}
