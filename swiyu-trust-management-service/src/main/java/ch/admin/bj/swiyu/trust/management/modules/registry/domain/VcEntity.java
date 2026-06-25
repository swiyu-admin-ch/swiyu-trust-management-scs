/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.trust.management.modules.registry.domain;

import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A VcEntity represents a VC of different formats and types in our data store.
 * <p>
 * VcEntity does store the actual vc (with signature and other addenda)
 * along the payload json.
 * This allows to filter and search for payload driven elements specific to the
 * type of VC handled while also providing a generalized way how to handle VCs of
 * different types and formats.
 */
@Entity
@NoArgsConstructor // JPA
@Getter
@Table(name = "vc_entity")
@EntityListeners(AuditingEntityListener.class)
public class VcEntity {

    @Embedded
    @Valid
    private final AuditMetadata audit = new AuditMetadata();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "base_id")
    private UUID datastoreEntityId;

    /**
     * The VC in its encoded form, the way it is delivered to the registry.
     * <p>
     */
    @Column(name = "raw_vc")
    private String rawVc;

    /**
     * The payload of the VC in its resolved form
     * SD-JWT/JWT Example:
     * {
     * "iss": "did:example:something",
     * "sub": "did:example:anything",
     * "key": "value"
     * }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "vc_payload")
    private JsonNode vcPayload;

    public VcEntity(UUID datastoreEntityId, String rawVc, JsonNode vcPayload) {
        this.datastoreEntityId = datastoreEntityId;
        this.rawVc = rawVc;
        this.vcPayload = vcPayload;
    }

    public void updateVc(String rawVc, JsonNode vcPayload) {
        this.vcPayload = vcPayload;
        this.rawVc = rawVc;
    }
}
