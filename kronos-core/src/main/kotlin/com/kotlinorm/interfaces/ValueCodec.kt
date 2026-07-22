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

package com.kotlinorm.interfaces

import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueCodecOrigin
import com.kotlinorm.enums.ValueStorage
import kotlin.reflect.KType

import com.kotlinorm.beans.dsl.Field

/**
 * Public information available while selecting and running a [ValueCodec].
 *
 * @property direction whether the logical Kotlin value is being encoded for storage or decoded from input
 * @property origin the conversion boundary; codecs may use it for fine-grained matching
 * @property sourceType the declared source type when known, otherwise a runtime star-projected fallback
 * @property targetType the complete logical Kotlin target type, including nullability and type arguments
 * @property field field metadata when the conversion belongs to a mapped property
 * @property storage the field storage policy; serialized codecs must require [ValueStorage.SERIALIZED]
 */
data class ValueCodecContext(
    val direction: ValueCodecDirection,
    val origin: ValueCodecOrigin,
    val sourceType: KType?,
    val targetType: KType,
    val field: Field? = null,
    val storage: ValueStorage = ValueStorage.NONE
)

/**
 * Bidirectional value conversion extension used by every safe mapping boundary.
 *
 * The registry handles SQL `NULL` before invoking a codec, checks codecs in
 * newest-registration-first order, wraps failures as value-mapping exceptions,
 * and validates the returned value. A codec should therefore implement a pure
 * match in [supports] and perform only one conversion in [convert].
 */
interface ValueCodec {
    /**
     * Returns whether this codec accepts the non-null [value] for [context].
     * Implementations should include direction, storage, origin and target type
     * conditions needed to avoid intercepting unrelated requests. This method
     * must not mutate registration state or perform the conversion itself.
     *
     * @param value non-null source value; SQL null is handled by the registry
     * @param context complete public matching context for this request
     * @return `true` only when [convert] can produce a valid result for the request
     */
    fun supports(value: Any, context: ValueCodecContext): Boolean

    /**
     * Converts a value previously accepted by [supports].
     *
     * Returning `null` is allowed only when [ValueCodecContext.targetType] is nullable.
     * Thrown exceptions are wrapped with field, direction, origin and batch context.
     * The implementation must perform one semantic conversion and must not call
     * back into the registry.
     *
     * @param value non-null source previously accepted by [supports]
     * @param context the same context supplied to [supports]
     * @return a value assignable to the logical target on decode, or a bindable
     * scalar on encode
     */
    fun convert(value: Any, context: ValueCodecContext): Any?
}

/**
 * Handle for one user codec registration.
 *
 * Closing is idempotent, removes only this registration from future snapshots,
 * and leaves conversions already using an earlier snapshot unchanged.
 */
interface ValueCodecRegistration : AutoCloseable {
    /**
     * Removes this codec from future registry snapshots; repeated calls are no-ops.
     */
    override fun close()
}

/**
 * Creates a [ValueCodec] from matching and conversion functions.
 *
 * This helper does not register the codec or alter priority. Pass the returned
 * codec to `Kronos.registerValueCodec`; later registrations are checked first.
 *
 * @param supports pure predicate used for non-null values
 * @param convert conversion invoked only after [supports] returns `true`
 * @return an unregistered codec using the supplied functions
 */
fun valueCodec(
    supports: (value: Any, context: ValueCodecContext) -> Boolean,
    convert: (value: Any, context: ValueCodecContext) -> Any?
): ValueCodec {
    val predicate = supports
    val converter = convert
    return object : ValueCodec {
        override fun supports(value: Any, context: ValueCodecContext): Boolean = predicate(value, context)

        override fun convert(value: Any, context: ValueCodecContext): Any? = converter(value, context)
    }
}
