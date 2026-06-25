package ch.admin.bj.swiyu.trust.management.modules.ui.api;

import io.swagger.v3.oas.annotations.media.*;

@Schema(name = "TrustOnboardingTaskContactType", enumAsRef = true)
public enum TrustOnboardingTaskContactTypeDto {
    AUTHORISED_SIGNATORY,
    CONTACT_PERSON,
}
