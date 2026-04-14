package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC) // JPA
public class PartnerName {

    @Column
    private String partnerNameDe;

    @Column
    private String partnerNameFr;

    @Column
    private String partnerNameIt;

    @Column
    private String partnerNameEn;

    @Column
    private String partnerNameRm;
}
