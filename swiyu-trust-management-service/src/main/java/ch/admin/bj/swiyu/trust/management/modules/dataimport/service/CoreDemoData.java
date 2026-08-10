package ch.admin.bj.swiyu.trust.management.modules.dataimport.service;

import ch.admin.bj.swiyu.trust.management.modules.common.i18n.LocalizedMapUtil;
import java.util.*;
import lombok.experimental.*;

/*
 * This class must be kept synchronized between CBS and TMS.
 * If something change on one side, it must be pushed on the other.
 * CBS: ch.admin.bj.swiyu.trust.management.modules.dataimport.service.CoreDemoData
 * TMS: ch.admin.bj.swiyu.core.business.modules.dataimport.domain.CoreDemoData
 */
@SuppressWarnings(
    {
        "java:S1192", // Allow text repeats in demo data
        "java:S2386", // Sonar does not catch that the maps are unmodifiable
    }
)
@UtilityClass
public class CoreDemoData {

    // Keep in sync with DemoDataConstants
    public static final UUID CORE_ID_BP_DEFAULT = UUID.fromString("9f425029-9775-4984-99ba-bacc60069502");
    public static final String CORE_ID_BP_WANTS_TO_BE_TRUSTED_S = "897edd6b-2e3e-4cc2-95a8-5b759c301df8";
    public static final UUID CORE_ID_BP_WANTS_TO_BE_TRUSTED = UUID.fromString(CORE_ID_BP_WANTS_TO_BE_TRUSTED_S);
    public static final UUID CORE_ID_BP_GOV = UUID.fromString("39f92e48-619e-4e92-8958-468ae138d8a3");
    public static final UUID CORE_ID_BP_GOV_TRUSTED = UUID.fromString("77054cd8-6fe6-44eb-be22-b56ebf9c8622");
    public static final UUID CORE_ID_BP_BASE_ONBOARDING_ONLY = UUID.fromString("e97e84e6-f40e-47ba-bdfe-d92f3d3dbc84");
    public static final UUID CORE_ID_BP_OVERDUE = UUID.fromString("4b9f08ac-aa29-4bcf-97a4-88e73e49c3e1");

    public static final String CORE_ID_TOS_UNSUBMITTED_S = "46ada91a-84ce-422b-b9b5-e0d2e3e8c46d";
    public static final UUID CORE_ID_TOS_UNSUBMITTED = UUID.fromString(CORE_ID_TOS_UNSUBMITTED_S);
    public static final UUID CORE_ID_TOS_REJECTED = UUID.fromString("913a09b4-6f6b-4703-a682-1046ccb26abb");
    public static final UUID CORE_ID_TOS_SUBMITTED = UUID.fromString("3299cd25-8bab-47b7-9d46-f740be76e57e");
    public static final String CORE_ID_TOS_SUCCEEDED_S = "8369160f-697c-4b12-80d3-91abff1a29ee";
    public static final UUID CORE_ID_TOS_SUCCEEDED = UUID.fromString(CORE_ID_TOS_SUCCEEDED_S);
    public static final UUID CORE_ID_TOS_INFO_REQUESTED = UUID.fromString("dc828a98-ffb1-4ae4-8f07-b35d2818ac87");
    public static final UUID CORE_ID_TOS_OVERDUE = UUID.fromString("161d56d8-0999-46e5-a618-ba922414382a");

    public static final UUID CORE_ID_BP_E2ETESTS = UUID.fromString("7b24f978-afe7-4f60-af2f-57ae5a01d303");
    public static final UUID CORE_ID_IDENTIFIER_E2ETESTS_LOCAL = UUID.fromString(
        "1133c5ee-f7b2-400c-a358-3bae4bba7672"
    );
    public static final String CORE_ID_IDENTIFIER_E2ETESTS_LOCAL_DIDLOG =
        "{\"versionId\":\"1-QmStVvgLKv3oVnerc74p54pt8UcF8urHH32t9LvDVYLhMu\",\"versionTime\":\"2026-07-07T15:10:31Z\",\"parameters\":{\"method\":\"did:webvh:1.0\",\"scid\":\"QmPidcC5K8DxBHUCTwn12YCYK6VAkAc3EyEZpsbMaHooyy\",\"updateKeys\":[\"z6MknJEsZ8hynVFFF1oFFrJEdtsgTtSjuaE3k3RtpJfzLK9G\"],\"portable\":false},\"state\":{\"id\":\"did:webvh:QmPidcC5K8DxBHUCTwn12YCYK6VAkAc3EyEZpsbMaHooyy:localhost%3A8190:api:v1:did:1133c5ee-f7b2-400c-a358-3bae4bba7672\",\"profile_version\":\"swiss-profile-anchor:1.0.0\",\"authentication\":[\"did:webvh:QmPidcC5K8DxBHUCTwn12YCYK6VAkAc3EyEZpsbMaHooyy:localhost%3A8190:api:v1:did:1133c5ee-f7b2-400c-a358-3bae4bba7672#auth-key-01\"],\"assertionMethod\":[\"did:webvh:QmPidcC5K8DxBHUCTwn12YCYK6VAkAc3EyEZpsbMaHooyy:localhost%3A8190:api:v1:did:1133c5ee-f7b2-400c-a358-3bae4bba7672#assert-key-01\"],\"verificationMethod\":[{\"id\":\"did:webvh:QmPidcC5K8DxBHUCTwn12YCYK6VAkAc3EyEZpsbMaHooyy:localhost%3A8190:api:v1:did:1133c5ee-f7b2-400c-a358-3bae4bba7672#auth-key-01\",\"controller\":\"did:webvh:QmPidcC5K8DxBHUCTwn12YCYK6VAkAc3EyEZpsbMaHooyy:localhost%3A8190:api:v1:did:1133c5ee-f7b2-400c-a358-3bae4bba7672\",\"type\":\"JsonWebKey2020\",\"publicKeyJwk\":{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"lJHTz-hxyWyudXR-Ik3n59Njh0ZDe67LOAC4rWtGwVI\",\"y\":\"UQ-GiYrC3ZPwVhKNc00u1-QHYXRU1tIz8_KtkC-MGSs\",\"kid\":\"auth-key-01\"}},{\"id\":\"did:webvh:QmPidcC5K8DxBHUCTwn12YCYK6VAkAc3EyEZpsbMaHooyy:localhost%3A8190:api:v1:did:1133c5ee-f7b2-400c-a358-3bae4bba7672#assert-key-01\",\"controller\":\"did:webvh:QmPidcC5K8DxBHUCTwn12YCYK6VAkAc3EyEZpsbMaHooyy:localhost%3A8190:api:v1:did:1133c5ee-f7b2-400c-a358-3bae4bba7672\",\"type\":\"JsonWebKey2020\",\"publicKeyJwk\":{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"b8iC-SjRKmuoSIGDhD7C7GTiSq06aIyx07yI9WlryF4\",\"y\":\"GsyVRZcIyc6pcXKClxTiwXGTCcXZf6sWXA8tQmWgRBI\",\"kid\":\"assert-key-01\"}}]},\"proof\":[{\"type\":\"DataIntegrityProof\",\"cryptosuite\":\"eddsa-jcs-2022\",\"created\":\"2026-07-07T15:10:31Z\",\"verificationMethod\":\"did:key:z6MknJEsZ8hynVFFF1oFFrJEdtsgTtSjuaE3k3RtpJfzLK9G#z6MknJEsZ8hynVFFF1oFFrJEdtsgTtSjuaE3k3RtpJfzLK9G\",\"proofPurpose\":\"assertionMethod\",\"proofValue\":\"z4FaBXsyaD4ZXY3WYTsjiPLp4N2HDGfz6ax2sELpxLMrJrTFyk5T6MhgEUCrfyPPxPsicNdqPRSiXFMLFWREpoYkp\"}]}";

    // CORE_ID_BP_DEFAULT
    public static final Map<String, String> CORE_ID_BP_DEFAULT_NAMES = LocalizedMapUtil.fromLanguages(
        "Vertrau mir Beratung GmbH",
        "Vertrau mir Beratung GmbH (DE)",
        "Confiance Conseil GmbH (FR)",
        "Trusty Consulting S.r.l. (IT)",
        "Trusty Consulting GmbH (EN)",
        "Trusty Consulting GmbH (RM)"
    );
    public static final String CORE_ID_BP_DEFAULT_CORRESPONDING_LANG = "de-CH";
    public static final String CORE_ID_BP_DEFAULT_PHONE = "+41791234567";
    public static final String CORE_ID_BP_DEFAULT_EMAIL = "erika.mueller@trusty-consulting.com";

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
    public static final String CORE_ID_BP_GOV_EMAIL_JOHN = "j.doe@democanton.admin.ch";
    public static final String CORE_ID_BP_GOV_EMAIL_ERIKA = "e.mueller@democanton.admin.ch";

    // CORE_ID_BP_GOV_TRUSTED
    public static final Map<String, String> CORE_ID_BP_GOV_TRUSTED_NAMES = LocalizedMapUtil.fromLanguages(
        "Trusted Gov Partner",
        "Trusted Gov Partner (DE)",
        "Trusted Gov Partner (FR)",
        "Trusted Gov Partner (IT)",
        "Trusted Gov Partner (EN)",
        "Trusted Gov Partner (RM)"
    );
    public static final String CORE_ID_BP_GOV_TRUSTED_CORRESPONDING_LANG = "de-CH";
    public static final String CORE_ID_BP_GOV_TRUSTED_PHONE = "+41791234567";
    public static final String CORE_ID_BP_GOV_TRUSTED_EMAIL = "helvetica@trusting-gov.com";
    public static final UUID CORE_ID_BP_GOV_TRUSTED_IDENTIFIER_DATASTORE_ID = UUID.fromString(
        "effaab62-ab2d-4794-ba7f-48cbbe3ea383"
    );
    public static final String CORE_ID_BP_GOV_TRUSTED_DIDLOG_LOCAL =
        "{\"versionId\":\"1-QmSfpPzd7kBQ6DPuccaJjBW9m6nScfNYnPuEduoxSFkD26\",\"versionTime\":\"2026-08-06T11:08:35Z\",\"parameters\":{\"method\":\"did:webvh:1.0\",\"scid\":\"QmSa8SMKvTxwJxavsHP6uxsrVL4B7eTPMVHct8LbT9yTty\",\"updateKeys\":[\"z6Mkozy1Dpit4opQXbfLthUM5KdZDoQLzMGzN3jLes6KahrQ\"],\"portable\":false},\"state\":{\"id\":\"did:webvh:QmSa8SMKvTxwJxavsHP6uxsrVL4B7eTPMVHct8LbT9yTty:localhost%3A8190:api:v1:did:effaab62-ab2d-4794-ba7f-48cbbe3ea383\",\"profile_version\":\"swiss-profile-anchor:1.0.0\",\"authentication\":[\"did:webvh:QmSa8SMKvTxwJxavsHP6uxsrVL4B7eTPMVHct8LbT9yTty:localhost%3A8190:api:v1:did:effaab62-ab2d-4794-ba7f-48cbbe3ea383#auth-key-01\"],\"assertionMethod\":[\"did:webvh:QmSa8SMKvTxwJxavsHP6uxsrVL4B7eTPMVHct8LbT9yTty:localhost%3A8190:api:v1:did:effaab62-ab2d-4794-ba7f-48cbbe3ea383#assert-key-01\"],\"verificationMethod\":[{\"id\":\"did:webvh:QmSa8SMKvTxwJxavsHP6uxsrVL4B7eTPMVHct8LbT9yTty:localhost%3A8190:api:v1:did:effaab62-ab2d-4794-ba7f-48cbbe3ea383#auth-key-01\",\"controller\":\"did:webvh:QmSa8SMKvTxwJxavsHP6uxsrVL4B7eTPMVHct8LbT9yTty:localhost%3A8190:api:v1:did:effaab62-ab2d-4794-ba7f-48cbbe3ea383\",\"type\":\"JsonWebKey2020\",\"publicKeyJwk\":{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"1rj9SCVfrol4JSYUXPFH50MSl28QuP1lRslb9C3jvi8\",\"y\":\"Pmit2QvqFNEAT9kKhVpfN7vSLk0sbzTVsun4OKO8o9g\",\"kid\":\"auth-key-01\"}},{\"id\":\"did:webvh:QmSa8SMKvTxwJxavsHP6uxsrVL4B7eTPMVHct8LbT9yTty:localhost%3A8190:api:v1:did:effaab62-ab2d-4794-ba7f-48cbbe3ea383#assert-key-01\",\"controller\":\"did:webvh:QmSa8SMKvTxwJxavsHP6uxsrVL4B7eTPMVHct8LbT9yTty:localhost%3A8190:api:v1:did:effaab62-ab2d-4794-ba7f-48cbbe3ea383\",\"type\":\"JsonWebKey2020\",\"publicKeyJwk\":{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"hfLUyfOj8tNfzKybUKCUfUh7WWvxdQCbZiYn94kmYFY\",\"y\":\"ITknv43a5zNcSIR5pL3XZLzru905EuUAdMZyC4a5Bz4\",\"kid\":\"assert-key-01\"}}]},\"proof\":[{\"type\":\"DataIntegrityProof\",\"cryptosuite\":\"eddsa-jcs-2022\",\"created\":\"2026-08-06T11:08:35Z\",\"verificationMethod\":\"did:key:z6Mkozy1Dpit4opQXbfLthUM5KdZDoQLzMGzN3jLes6KahrQ#z6Mkozy1Dpit4opQXbfLthUM5KdZDoQLzMGzN3jLes6KahrQ\",\"proofPurpose\":\"assertionMethod\",\"proofValue\":\"z2AoWEb9hZ7CQVdvvFMxEQn2NkZRd8K6xx4UPx9nJahYvmAy1LaDcfNQJ9DmAAKvTbEkKwun2BgfSMoFm81MxDDzS\"}]}";
    public static final String CORE_ID_BP_GOV_TRUSTED_DIDLOG_DEV =
        "{\"versionId\":\"1-QmWWJNcbMXiPxH7UnqAwADuc5jXZgdddZY8zZfihJMTbjk\",\"versionTime\":\"2026-08-06T11:09:40Z\",\"parameters\":{\"method\":\"did:webvh:1.0\",\"scid\":\"QmZor7HwsF6qt9Wv1dRs75QYsfFRFFeCtJLjCMN3T9me9n\",\"updateKeys\":[\"z6MkkP8NLJaDCGhT4cgabqJ8R9dzcqdFoqmx2Tv7piFdBFV7\"],\"portable\":false},\"state\":{\"id\":\"did:webvh:QmZor7HwsF6qt9Wv1dRs75QYsfFRFFeCtJLjCMN3T9me9n:identifier-reg-d.trust-infra.swiyu.admin.ch:api:v1:did:effaab62-ab2d-4794-ba7f-48cbbe3ea383\",\"profile_version\":\"swiss-profile-anchor:1.0.0\",\"authentication\":[\"did:webvh:QmZor7HwsF6qt9Wv1dRs75QYsfFRFFeCtJLjCMN3T9me9n:identifier-reg-d.trust-infra.swiyu.admin.ch:api:v1:did:effaab62-ab2d-4794-ba7f-48cbbe3ea383#auth-key-01\"],\"assertionMethod\":[\"did:webvh:QmZor7HwsF6qt9Wv1dRs75QYsfFRFFeCtJLjCMN3T9me9n:identifier-reg-d.trust-infra.swiyu.admin.ch:api:v1:did:effaab62-ab2d-4794-ba7f-48cbbe3ea383#assert-key-01\"],\"verificationMethod\":[{\"id\":\"did:webvh:QmZor7HwsF6qt9Wv1dRs75QYsfFRFFeCtJLjCMN3T9me9n:identifier-reg-d.trust-infra.swiyu.admin.ch:api:v1:did:effaab62-ab2d-4794-ba7f-48cbbe3ea383#auth-key-01\",\"controller\":\"did:webvh:QmZor7HwsF6qt9Wv1dRs75QYsfFRFFeCtJLjCMN3T9me9n:identifier-reg-d.trust-infra.swiyu.admin.ch:api:v1:did:effaab62-ab2d-4794-ba7f-48cbbe3ea383\",\"type\":\"JsonWebKey2020\",\"publicKeyJwk\":{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"spNAudLJfn_sZda5jUy92uo0HiUcTRYirx4p0S5MNrQ\",\"y\":\"HgkfxI2CohNhq3TqYc5uS3EWyQfIBkjMxzvLxrpEeh0\",\"kid\":\"auth-key-01\"}},{\"id\":\"did:webvh:QmZor7HwsF6qt9Wv1dRs75QYsfFRFFeCtJLjCMN3T9me9n:identifier-reg-d.trust-infra.swiyu.admin.ch:api:v1:did:effaab62-ab2d-4794-ba7f-48cbbe3ea383#assert-key-01\",\"controller\":\"did:webvh:QmZor7HwsF6qt9Wv1dRs75QYsfFRFFeCtJLjCMN3T9me9n:identifier-reg-d.trust-infra.swiyu.admin.ch:api:v1:did:effaab62-ab2d-4794-ba7f-48cbbe3ea383\",\"type\":\"JsonWebKey2020\",\"publicKeyJwk\":{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"jWusu8jhVS3SYvoaUHcYrZgVMMNClfSFF7OIx9e9g_0\",\"y\":\"owZL90fzkMwiqL4rEYcY-gor6StikYlkGSqxJ_qklYA\",\"kid\":\"assert-key-01\"}}]},\"proof\":[{\"type\":\"DataIntegrityProof\",\"cryptosuite\":\"eddsa-jcs-2022\",\"created\":\"2026-08-06T11:09:40Z\",\"verificationMethod\":\"did:key:z6MkkP8NLJaDCGhT4cgabqJ8R9dzcqdFoqmx2Tv7piFdBFV7#z6MkkP8NLJaDCGhT4cgabqJ8R9dzcqdFoqmx2Tv7piFdBFV7\",\"proofPurpose\":\"assertionMethod\",\"proofValue\":\"z2aNuTuU4VAtvWe41bSZh8kxMMusWqpqrvy7JTp1QW4LviTu7Z8wnkaizdKZKSN6eQgTNuz9muHqqWAmcorir4jMh\"}]}";

    // CORE_ID_BP_BASE_ONBOARDING_ONLY
    public static final Map<String, String> CORE_ID_BP_BASE_ONBOARDING_ONLY_NAMES = LocalizedMapUtil.fromLanguages(
        "Demo Unternehmen",
        "Demo Unternehmen",
        "Démonstration Entreprise",
        "Demo Azienda",
        "Demo Company",
        "Demo Unternehmen"
    );
    public static final String CORE_ID_BP_BASE_ONBOARDING_ONLY_PHONE = "+41791234567";
    public static final String CORE_ID_BP_BASE_ONBOARDING_ONLY_EMAIL = "helvetica@demo-comp.com";

    // CORE_ID_BP_OVERDUE
    public static final Map<String, String> CORE_ID_BP_OVERDUE_NAMES = LocalizedMapUtil.fromLanguages(
        "Schleppende Logistik AG",
        "Schleppende Logistik AG",
        "Logistique Lente SA",
        "Logistica Lenta S.r.l.",
        "Sluggish Logistics AG",
        "S.L. AG"
    );
    public static final String CORE_ID_BP_OVERDUE_EMAIL = "p.keller@schleppende-logistik.ch";

    // CORE_ID_BP_E2ETESTS
    public static final Map<String, String> CORE_ID_BP_E2ETESTS_NAMES = LocalizedMapUtil.fromLanguages(
        "E2e GmbH (DE)",
        "E2e GmbH (DE)",
        "E2e GmbH (FR)",
        "E2e S.r.l. (IT)",
        "E2e GmbH (EN)",
        "E2e GmbH (RM)"
    );
    public static final String CORE_ID_BP_E2ETESTS_EMAIL = "erika.mueller@e2e.test";
}
