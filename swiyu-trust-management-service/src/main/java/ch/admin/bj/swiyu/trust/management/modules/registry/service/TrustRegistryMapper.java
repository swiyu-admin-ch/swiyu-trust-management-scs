/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.trust.management.modules.registry.service;

import ch.admin.bj.swiyu.trust.management.modules.registry.api.DatastoreDto;
import ch.admin.bj.swiyu.trust.management.modules.registry.api.DatastoreStatusDto;
import ch.admin.bj.swiyu.trust.management.modules.registry.api.VcEntityDto;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.DatastoreEntity;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.DatastoreStatus;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.VcEntity;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TrustRegistryMapper {

    public static VcEntityDto toVcEntityDto(VcEntity entity) {
        return new VcEntityDto(entity.getRawVc() != null);
    }

    public static DatastoreDto toDatastoreEntityResponseDto(DatastoreEntity entity, VcEntity vcEntity) {
        return new DatastoreDto(entity.getId(), toDatastoreStatusDto(entity.getStatus()), toVcEntityDto(vcEntity));
    }

    public static DatastoreStatusDto toDatastoreStatusDto(DatastoreStatus source) {
        if (source == null) {
            return null;
        }
        return switch (source) {
            case ACTIVE -> DatastoreStatusDto.ACTIVE;
            case DEACTIVATED -> DatastoreStatusDto.DEACTIVATED;
            case DISABLED -> DatastoreStatusDto.DISABLED;
            case SETUP -> DatastoreStatusDto.SETUP;
        };
    }
}
