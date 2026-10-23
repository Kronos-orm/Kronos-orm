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

@file:OptIn(ExperimentalStdlibApi::class, com.kotlinorm.annotations.InternalKronosApi::class)

// Verifies direct KronosAtomicQueryTask typeOf targets receive matching static metadata.

import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.EnumFactory
import com.kotlinorm.utils.GeneratedTypeProvider
import com.kotlinorm.utils.GeneratedTypeRegistrar
import com.kotlinorm.utils.KPojoFactory
import com.kotlinorm.utils.StaticContainerKind
import com.kotlinorm.utils.StaticTypeArgument
import com.kotlinorm.utils.StaticTypeInfo
import com.kotlinorm.utils.StaticTypeRef
import com.kotlinorm.utils.StaticTypeVariance
import java.security.MessageDigest
import kotlin.reflect.KType
import kotlin.reflect.typeOf

data class DirectTaskMetadataUser(
    var id: Int? = null,
) : KPojo

enum class DirectTaskMetadataStatus { READY, CLOSED }

open class DirectTaskMetadataAncestor

open class DirectTaskMetadataParent : DirectTaskMetadataAncestor()

class DirectTaskMetadataChild : DirectTaskMetadataParent()

private fun kPojoTask(): KronosAtomicQueryTask = KronosAtomicQueryTask(
    sql = "SELECT id FROM direct_task_metadata_user",
    targetType = typeOf<DirectTaskMetadataUser>(),
)

private fun enumTask(): KronosAtomicQueryTask = KronosAtomicQueryTask(
    sql = "SELECT status FROM direct_task_metadata_user",
    targetType = typeOf<DirectTaskMetadataStatus>(),
)

private fun nestedTask(): KronosAtomicQueryTask = KronosAtomicQueryTask(
    sql = "SELECT payload FROM direct_task_metadata_user",
    targetType = typeOf<Map<String, List<DirectTaskMetadataStatus?>>?>(),
)

private fun inheritanceTask(): KronosAtomicQueryTask = KronosAtomicQueryTask(
    sql = "SELECT payload FROM direct_task_metadata_user",
    targetType = typeOf<DirectTaskMetadataChild>(),
)

fun box(): String {
    val kPojo = kPojoTask()
    if (kPojo.targetType != typeOf<DirectTaskMetadataUser>()) {
        return "Fail: KPojo target type was ${kPojo.targetType}"
    }
    val kPojoInfo = kPojo.targetTypeInfo ?: return "Fail: KPojo target metadata was null"
    if (!kPojoInfo.isKPojo) return "Fail: KPojo target was not marked KPojo"
    if (kPojoInfo.type != StaticTypeRef("DirectTaskMetadataUser", nullable = false)) {
        return "Fail: KPojo target metadata was ${kPojoInfo.type}"
    }

    val enum = enumTask()
    if (enum.targetType != typeOf<DirectTaskMetadataStatus>()) {
        return "Fail: enum target type was ${enum.targetType}"
    }
    val enumInfo = enum.targetTypeInfo ?: return "Fail: enum target metadata was null"
    if (enumInfo.enumKey != "DirectTaskMetadataStatus") {
        return "Fail: enum key was ${enumInfo.enumKey}"
    }
    if (enumInfo.type != StaticTypeRef("DirectTaskMetadataStatus", nullable = false)) {
        return "Fail: enum target metadata was ${enumInfo.type}"
    }

    val nested = nestedTask()
    if (nested.targetType != typeOf<Map<String, List<DirectTaskMetadataStatus?>>?>()) {
        return "Fail: nested target type was ${nested.targetType}"
    }
    val nestedInfo = nested.targetTypeInfo ?: return "Fail: nested target metadata was null"
    val expectedNestedType = StaticTypeRef(
        key = "kotlin.collections.Map",
        nullable = true,
        arguments = listOf(
            StaticTypeArgument(StaticTypeVariance.INVARIANT, StaticTypeRef("kotlin.String", nullable = false)),
            StaticTypeArgument(
                StaticTypeVariance.INVARIANT,
                StaticTypeRef(
                    key = "kotlin.collections.List",
                    nullable = false,
                    arguments = listOf(
                        StaticTypeArgument(
                            StaticTypeVariance.INVARIANT,
                            StaticTypeRef("DirectTaskMetadataStatus", nullable = true)
                        )
                    )
                )
            )
        )
    )
    if (nestedInfo.type != expectedNestedType) {
        return "Fail: nested target metadata was ${nestedInfo.type}"
    }
    if (nestedInfo.containerKind != StaticContainerKind.MAP) {
        return "Fail: nested target kind was ${nestedInfo.containerKind}"
    }
    if (nestedInfo.elementType != null) return "Fail: map element type was ${nestedInfo.elementType}"
    if (nestedInfo.declarationVariances != listOf(StaticTypeVariance.INVARIANT, StaticTypeVariance.OUT)) {
        return "Fail: map declaration variance was ${nestedInfo.declarationVariances}"
    }

    val inheritance = inheritanceTask()
    val childInfo = inheritance.targetTypeInfo ?: return "Fail: inheritance target metadata was null"
    val parentType = StaticTypeRef("DirectTaskMetadataParent", nullable = false)
    val ancestorType = StaticTypeRef("DirectTaskMetadataAncestor", nullable = false)
    if (childInfo.type != StaticTypeRef("DirectTaskMetadataChild", nullable = false)) {
        return "Fail: inheritance target type was ${childInfo.type}"
    }
    if (childInfo.directSupertypes != listOf(parentType)) {
        return "Fail: inheritance target supertypes were ${childInfo.directSupertypes}"
    }

    val provider = Class.forName(staticMetadataProviderIdentity("kronos-test:main").second)
        .getDeclaredConstructor()
        .newInstance() as GeneratedTypeProvider
    val contributed = mutableListOf<StaticTypeInfo>()
    provider.contributeTo(object : GeneratedTypeRegistrar {
        override fun registerKPojo(
            type: KType,
            ownerId: String,
            constructorSignature: String,
            factory: KPojoFactory,
        ) = Unit

        override fun registerEnum(type: KType, entryNames: List<String>, factory: EnumFactory) = Unit

        override fun registerTypeInfo(typeInfo: StaticTypeInfo) {
            contributed += typeInfo
        }
    })
    if (contributed.none { it.type == childInfo.type }) {
        return "Fail: provider did not contribute ${childInfo.type}"
    }
    val contributedParent = contributed.singleOrNull { it.type == parentType }
        ?: return "Fail: provider did not contribute $parentType"
    if (contributedParent.directSupertypes != listOf(ancestorType)) {
        return "Fail: provider parent supertypes were ${contributedParent.directSupertypes}"
    }
    if (contributed.none { it.type == ancestorType }) {
        return "Fail: provider did not contribute $ancestorType"
    }

    return "OK"
}

private fun staticMetadataProviderIdentity(moduleCoordinate: String): Pair<String, String> {
    val hash = MessageDigest.getInstance("SHA-256")
        .digest(moduleCoordinate.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
        .take(16)
    return "$moduleCoordinate#$hash" to
        "com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_$hash"
}
