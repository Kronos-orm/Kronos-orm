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

package com.kotlinorm.wrappers

import java.io.PrintWriter
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.SQLFeatureNotSupportedException
import java.util.logging.Logger
import javax.sql.DataSource

/**
 * A lightweight, non-pooling [DataSource] backed by [DriverManager].
 *
 * Each [getConnection] call creates a new physical JDBC connection. Use an application-managed
 * pooling [DataSource] for workloads that need connection reuse.
 *
 * @param url JDBC URL passed to [DriverManager]
 * @param userName optional JDBC user name
 * @param password optional JDBC password
 * @param driverClassName optional JDBC driver class loaded before connections are requested
 */
class DriverManagerDataSource @JvmOverloads constructor(
    val url: String,
    val userName: String? = null,
    val password: String? = null,
    driverClassName: String? = null
) : DataSource {
    val driverClassName: String? = driverClassName?.takeIf { it.isNotBlank() }

    init {
        this.driverClassName?.let { Class.forName(it) }
    }

    override fun getConnection(): Connection =
        if (userName == null && password == null) {
            DriverManager.getConnection(url)
        } else {
            DriverManager.getConnection(url, userName, password)
        }

    override fun getConnection(userName: String?, password: String?): Connection =
        DriverManager.getConnection(url, userName, password)

    override fun getLogWriter(): PrintWriter? = DriverManager.getLogWriter()

    override fun setLogWriter(out: PrintWriter?) {
        DriverManager.setLogWriter(out)
    }

    override fun setLoginTimeout(seconds: Int) {
        DriverManager.setLoginTimeout(seconds)
    }

    override fun getLoginTimeout(): Int = DriverManager.getLoginTimeout()

    override fun getParentLogger(): Logger =
        throw SQLFeatureNotSupportedException("DriverManager does not expose a parent logger")

    override fun <T> unwrap(iface: Class<T>): T =
        if (iface.isInstance(this)) {
            iface.cast(this)
        } else {
            throw SQLException("${javaClass.name} is not a wrapper for ${iface.name}")
        }

    override fun isWrapperFor(iface: Class<*>): Boolean = iface.isInstance(this)
}
