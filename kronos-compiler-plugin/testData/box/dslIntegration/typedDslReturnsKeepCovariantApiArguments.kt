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

// Verifies explicitly typed DSL lambdas remain covariant arguments to the public Any?-returning APIs.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.beans.dsl.KTableForReference
import com.kotlinorm.compiler.support.CompilerTestDataSourceWrapper
import com.kotlinorm.database.SqlManager
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.types.ToInsertSelect
import com.kotlinorm.types.ToReference
import com.kotlinorm.types.ToSelect
import com.kotlinorm.types.ToSort
import kotlin.reflect.KMutableProperty0

@Table("typed_dsl_return_user")
data class TypedDslReturnUser(
    var nullableId: Int? = null,
    var nullableName: String? = null,
    var id: Int = 7,
    var name: String = "source"
) : KPojo

@Table("typed_dsl_return_target")
data class TypedDslReturnTarget(@PrimaryKey var name: String = "") : KPojo

class TypedDslReturnCapture {
    var reads = 0
    fun read(): String { reads++; return "captured" }
}

fun box(): String {
    Kronos.fieldNamingStrategy = Kronos.lineHumpNamingStrategy
    Kronos.tableNamingStrategy = Kronos.lineHumpNamingStrategy
    val user = TypedDslReturnUser()
    val nullableInt: ToSelect<TypedDslReturnUser, Int?> = { it.nullableId }
    val nullableString: ToSelect<TypedDslReturnUser, String?> = { it.nullableName }
    val nonNullInt: ToSelect<TypedDslReturnUser, Int> = { it.id }
    val nonNullString: ToSelect<TypedDslReturnUser, String> = { it.name }
    val nonNullList: ToSelect<TypedDslReturnUser, List<Any>> = { listOf(it.id, it.name) }
    val nullableAny: ToSelect<TypedDslReturnUser, Any?> = { it.id }

    // This conversion uses Kotlin function return-type covariance without casts or a wrapper lambda.
    val selections: List<Pair<ToSelect<TypedDslReturnUser, Any?>, String>> = listOf(
        nullableInt to "`nullable_id` AS `nullableId`",
        nullableString to "`nullable_name` AS `nullableName`",
        nonNullInt to "`id`",
        nonNullString to "`name`",
        nonNullList to "`id`, `name`",
        nullableAny to "`id`"
    )
    val wrapper = CompilerTestDataSourceWrapper
    for ((index, selection) in selections.withIndex()) {
        val task = user.select(selection.first).build(wrapper).atomicTask
        val expectedSql = "SELECT ${selection.second} FROM `typed_dsl_return_user`"
        if (task.sql != expectedSql) return "Fail: typed selection $index SQL was ${task.sql}"
        if (task.paramMap != emptyMap<String, Any?>()) {
            return "Fail: typed selection $index parameters were ${task.paramMap}"
        }
    }

    val sort: ToSort<TypedDslReturnUser, Int> = { it.id }
    val sorted = user.select(nonNullString).orderBy(sort).build(wrapper).atomicTask
    val expectedSort = "SELECT `name` FROM `typed_dsl_return_user` ORDER BY `id` ASC"
    if (sorted.sql != expectedSort) return "Fail: typed sort SQL was ${sorted.sql}"
    if (sorted.paramMap != emptyMap<String, Any?>()) return "Fail: typed sort parameters were ${sorted.paramMap}"

    val reference: ToReference<TypedDslReturnUser, KMutableProperty0<String>> = { it::name }
    val references = KTableForReference<TypedDslReturnUser>()
    reference!!(references, user)
    if (references.fields != listOf(user.__columns.single { it.name == "name" })) {
        return "Fail: typed references were ${references.fields}"
    }
    val cascade = user.select(nonNullString).cascade(reference).build(wrapper).atomicTask
    val expectedCascade = "SELECT `name` FROM `typed_dsl_return_user`"
    if (cascade.sql != expectedCascade) return "Fail: typed cascade SQL was ${cascade.sql}"

    val capture = TypedDslReturnCapture()
    val insert: ToInsertSelect<TypedDslReturnUser, String> = { capture.read() }
    val parameters = linkedMapOf<String, Any?>()
    val statement = user.select().insert<TypedDslReturnTarget>(insert).toSqlStatement(parameters, wrapper)
    val rendered = SqlManager.renderStatement(wrapper, statement, parameters)
    val expectedInsert = "INSERT INTO `typed_dsl_return_target` (`name`) SELECT :name FROM `typed_dsl_return_user`"
    if (rendered.sql != expectedInsert) return "Fail: typed insert SQL was ${rendered.sql}"
    if (rendered.parameters != mapOf("name" to "captured")) {
        return "Fail: typed insert parameters were ${rendered.parameters}"
    }
    if (capture.reads != 1) return "Fail: typed insert captured value was evaluated ${capture.reads} times"
    return "OK"
}
