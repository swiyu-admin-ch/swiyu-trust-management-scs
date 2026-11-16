/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.trust.management.modules.registry.domain;

import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatastoreEntityRepository extends JpaRepository<DatastoreEntity, UUID> {
    Stream<DatastoreEntity> findAllByStatus(DatastoreStatus datastoreStatus);
}
