/**
 * Copyright 2022-2024 kronos-orm
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
import com.kotlinorm.KronosBasicWrapper
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.database.SqlManager.getTableColumns
import com.kotlinorm.database.SqlManager.getTableIndexes
import com.kotlinorm.orm.ddl.queryTableComment
import javax.sql.DataSource

class TemplateConfig(
    val table: TableConfig,
    val output: OutputConfig,
    val dataSource: DataSource,
) {

    val wrapper = KronosBasicWrapper(dataSource)
    val tableComment: String
    val tableCommentLineWords: Int
    val fields: List<Field>
    val indexes: List<KTableIndex>
    val packageName: String
    val targetDir: String
    val tableName: String
    val className: String

    val formatedKotlinComment by lazy {
        if (tableComment.isEmpty()) {
            ""
        } else {
            val words = tableComment.split(" ")
            val lines = mutableListOf<String>()
            var currentLine = ""
            for (word in words) {
                if (currentLine.length + word.length + 1 > tableCommentLineWords) {
                    lines.add(currentLine.trim())
                    currentLine = ""
                }
                currentLine += "$word "
            }
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine.trim())
            }
            lines.joinToString("\n") { "// $it" }
        }
    }


    init {
        Kronos.init {
            Kronos.tableNamingStrategy = table.tableNamingStrategy ?: lineHumpNamingStrategy
            Kronos.fieldNamingStrategy = table.fieldNamingStrategy ?: lineHumpNamingStrategy
            Kronos.createTimeStrategy = table.createTimeStrategy ?: Kronos.createTimeStrategy
            Kronos.updateTimeStrategy = table.updateTimeStrategy ?: Kronos.updateTimeStrategy
            Kronos.logicDeleteStrategy = table.logicDeleteStrategy ?: Kronos.logicDeleteStrategy
            Kronos.optimisticLockStrategy = table.optimisticLockStrategy ?: Kronos.optimisticLockStrategy
            Kronos.dataSource = { wrapper }
        }
        tableName = table.name
        tableComment = queryTableComment(tableName, wrapper)
        fields = getTableColumns(wrapper, tableName)
        indexes = getTableIndexes(wrapper, tableName)
        targetDir = output.targetDir
        tableCommentLineWords = output.tableCommentLineWords ?: 100
        packageName = output.packageName ?: targetDir.split("/").drop(4).joinToString(".")
        className = table.className ?: Kronos.tableNamingStrategy.db2k(tableName).replaceFirstChar(Char::titlecase)
    }
}