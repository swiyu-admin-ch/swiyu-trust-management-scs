/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditMetadata;
import ch.admin.bj.swiyu.trust.management.modules.common.i18n.ValidLocalizedMap;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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

    @ValidLocalizedMap
    @Column(name = "name", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, @NotBlank String> name;

    public ProtectedIssuanceEntry(UUID id, String vct, Instant protectedAt, Map<String, String> name) {
        this.id = id;
        this.vct = vct;
        this.protectedAt = protectedAt;
        this.name = name;
    }
}
