/**
 * Copyright 2022-2026 kronos-orm
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

package com.kotlinorm

import com.kotlinorm.enums.DBType
import com.kotlinorm.wrappers.DriverManagerDataSource
import com.kotlinorm.wrappers.KronosJdbcConfig
import com.kotlinorm.wrappers.KronosJdbcWrapper

/**
 * Connects Kronos through a lightweight [DriverManagerDataSource] and installs the resulting
 * wrapper as [Kronos.dataSource].
 *
 * Wrapper construction opens one connection to read JDBC metadata, so connection failures are
 * reported when this function is called. The returned data source is not a connection pool:
 * each later operation opens and closes a physical JDBC connection, while a transaction reuses
 * one connection for its whole scope. Pass a pooled `DataSource` to [KronosJdbcWrapper] for
 * production connection reuse.
 *
 * @param url JDBC URL
 * @param userName optional JDBC user name
 * @param password optional JDBC password
 * @param driverClassName optional JDBC driver class loaded before a connection is requested
 * @param databaseType optional explicit dialect; otherwise inferred from [url]
 * @param configure JDBC wrapper configuration
 * @return the registered JDBC wrapper
 */
@JvmOverloads
fun Kronos.connect(
    url: String,
    userName: String? = null,
    password: String? = null,
    driverClassName: String? = null,
    databaseType: DBType? = null,
    configure: KronosJdbcConfig.() -> Unit = {}
): KronosJdbcWrapper {
    val dataSource = DriverManagerDataSource(url, userName, password, driverClassName)
    val wrapper = KronosJdbcWrapper(dataSource, databaseType, configure)
    this.dataSource = { wrapper }
    return wrapper
}
