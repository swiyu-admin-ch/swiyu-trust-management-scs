/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.trust.management.modules.registry.domain;

import lombok.Getter;

@Getter
public enum DatastoreStatus {
    ACTIVE,
    DEACTIVATED,
    DISABLED,
    SETUP,
}
