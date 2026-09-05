/**
 * Copyright 2022-2025 kronos-orm
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

package com.kotlinorm.beans.serialize

import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueCodecOrigin
import com.kotlinorm.enums.ValueStorage
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.codec.ValueCodecRegistry
import com.kotlinorm.utils.codec.ValueConversionRequest
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Creates a nullable typed property backed by the supplied `String?` KPojo property.
 *
 * The nullable form of the complete reified [T] type, including nested generic
 * arguments, is retained for both directions. A null backing value is returned
 * directly; non-null reads and writes each enter the ValueCodec registry exactly
 * once with `origin = DELEGATE` and `storage = SERIALIZED`.
 *
 * @param toSerialize reference to the persisted `String?` backing property
 * @return a delegate retaining the complete nullable reified [T] for both directions
 * @throws com.kotlinorm.exceptions.MissingSerializedCodec when no registered codec accepts serialized storage
 * @throws com.kotlinorm.exceptions.ValueMappingException when conversion fails or returns an incompatible value
 */
@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T> serialize(toSerialize: KProperty0<String?>): Serialize<T?> {
    return Serialize(toSerialize.name, typeOf<T?>())
}

/**
 * Exposes a typed property backed by a serialized `String?` KPojo field.
 *
 * Both reads and writes use the single ValueCodec registry with the complete
 * declaration [targetKType]. The delegate does not own a serializer or keep
 * a second conversion path.
 *
 * @param toSerialize the backing KPojo field name
 * @param targetKType the complete Kotlin type restored by the delegate
 */
class Serialize<T>(
    private val toSerialize: String,
    private val targetKType: KType
) : ReadWriteProperty<Any, T?> {
    /**
     * Returns null for a null backing field; otherwise decodes its text exactly once.
     * Missing codecs and decode/type failures retain DELEGATE context in their exception.
     *
     * @param thisRef KPojo containing the serialized backing field
     * @param property typed delegated property requested by Kotlin
     * @return the decoded value, or `null` when the backing field is null
     * @throws ClassCastException when [thisRef] is not a KPojo
     * @throws com.kotlinorm.exceptions.ValueMappingException when registry conversion fails
     */
    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Any, property: KProperty<*>): T? {
        val stored = (thisRef as KPojo)[toSerialize] ?: return null
        return ValueCodecRegistry.convert(
            ValueConversionRequest(
                value = stored,
                direction = ValueCodecDirection.DECODE,
                origin = ValueCodecOrigin.DELEGATE,
                targetType = targetKType,
                sourceType = typeOf<String>(),
                storage = ValueStorage.SERIALIZED
            )
        ) as T?
    }

    /**
     * Writes null directly to the backing field, or encodes a non-null value once.
     * The encoded result is validated as String before assignment; codec failures
     * retain DELEGATE context in their exception.
     *
     * @param thisRef KPojo receiving the serialized backing field
     * @param property typed delegated property being assigned
     * @param value logical value to encode, or `null` to clear the backing field
     * @throws ClassCastException when [thisRef] is not a KPojo
     * @throws com.kotlinorm.exceptions.ValueMappingException when registry conversion fails
     */
    override fun setValue(thisRef: Any, property: KProperty<*>, value: T?) {
        if (value == null) {
            (thisRef as KPojo)[toSerialize] = null
            return
        }
        (thisRef as KPojo)[toSerialize] = ValueCodecRegistry.convert(
            ValueConversionRequest(
                value = value,
                direction = ValueCodecDirection.ENCODE,
                origin = ValueCodecOrigin.DELEGATE,
                targetType = targetKType,
                sourceType = targetKType,
                physicalTargetType = typeOf<String>(),
                storage = ValueStorage.SERIALIZED
            )
        ) as String
    }
}
