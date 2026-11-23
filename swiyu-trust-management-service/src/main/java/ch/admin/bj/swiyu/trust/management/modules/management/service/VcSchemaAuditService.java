package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.persistence.TransactionManagerNames.MANAGEMENT_TRANSACTION_MANAGER;

import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditMapper;
import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.trust.management.modules.registry.api.VcSchemaDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Commits the audit record for a VC schema deletion on its own, before the schema is removed from the registry
 * database.
 *
 * <p>The registry database commits in a different transaction than the management database, so an audit record that
 * merely joined the surrounding management transaction could be rolled back while the schema is already gone - an
 * irreversible deletion without a record. Committing the record first makes the audit at-least-once; a retry after a
 * crash finds no schema and emits nothing further.
 */
@Service
@RequiredArgsConstructor
public class VcSchemaAuditService {

    private final AuditPublisher auditPublisher;

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER, propagation = Propagation.REQUIRES_NEW)
    public void auditDeletion(VcSchemaDto vcSchema, UUID partnerId) {
        auditPublisher.vcSchemaDeleted(
            vcSchema.id().toString(),
            partnerId.toString(),
            0L, // a VC schema has no version field
            AuditMapper.toAuditJson(vcSchema),
            vcSchema.file()
        );
    }
}
