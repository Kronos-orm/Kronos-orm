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

@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

// Verifies generated field and provider metadata preserves complete static type structure.

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToSelect
import com.kotlinorm.utils.EnumFactory
import com.kotlinorm.utils.GeneratedTypeProvider
import com.kotlinorm.utils.GeneratedTypeRegistrar
import com.kotlinorm.utils.KPojoFactory
import com.kotlinorm.utils.StaticContainerKind
import com.kotlinorm.utils.StaticTypeInfo
import com.kotlinorm.utils.StaticTypeRef
import com.kotlinorm.utils.StaticTypeVariance
import java.security.MessageDigest
import kotlin.reflect.KType

enum class StaticMetadataStatus { READY, CLOSED }

interface StaticMetadataMarker : KPojo

data class StaticMetadataChild(
    var id: Int? = null,
) : KPojo

class StaticMetadataIterable<out T>(private val values: List<T>) : Iterable<T> {
    override fun iterator(): Iterator<T> = values.iterator()
}

data class StaticMetadataRow(
    var nestedMap: Map<String, List<StaticMetadataStatus?>>? = null,
    var starList: List<*>? = null,
    var inList: MutableList<in StaticMetadataChild>? = null,
    var outList: MutableList<out StaticMetadataChild>? = null,
    var primitiveArray: IntArray? = null,
    var objectArray: Array<StaticMetadataChild>? = null,
    var genericIterable: StaticMetadataIterable<StaticMetadataChild>? = null,
    var child: StaticMetadataChild? = null,
    var status: StaticMetadataStatus? = null,
) : KPojo

private fun StaticMetadataRow.captureFields(
    block: ToSelect<StaticMetadataRow, Any?>,
): List<Field> {
    val result = mutableListOf<Field>()
    afterSelect {
        block!!(it)
        result += fields
    }
    return result
}

fun box(): String {
    val row = StaticMetadataRow()
    val columns = row.__columns.associateBy { it.name }
    fun info(name: String): StaticTypeInfo? = columns[name]?.typeInfo

    val nestedMap = info("nestedMap") ?: return "Fail: nestedMap metadata was missing"
    val starList = info("starList") ?: return "Fail: starList metadata was missing"
    val inList = info("inList") ?: return "Fail: inList metadata was missing"
    val outList = info("outList") ?: return "Fail: outList metadata was missing"
    val primitiveArray = info("primitiveArray") ?: return "Fail: primitiveArray metadata was missing"
    val objectArray = info("objectArray") ?: return "Fail: objectArray metadata was missing"
    val genericIterable = info("genericIterable") ?: return "Fail: genericIterable metadata was missing"
    val child = info("child") ?: return "Fail: child metadata was missing"
    val status = info("status") ?: return "Fail: status metadata was missing"

    if (!nestedMap.type.nullable) return "Fail: nested map nullability was ${nestedMap.type.nullable}"
    if (nestedMap.containerKind != StaticContainerKind.MAP) return "Fail: map kind was ${nestedMap.containerKind}"
    if (nestedMap.elementType != null) return "Fail: map element type was ${nestedMap.elementType}"
    if (nestedMap.type.arguments.size != 2) return "Fail: map argument count was ${nestedMap.type.arguments.size}"
    val mapValue = nestedMap.type.arguments[1].type
    if (mapValue?.arguments?.singleOrNull()?.type?.nullable != true) {
        return "Fail: nested enum nullability was ${mapValue?.arguments?.singleOrNull()?.type?.nullable}"
    }
    if (starList.type.arguments.singleOrNull()?.variance != StaticTypeVariance.STAR) {
        return "Fail: star list arguments were ${starList.type.arguments}"
    }
    if (inList.type.arguments.singleOrNull()?.variance != StaticTypeVariance.IN) {
        return "Fail: in-list projection was ${inList.type.arguments}"
    }
    if (outList.type.arguments.singleOrNull()?.variance != StaticTypeVariance.OUT) {
        return "Fail: out-list projection was ${outList.type.arguments}"
    }
    if (inList.declarationVariances != listOf(StaticTypeVariance.INVARIANT)) {
        return "Fail: mutable-list declaration variance was ${inList.declarationVariances}"
    }
    if (primitiveArray.containerKind != StaticContainerKind.PRIMITIVE_ARRAY) {
        return "Fail: primitive array kind was ${primitiveArray.containerKind}"
    }
    if (primitiveArray.elementType?.key != "kotlin.Int") {
        return "Fail: primitive array element was ${primitiveArray.elementType}"
    }
    if (objectArray.containerKind != StaticContainerKind.ARRAY) {
        return "Fail: object array kind was ${objectArray.containerKind}"
    }
    if (objectArray.elementType?.key != child.type.key) {
        return "Fail: object array element was ${objectArray.elementType}"
    }
    if (!child.isKPojo) return "Fail: child was not marked KPojo"
    if (status.enumKey != status.type.key) return "Fail: enum key was ${status.enumKey}"
    if (genericIterable.declarationVariances != listOf(StaticTypeVariance.OUT)) {
        return "Fail: generic iterable declaration variance was ${genericIterable.declarationVariances}"
    }
    val iterableSupertype = genericIterable.directSupertypes.singleOrNull {
        it.key == "kotlin.collections.Iterable"
    }
    if (iterableSupertype?.arguments?.singleOrNull()?.type?.key != child.type.key) {
        return "Fail: generic iterable supertype was ${genericIterable.directSupertypes}"
    }

    val captured = row.captureFields { [it.genericIterable] }.singleOrNull()
        ?: return "Fail: generated DSL field was not captured"
    if (captured.typeInfo != genericIterable) return "Fail: DSL field metadata was ${captured.typeInfo}"

    return verifyProviderContribution(genericIterable, iterableSupertype)
}

private fun verifyProviderContribution(
    genericIterable: StaticTypeInfo,
    iterableSupertype: StaticTypeRef?
): String {
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
    val contributedGeneric = contributed.singleOrNull { it.type == genericIterable.type }
        ?: return "Fail: provider did not contribute ${genericIterable.type}"
    if (contributedGeneric.directSupertypes != genericIterable.directSupertypes) {
        return "Fail: provider supertype graph was ${contributedGeneric.directSupertypes}"
    }
    val contributedIterable = contributed.singleOrNull { it.type == iterableSupertype }
        ?: return "Fail: provider did not contribute $iterableSupertype"
    if (contributedIterable.declarationVariances != listOf(StaticTypeVariance.OUT)) {
        return "Fail: provider iterable declaration variance was ${contributedIterable.declarationVariances}"
    }
    val markerInfo = contributed.singleOrNull {
        it.type.key == "StaticMetadataMarker" && !it.type.nullable
    } ?: return "Fail: provider did not contribute StaticMetadataMarker"
    if (!markerInfo.isKPojo) {
        return "Fail: interface KPojo metadata was $markerInfo"
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
