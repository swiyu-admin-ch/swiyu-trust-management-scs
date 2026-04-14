package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC) // JPA
public class NonCompliantReasonText {

    private String reasonDe;
    private String reasonFr;
    private String reasonIt;
    private String reasonEn;
    private String reasonRm;
}
