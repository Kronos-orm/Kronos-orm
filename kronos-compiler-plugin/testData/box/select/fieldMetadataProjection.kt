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

// Verifies select field metadata preserves annotation-derived field attributes.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Cascade
import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.DateTimeFormat
import com.kotlinorm.annotations.Default
import com.kotlinorm.annotations.Ignore
import com.kotlinorm.annotations.NonNull
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.enums.CascadeDeleteAction
import com.kotlinorm.enums.IgnoreAction
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToSelect
import kotlin.reflect.KClass

data class MetadataProjectionChild(
    var id: Int? = null,
    var ownerId: Int? = null,
) : KPojo

data class MetadataProjectionUser(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    @PrimaryKey
    var defaultId: Int? = null,
    @PrimaryKey(uuid = true)
    var uuidId: String? = null,
    @PrimaryKey(snowflake = true)
    var snowflakeId: Long? = null,
    @PrimaryKey(custom = true)
    var customId: String? = null,
    @Column("user_name")
    @ColumnType(KColumnType.VARCHAR, length = 64, scale = 2)
    @NonNull
    var name: String? = null,
    @DateTimeFormat("yyyy-MM-dd")
    @Default("CURRENT_DATE")
    var createdAt: String? = null,
    @Serialize
    var payload: String? = null,
    @Ignore([IgnoreAction.SELECT, IgnoreAction.CASCADE_SELECT])
    var transientValue: String? = null,
    @Cascade(
        ["id"],
        ["ownerId"],
        onDelete = CascadeDeleteAction.SET_DEFAULT,
        defaultValue = ["0"],
        usage = [KOperationType.SELECT]
    )
    var child: MetadataProjectionChild? = null,
    @Cascade(["id"], ["ownerId"])
    var children: List<MetadataProjectionChild> = emptyList(),
) : KPojo

fun MetadataProjectionUser.collectMetadataFields(block: ToSelect<MetadataProjectionUser, Any?>): List<Field> {
    val result = mutableListOf<Field>()
    afterSelect {
        block!!(it)
        result += fields
    }
    return result
}

fun expectMetadata(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val fields = MetadataProjectionUser().collectMetadataFields {
        [
            it.id,
            it.defaultId,
            it.uuidId,
            it.snowflakeId,
            it.customId,
            it.name,
            it.createdAt,
            it.payload,
            it.transientValue,
            it.child,
            it.children,
        ]
    }.associateBy { it.name }

    val id = fields["id"]
    val defaultId = fields["defaultId"]
    val uuidId = fields["uuidId"]
    val snowflakeId = fields["snowflakeId"]
    val customId = fields["customId"]
    val name = fields["name"]
    val createdAt = fields["createdAt"]
    val payload = fields["payload"]
    val transientValue = fields["transientValue"]
    val child = fields["child"]
    val children = fields["children"]
    val childrenKType = children?.kType
    val childrenClassifier = childrenKType?.classifier as? KClass<*>
    val childrenElementType = childrenKType?.arguments?.singleOrNull()?.type
    val childrenElementClassifier = childrenElementType?.classifier as? KClass<*>
    val childrenDerivedElementType = children?.elementKType
    val childrenDerivedElementClassifier = childrenDerivedElementType?.classifier as? KClass<*>

    val failures = listOfNotNull(
        expectMetadata(id?.primaryKey == PrimaryKeyType.IDENTITY) { "id primaryKey was ${id?.primaryKey}" },
        expectMetadata(id?.nullable == false) { "id nullable was ${id?.nullable}" },
        expectMetadata(defaultId?.primaryKey == PrimaryKeyType.DEFAULT) {
            "defaultId primaryKey was ${defaultId?.primaryKey}"
        },
        expectMetadata(uuidId?.primaryKey == PrimaryKeyType.UUID) { "uuidId primaryKey was ${uuidId?.primaryKey}" },
        expectMetadata(snowflakeId?.primaryKey == PrimaryKeyType.SNOWFLAKE) {
            "snowflakeId primaryKey was ${snowflakeId?.primaryKey}"
        },
        expectMetadata(customId?.primaryKey == PrimaryKeyType.CUSTOM) {
            "customId primaryKey was ${customId?.primaryKey}"
        },
        expectMetadata(name?.columnName == "user_name") { "name column was ${name?.columnName}" },
        expectMetadata(name?.type == KColumnType.VARCHAR) { "name type was ${name?.type}" },
        expectMetadata(name?.length == 64) { "name length was ${name?.length}" },
        expectMetadata(name?.scale == 2) { "name scale was ${name?.scale}" },
        expectMetadata(name?.nullable == false) { "name nullable was ${name?.nullable}" },
        expectMetadata(createdAt?.dateFormat == "yyyy-MM-dd") { "createdAt dateFormat was ${createdAt?.dateFormat}" },
        expectMetadata(createdAt?.defaultValue == "CURRENT_DATE") { "createdAt default was ${createdAt?.defaultValue}" },
        expectMetadata(payload?.serializable == true) { "payload serializable was ${payload?.serializable}" },
        expectMetadata(transientValue?.ignore?.toSet() == setOf(IgnoreAction.SELECT, IgnoreAction.CASCADE_SELECT)) {
            "transient ignore was ${transientValue?.ignore?.toList()}"
        },
        expectMetadata(child?.cascade?.properties?.contentEquals(arrayOf("id")) == true) {
            "child cascade properties were ${child?.cascade?.properties?.toList()}"
        },
        expectMetadata(child?.cascade?.targetProperties?.contentEquals(arrayOf("ownerId")) == true) {
            "child cascade target was ${child?.cascade?.targetProperties?.toList()}"
        },
        expectMetadata(child?.cascade?.onDelete == CascadeDeleteAction.SET_DEFAULT) {
            "child cascade onDelete was ${child?.cascade?.onDelete}"
        },
        expectMetadata(child?.cascade?.defaultValue?.contentEquals(arrayOf("0")) == true) {
            "child cascade default was ${child?.cascade?.defaultValue?.toList()}"
        },
        expectMetadata(child?.cascade?.usage?.contentEquals(arrayOf(KOperationType.SELECT)) == true) {
            "child cascade usage was ${child?.cascade?.usage?.toList()}"
        },
        expectMetadata(child?.cascadeIsCollectionOrArray == false) {
            "child collection flag was ${child?.cascadeIsCollectionOrArray}"
        },
        expectMetadata(child?.kClass == MetadataProjectionChild::class) { "child kClass was ${child?.kClass}" },
        expectMetadata(children?.cascadeIsCollectionOrArray == true) {
            "children collection flag was ${children?.cascadeIsCollectionOrArray}"
        },
        expectMetadata(children?.kClass == List::class) { "children kClass was ${children?.kClass}" },
        expectMetadata(childrenClassifier == List::class) { "children kType classifier was $childrenClassifier" },
        expectMetadata(childrenElementClassifier == MetadataProjectionChild::class) {
            "children kType element was $childrenElementType"
        },
        expectMetadata(childrenDerivedElementType == childrenElementType) {
            "children elementKType was $childrenDerivedElementType, expected $childrenElementType"
        },
        expectMetadata(childrenDerivedElementClassifier == MetadataProjectionChild::class) {
            "children elementKType classifier was $childrenDerivedElementClassifier"
        },
    )

    return failures.firstOrNull() ?: "OK"
}
