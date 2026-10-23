package com.kotlinorm.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StaticTypeRelationContractTest {
    private val any = StaticTypeRef("example.Any", false)
    private val text = StaticTypeRef("example.Text", false)
    private val base = listOf(StaticTypeInfo(any), StaticTypeInfo(text, directSupertypes = listOf(any)))

    @Test
    fun useSiteProjectionsRespectDeclarationVariance() {
        for (declaration in listOf(StaticTypeVariance.INVARIANT, StaticTypeVariance.IN, StaticTypeVariance.OUT)) {
            val narrow = box(text, declaration)
            val readable = box(any, declaration, StaticTypeVariance.OUT)
            assertEquals(declaration != StaticTypeVariance.IN, related(narrow, readable))
            val broad = box(any, declaration)
            val writable = box(text, declaration, StaticTypeVariance.IN)
            assertEquals(declaration != StaticTypeVariance.OUT, related(broad, writable))
        }
    }

    @Test
    fun invariantArgumentsRequireBothDirectionsAndPreserveNestedNullability() {
        assertFalse(related(box(text, StaticTypeVariance.INVARIANT), box(any, StaticTypeVariance.INVARIANT)))
        assertFalse(related(box(text, StaticTypeVariance.INVARIANT), box(text.copy(nullable = true), StaticTypeVariance.INVARIANT)))
        assertFalse(related(box(text.copy(nullable = true), StaticTypeVariance.INVARIANT), box(text, StaticTypeVariance.INVARIANT)))
        assertTrue(related(box(text, StaticTypeVariance.INVARIANT), box(text, StaticTypeVariance.INVARIANT)))
    }

    @Test
    fun sourceStarAndProjectedSourcesCannotBecomeConcreteInvariantArguments() {
        val target = box(any, StaticTypeVariance.INVARIANT)
        val star = StaticTypeInfo(StaticTypeRef("example.Box", false, listOf(StaticTypeArgument(StaticTypeVariance.STAR))),
            declarationVariances = listOf(StaticTypeVariance.INVARIANT))
        assertFalse(related(star, target))
        assertFalse(related(box(text, StaticTypeVariance.INVARIANT, StaticTypeVariance.OUT), target))
        assertFalse(related(box(text, StaticTypeVariance.INVARIANT, StaticTypeVariance.IN), target))
        val targetStar = star
        assertTrue(related(box(text, StaticTypeVariance.INVARIANT, StaticTypeVariance.OUT), targetStar))
    }

    @Test
    fun incompleteGenericMetadataAndArgumentArityFailClosed() {
        val missingVariance = box(text, StaticTypeVariance.INVARIANT).copy(declarationVariances = emptyList())
        assertFalse(related(missingVariance, box(any, StaticTypeVariance.INVARIANT)))
        val source = box(text, StaticTypeVariance.INVARIANT)
        assertFalse(related(source, StaticTypeInfo(StaticTypeRef("example.Box", false))))
        assertFalse(source.isStructurallyAssignableTo(box(any, StaticTypeVariance.INVARIANT)) { null })
    }

    @Test
    fun cyclicAndNullableSupertypeGraphsTerminateWithoutLosingNullability() {
        val a = StaticTypeInfo(StaticTypeRef("example.A", false), directSupertypes = listOf(StaticTypeRef("example.B", false)))
        val b = StaticTypeInfo(StaticTypeRef("example.B", false), directSupertypes = listOf(a.type))
        val target = StaticTypeInfo(StaticTypeRef("example.Unrelated", false))
        assertFalse(related(a, target, listOf(a, b, target)))
        val nullableText = base.last().copy(type = text.copy(nullable = true))
        assertTrue(related(nullableText, StaticTypeInfo(any.copy(nullable = true)), base))
        assertFalse(related(nullableText, StaticTypeInfo(any), base))
    }

    private fun box(argument: StaticTypeRef, declaration: StaticTypeVariance, useSite: StaticTypeVariance = StaticTypeVariance.INVARIANT) =
        StaticTypeInfo(StaticTypeRef("example.Box", false, listOf(StaticTypeArgument(useSite, argument))),
            declarationVariances = listOf(declaration))

    private fun related(source: StaticTypeInfo, target: StaticTypeInfo, supplied: List<StaticTypeInfo> = base): Boolean {
        val metadata = (supplied + target + source).associateBy { it.type.withoutTopLevelNullability() }
        return source.isStructurallyAssignableTo(target) { metadata[it.withoutTopLevelNullability()] }
    }
}
