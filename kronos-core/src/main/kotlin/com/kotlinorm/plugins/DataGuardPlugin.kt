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

package com.kotlinorm.plugins

import com.kotlinorm.beans.task.registerTaskEventPlugin
import com.kotlinorm.beans.task.unregisterTaskEventPlugin
import com.kotlinorm.database.SqlManager.getDBNameFrom
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.TaskEventPlugin
import com.kotlinorm.types.ActionTaskEvent
import com.kotlinorm.types.QueryTaskEvent


/**
 * Plugin to block the whole table from being deleted, updated, or truncated.
 */
object DataGuardPlugin : TaskEventPlugin {

    data class PolicyInfo(
        var databaseName: String = "%",
        var tableName: String? = null,
    ) {
        /**
         * 检查目标数据库和表是否匹配当前策略
         * - databaseName为null表示匹配任意数据库
         * - tableName为null表示匹配任意表
         */
        fun matches(targetDatabase: String, targetTable: String?): Boolean {
            fun isMatch(pattern: String?, target: String?) = when {
                pattern == null -> true
                target == null -> false
                "%" in pattern -> pattern.toRegex().matches(target)
                else -> pattern == target
            }

            return isMatch(databaseName, targetDatabase) && isMatch(tableName, targetTable)
        }

        private fun String.toRegex() = replace("%", ".*")
            .let { Regex("^$it$") }
    }

    data class OperationPolicy(
        val preventByDefault: Boolean = true,
        val whitelist: Set<PolicyInfo> = emptySet(),
        val blacklist: Set<PolicyInfo> = emptySet(),
    ) {
        fun isAllowed(databaseName: String, tableName: String?): Boolean {
            // 当tableName为null时始终允许
            if (tableName == null) return true

            return if (preventByDefault) {
                // 默认阻止模式：只有白名单中的条目能通过
                whitelist.any { it.matches(databaseName, tableName) }
            } else {
                // 默认允许模式：黑名单中的条目会被阻止
                !blacklist.any { it.matches(databaseName, tableName) }
            }
        }
    }

    /**
     * Configuration for the protection plugin.
     */
    data class ProtectionConfig(
        /**
         * Config for delete without where operation
         */
        val deleteAll: OperationPolicy = OperationPolicy(preventByDefault = true),
        /**
         * Config for update without where operation
         */
        val updateAll: OperationPolicy = OperationPolicy(preventByDefault = true),
        /**
         * Config for truncate operation
         */
        val truncate: OperationPolicy = OperationPolicy(preventByDefault = true),
        /**
         * Config for drop operation
         */
        val drop: OperationPolicy = OperationPolicy(preventByDefault = true),
        /**
         * Config for alter operation
         */
        val alter: OperationPolicy = OperationPolicy(preventByDefault = true)
    )

    private var loaded = false
    var enabled
        get() = loaded
        set(value) {
            if (value && !loaded) {
                registerTaskEventPlugin(DataGuardPlugin)
                loaded = true
            }
            if (!value && loaded) {
                unregisterTaskEventPlugin(DataGuardPlugin)
                loaded = false
            }
        }

    private var pluginConfig = ProtectionConfig()

    class OperationPolicyBuilder {
        var preventByDefault: Boolean = true
        val whitelist = mutableSetOf<PolicyInfo>()
        val blacklist = mutableSetOf<PolicyInfo>()

        fun denyAll() {
            preventByDefault = true
        }

        fun allowAll() {
            preventByDefault = false
        }

        fun allow(block: PolicyInfo.() -> Unit) {
            whitelist.add(PolicyInfo().apply(block))
        }

        fun deny(block: PolicyInfo.() -> Unit) {
            blacklist.add(PolicyInfo().apply(block))
        }

        fun build(): OperationPolicy {
            return OperationPolicy(preventByDefault, whitelist.toSet(), blacklist.toSet())
        }
    }

    class ProtectionConfigBuilder {
        private val deleteAllBuilder = OperationPolicyBuilder()
        private val updateAllBuilder = OperationPolicyBuilder()
        private val truncateBuilder = OperationPolicyBuilder()
        private val dropBuilder = OperationPolicyBuilder()
        private val alterBuilder = OperationPolicyBuilder()

        fun deleteAll(block: OperationPolicyBuilder.() -> Unit) {
            deleteAllBuilder.apply(block)
        }

        fun updateAll(block: OperationPolicyBuilder.() -> Unit) {
            updateAllBuilder.apply(block)
        }

        fun truncate(block: OperationPolicyBuilder.() -> Unit) {
            truncateBuilder.apply(block)
        }

        fun drop(block: OperationPolicyBuilder.() -> Unit) {
            dropBuilder.apply(block)
        }

        fun alter(block: OperationPolicyBuilder.() -> Unit) {
            alterBuilder.apply(block)
        }

        fun build(): ProtectionConfig {
            return ProtectionConfig(
                deleteAll = deleteAllBuilder.build(),
                updateAll = updateAllBuilder.build(),
                truncate = truncateBuilder.build(),
                drop = dropBuilder.build(),
                alter = alterBuilder.build()
            )
        }
    }

    fun enable(config: (ProtectionConfigBuilder.() -> Unit)? = null) {
        this.enabled = true
        if (config != null) {
            val builder = ProtectionConfigBuilder()
            builder.apply(config)
            pluginConfig = builder.build()
        }
    }

    fun disable() {
        this.enabled = false
    }

    override val doBeforeQuery: QueryTaskEvent? = null
    override val doAfterQuery: QueryTaskEvent? = null

    override val doBeforeAction: ActionTaskEvent = {
        val dbName = getDBNameFrom(it)
        val tableName = actionInfo?.tableName
        val whereClause = actionInfo?.whereClause

        when (operationType) {
            KOperationType.TRUNCATE -> {
                if (!pluginConfig.truncate.isAllowed(dbName, tableName)) {
                    throw UnsupportedOperationException("Truncate operation is not allowed.")
                }
            }

            KOperationType.DROP -> {
                if (!pluginConfig.drop.isAllowed(dbName, tableName)) {
                    throw UnsupportedOperationException("Drop operation is not allowed.")
                }
            }

            KOperationType.ALTER -> {
                if (!pluginConfig.alter.isAllowed(dbName, tableName)) {
                    throw UnsupportedOperationException("Alter operation is not allowed.")
                }
            }

            KOperationType.DELETE -> {
                if (!pluginConfig.deleteAll.isAllowed(dbName, tableName) && whereClause.isNullOrBlank()) {
                    throw UnsupportedOperationException("Delete operation is not allowed.")
                }
            }

            KOperationType.UPDATE -> {
                if (!pluginConfig.updateAll.isAllowed(dbName, tableName) && whereClause.isNullOrBlank()) {
                    throw UnsupportedOperationException("Update operation is not allowed.")
                }
            }


            else -> {}
        }
    }

    override val doAfterAction: ActionTaskEvent? = null
}