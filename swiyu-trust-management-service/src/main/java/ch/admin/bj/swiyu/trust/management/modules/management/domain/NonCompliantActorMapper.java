package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.NonComplianceV2Details;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NonCompliantActorMapper {

    public static List<NonComplianceV2Details.NonCompliantActor> toNonComplianceV2Details(
        List<NonCompliantActor> nonCompliantActors
    ) {
        return nonCompliantActors
            .stream()
            .map(nonCompliantActor ->
                new NonComplianceV2Details.NonCompliantActor(
                    nonCompliantActor.getDid(),
                    nonCompliantActor.getFlaggedAsNonCompliantAt(),
                    toNonComplianceV2DetailsActorReason(nonCompliantActor.getReason())
                )
            )
            .toList();
    }

    @SuppressWarnings("java:S5738") // EID-6303
    private static Map<NonComplianceV2Details.Language, String> toNonComplianceV2DetailsActorReason(
        NonCompliantReasonText reason
    ) {
        var ret = new EnumMap<NonComplianceV2Details.Language, String>(NonComplianceV2Details.Language.class);

        if (reason.getReasonRm() != null && !reason.getReasonRm().isBlank()) {
            ret.put(NonComplianceV2Details.Language.RM_CH, reason.getReasonRm());
            ret.put(NonComplianceV2Details.Language.DEFAULT, reason.getReasonRm());
        }
        if (reason.getReasonIt() != null && !reason.getReasonIt().isBlank()) {
            ret.put(NonComplianceV2Details.Language.IT_CH, reason.getReasonIt());
            ret.put(NonComplianceV2Details.Language.DEFAULT, reason.getReasonIt());
        }
        if (reason.getReasonFr() != null && !reason.getReasonFr().isBlank()) {
            ret.put(NonComplianceV2Details.Language.FR_CH, reason.getReasonFr());
            ret.put(NonComplianceV2Details.Language.DEFAULT, reason.getReasonFr());
        }
        if (reason.getReasonEn() != null && !reason.getReasonEn().isBlank()) {
            ret.put(NonComplianceV2Details.Language.EN, reason.getReasonEn());
            ret.put(NonComplianceV2Details.Language.DEFAULT, reason.getReasonEn());
        }
        if (reason.getReasonDe() != null && !reason.getReasonDe().isBlank()) {
            ret.put(NonComplianceV2Details.Language.DE_CH, reason.getReasonDe());
            ret.put(NonComplianceV2Details.Language.DEFAULT, reason.getReasonDe());
        }

        return ret;
    }
}
