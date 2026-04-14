/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.trust.management.modules.registry.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VcEntityRepository extends JpaRepository<VcEntity, Long> {
    Optional<VcEntity> findByDatastoreEntityId(UUID baseId);
}
