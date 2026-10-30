package ch.admin.bj.swiyu.trust.management.modules.management.domain.task;

import static ch.admin.bj.swiyu.trust.management.modules.common.i18n.LocalizedMapConstants.DEFAULT_VALUE_KEY;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request to irreversibly delete a business partner. Unlike the other task types this one is not approved by a
 * reviewer: it is auto-approved by a job once {@link #hardDeleteDueAt} is reached (EID-6994). Until then it can be
 * rejected, which is the only way to call the deletion off.
 */
@Entity
@Getter
@Table(name = "delete_business_partner_task")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeleteBusinessPartnerTask extends Task {

    /**
     * When the partner becomes due for hard deletion. Kept separate from the inherited {@code dueAt}, which is
     * cleared on approve/reject, so the moment the deletion was scheduled for stays readable afterwards.
     */
    @NotNull
    private Instant hardDeleteDueAt;

    /**
     * The partner name is a single string: it is taken from the partner projection of the core business service, which
     * has exactly one. The inherited column is a localized map shared with the other task types, so the name is stored
     * under the default key.
     */
    public DeleteBusinessPartnerTask(UUID partnerId, String partnerName, Instant submittedAt, Instant hardDeleteDueAt) {
        super(
            UUID.randomUUID(),
            partnerId,
            Map.of(DEFAULT_VALUE_KEY, partnerName),
            hardDeleteDueAt,
            submittedAt,
            TaskType.DELETE_BUSINESS_PARTNER
        );
        this.hardDeleteDueAt = hardDeleteDueAt;
    }
}
