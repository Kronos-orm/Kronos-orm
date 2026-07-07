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
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Shares projection declarations from the FIR compiler-plugin classloader to
 * the IDEA plugin classloader. Both live in the same IDE JVM but do not share
 * static objects, so the payload is intentionally plain text.
 */
object KronosProjectionIdeBridge {
    private const val PropertyName = "com.kotlinorm.kronos.ide.projections"
    private const val IdeActivePropertyName = "com.kotlinorm.kronos.ide.active"
    private const val ResolveExtensionFallbackPropertyName = "com.kotlinorm.kronos.ide.resolveExtensionFallback"
    private const val LastPublishCountPropertyName = "com.kotlinorm.kronos.ide.projections.count"
    private const val LastPublishPayloadSizePropertyName = "com.kotlinorm.kronos.ide.projections.payloadSize"
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun publish(models: Collection<Pair<String, KronosProjectionModel>>) {
        val distinctModels = models.distinctBy { it.second.classId }
        val payload = distinctModels.joinToString("\n") { (moduleName, model) ->
            listOf(
                moduleName,
                model.name.asString(),
                model.fields.encodeFields(),
                model.contextName.asString(),
                model.contextFields.encodeFields(),
            ).joinToString("|") { it.encode() }
        }
        System.setProperty(PropertyName, payload)
        System.setProperty(LastPublishCountPropertyName, distinctModels.size.toString())
        System.setProperty(LastPublishPayloadSizePropertyName, payload.length.toString())
    }

    fun markIdeActive() {
        System.setProperty(IdeActivePropertyName, "true")
    }

    fun isIdeActive(): Boolean =
        System.getProperty(IdeActivePropertyName) == "true" ||
            System.getProperty("idea.paths.selector") != null ||
            System.getProperty("idea.platform.prefix") != null

    fun isResolveExtensionFallbackEnabled(): Boolean =
        System.getProperty(ResolveExtensionFallbackPropertyName) == "true"

    fun read(): List<KronosIdeProjectionModel> {
        val payload = System.getProperty(PropertyName) ?: return emptyList()
        return payload
            .lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size != 5) return@mapNotNull null
                KronosIdeProjectionModel(
                    moduleName = parts[0].decode(),
                    name = parts[1].decode(),
                    fields = parts[2].decode().decodeFields(),
                    contextName = parts[3].decode(),
                    contextFields = parts[4].decode().decodeFields(),
                )
            }
            .distinctBy { "${it.moduleName}:${it.name}" }
            .toList()
    }

    fun lastPublishSummary(): String =
        "count=${System.getProperty(LastPublishCountPropertyName, "0")}, " +
            "payloadSize=${System.getProperty(LastPublishPayloadSizePropertyName, "0")}"

    private fun List<KronosProjectionField>.encodeFields(): String =
        joinToString(",") { "${it.name.asString().encode()}:${it.type.renderIdeType().encode()}" }

    private fun String.decodeFields(): List<KronosIdeProjectionField> =
        splitToSequence(",")
            .filter { it.isNotBlank() }
            .mapNotNull { item ->
                val separator = item.indexOf(':')
                if (separator <= 0) return@mapNotNull null
                KronosIdeProjectionField(
                    name = item.substring(0, separator).decode(),
                    type = item.substring(separator + 1).decode(),
                )
            }
            .toList()

    private fun String.encode(): String =
        encoder.encodeToString(toByteArray(StandardCharsets.UTF_8))

    private fun String.decode(): String =
        String(decoder.decode(this), StandardCharsets.UTF_8)
}

data class KronosIdeProjectionModel(
    val moduleName: String,
    val name: String,
    val fields: List<KronosIdeProjectionField>,
    val contextName: String,
    val contextFields: List<KronosIdeProjectionField>,
)

data class KronosIdeProjectionField(
    val name: String,
    val type: String,
)

private fun ConeKotlinType.renderIdeType(): String {
    val classLike = this as? ConeClassLikeType ?: return "kotlin.Any?"
    val base = classLike.lookupTag.classId.asFqNameString()
    val args = mutableListOf<String>()
    for (argument in classLike.typeArguments) {
        val projection = argument as? ConeKotlinTypeProjection ?: continue
        args += projection.type.renderIdeType().removeSuffix("?")
    }
    val renderedArgs = if (args.isEmpty()) "" else args.joinToString(prefix = "<", postfix = ">")
    return base + renderedArgs + if (classLike.isMarkedNullable) "?" else ""
}
