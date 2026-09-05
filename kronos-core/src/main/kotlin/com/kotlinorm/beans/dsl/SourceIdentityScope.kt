/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.beans.dsl

import com.kotlinorm.interfaces.KPojo
import java.util.IdentityHashMap

/**
 * Runtime source-identity scope used by compiler-lowered DSL field access.
 *
 * The public methods are runtime plumbing for generated code; applications should
 * not call them directly.
 */
object SourceIdentityScope {
    private val frames = ThreadLocal.withInitial { mutableListOf<Frame>() }

    fun frame(sources: List<KPojo>): Frame =
        Frame(sources.mapIndexed { index, source -> Source(source, source.__tableName, index) })

    fun <T> withFrame(frame: Frame, block: () -> T): T {
        val stack = frames.get()
        stack += frame
        return try {
            block()
        } finally {
            stack.removeAt(stack.lastIndex)
            if (stack.isEmpty()) {
                frames.remove()
            }
        }
    }

    fun resolveTableName(receiver: Any?, declaredTableName: String): String {
        val source = receiver as? KPojo ?: return declaredTableName
        val stack = frames.get()
        if (stack.isEmpty()) return declaredTableName

        val frameIndex = stack.indexOfLast { it.contains(source) }
        if (frameIndex < 0) return declaredTableName

        val frame = stack[frameIndex]
        val currentFrame = stack.last()
        val matched = frame.sourceOf(source) ?: return declaredTableName
        val physicalTableName = matched.tableName.ifBlank { declaredTableName }
        val crossesIntoSameTableNestedScope = frameIndex != stack.lastIndex &&
            currentFrame.hasPhysicalTable(physicalTableName)
        val currentSourceShadowsCorrelatedOuterAlias = frameIndex == stack.lastIndex &&
            stack.dropLast(1).any { parent ->
                parent.hasAliasForPhysicalTable(physicalTableName) &&
                    !parent.hasDuplicatePhysicalTable(physicalTableName)
            }
        val sharesTableInCurrentFrame = frame.hasDuplicatePhysicalTable(physicalTableName)

        return if (crossesIntoSameTableNestedScope || sharesTableInCurrentFrame || currentSourceShadowsCorrelatedOuterAlias) {
            val reservedAliases = if (currentSourceShadowsCorrelatedOuterAlias) {
                stack.dropLast(1).flatMap { it.allAliases() }.toSet()
            } else {
                emptySet()
            }
            frame.aliasFor(source, reservedAliases)
        } else {
            frame.existingAliasFor(source) ?: physicalTableName
        }
    }

    class Frame internal constructor(
        private val sources: List<Source>
    ) {
        private val aliases = IdentityHashMap<KPojo, String>()
        private val sourceTableCounts = sources
            .groupingBy { it.tableName }
            .eachCount()

        fun contains(source: KPojo): Boolean =
            sources.any { it.pojo === source }

        fun sourceOf(source: KPojo): Source? =
            sources.firstOrNull { it.pojo === source }

        fun hasPhysicalTable(tableName: String): Boolean =
            sources.any { it.tableName == tableName }

        fun hasDuplicatePhysicalTable(tableName: String): Boolean =
            sourceTableCounts.getOrDefault(tableName, 0) > 1

        fun aliasFor(source: KPojo, reservedAliases: Set<String> = emptySet()): String =
            aliases.getOrPut(source) {
                val sourceInfo = sourceOf(source)
                val base = sourceInfo?.tableName?.takeIf { it.isNotBlank() } ?: source.__tableName
                var index = sourceInfo?.index ?: aliases.size
                var alias: String
                do {
                    alias = "${base}__k${index + 1}"
                    index++
                } while (alias in reservedAliases)
                alias
            }

        fun aliasForSource(source: KPojo): String? {
            val sourceInfo = sourceOf(source) ?: return null
            return if (hasDuplicatePhysicalTable(sourceInfo.tableName) || aliases.containsKey(source)) {
                aliasFor(source)
            } else {
                null
            }
        }

        fun existingAliasFor(source: KPojo): String? =
            aliases[source]

        fun hasAliasForPhysicalTable(tableName: String): Boolean =
            sources.any { source -> source.tableName == tableName && aliases.containsKey(source.pojo) }

        fun allAliases(): Collection<String> =
            aliases.values

        fun aliasesByTableName(): Map<String, String> =
            sources.mapNotNull { source ->
                if (hasDuplicatePhysicalTable(source.tableName)) {
                    null
                } else {
                    aliases[source.pojo]?.let { alias -> source.tableName to alias }
                }
            }.toMap()
    }

    class Source internal constructor(
        val pojo: KPojo,
        val tableName: String,
        val index: Int
    )
}
