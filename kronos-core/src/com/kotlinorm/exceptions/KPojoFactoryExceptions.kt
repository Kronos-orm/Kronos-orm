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

package com.kotlinorm.exceptions

import kotlin.reflect.KType

/**
 * Base failure for generated or user-supplied KPojo factory resolution.
 *
 * @param message complete factory failure description
 * @param cause wrapped non-control-flow construction failure, when present
 */
sealed class KPojoFactoryException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

/**
 * Indicates that no generated or user factory exists for an exact KPojo type.
 *
 * @property targetType complete requested KPojo type
 */
class MissingKPojoFactory(val targetType: KType) : KPojoFactoryException(
    "No generated or registered KPojo factory matched $targetType"
)

/**
 * Wraps an ordinary exception thrown while invoking a KPojo factory.
 *
 * Errors and cancellation are control-flow signals and are never represented
 * by this exception.
 *
 * @property targetType complete requested KPojo type
 * @param cause original business construction failure
 */
class KPojoConstructionException(
    val targetType: KType,
    cause: Throwable
) : KPojoFactoryException("KPojo factory failed while constructing $targetType", cause)

/**
 * Indicates that a factory returned a KPojo whose complete type is incompatible.
 *
 * @property targetType complete requested KPojo type
 * @property actualType complete type reported by the returned KPojo
 * @param reason validation detail included in the failure message
 */
class InvalidKPojoFactoryResult(
    val targetType: KType,
    val actualType: KType,
    reason: String
) : KPojoFactoryException("KPojo factory returned an invalid result for $targetType: $reason (actual type: $actualType)")

/**
 * Indicates that a requested factory type is outside the supported KPojo set.
 *
 * @property targetType complete rejected type
 * @param reason unsupported classifier or generic-shape detail
 */
class UnsupportedType(
    val targetType: KType,
    reason: String
) : IllegalArgumentException("Unsupported KPojo type $targetType: $reason")

/**
 * Indicates conflicting generated factories for one exact KPojo type.
 *
 * @property targetType complete conflicting KPojo type
 */
class ConflictingGeneratedKPojoFactory(val targetType: KType) : KPojoFactoryException(
    "Conflicting generated KPojo factories were registered for $targetType"
)

/**
 * Indicates that duplicate generated provider ids contributed different metadata.
 *
 * @property providerId stable duplicated provider identity
 */
class ConflictingGeneratedProvider(val providerId: String) : KPojoFactoryException(
    "Generated type provider '$providerId' contributed conflicting metadata"
)
