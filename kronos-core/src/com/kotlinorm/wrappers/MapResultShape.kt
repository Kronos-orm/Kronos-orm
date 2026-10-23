@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm.wrappers

import com.kotlinorm.annotations.InternalKronosApi
import com.kotlinorm.beans.task.ResultColumnMetadata
import kotlin.reflect.KType

/** One declaration policy for JDBC and platform execution Map results. */
@InternalKronosApi
class MapResultShape internal constructor(val valueType: KType?) {
    fun column(label: String, declared: ResultColumnMetadata?): ResultColumnMetadata? =
        resultColumnForType(valueType, declared, label)

    fun value(value: Any?, label: String): Any? = requireResultValueType(value, valueType, label)
}
