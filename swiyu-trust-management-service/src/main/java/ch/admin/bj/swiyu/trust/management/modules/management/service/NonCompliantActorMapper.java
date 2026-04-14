package ch.admin.bj.swiyu.trust.management.modules.management.service;

import ch.admin.bj.swiyu.trust.management.modules.management.api.NonCompliantActorDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.NonCompliantReasonTextDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.NonCompliantActor;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NonCompliantActorMapper {

    public static NonCompliantActorDto map(NonCompliantActor nonCompliantActor) {
        return new NonCompliantActorDto(
            nonCompliantActor.getId(),
            nonCompliantActor.getDid(),
            nonCompliantActor.getFlaggedAsNonCompliantAt(),
            new NonCompliantReasonTextDto(
                nonCompliantActor.getReason().getReasonDe(),
                nonCompliantActor.getReason().getReasonFr(),
                nonCompliantActor.getReason().getReasonIt(),
                nonCompliantActor.getReason().getReasonEn(),
                nonCompliantActor.getReason().getReasonRm()
            )
        );
    }
}
