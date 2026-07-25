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

import com.kotlinorm.Kronos
import com.kotlinorm.connect
import com.kotlinorm.enums.DBType
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.Driver
import java.sql.DriverManager
import java.sql.DriverPropertyInfo
import java.util.Properties
import java.util.logging.Logger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class DriverManagerDataSourceTest {
    @Test
    fun `Kronos connect registers a configured wrapper after reading JDBC metadata`() {
        val url = "jdbc:kronos-connect:primary"
        val driver = RecordingDriver(setOf(url))
        val previousDataSource = Kronos.dataSource
        DriverManager.registerDriver(driver)
        try {
            val wrapper = Kronos.connect(
                url = url,
                userName = "kronos",
                password = "secret",
                databaseType = DBType.H2
            ) {
                statement.fetchSize = 128
            }

            assertSame(wrapper, Kronos.dataSource())
            assertEquals(DBType.H2, wrapper.dbType)
            assertEquals(url, wrapper.url)
            assertEquals("kronos", wrapper.userName)
            assertEquals(128, wrapper.config.statement.fetchSize)
            assertEquals(
                listOf(ConnectionRequest(url, "kronos", "secret")),
                driver.requests
            )

            wrapper.dataSource.connection.use { }

            assertEquals(
                listOf(
                    ConnectionRequest(url, "kronos", "secret"),
                    ConnectionRequest(url, "kronos", "secret")
                ),
                driver.requests
            )
        } finally {
            Kronos.dataSource = previousDataSource
            DriverManager.deregisterDriver(driver)
        }
    }

    @Test
    fun `driver manager data sources support independent wrappers and create a connection per request`() {
        val writerUrl = "jdbc:kronos-connect:writer"
        val readerUrl = "jdbc:kronos-connect:reader"
        val driver = RecordingDriver(setOf(writerUrl, readerUrl))
        DriverManager.registerDriver(driver)
        try {
            val writerDataSource = DriverManagerDataSource(writerUrl, "writer", "writer-password")
            val readerDataSource = DriverManagerDataSource(readerUrl, "reader", "reader-password")
            val wrappers = mapOf(
                "writer" to KronosJdbcWrapper(writerDataSource, DBType.H2),
                "reader" to KronosJdbcWrapper(readerDataSource, DBType.Postgres)
            )

            assertEquals(DBType.H2, wrappers.getValue("writer").dbType)
            assertEquals(DBType.Postgres, wrappers.getValue("reader").dbType)

            writerDataSource.connection.use { }
            writerDataSource.connection.use { }
            readerDataSource.connection.use { }

            assertEquals(
                listOf(
                    ConnectionRequest(writerUrl, "writer", "writer-password"),
                    ConnectionRequest(readerUrl, "reader", "reader-password"),
                    ConnectionRequest(writerUrl, "writer", "writer-password"),
                    ConnectionRequest(writerUrl, "writer", "writer-password"),
                    ConnectionRequest(readerUrl, "reader", "reader-password")
                ),
                driver.requests
            )
        } finally {
            DriverManager.deregisterDriver(driver)
        }
    }

    @Test
    fun `driver manager data source loads an explicit driver class`() {
        assertFailsWith<ClassNotFoundException> {
            DriverManagerDataSource(
                url = "jdbc:kronos-connect:missing-driver",
                driverClassName = "com.kotlinorm.wrappers.MissingJdbcDriver"
            )
        }
    }

    @Test
    fun `URL dialect inference supports SQL Server and DM8 driver URLs`() {
        assertEquals(
            DBType.Mssql,
            KronosJdbcPlugins.detectDbType("", "jdbc:sqlserver://localhost:1433;databaseName=kronos")
        )
        assertEquals(DBType.DM8, KronosJdbcPlugins.detectDbType("", "jdbc:dm://localhost:5236"))
    }

    private data class ConnectionRequest(
        val url: String,
        val userName: String?,
        val password: String?
    )

    private class RecordingDriver(private val acceptedUrls: Set<String>) : Driver {
        val requests = mutableListOf<ConnectionRequest>()

        override fun connect(url: String?, info: Properties?): Connection? {
            val jdbcUrl = url ?: return null
            if (jdbcUrl !in acceptedUrls) return null
            requests += ConnectionRequest(
                url = jdbcUrl,
                userName = info?.getProperty("user"),
                password = info?.getProperty("password")
            )
            return connection(jdbcUrl, info?.getProperty("user"))
        }

        override fun acceptsURL(url: String?): Boolean = url in acceptedUrls

        override fun getPropertyInfo(url: String?, info: Properties?): Array<DriverPropertyInfo> = emptyArray()

        override fun getMajorVersion(): Int = 1

        override fun getMinorVersion(): Int = 0

        override fun jdbcCompliant(): Boolean = false

        override fun getParentLogger(): Logger = Logger.getGlobal()

        private fun connection(url: String, userName: String?): Connection {
            val metaData = Proxy.newProxyInstance(
                DatabaseMetaData::class.java.classLoader,
                arrayOf(DatabaseMetaData::class.java)
            ) { _, method, _ ->
                when (method.name) {
                    "getDatabaseProductName" -> "Kronos Test"
                    "getURL" -> url
                    "getUserName" -> userName
                    "getDriverName" -> "Kronos Test Driver"
                    else -> defaultValue(method.returnType)
                }
            } as DatabaseMetaData

            return Proxy.newProxyInstance(
                Connection::class.java.classLoader,
                arrayOf(Connection::class.java)
            ) { _, method, _ ->
                when (method.name) {
                    "getMetaData" -> metaData
                    else -> defaultValue(method.returnType)
                }
            } as Connection
        }
    }

    private companion object {
        fun defaultValue(returnType: Class<*>): Any? = when (returnType) {
            Boolean::class.javaPrimitiveType -> false
            Byte::class.javaPrimitiveType -> 0.toByte()
            Short::class.javaPrimitiveType -> 0.toShort()
            Int::class.javaPrimitiveType -> 0
            Long::class.javaPrimitiveType -> 0L
            Float::class.javaPrimitiveType -> 0f
            Double::class.javaPrimitiveType -> 0.0
            Char::class.javaPrimitiveType -> '\u0000'
            else -> null
        }
    }
}
