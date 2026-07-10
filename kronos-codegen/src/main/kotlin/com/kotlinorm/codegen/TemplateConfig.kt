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
package com.kotlinorm.codegen

import com.kotlinorm.Kronos
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.database.SqlManager
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.syntax.statement.SqlQuery
import java.io.File.separator
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class TemplateConfig(
    val table: List<TableConfig>,
    val strategy: StrategyConfig,
    val output: OutputConfig,
    val dataSource: KronosDataSourceWrapper,
) {

    val wrapper = dataSource
    private val statements by lazy { SqlManager.statementsOf(wrapper.dbType) }
    val tableCommentLineWords: Int
    val packageName: String
    val targetDir: String
    val tableNames: List<String> by lazy {
        table.map { it.name }
    }
    val classNames: List<String> by lazy {
        table.map {
            it.className ?: Kronos.tableNamingStrategy.db2k(it.name).replaceFirstChar(Char::titlecase)
        }
    }
    val tableComments: List<String> by lazy {
        table.map {
            queryTableComment(it.name)
        }
    }
    val fields: List<List<Field>> by lazy {
        table.map {
            val rows = queryMapRows(statements.tableColumns(it.name), it.name)
            statements.mapColumns(it.name, rows)
        }
    }
    val indexes: List<List<KTableIndex>> by lazy {
        table.map {
            val rows = queryMapRows(statements.tableIndexes(it.name), it.name)
            statements.mapIndexes(it.name, rows)
        }
    }

    init {
        Kronos.tableNamingStrategy = strategy.tableNamingStrategy ?: Kronos.lineHumpNamingStrategy
        Kronos.fieldNamingStrategy = strategy.fieldNamingStrategy ?: Kronos.lineHumpNamingStrategy
        Kronos.createTimeStrategy = strategy.createTimeStrategy ?: Kronos.createTimeStrategy
        Kronos.updateTimeStrategy = strategy.updateTimeStrategy ?: Kronos.updateTimeStrategy
        Kronos.logicDeleteStrategy = strategy.logicDeleteStrategy ?: Kronos.logicDeleteStrategy
        Kronos.optimisticLockStrategy = strategy.optimisticLockStrategy ?: Kronos.optimisticLockStrategy
        Kronos.dataSource = { wrapper }
        targetDir = output.targetDir
        tableCommentLineWords = output.tableCommentLineWords ?: MAX_COMMENT_LINE_WORDS
        packageName = output.packageName ?: targetDir.split("main/kotlin/").lastOrNull()?.replace('/', '.')
                ?: "com.kotlinorm.orm.table"
    }

    fun toKronosConfigs() = table.mapIndexed { index, tableConfig ->
        KronosTemplate(
            packageName = packageName,
            tableName = tableConfig.name,
            className = classNames[index],
            tableComment = tableComments[index],
            fields = fields[index],
            indexes = indexes[index],
            tableCommentLineWords = tableCommentLineWords
        )
    }

    private fun queryTableComment(tableName: String): String {
        val statement = statements.tableComment() ?: return ""
        return wrapper.first(queryTask(statement, tableName, typeOf<String?>())) as String? ?: ""
    }

    @Suppress("UNCHECKED_CAST")
    private fun queryMapRows(statement: SqlQuery, tableName: String): List<Map<String, Any>> {
        return wrapper.toList(queryTask(statement, tableName)) as List<Map<String, Any>>
    }

    private fun queryTask(
        statement: SqlQuery,
        tableName: String,
        targetType: KType = typeOf<Map<String, Any?>>()
    ): KronosAtomicQueryTask {
        val parameters = mapOf(
            "tableName" to tableName,
            "dbName" to statements.databaseName(wrapper)
        )
        val rendered = SqlManager.renderStatement(wrapper, statement, parameters)
        return KronosAtomicQueryTask(
            rendered.sql,
            rendered.parameters,
            statement = statement,
            targetType = targetType
        )
    }

    companion object {
        fun template(render: KronosTemplate.() -> Unit): List<KronosConfig> {
            codeGenConfig
                ?: error("TemplateConfig is not initialized. Please call init(path: String) first.")
            return codeGenConfig!!.toKronosConfigs().map {
                it.render()
                KronosConfig(
                    it.content,
                    codeGenConfig!!.output.targetDir + separator + it.className + ".kt",
                )
            }
        }
    }
}
