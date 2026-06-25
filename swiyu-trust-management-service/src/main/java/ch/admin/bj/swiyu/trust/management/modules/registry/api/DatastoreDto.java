/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.trust.management.modules.registry.api;

import java.util.UUID;
import net.minidev.json.annotate.JsonIgnore;

public record DatastoreDto(UUID id, DatastoreStatusDto status, VcEntityDto vc) {
    @JsonIgnore
    public boolean isActive() {
        return DatastoreStatusDto.ACTIVE.equals(status);
    }
}
