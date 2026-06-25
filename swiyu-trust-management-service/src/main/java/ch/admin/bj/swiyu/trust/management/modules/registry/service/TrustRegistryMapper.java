/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.trust.management.modules.registry.service;

import ch.admin.bj.swiyu.trust.management.modules.registry.api.*;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.*;
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

    public static StatementDto toStatementDto(Statement statement) {
        return new StatementDto(
            statement.getId(),
            statement.isSoftDeleted(),
            statement.isActiveInStatuslist(),
            statement.getSerialized()
        );
    }

    public static StatementType toStatementType(StatementTypeDto type) {
        return switch (type) {
            case TRUST_STATEMENT_IDENTITY_V2 -> StatementType.IDENTITY_TRUST_STATEMENT_V2;
            case TRUST_STATEMENT_VERIFICATION_AUTHORIZATION_V2 -> StatementType.PROTECTED_VERIFICATION_AUTHORIZATION_TRUST_STATEMENT_V2;
            case TRUST_STATEMENT_ISSUANCE_AUTHORIZATION_V2 -> StatementType.PROTECTED_ISSUANCE_AUTHORIZATION_TRUST_STATEMENT_V2;
            case TRUST_LIST_STATEMENT_NON_COMPLIANCE_V2 -> StatementType.NON_COMPLIANCE_TRUST_LIST_STATEMENT_V2;
            case TRUST_LIST_STATEMENT_PROTECTED_ISSUANCE_V2 -> StatementType.PROTECTED_ISSUANCE_TRUST_LIST_STATEMENT_V2;
            case PUBLIC_STATEMENT_VERIFICATION_QUERY_V2 -> StatementType.VERIFICATION_QUERY_PUBLIC_STATEMENT_V2;
        };
    }
}
