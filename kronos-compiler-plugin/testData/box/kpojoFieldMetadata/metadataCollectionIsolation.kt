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

// Verifies generated metadata keeps empty/nested type shapes, per-instance mutation isolation, and construction-time naming.

import com.kotlinorm.Kronos
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosNamingStrategy
import com.kotlinorm.utils.StaticTypeArgument
import com.kotlinorm.utils.StaticTypeRef
import com.kotlinorm.utils.StaticTypeVariance

class MetadataCollectionRow(
    var id: Int? = null,
    var values: Map<String, List<Int?>>? = null,
) : KPojo

private class RecordingMetadataNaming(var prefix: String) : KronosNamingStrategy {
    val calls = mutableListOf<String>()
    override fun k2db(name: String): String {
        calls += name
        return "${prefix}_$name"
    }
    override fun db2k(name: String): String = name
}

fun box(): String {
    val originalFields = Kronos.fieldNamingStrategy
    val originalTables = Kronos.tableNamingStrategy
    val fields = RecordingMetadataNaming("first")
    val tables = RecordingMetadataNaming("first")
    Kronos.fieldNamingStrategy = fields
    Kronos.tableNamingStrategy = tables
    try {
        val first = MetadataCollectionRow()
        fields.prefix = "second"
        tables.prefix = "second"
        val second = MetadataCollectionRow()
        val firstId = first.__columns.single { it.name == "id" }
        val secondId = second.__columns.single { it.name == "id" }
        val firstValues = first.__columns.single { it.name == "values" }
        val secondValues = second.__columns.single { it.name == "values" }
        val scalar = firstId.typeInfo ?: return "Fail: scalar metadata was missing"
        val nested = firstValues.typeInfo ?: return "Fail: nested metadata was missing"
        val expectedType = StaticTypeRef(
            "kotlin.collections.Map", true,
            listOf(
                StaticTypeArgument(StaticTypeVariance.INVARIANT, StaticTypeRef("kotlin.String", false)),
                StaticTypeArgument(
                    StaticTypeVariance.INVARIANT,
                    StaticTypeRef(
                        "kotlin.collections.List", false,
                        listOf(StaticTypeArgument(StaticTypeVariance.INVARIANT, StaticTypeRef("kotlin.Int", true)))
                    )
                )
            )
        )
        if (scalar.type != StaticTypeRef("kotlin.Int", true)) {
            return "Fail: scalar type was ${scalar.type}"
        }
        if (scalar.declarationVariances != emptyList<StaticTypeVariance>()) {
            return "Fail: scalar declaration variances were ${scalar.declarationVariances}"
        }
        if (nested.type != expectedType) return "Fail: nested type was ${nested.type}"
        if (nested.declarationVariances != listOf(StaticTypeVariance.INVARIANT, StaticTypeVariance.OUT)) {
            return "Fail: map declaration variances were ${nested.declarationVariances}"
        }
        if (first.__columns === second.__columns || firstId === secondId || scalar === secondId.typeInfo) {
            return "Fail: mutable fields or metadata instances were shared"
        }
        if (firstValues.typeInfo === secondValues.typeInfo || nested.type.arguments === secondValues.typeInfo?.type?.arguments) {
            return "Fail: nested metadata instances were shared"
        }
        val firstNames = first.__columns.map { it.columnName }
        val secondNames = second.__columns.map { it.columnName }
        if (firstNames != listOf("first_id", "first_values") || secondNames != listOf("second_id", "second_values")) {
            return "Fail: construction-time column names were $firstNames and $secondNames"
        }
        if (first.__tableName != "first_MetadataCollectionRow" || second.__tableName != "second_MetadataCollectionRow") {
            return "Fail: construction-time table names were ${first.__tableName} and ${second.__tableName}"
        }
        if (fields.calls != listOf("id", "values", "id", "values")) {
            return "Fail: field naming callbacks were ${fields.calls}"
        }
        if (tables.calls != List(6) { "MetadataCollectionRow" }) {
            return "Fail: table naming callbacks were ${tables.calls}"
        }
        // Nonempty generated lists retain their existing independently mutable backing arrays.
        val replacement = StaticTypeArgument(StaticTypeVariance.STAR)
        (nested.type.arguments as MutableList<StaticTypeArgument>)[0] = replacement
        if (nested.type.arguments != listOf(replacement, expectedType.arguments[1])) {
            return "Fail: nonempty generated arguments did not retain mutation behavior"
        }
        if (secondValues.typeInfo?.type != expectedType) {
            return "Fail: mutating one descriptor changed the other: ${secondValues.typeInfo}"
        }
        firstId.columnName = "changed"
        first.__columns.remove(firstValues)
        if (secondId.columnName != "second_id" || second.__columns.map { it.name } != listOf("id", "values")) {
            return "Fail: mutating one entity changed the other: ${second.__columns}"
        }
        return "OK"
    } finally {
        Kronos.fieldNamingStrategy = originalFields
        Kronos.tableNamingStrategy = originalTables
    }
}
