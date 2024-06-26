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

extra["export"] = {
    val root = project.layout.projectDirectory.asFile.parent
    val joinDir = File("$root/kronos-core/src/main/kotlin/com/kotlinorm/orm/join")
    val maxJoinNum = 16
    val joinRange = 2..maxJoinNum

    fun generatePatch() {
        val fileName = "Patch.kt"
        with(File(joinDir, fileName)) {
            if (!exists()) {
                createNewFile()
            }

            writeText("""
                    |/**
                    |* Copyright 2022-2024 kronos-orm
                    |*
                    |* Licensed under the Apache License, Version 2.0 (the "License");
                    |* you may not use this file except in compliance with the License.
                    |* You may obtain a copy of the License at
                    |*
                    |*     http://www.apache.org/licenses/LICENSE-2.0
                    |*
                    |* Unless required by applicable law or agreed to in writing, software
                    |* distributed under the License is distributed on an "AS IS" BASIS,
                    |* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
                    |* See the License for the specific language governing permissions and
                    |* limitations under the License.
                    |*/
                    |
                    |package com.kotlinorm.orm.join
                    | 
                    |import com.kotlinorm.beans.dsl.KPojo
                    |${
                joinRange.joinToString("\n\n") { index ->
                    val range = 1..index
                    val nthOfType = range.joinToString(", ") { "T${it}" }
                    """
                            |inline fun <${range.joinToString(",") { "reified T$it : KPojo" }}> T1.join(
                            |${range.drop(1).joinToString(",\n") { "    table$it: T$it" }},
                            |    selectFrom: SelectFrom$index<$nthOfType>.($nthOfType) -> Unit
                            |): SelectFrom$index<$nthOfType> {
                            |    return SelectFrom$index(this, ${
                        range.drop(1).joinToString(", ") { "table$it" }
                    }).apply {
                            |        selectFrom(${range.joinToString(", ") { "t$it" }})
                            |    }
                            |}
                        """
                }
            }
                    """.trimMargin())
        }
    }

    fun generateSelectFromN(n: Int) {
        val fileName = "SelectFrom$n.kt"
        with(File(joinDir, fileName)) {
            if (!exists()) {
                createNewFile()
            }
            val range = 1..n
            val nthOfType = range.joinToString(", ") { "T${it}" }
            val nthOfType_ = range.joinToString(", ") { "T${it}: KPojo" }
            val className = "SelectFrom$n<$nthOfType>"
            writeText(
                """
                        |/**
                        |* Copyright 2022-2024 kronos-orm
                        |*
                        |* Licensed under the Apache License, Version 2.0 (the "License");
                        |* you may not use this file except in compliance with the License.
                        |* You may obtain a copy of the License at
                        |*
                        |*     http://www.apache.org/licenses/LICENSE-2.0
                        |*
                        |* Unless required by applicable law or agreed to in writing, software
                        |* distributed under the License is distributed on an "AS IS" BASIS,
                        |* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
                        |* See the License for the specific language governing permissions and
                        |* limitations under the License.
                        |*/
                        |
                        |package com.kotlinorm.orm.join
                        |
                        |import com.kotlinorm.beans.dsl.KPojo
                        |import com.kotlinorm.pagination.PagedClause
                        |import com.kotlinorm.utils.toLinkedSet
                        |
                        |class SelectFrom$n<$nthOfType_>(
                        |    override var t1: T1,
                        |    ${range.drop(1).joinToString(", ") { "var t$it: T$it" }}
                        |) : SelectFrom<T1>(t1) {
                        |    override var tableName = t1.kronosTableName()
                        |    override var paramMap = t1.toDataMap()
                        |    override var logicDeleteStrategy = t1.kronosLogicDelete()
                        |    override var allFields = t1.kronosColumns().toLinkedSet()
                        |    
                        |    fun withTotal(): PagedClause<T1, SelectFrom$n<$nthOfType>> {
                        |        return PagedClause(this)
                        |    }
                        |}
                    """.trimMargin()
            )
        }
    }

    joinDir.listFiles()?.forEach {
        if (it.name != "SelectFrom.kt") {
            it.delete()
        }
    }
    generatePatch()
    joinRange.forEach {
        generateSelectFromN(it)
    }
}