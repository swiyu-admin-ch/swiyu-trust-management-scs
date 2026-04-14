package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.persistence.TransactionManagerNames.MANAGEMENT_TRANSACTION_MANAGER;

import ch.admin.bj.swiyu.trust.management.modules.common.exception.VcSchemaPublicationFailedException;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.VcSchemaPartnerLink;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.VcSchemaPartnerLinkRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VcSchemaPartnerLinkService {

    private final VcSchemaPartnerLinkRepository vcSchemaPartnerLinkRepository;

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
}
