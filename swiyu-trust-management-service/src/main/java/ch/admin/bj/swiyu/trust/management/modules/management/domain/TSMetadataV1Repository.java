/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @deprecated Use TrustStatementPartnerLink with embedded json instead, this will be removed with EID-5295
 */
@Deprecated(since = "2.2.0")
public interface TSMetadataV1Repository extends JpaRepository<TSMetadataV1, UUID> {} // NOSONAR
