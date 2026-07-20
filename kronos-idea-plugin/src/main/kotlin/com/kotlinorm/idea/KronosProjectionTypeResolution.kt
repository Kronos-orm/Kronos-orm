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

@file:OptIn(org.jetbrains.kotlin.analysis.api.KaContextParameterApi::class)

package com.kotlinorm.idea

import com.kotlinorm.compiler.fir.KronosIdeProjectionField
import com.kotlinorm.compiler.fir.KronosIdeProjectionModel
import com.kotlinorm.compiler.utils.GeneratedProjectionPackageFqName
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.isMarkedNullable
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaStarTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection
import org.jetbrains.kotlin.types.Variance

internal data class KronosProjectionType(
    val classFqName: String,
    val subjectType: String,
    val carrierPath: List<KronosProjectionCarrierKind> = listOf(KronosProjectionCarrierKind.Direct),
) {
    val className: String = classFqName.substringAfterLast('.')

    val carrierKind: KronosProjectionCarrierKind
        get() = carrierPath.firstOrNull() ?: KronosProjectionCarrierKind.Direct
}

internal sealed interface KronosProjectionTypeTextShape

internal data class KronosProjectionTypeShape(
    val classFqName: String,
    val typeArguments: List<KronosProjectionTypeArgumentShape> = emptyList(),
    val nullable: Boolean = false,
) : KronosProjectionTypeTextShape {
    val className: String = classFqName.substringAfterLast('.')
}

internal data class KronosOpaqueTypeTextShape(
    val text: String,
) : KronosProjectionTypeTextShape

internal sealed interface KronosProjectionTypeArgumentShape {
    data object Star : KronosProjectionTypeArgumentShape

    data class Type(
        val type: KronosProjectionTypeTextShape,
        val variance: KronosProjectionTypeVariance = KronosProjectionTypeVariance.Invariant,
    ) : KronosProjectionTypeArgumentShape
}

internal enum class KronosProjectionTypeVariance(val prefix: String) {
    Invariant(""),
    In("in "),
    Out("out "),
}

internal enum class KronosProjectionCarrierKind {
    Direct,
    SelectableQuery,
    Collection,
    ExecutionStage,
    ResultEnvelope,
}

internal data class KronosProjectionTypeDiscovery(
    val classFqName: String,
    val carrierPath: List<KronosProjectionCarrierKind>,
) {
    val carrierKind: KronosProjectionCarrierKind
        get() = carrierPath.firstOrNull() ?: KronosProjectionCarrierKind.Direct
}

private data class ProjectionCarrier(
    val kind: KronosProjectionCarrierKind,
    val projectionArgumentIndex: Int,
)

private val selectableQueryCarriers = mapOf(
    "com.kotlinorm.orm.select.SelectClause" to 1,
    "com.kotlinorm.orm.join.JoinedSelectQuery" to 1,
    "com.kotlinorm.orm.union.UnionClause" to 0,
    "com.kotlinorm.beans.dsl.KSelectable" to 0,
    "com.kotlinorm.orm.pagination.OffsetPageQuery" to 0,
).mapValues { (_, index) -> ProjectionCarrier(KronosProjectionCarrierKind.SelectableQuery, index) }

private val executionStageCarriers = mapOf(
    "com.kotlinorm.orm.pagination.TotalPageQuery" to 0,
    "com.kotlinorm.orm.pagination.CursorPageQuery" to 0,
).mapValues { (_, index) -> ProjectionCarrier(KronosProjectionCarrierKind.ExecutionStage, index) }

private val resultEnvelopeCarriers = mapOf(
    "com.kotlinorm.orm.pagination.PageResult" to 0,
    "com.kotlinorm.orm.pagination.CursorResult" to 0,
).mapValues { (_, index) -> ProjectionCarrier(KronosProjectionCarrierKind.ResultEnvelope, index) }

private val collectionCarriers = mapOf(
    "kotlin.Array" to 0,
    "kotlin.collections.Iterable" to 0,
    "kotlin.collections.MutableIterable" to 0,
    "kotlin.collections.Collection" to 0,
    "kotlin.collections.MutableCollection" to 0,
    "kotlin.collections.List" to 0,
    "kotlin.collections.MutableList" to 0,
    "kotlin.collections.Set" to 0,
    "kotlin.collections.MutableSet" to 0,
    "kotlin.sequences.Sequence" to 0,
).mapValues { (_, index) -> ProjectionCarrier(KronosProjectionCarrierKind.Collection, index) }

private val projectionCarriers = buildMap {
    putAll(selectableQueryCarriers)
    putAll(collectionCarriers)
    putAll(executionStageCarriers)
    putAll(resultEnvelopeCarriers)
}

internal val KronosProjectionType.isDirectCompletionReceiver: Boolean
    get() = carrierPath == listOf(KronosProjectionCarrierKind.Direct)

internal val KronosProjectionType.canExtractDirectRow: Boolean
    get() = carrierPath == listOf(
        KronosProjectionCarrierKind.SelectableQuery,
        KronosProjectionCarrierKind.Direct,
    ) || carrierPath == listOf(
        KronosProjectionCarrierKind.Collection,
        KronosProjectionCarrierKind.Direct,
    )

internal fun KronosProjectionType.asDirectCompletionReceiver(nullable: Boolean): KronosProjectionType =
    KronosProjectionType(
        classFqName = classFqName,
        subjectType = classFqName + if (nullable) "?" else "",
        carrierPath = listOf(KronosProjectionCarrierKind.Direct),
    )

internal fun KronosProjectionTypeShape.findProjectionType(): KronosProjectionTypeDiscovery? {
    if (isGeneratedProjection()) {
        return KronosProjectionTypeDiscovery(classFqName, listOf(KronosProjectionCarrierKind.Direct))
    }
    if (isRawJoinSource()) return null

    val carrier = projectionCarriers[classFqName] ?: return null
    val nested = typeArguments
        .getOrNull(carrier.projectionArgumentIndex)
        ?.typeShape()
        ?.findProjectionType()
        ?: return null
    return nested.copy(carrierPath = listOf(carrier.kind) + nested.carrierPath)
}

internal fun KronosProjectionTypeShape.findProjectionClassFqName(): String? =
    findProjectionType()?.classFqName

internal fun KronosIdeProjectionModel.fieldsForProjectionClass(className: String): List<KronosIdeProjectionField>? =
    when (className) {
        name -> fields
        contextName -> contextFields
        else -> null
    }

context(_: KaSession)
internal fun KaType.toKronosProjectionType(): KronosProjectionType? {
    val typeShape = toKronosProjectionTypeTextShape()
    val projectionType = (typeShape as? KronosProjectionTypeShape)?.findProjectionType() ?: return null
    return KronosProjectionType(
        projectionType.classFqName,
        typeShape.renderKronosProjectionTypeText(),
        projectionType.carrierPath,
    )
}

context(_: KaSession)
internal fun KaType.renderKronosProjectionTypeText(): String =
    toKronosProjectionTypeTextShape().renderKronosProjectionTypeText()

internal fun KronosProjectionTypeTextShape.renderKronosProjectionTypeText(): String = when (this) {
    is KronosOpaqueTypeTextShape -> text
    is KronosProjectionTypeShape -> {
        val arguments = typeArguments
            .map(KronosProjectionTypeArgumentShape::renderKronosProjectionTypeText)
            .takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = "<", postfix = ">")
            .orEmpty()
        classFqName + arguments + if (nullable) "?" else ""
    }
}

private fun KronosProjectionTypeArgumentShape.renderKronosProjectionTypeText(): String = when (this) {
    KronosProjectionTypeArgumentShape.Star -> "*"
    is KronosProjectionTypeArgumentShape.Type -> variance.prefix + type.renderKronosProjectionTypeText()
}

context(_: KaSession)
private fun KaType.toKronosProjectionTypeTextShape(): KronosProjectionTypeTextShape {
    val classType = this as? KaClassType ?: return KronosOpaqueTypeTextShape(this.toString())
    return KronosProjectionTypeShape(
        classFqName = classType.classId.asFqNameString(),
        typeArguments = classType.typeArguments.map { it.toKronosProjectionTypeArgumentShape() },
        nullable = isMarkedNullable,
    )
}

context(_: KaSession)
private fun KaTypeProjection.toKronosProjectionTypeArgumentShape(): KronosProjectionTypeArgumentShape = when (this) {
    is KaStarTypeProjection -> KronosProjectionTypeArgumentShape.Star
    is KaTypeArgumentWithVariance -> KronosProjectionTypeArgumentShape.Type(
        type = type.toKronosProjectionTypeTextShape(),
        variance = variance.toKronosProjectionTypeVariance(),
    )
}

private fun Variance.toKronosProjectionTypeVariance(): KronosProjectionTypeVariance = when (this) {
    Variance.INVARIANT -> KronosProjectionTypeVariance.Invariant
    Variance.IN_VARIANCE -> KronosProjectionTypeVariance.In
    Variance.OUT_VARIANCE -> KronosProjectionTypeVariance.Out
}

private fun KronosProjectionTypeArgumentShape.typeShape(): KronosProjectionTypeShape? =
    (this as? KronosProjectionTypeArgumentShape.Type)?.type as? KronosProjectionTypeShape

private fun KronosProjectionTypeShape.isGeneratedProjection(): Boolean =
    classFqName.substringBeforeLast('.', missingDelimiterValue = "") == GeneratedProjectionPackageFqName.asString() &&
        (className.startsWith("KronosSelectResult_") || className.startsWith("KronosSelectContext_"))

private fun KronosProjectionTypeShape.isRawJoinSource(): Boolean {
    if (classFqName.substringBeforeLast('.', missingDelimiterValue = "") != "com.kotlinorm.orm.join") return false
    return className == "JoinSource" ||
        className.removePrefix("JoinSource").toIntOrNull()?.let { it in 2..16 } == true
}
