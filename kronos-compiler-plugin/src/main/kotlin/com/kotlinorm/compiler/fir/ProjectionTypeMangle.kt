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

package com.kotlinorm.compiler.fir

import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjection
import org.jetbrains.kotlin.fir.types.ProjectionKind
import java.security.MessageDigest

private const val ProjectionHashByteCount = 8

/** Generates a stable synthetic class suffix from the source row type and selected field signatures. */
internal fun mangleProjectionName(sourceType: ConeKotlinType, fields: List<KronosProjectionField>): String {
    val sourceClass = sourceType.renderForMangle()
    val raw = buildString {
        append(sourceClass)
        fields.forEach { field ->
            append('|')
            append(field.requestedName.asString())
            append("->")
            append(field.name.asString())
            append(':')
            append(field.sourceName.asString())
            append(':')
            append(if (field.isSourceAlias) "source" else "expression")
            append(':')
            append(field.signature)
            append(':')
            append(field.type.renderForMangle())
        }
    }
    return stableHash(raw)
}

private fun stableHash(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
    return digest.take(ProjectionHashByteCount).joinToString("") { byte -> "%02x".format(byte) }
}

private fun ConeKotlinType.renderForMangle(): String {
    if (this !is ConeClassLikeType) return toString()
    val arguments = typeArguments.joinToString(",", prefix = "<", postfix = ">") { argument ->
        val projection = argument as? ConeKotlinTypeProjection
        val type = projection?.type?.renderForMangle() ?: "*"
        when (projection?.kind) {
            ProjectionKind.IN -> "in $type"
            ProjectionKind.OUT -> "out $type"
            else -> type
        }
    }.takeUnless { typeArguments.isEmpty() }.orEmpty()
    return buildString {
        append(lookupTag.classId.asFqNameString())
        append(arguments)
        if (isMarkedNullable) append('?')
    }
}
