/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditMetadata;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@NoArgsConstructor // JPA
@Getter
@Table(name = "protected_issuance_entry")
@EntityListeners(AuditingEntityListener.class)
public class ProtectedIssuanceEntry {

    @Embedded
    @Valid
    private final AuditMetadata audit = new AuditMetadata();

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "vct")
    private String vct;

    @Column(name = "protected_at")
    private Instant protectedAt;

    public ProtectedIssuanceEntry(String vct, Instant protectedAt) {
        this.id = UUID.randomUUID();
        this.vct = vct;
        this.protectedAt = protectedAt;
    }
}
