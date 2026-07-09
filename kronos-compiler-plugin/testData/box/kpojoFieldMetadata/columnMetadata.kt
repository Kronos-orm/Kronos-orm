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

// Verifies generated KPojo Field metadata, including KType generic arguments.

import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.DateTimeFormat
import com.kotlinorm.annotations.Default
import com.kotlinorm.annotations.NonNull
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.interfaces.KPojo
import kotlin.reflect.KClass

data class ColumnUser(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    @Column("user_name")
    @NonNull
    @Default("guest")
    var name: String? = null,
    @ColumnType(KColumnType.VARCHAR, length = 64)
    var status: String? = null,
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    var createdAt: String? = null,
    @Serialize
    var tags: List<String>? = null,
    @Serialize
    var listData: List<Int>? = null,
    @Serialize
    var matrixData: List<List<String>>? = null,
) : KPojo

fun box(): String {
    val columns = ColumnUser().kronosColumns()
    fun column(name: String) = columns.singleOrNull { it.name == name }
        ?: error("missing column $name")

    val id = column("id")
    val name = column("name")
    val status = column("status")
    val createdAt = column("createdAt")
    val tags = column("tags")
    val listData = column("listData")
    val listDataClassifier = listData.kType?.classifier as? KClass<*>
    val listDataArgument = listData.kType?.arguments?.singleOrNull()?.type
    val listDataArgumentClassifier = listDataArgument?.classifier as? KClass<*>
    val matrixData = column("matrixData")
    val matrixDataClassifier = matrixData.kType?.classifier as? KClass<*>
    val matrixDataArgument = matrixData.kType?.arguments?.singleOrNull()?.type
    val matrixDataArgumentClassifier = matrixDataArgument?.classifier as? KClass<*>
    val matrixDataInnerArgument = matrixDataArgument?.arguments?.singleOrNull()?.type
    val matrixDataInnerArgumentClassifier = matrixDataInnerArgument?.classifier as? KClass<*>

    return when {
        id.primaryKey != PrimaryKeyType.IDENTITY -> "Fail: id primary key was ${id.primaryKey}"
        name.columnName != "user_name" -> "Fail: name columnName was ${name.columnName}"
        name.nullable -> "Fail: name should be non-nullable"
        name.defaultValue != "guest" -> "Fail: name default was ${name.defaultValue}"
        status.type != KColumnType.VARCHAR -> "Fail: status type was ${status.type}"
        status.length != 64 -> "Fail: status length was ${status.length}"
        createdAt.dateFormat != "yyyy-MM-dd HH:mm:ss" -> "Fail: date format was ${createdAt.dateFormat}"
        !tags.serializable -> "Fail: tags should be serializable"
        !listData.serializable -> "Fail: listData should be serializable"
        listDataClassifier != List::class -> "Fail: listData kType classifier was $listDataClassifier"
        listDataArgumentClassifier != Int::class -> "Fail: listData kType argument was $listDataArgument"
        listDataArgument?.isMarkedNullable == true -> "Fail: listData element type should not be nullable: $listDataArgument"
        listData.kType?.isMarkedNullable != true -> "Fail: listData kType should be nullable: ${listData.kType}"
        !matrixData.serializable -> "Fail: matrixData should be serializable"
        matrixDataClassifier != List::class -> "Fail: matrixData kType classifier was $matrixDataClassifier"
        matrixDataArgumentClassifier != List::class -> "Fail: matrixData outer argument was $matrixDataArgument"
        matrixDataInnerArgumentClassifier != String::class -> "Fail: matrixData inner argument was $matrixDataInnerArgument"
        matrixDataArgument?.isMarkedNullable == true -> "Fail: matrixData nested list should not be nullable: $matrixDataArgument"
        matrixDataInnerArgument?.isMarkedNullable == true -> "Fail: matrixData string element should not be nullable: $matrixDataInnerArgument"
        matrixData.kType?.isMarkedNullable != true -> "Fail: matrixData kType should be nullable: ${matrixData.kType}"
        else -> "OK"
    }
}
