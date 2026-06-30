package ch.admin.bj.swiyu.trust.management.modules.dataimport.service;

import ch.admin.bj.swiyu.trust.management.modules.common.i18n.LocalizedMapUtil;
import java.util.*;
import lombok.experimental.*;

@SuppressWarnings({ "java:S1192" })
@UtilityClass
public class CoreDemoData {

    public static final UUID CORE_ID_BP_DEFAULT = UUID.fromString("9f425029-9775-4984-99ba-bacc60069502");
    public static final String CORE_ID_BP_WANTS_TO_BE_TRUSTED_S = "897edd6b-2e3e-4cc2-95a8-5b759c301df8";
    public static final UUID CORE_ID_BP_WANTS_TO_BE_TRUSTED = UUID.fromString(CORE_ID_BP_WANTS_TO_BE_TRUSTED_S);
    public static final UUID CORE_ID_BP_GOV = UUID.fromString("39f92e48-619e-4e92-8958-468ae138d8a3");
    public static final UUID CORE_ID_BP_BASE_ONBOARDING_ONLY = UUID.fromString("e97e84e6-f40e-47ba-bdfe-d92f3d3dbc84");

    public static final String CORE_ID_TOS_UNSUBMITTED_S = "46ada91a-84ce-422b-b9b5-e0d2e3e8c46d";
    public static final UUID CORE_ID_TOS_UNSUBMITTED = UUID.fromString(CORE_ID_TOS_UNSUBMITTED_S);
    public static final UUID CORE_ID_TOS_REJECTED = UUID.fromString("913a09b4-6f6b-4703-a682-1046ccb26abb");
    public static final UUID CORE_ID_TOS_SUBMITTED = UUID.fromString("3299cd25-8bab-47b7-9d46-f740be76e57e");
    public static final String CORE_ID_TOS_SUCCEEDED_S = "8369160f-697c-4b12-80d3-91abff1a29ee";
    public static final UUID CORE_ID_TOS_SUCCEEDED = UUID.fromString(CORE_ID_TOS_SUCCEEDED_S);
    public static final UUID CORE_ID_TOS_INFO_REQUESTED = UUID.fromString("dc828a98-ffb1-4ae4-8f07-b35d2818ac87");

    // CORE_ID_BP_DEFAULT
    public static final Map<String, String> CORE_ID_BP_DEFAULT_NAMES = LocalizedMapUtil.fromLanguages(
        "Vertrau mir Beratung GmbH",
        "Vertrau mir Beratung GmbH (DE)",
        "Confiance Conseil GmbH (FR)",
        "Trusty Consulting S.r.l. (IT)",
        "Trusty Consulting GmbH (EN)",
        "Trusty Consulting GmbH (RM)"
    );
    public static final String CORE_ID_BP_DEFAULT_EMAIL = "erika.müller@trusty-consulting.com";

    // CORE_ID_BP_WANTS_TO_BE_TRUSTED
    public static final Map<String, String> CORE_ID_BP_WANTS_TO_BE_TRUSTED_NAMES = LocalizedMapUtil.fromLanguages(
        "Böswilliges Umzugsunternehmen GmbH",
        "Böswilliges Umzugsunternehmen GmbH",
        "Déménageurs malveillants GmbH",
        "Traslocatori malintenzionati S.r.l.",
        "Malicious Movers GmbH",
        "M. M. GmbH"
    );
    public static final String CORE_ID_BP_WANTS_TO_BE_TRUSTED_EMAIL = "ceo@m-m.com";

    // CORE_ID_BP_GOV
    public static final Map<String, String> CORE_ID_BP_GOV_NAMES = LocalizedMapUtil.fromLanguages(
        "Demo Kanton",
        "Demo Kanton",
        "Demo Canton",
        "Demo Cantone",
        "Demo Canton",
        "Demochaun"
    );
    public static final String CORE_ID_BP_GOV_EMAIL = "s.schmid@democanton.admin.ch";
}
