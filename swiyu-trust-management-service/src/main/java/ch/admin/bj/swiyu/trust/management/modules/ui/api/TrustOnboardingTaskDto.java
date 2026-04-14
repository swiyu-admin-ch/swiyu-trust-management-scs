package ch.admin.bj.swiyu.trust.management.modules.ui.api;

import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustOnboardingTaskActionDto;
import com.fasterxml.jackson.annotation.*;
import io.swagger.v3.oas.annotations.media.*;
import jakarta.validation.constraints.NotNull;
import java.time.*;
import java.util.*;
import lombok.*;

/**
 * @param uid Unternehmens-Identifikationsnummer (UID)
 */
@Schema(name = "TrustOnboardingTask", enumAsRef = true)
public record TrustOnboardingTaskDto(
    @NotNull UUID id,
    String assignee,
    @NotNull Instant submittedAt,
    @NotNull Instant dueAt,
    @NotNull TrustOnboardingTaskStatusDto state,
    @NotNull BusinessPartnerTypeDto partnerType,
    String uid,
    Boolean isRegisteredInCommercialRegister,
    @NotNull String entityNameDefault,
    @NotNull Map<LanguageDto, String> entityName,
    String address,
    String zipCodeCity,
    String country,
    String email,
    @NotNull LanguageDto correspondenceLanguage,
    @NotNull List<ContactDto> contacts,
    @NotNull List<DocumentDto> documents,
    @NotNull List<DidDto> dids,
    @NotNull Set<TrustOnboardingTaskActionDto> allowedActions
) {
    @RequiredArgsConstructor
    @Schema(name = "Language", enumAsRef = true)
    public enum LanguageDto {
        EN("en"),
        DE_CH("de-CH"),
        FR_CH("fr-CH"),
        IT_CH("it-CH"),
        RM_CH("rm-CH");

        private final String value;

        @JsonValue
        @Override
        public String toString() {
            return value;
        }
    }

    @Schema(name = "Contact", enumAsRef = true)
    public record ContactDto(
        @NotNull String name,
        @NotNull TrustOnboardingTaskContactTypeDto type,
        String phone,
        String email
    ) {}

    @Schema(name = "Document", enumAsRef = true)
    public record DocumentDto(@NotNull UUID id, @NotNull String name, @NotNull Instant submittedAt) {}

    @Schema(name = "Did", enumAsRef = true)
    public record DidDto(@NotNull String did, Instant proofOfPossessionDate) {}
}
