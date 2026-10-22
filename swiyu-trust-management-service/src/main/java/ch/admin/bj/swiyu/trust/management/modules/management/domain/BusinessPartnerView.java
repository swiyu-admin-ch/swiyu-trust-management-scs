package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Immutable;

/**
 * Read model over the {@code business_partner_view} database view, a FULL OUTER JOIN of two sides:
 *
 * <ul>
 *     <li><b>BP</b> — {@link BusinessPartner}, the local projection of the partner owned by the core business service,
 *     refreshed by the partner sync. Absent for a partner that has never been synced.</li>
 *     <li><b>BPI</b> — {@link BusinessPartnerIdentity}, the identity this service owns. Absent for a partner that is
 *     registered in the core business service but has never been onboarded here.</li>
 * </ul>
 *
 * <p>Each field below names the side it comes from. Everything taken from only one side is nullable, because a partner
 * may be known to only one of them.
 */
@Entity
@Immutable
@Table(name = "business_partner_view")
@NoArgsConstructor
@Getter
@ToString
public class BusinessPartnerView {

    /** BP or BPI: the partner id. Both sides key on it, so it is set for every row. */
    @Id
    @Column(name = "business_partner_id")
    private UUID businessPartnerId;

    /**
     * BP, with BPI as fallback: the partner name. The projection carries the authoritative name from the core business
     * service; while no sync has run, the BPI's {@code default} localized name stands in.
     */
    private String partnerName;

    /** BP or BPI: the UID of the organisation (commercial register identifier). Whichever side knows it. */
    private String uid;

    /** BP only: the partner type (e.g. GOV, PRIVATE). Empty until the partner sync has run for this partner. */
    @Enumerated(EnumType.STRING)
    private BusinessPartnerType type;

    /**
     * BP only: whether the core business service has explicitly permitted a hard delete of this partner. Defaults to
     * {@code false} in the view, so a partner without a projection offers no deletion.
     */
    private boolean hardDeleteAllowed;

    /**
     * BP only: when the <b>BP projection</b> was last refreshed from the core business service — not the BPI and not
     * any trust statement. Empty for a partner that has never been synced.
     */
    private Instant syncedAt;

    /** BPI only: the identity status (e.g. ACTIVE, DEACTIVATED). Empty for a partner that was never onboarded. */
    @Enumerated(EnumType.STRING)
    private BusinessPartnerIdentityStatus status;

    /** BPI only: when the current identity expires. */
    private Instant validUntil;

    /** BPI only: when the identity was last activated. */
    private Instant lastActivated;

    /** BPI only: when an identity trust statement was last issued for this partner. */
    private Instant lastIssuanceAt;

    /** BPI only: whether the partner acts on behalf of the state, as recorded on the identity. */
    private Boolean isStateActor;

    /** BPI only: whether the organisation is listed in the commercial register, as recorded on the identity. */
    private Boolean isRegisteredInCommercialRegister;

    /** BPI only: the language the partner is to be corresponded in. */
    private String correspondingLanguage;
}
