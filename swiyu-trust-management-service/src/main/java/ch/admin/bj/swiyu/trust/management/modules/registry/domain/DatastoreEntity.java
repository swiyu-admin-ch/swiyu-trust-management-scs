/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.trust.management.modules.registry.domain;

import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditMetadata;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.Getter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A DatastoreEntity is the generic anchor for different files which are managed by this datastore.
 * <p>
 * It allows for unified handling of the most common management actions which we want to perform on our stored data.
 * For example: Deleting or deactivation of entries.
 */
@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
@Table(name = "datastore_entity")
public class DatastoreEntity {

    @Embedded
    @Valid
    private final AuditMetadata auditMetadata = new AuditMetadata();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private DatastoreStatus status;

    public DatastoreEntity() {
        this.status = DatastoreStatus.SETUP;
    }

    public void changeStatus(DatastoreStatus status) {
        this.status = status;
    }
}
