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

/**
 * Creates one ordinary [ValueCodec] for every field using serialized storage.
 *
 * The supplied functions receive the field's complete [KType], so one
 * registration can handle objects, generic collections, nested collections and
 * collections of enums. Encoding always produces one database `String` value;
 * database and delegate decoding require a stored `String`. Safe Map mapping
 * keeps an already-typed value through the registry identity rule.
 *
 * This is only a convenience factory. Register its result through
 * `Kronos.registerValueCodec`; there is no separate serializer registry.
 * SQL null is handled before these functions run, later registrations have
 * higher priority, and thrown errors or invalid outputs are wrapped/validated
 * by the same registry used by all other conversions. Each request invokes at
 * most one of [encode] or [decode].
 *
 * @param encode converts a non-null logical value and complete type to stored text
 * @param decode restores stored text to the complete logical type and may return null only for nullable targets
 * @return an unregistered codec restricted to [ValueStorage.SERIALIZED] requests
 */
fun serializedValueCodec(
    encode: (value: Any, type: KType) -> String,
    decode: (text: String, type: KType) -> Any?
): ValueCodec = valueCodec(
    supports = { value, context ->
        if (context.storage != ValueStorage.SERIALIZED) {
            false
        } else {
            when (context.direction) {
                ValueCodecDirection.ENCODE -> true
                ValueCodecDirection.DECODE -> value is String && context.origin != ValueCodecOrigin.PARAMETER
            }
        }
    },
    convert = { value, context ->
        when (context.direction) {
            ValueCodecDirection.ENCODE -> encode(value, context.targetType)
            ValueCodecDirection.DECODE -> decode(value as String, context.targetType)
        }
    }
)
