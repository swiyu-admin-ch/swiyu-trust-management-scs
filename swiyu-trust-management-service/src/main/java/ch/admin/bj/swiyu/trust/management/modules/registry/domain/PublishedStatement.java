/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.trust.management.modules.registry.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A PublishedStatement represents a statement in our data store.
 * <p>
 * PublishedStatement does store the actual jwt (with signature and other addenda)
 * along the payload and header json.
 * This allows to filter and search for payload driven elements specific to the
 * type of statement handled while also providing a generalized way how to handle statements of
 * different types and formats.
 */
@Entity
@Getter
@Table(name = "published_statement")
@NoArgsConstructor // JPA
@AllArgsConstructor
public class PublishedStatement {

    @Id
    private UUID id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PublishedStatementStatus status;

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
     * SD-JWT/JWT Example:
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
    private PublishedStatementType type;
}
