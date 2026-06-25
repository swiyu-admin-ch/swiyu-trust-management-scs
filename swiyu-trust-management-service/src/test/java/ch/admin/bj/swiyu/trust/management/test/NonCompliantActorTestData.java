package ch.admin.bj.swiyu.trust.management.test;

import ch.admin.bj.swiyu.trust.management.modules.management.api.NonCompliantActorRequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.NonCompliantReasonTextDto;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NonCompliantActorTestData {

    public static NonCompliantActorRequestDto nonCompliantActorRequestDto() {
        return new NonCompliantActorRequestDto(
            "did:tdw:alpha123",
            new NonCompliantReasonTextDto(null, null, null, "Violation of policy", null)
        );
    }
}
