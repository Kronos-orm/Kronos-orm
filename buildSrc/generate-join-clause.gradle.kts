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

    fun File.writeKotlin(source: String) {
        writeText(source)
    }

    fun generatePatch() {
        val fileName = "Patch.kt"
        with(File(joinDir, fileName)) {
            if (!exists()) {
                createNewFile()
            }

            writeKotlin("""
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
                            |    TODO()
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
            val className = "SelectFrom$n<$nthOfType>"
            writeKotlin(
                """
                        |package com.kotlinorm.orm.join

                        |import com.kotlinorm.beans.dsl.KPojo
                        |import com.kotlinorm.beans.dsl.KSelectable
                        |import com.kotlinorm.beans.task.KronosAtomicQueryTask
                        |import com.kotlinorm.interfaces.KronosDataSourceWrapper
                        |import com.kotlinorm.pagination.PagedClause
                        |import com.kotlinorm.types.KTableConditionalField
                        |import com.kotlinorm.types.KTableField
                        |import com.kotlinorm.types.KTableSortableField
                        
                        |class SelectFrom$n<${range.joinToString(", ") { "T$it: KPojo" }}>(
                        |${range.joinToString(",\n") { "    var t$it: T$it" }}
                        |) : KSelectable<T1>(t1) {
                        |    inline fun <reified T : KPojo> leftJoin(another: T, noinline on: KTableConditionalField<Nothing, Boolean?>) {
                        |        TODO()
                        |    }
                        |
                        |    inline fun <reified T : KPojo> rightJoin(another: T, noinline on: KTableConditionalField<Nothing, Boolean?>) {
                        |        TODO()
                        |    }
                        |
                        |    inline fun <reified T : KPojo> crossJoin(another: T, noinline on: KTableConditionalField<Nothing, Boolean?>) {
                        |        TODO()
                        |    }
                        |
                        |    inline fun <reified T : KPojo> innerJoin(another: T, noinline on: KTableConditionalField<Nothing, Boolean?>) {
                        |        TODO()
                        |    }
                        |
                        |    inline fun <reified T : KPojo> fullJoin(another: T, noinline on: KTableConditionalField<Nothing, Boolean?>) {
                        |        TODO()
                        |   }
                        |
                        |    fun select(fields: KTableField<Nothing, Any?>) {
                        |        TODO()
                        |    }
                        |
                        |    fun orderBy(lambda: KTableSortableField<Nothing, Any?>) {
                        |        TODO()
                        |    }
                        |
                        |    fun groupBy(lambda: KTableField<Nothing, Any?>) {
                        |        TODO()
                        |    }
                        
                        |    fun distinct() {
                        |        TODO()
                        |    }
                        
                        |    fun limit(num: Int) {
                        |        TODO()
                        |    }
                        
                        |    fun offset(num: Int) {
                        |        TODO()
                        |    }
                        
                        |    fun page(pi: Int, ps: Int) {
                        |        TODO()
                        |    }
                        
                        |    fun by(lambda: KTableField<Nothing, Any?>) {
                        |        TODO()
                        |    }
                        
                        |    fun where(lambda: KTableConditionalField<Nothing, Boolean?> = null) {
                        |        TODO()
                        |    }
                        
                        |    fun having(lambda: KTableConditionalField<Nothing, Boolean?> = null) {
                        |        TODO()
                        |    }
                        
                        |    fun withTotal(): PagedClause<T1, $className> {
                        |        TODO()
                        |    }
                        
                        |    fun query(): List<Map<String, Any>> {
                        |        TODO()
                        |    }
                        
                        |    fun count(): List<Map<String, Any>> {
                        |        TODO()
                        |    }
                        
                        |    operator fun component1(): String {
                        |        TODO()
                        |    }
                        
                        |    operator fun component2(): Map<String, Any?> {
                        |        TODO()
                        |    }
                        
                        |    override fun build(wrapper: KronosDataSourceWrapper?): KronosAtomicQueryTask {
                        |        TODO("Not yet implemented")
                        |    }
                        |}

                    """.trimMargin()
            )
        }
    }

    joinDir.listFiles()?.forEach { it.delete() }
    generatePatch()
    joinRange.forEach {
        generateSelectFromN(it)
    }
}