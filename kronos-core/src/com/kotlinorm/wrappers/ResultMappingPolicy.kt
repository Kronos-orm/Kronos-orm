@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm.wrappers

import com.kotlinorm.annotations.InternalKronosApi
import com.kotlinorm.beans.task.ResultColumnMetadata
import com.kotlinorm.utils.StaticContainerKind
import com.kotlinorm.utils.StaticTypeInfo
import com.kotlinorm.utils.platformClassAssignable
import com.kotlinorm.utils.codec.accepts
import kotlin.reflect.KClass
import kotlin.reflect.KType

/** Only direct Map/MutableMap declarations can describe the rows we materialize. */
@InternalKronosApi
fun KType.mapResultShapeOrNull(typeInfo: StaticTypeInfo? = null): MapResultShape? {
    val declaration = classifier as? KClass<*> ?: return null
    val direct = declaration == Map::class || declaration == MutableMap::class
    if (!direct) {
        val mapSubtype = declaration == HashMap::class || declaration == LinkedHashMap::class ||
            platformClassAssignable(declaration, Map::class) || typeInfo?.containerKind == StaticContainerKind.MAP
        require(!mapSubtype) {
            "Unsupported Map result type $this: row mapping returns LinkedHashMap " +
                "and supports only direct Map or MutableMap declarations"
        }
        return null
    }
    require(arguments.size == 2) { "Map result type must retain both key and value arguments: $this" }
    val key = arguments[0].type
    require(key == null || (key.classifier == String::class && key.arguments.isEmpty())) {
        "Unsupported Map key type $key in $this: row labels require String, String?, or *"
    }
    return MapResultShape(arguments[1].type)
}

/** Replacing a logical target also replaces its descriptor; source-field storage and format policies stay intact. */
@InternalKronosApi
fun ResultColumnMetadata.withTargetType(target: KType, targetTypeInfo: StaticTypeInfo? = null): ResultColumnMetadata =
    copy(type = target, typeInfo = targetTypeInfo ?: typeInfo.takeIf { type == target })

/** Any/star preserves planned logical metadata; explicit types replace stale descriptors. */
@InternalKronosApi
fun resultColumnForType(target: KType?, declared: ResultColumnMetadata?, label: String, targetTypeInfo: StaticTypeInfo? = null): ResultColumnMetadata? {
    if (target == null || target.isAnyResultType()) return declared?.copy(columnLabel = label)
    return declared?.withTargetType(target, targetTypeInfo)?.copy(columnLabel = label)
        ?: ResultColumnMetadata(target, columnLabel = label, typeInfo = targetTypeInfo)
}

/** Validate broad/raw bounds too: Any disallows NULL, while Any? and * preserve it. */
@InternalKronosApi
fun requireResultValueType(value: Any?, target: KType?, label: String): Any? {
    require(target == null || if (value == null) target.isMarkedNullable else target.accepts(value)) {
        "Result column '$label' is not assignable to $target"
    }
    return value
}

@InternalKronosApi
fun KType.isAnyResultType(): Boolean = classifier == Any::class && arguments.isEmpty()
