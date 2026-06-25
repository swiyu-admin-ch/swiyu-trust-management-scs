package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Serves as an abstraction layer between the different input channels (TrustOnboardingSubmission, AddDidSubmission)
 * and the statements which express the current identity (IdentityTrustStatementV1,IdentityTrustStatementV2)
 *
 * @param businessPartnerId
 * @param entityName
 * @param defaultLanguage
 * @param isStateActor
 * @param registryIds
 */
@Valid
public record BusinessPartnerIdentity(
    UUID businessPartnerId,
    @Size(min = 1) Map<Language, @NotBlank String> entityName,
    Language defaultLanguage,
    Boolean isStateActor,
    List<@Valid RegistryId> registryIds
) {
    @RequiredArgsConstructor
    public enum Language {
        EN("en"),
        DE_CH("de-CH"),
        FR_CH("fr-CH"),
        IT_CH("it-CH"),
        RM_CH("rm-CH");

        @Getter
        private final String languageCode;

        public static Language fromLanguageCode(String code) {
            return switch (code) {
                case "en" -> EN;
                case "de-CH" -> DE_CH;
                case "fr-CH" -> FR_CH;
                case "it-CH" -> IT_CH;
                case "rm-CH" -> RM_CH;
                default -> throw new IllegalArgumentException("Invalid language code: " + code);
            };
        }
    }

    public record RegistryId(@NotBlank String type, @NotBlank String value) {}
}
