/**
 * Copyright 2022-2025 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotlinorm.enums

import java.sql.Connection

/**
 * Transaction Isolation Level.
 *
 * Defines the standard SQL transaction isolation levels supported by Kronos.
 * Each level corresponds to a JDBC [Connection] transaction isolation constant.
 *
 * @property level The JDBC isolation level constant value.
 * @see java.sql.Connection.TRANSACTION_READ_UNCOMMITTED
 * @see java.sql.Connection.TRANSACTION_READ_COMMITTED
 * @see java.sql.Connection.TRANSACTION_REPEATABLE_READ
 * @see java.sql.Connection.TRANSACTION_SERIALIZABLE
 */
enum class TransactionIsolation(val level: Int) {
    READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),
    READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),
    REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),
    SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE)
}
