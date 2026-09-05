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
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Properties
import java.util.UUID

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
    private const val ModuleOwnerPropertyPrefix = "com.kotlinorm.kronos.ide.projections.owner."
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun publishModule(
        moduleName: String,
        generationToken: String,
        models: Collection<KronosProjectionModel>
    ) {
        publishModuleSnapshot(
            moduleName,
            generationToken,
            models.map { model ->
                KronosIdeProjectionModel(
                    moduleName = moduleName,
                    name = model.name.asString(),
                    fields = model.fields.toIdeFields(),
                    contextName = model.contextName.asString(),
                    contextFields = model.contextFields.toIdeFields(),
                )
            }
        )
    }

    /** System properties keep ownership visible when IDEA replaces the compiler-plugin classloader. */
    internal fun beginModuleSession(moduleName: String): String {
        val generationToken = UUID.randomUUID().toString()
        val properties = System.getProperties()
        synchronized(properties) {
            properties.setProperty(moduleOwnerPropertyName(moduleName), generationToken)
            replaceModuleSnapshotLocked(properties, moduleName, emptyList())
        }
        return generationToken
    }

    internal fun publishModuleSnapshot(
        moduleName: String,
        generationToken: String,
        models: Collection<KronosIdeProjectionModel>
    ) {
        val properties = System.getProperties()
        synchronized(properties) {
            if (properties.getProperty(moduleOwnerPropertyName(moduleName)) != generationToken) return
            replaceModuleSnapshotLocked(properties, moduleName, models)
        }
    }

    private fun replaceModuleSnapshotLocked(
        properties: Properties,
        moduleName: String,
        models: Collection<KronosIdeProjectionModel>
    ) {
        val merged = linkedMapOf<String, KronosIdeProjectionModel>()
        readPayload(properties.getProperty(PropertyName))
            .filterNot { it.moduleName == moduleName }
            .forEach { model -> merged[model.identityKey()] = model }
        models.forEach { model ->
            val normalized = model.copy(moduleName = moduleName)
            merged[normalized.identityKey()] = normalized
        }
        writePayload(properties, merged.values)
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
        return readPayload(System.getProperty(PropertyName))
    }

    fun lastPublishSummary(): String =
        "count=${System.getProperty(LastPublishCountPropertyName, "0")}, " +
            "payloadSize=${System.getProperty(LastPublishPayloadSizePropertyName, "0")}"

    private fun List<KronosProjectionField>.toIdeFields(): List<KronosIdeProjectionField> =
        map { field -> KronosIdeProjectionField(field.name.asString(), field.type.renderIdeType()) }

    private fun readPayload(payload: String?): List<KronosIdeProjectionModel> {
        if (payload.isNullOrBlank()) return emptyList()
        val modelsByIdentity = linkedMapOf<String, KronosIdeProjectionModel>()
        payload.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull(::decodeModel)
            .forEach { model -> modelsByIdentity[model.identityKey()] = model }
        return modelsByIdentity.values.sortedWith(IdeModelComparator)
    }

    private fun decodeModel(line: String): KronosIdeProjectionModel? = runCatching {
        val parts = line.split("|")
        if (parts.size != 5) return null
        KronosIdeProjectionModel(
            moduleName = parts[0].decode(),
            name = parts[1].decode(),
            fields = parts[2].decode().decodeFields(),
            contextName = parts[3].decode(),
            contextFields = parts[4].decode().decodeFields(),
        )
    }.getOrNull()

    private fun writePayload(properties: Properties, models: Collection<KronosIdeProjectionModel>) {
        val normalized = models
            .associateBy { it.identityKey() }
            .values
            .sortedWith(IdeModelComparator)
        val payload = normalized.joinToString("\n", transform = ::encodeModel)
        if (payload.isEmpty()) {
            properties.remove(PropertyName)
        } else {
            properties.setProperty(PropertyName, payload)
        }
        properties.setProperty(LastPublishCountPropertyName, normalized.size.toString())
        properties.setProperty(LastPublishPayloadSizePropertyName, payload.length.toString())
    }

    private fun encodeModel(model: KronosIdeProjectionModel): String =
        listOf(
            model.moduleName,
            model.name,
            model.fields.encodeFields(),
            model.contextName,
            model.contextFields.encodeFields(),
        ).joinToString("|") { it.encode() }

    private fun List<KronosIdeProjectionField>.encodeFields(): String =
        joinToString(",") { "${it.name.encode()}:${it.type.encode()}" }

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

    private fun KronosIdeProjectionModel.identityKey(): String = "$moduleName:$name"

    private fun moduleOwnerPropertyName(moduleName: String): String =
        ModuleOwnerPropertyPrefix + moduleName.encode()

    private val IdeModelComparator = compareBy<KronosIdeProjectionModel>(
        KronosIdeProjectionModel::moduleName,
        KronosIdeProjectionModel::name,
        KronosIdeProjectionModel::contextName,
    )
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

internal fun ConeKotlinType.renderIdeType(): String {
    val classLike = this as? ConeClassLikeType ?: return "kotlin.Any?"
    val base = classLike.lookupTag.classId.asFqNameString()
    val args = mutableListOf<String>()
    for (argument in classLike.typeArguments) {
        val projection = argument as? ConeKotlinTypeProjection
        args += renderIdeTypeArgument(projection?.type?.renderIdeType(), projection?.kind)
    }
    return renderIdeClassLikeType(base, args, classLike.isMarkedNullable)
}

internal fun renderIdeTypeArgument(type: String?, kind: ProjectionKind?): String {
    if (type == null || kind == null) return "*"
    return when (kind) {
        ProjectionKind.IN -> "in $type"
        ProjectionKind.OUT -> "out $type"
        else -> type
    }
}

internal fun renderIdeClassLikeType(
    classFqName: String,
    typeArguments: List<String> = emptyList(),
    nullable: Boolean = false,
): String {
    val renderedArguments = typeArguments
        .takeIf { it.isNotEmpty() }
        ?.joinToString(prefix = "<", postfix = ">")
        .orEmpty()
    return classFqName + renderedArguments + if (nullable) "?" else ""
}
