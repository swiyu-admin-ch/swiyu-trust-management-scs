package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import static ch.admin.bj.swiyu.trust.management.modules.common.i18n.LocalizedMapConstants.DEFAULT_VALUE_KEY;
import static ch.admin.bj.swiyu.trust.management.modules.common.i18n.LocalizedMapConstants.DE_CH;
import static ch.admin.bj.swiyu.trust.management.modules.common.i18n.LocalizedMapConstants.EN;
import static ch.admin.bj.swiyu.trust.management.modules.common.i18n.LocalizedMapConstants.FR_CH;
import static ch.admin.bj.swiyu.trust.management.modules.common.i18n.LocalizedMapConstants.IT_CH;
import static ch.admin.bj.swiyu.trust.management.modules.common.i18n.LocalizedMapConstants.RM_CH;

import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.NonComplianceV2Details;
import java.util.LinkedHashMap;
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

    private static Map<String, String> toNonComplianceV2DetailsActorReason(NonCompliantReasonText reason) {
        var ret = new LinkedHashMap<String, String>();

        if (reason.getReasonRm() != null && !reason.getReasonRm().isBlank()) {
            ret.put(RM_CH, reason.getReasonRm());
            ret.put(DEFAULT_VALUE_KEY, reason.getReasonRm());
        }
        if (reason.getReasonIt() != null && !reason.getReasonIt().isBlank()) {
            ret.put(IT_CH, reason.getReasonIt());
            ret.put(DEFAULT_VALUE_KEY, reason.getReasonIt());
        }
        if (reason.getReasonFr() != null && !reason.getReasonFr().isBlank()) {
            ret.put(FR_CH, reason.getReasonFr());
            ret.put(DEFAULT_VALUE_KEY, reason.getReasonFr());
        }
        if (reason.getReasonEn() != null && !reason.getReasonEn().isBlank()) {
            ret.put(EN, reason.getReasonEn());
            ret.put(DEFAULT_VALUE_KEY, reason.getReasonEn());
        }
        if (reason.getReasonDe() != null && !reason.getReasonDe().isBlank()) {
            ret.put(DE_CH, reason.getReasonDe());
            ret.put(DEFAULT_VALUE_KEY, reason.getReasonDe());
        }

        return ret;
    }
}
