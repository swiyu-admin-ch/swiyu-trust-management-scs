package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.persistence.TransactionManagerNames.MANAGEMENT_TRANSACTION_MANAGER;

import ch.admin.bj.swiyu.trust.management.modules.common.exception.VcSchemaPublicationFailedException;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.VcSchemaPartnerLink;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.VcSchemaPartnerLinkRepository;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.VcSchemaService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VcSchemaPartnerLinkService {

    private final VcSchemaPartnerLinkRepository vcSchemaPartnerLinkRepository;
    private final VcSchemaService vcSchemaService;
    private final VcSchemaAuditService vcSchemaAuditService;

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public void createVcSchemaPartnerLink(UUID vcSchemaId, UUID vcSchemaSubmissionId, UUID partnerId) {
        if (vcSchemaPartnerLinkRepository.existsByVcSchemaId(vcSchemaId)) {
            throw new VcSchemaPublicationFailedException(
                "A partner link has already been created for the vc schema %s. Cannot create a new partner link for it.".formatted(
                    vcSchemaId
                )
            );
        }
        vcSchemaPartnerLinkRepository.save(new VcSchemaPartnerLink(vcSchemaId, vcSchemaSubmissionId, partnerId));
    }

    /**
     * Removes all VC schemas of a hard-deleted business partner from the registry database and then the partner links
     * from the management database. The audit record is committed in its own transaction before the schema disappears
     * (see {@link VcSchemaAuditService}), so an irreversible deletion is never left without a record. Idempotent: a
     * missing registry row is not an error and a second run finds no links left.
     *
     * <p>Only the management-database part - reading the partner links and deleting them - is one transaction. The
     * registry deletions and the audit records commit on their own, because the three databases are not one unit of
     * work. That is why every step is retry-safe: a redelivery converges instead of starting over.
     */
    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public void hardDeleteByPartnerId(UUID partnerId) {
        for (var partnerLink : vcSchemaPartnerLinkRepository.findAllByPartnerId(partnerId)) {
            vcSchemaService
                .findVcSchema(partnerLink.getVcSchemaId())
                .ifPresent(vcSchema -> vcSchemaAuditService.auditDeletion(vcSchema, partnerId));
            vcSchemaService.hardDeleteVcSchema(partnerLink.getVcSchemaId());
        }
        vcSchemaPartnerLinkRepository.deleteAllByPartnerId(partnerId);
    }
}
