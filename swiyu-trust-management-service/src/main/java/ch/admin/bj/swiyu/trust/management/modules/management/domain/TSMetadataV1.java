package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.MetadataV1Details;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * @deprecated Use TrustStatementPartnerLink with embedded json instead, this will be removed with EID-5295
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
@Table(name = "ts_metadata_v1")
@PrimaryKeyJoinColumn(name = "trust_statement_id")
@Deprecated(since = "2.2.0")
public class TSMetadataV1 extends TrustStatementPartnerLink {

    // NOSONAR

    @NotNull
    private String preferredLanguage;

    private String orgNameEn;
    private String orgNameDeCh;
    private String orgNameFrCh;
    private String orgNameItCh;
    private String orgNameRmCh;
    private String logoUriEn;
    private String logoUriDeCh;
    private String logoUriFrCh;
    private String logoUriItCh;
    private String logoUriRmCh;

    public TSMetadataV1(
        /* NOSONAR */ String subject,
        Instant validFrom,
        Instant validUntil,
        String preferredLanguage,
        String orgNameEn,
        String orgNameDeCh,
        String orgNameFrCh,
        String orgNameItCh,
        String orgNameRmCh,
        String logoUriEn,
        String logoUriDeCh,
        String logoUriFrCh,
        String logoUriItCh,
        String logoUriRmCh
    ) {
        super(
            null,
            TrustStatementType.TRUST_STATEMENT_METADATA_V1,
            subject,
            validFrom,
            validUntil,
            /*
             * A non-null dummy object so the not null constraint is happy
             * Not that this constructor is deprecated and only used to test the migration
             * from TSMetadataV1 to TrustStatementPartnerLink.
             */
            new MetadataV1Details(null, null, null)
        );
        this.preferredLanguage = preferredLanguage;
        this.orgNameEn = orgNameEn;
        this.orgNameDeCh = orgNameDeCh;
        this.orgNameFrCh = orgNameFrCh;
        this.orgNameItCh = orgNameItCh;
        this.orgNameRmCh = orgNameRmCh;
        this.logoUriEn = logoUriEn;
        this.logoUriDeCh = logoUriDeCh;
        this.logoUriFrCh = logoUriFrCh;
        this.logoUriItCh = logoUriItCh;
        this.logoUriRmCh = logoUriRmCh;
    }
}
