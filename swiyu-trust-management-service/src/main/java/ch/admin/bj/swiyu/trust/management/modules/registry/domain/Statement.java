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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A Statement represents a statement in our data store.
 * <p>
 * Statement does store the actual jwt (with signature and other addenda)
 * along the payload and header json.
 * This allows to filter and search for payload driven elements specific to the
 * type of statement handled while also providing a generalized way how to handle statements of
 * different types and formats.
 */
@Entity
@Getter
@Table(name = "statement")
@NoArgsConstructor // JPA
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Statement {

    @Embedded
    @Valid
    private final AuditMetadata audit = new AuditMetadata();

    @Id
    private UUID id;

    /**
     * Should not be shown to users in the data registry endpoints.
     */
    @Column(name = "is_soft_deleted")
    private boolean isSoftDeleted;

    /**
     * As we do not want to validate the statuslist everytime,
     * and the TMS does have the authority over the statuslists we use this shortcut.
     */
    @Column(name = "is_active_in_statuslist")
    private boolean isActiveInStatuslist;

    /**
     * The VC in its encoded form, the way it is delivered to the registry.
     * <p>
     * SD-JWT Example:
     * eyJ0eXAiOiJzZCtqd3QiLCJhbGciOiJFUzI1NiJ9.eyJpZCI6IjEyMzQiLCJfc2QiOlsiYkRUUnZtNS1Zbi1IRzdjcXBWUjVPVlJJWHNTYUJrNTdKZ2lPcV9qMVZJNCIsImV0M1VmUnlsd1ZyZlhkUEt6Zzc5aGNqRDFJdHpvUTlvQm9YUkd0TW9zRmsiLCJ6V2ZaTlMxOUF0YlJTVGJvN3NKUm4wQlpRdldSZGNob0M3VVphYkZyalk4Il0sIl9zZF9hbGciOiJzaGEtMjU2In0.n27NCtnuwytlBYtUNjgkesDP_7gN7bhaLhWNL4SWT6MaHsOjZ2ZMp987GgQRL6ZkLbJ7Cd3hlePHS84GBXPuvg~WyI1ZWI4Yzg2MjM0MDJjZjJlIiwiZmlyc3RuYW1lIiwiSm9obiJd~WyJjNWMzMWY2ZWYzNTg4MWJjIiwibGFzdG5hbWUiLCJEb2UiXQ~WyJmYTlkYTUzZWJjOTk3OThlIiwic3NuIiwiMTIzLTQ1LTY3ODkiXQ~
     */
    @Column(name = "serialized")
    private String serialized;

    /**
     * The payload of the VC in its resolved form
     * JWT Example:
     * {
     * "iss": "did:example:something",
     * "sub": "did:example:anything",
     * "key": "value"
     * }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data")
    private JsonNode data;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private StatementType type;

    public void markAsInactiveInStatuslist() {
        this.isActiveInStatuslist = false;
    }
}
