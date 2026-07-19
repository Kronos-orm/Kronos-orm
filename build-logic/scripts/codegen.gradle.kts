/**
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

tasks.register("generateJoinClause") {
    group = "codegen"
    description = "generate join source APIs with maxJoinNum"

    doLast {
        val root = project.layout.projectDirectory.asFile.parent
        val joinDir = File("$root/kronos-core/src/main/kotlin/com/kotlinorm/orm/join")
        val maxJoinNum = 16
        val joinRange = 2..maxJoinNum

        fun typeNames(range: IntRange): String = range.joinToString(", ") { "T$it" }
        fun constrainedTypes(range: IntRange): String = range.joinToString(", ") { "reified T$it : KPojo" }
        fun sourceType(arity: Int, start: Int = 1): String =
            "JoinSource$arity<${typeNames(start until start + arity)}>"
        fun blockType(arity: Int): String =
            "${sourceType(arity)}.(${typeNames(1..arity)}) -> R"
        fun stateRows(arity: Int): String =
            (1..arity).joinToString(", ") { index -> "state.sourceAt<T$index>(${index - 1})" }

        fun directTableJoin(arity: Int): String {
            val range = 1..arity
            val tables = (2..arity).joinToString(",\n") { "    table$it: T$it" }
            val tableNames = (2..arity).joinToString(", ") { "table$it" }
            val blockArgs = range.joinToString(", ") { if (it == 1) "this" else "table$it" }
            return """
                |inline fun <${constrainedTypes(range)}, R : JoinResult> T1.join(
                |$tables,
                |    block: ${blockType(arity)}
                |): R {
                |    val source = ${sourceType(arity)}(tableJoinState(this, $tableNames))
                |    return source.block($blockArgs)
                |}
            """.trimMargin()
        }

        fun tableRawJoin(rawArity: Int, selectableLeft: Boolean): String {
            val arity = rawArity + 1
            val range = 1..arity
            val receiver = if (selectableLeft) "KSelectable<T1>" else "T1"
            val helper = if (selectableLeft) {
                "selectableRawJoinState(this, source.joinState)"
            } else {
                "prependJoinState(this, source.joinState)"
            }
            val args = if (selectableLeft) stateRows(arity) else
                "this, " + (2..arity).joinToString(", ") { "state.sourceAt<T$it>(${it - 1})" }
            return """
                |inline fun <${constrainedTypes(range)}, R : JoinResult> $receiver.join(
                |    source: ${sourceType(rawArity, 2)},
                |    block: ${blockType(arity)}
                |): R {
                |    val state = $helper
                |    val joined = ${sourceType(arity)}(state)
                |    return joined.block($args)
                |}
            """.trimMargin()
        }

        fun rawSingleJoin(leftArity: Int, selectableRight: Boolean): String {
            val arity = leftArity + 1
            val range = 1..arity
            val argument = if (selectableRight) "query: KSelectable<T$arity>" else "table$arity: T$arity"
            val helper = if (selectableRight) {
                "rawSelectableJoinState(joinState, query)"
            } else {
                "appendJoinState(joinState, table$arity)"
            }
            val args = if (selectableRight) stateRows(arity) else
                (1..arity).joinToString(", ") { index ->
                    if (index == arity) "table$arity" else "state.sourceAt<T$index>(${index - 1})"
                }
            return """
                |inline fun <${constrainedTypes(range)}, R : JoinResult> ${sourceType(leftArity)}.join(
                |    $argument,
                |    block: ${blockType(arity)}
                |): R {
                |    val state = $helper
                |    val joined = ${sourceType(arity)}(state)
                |    return joined.block($args)
                |}
            """.trimMargin()
        }

        fun rawRawJoin(leftArity: Int, rightArity: Int): String {
            val arity = leftArity + rightArity
            val range = 1..arity
            return """
                |inline fun <${constrainedTypes(range)}, R : JoinResult> ${sourceType(leftArity)}.join(
                |    source: ${sourceType(rightArity, leftArity + 1)},
                |    block: ${blockType(arity)}
                |): R {
                |    val state = rawRawJoinState(joinState, source.joinState)
                |    val joined = ${sourceType(arity)}(state)
                |    return joined.block(${stateRows(arity)})
                |}
            """.trimMargin()
        }

        val pairOverloads = """
            |inline fun <reified T1 : KPojo, reified T2 : KPojo, R : JoinResult> T1.join(
            |    query: KSelectable<T2>,
            |    block: JoinSource2<T1, T2>.(T1, T2) -> R
            |): R {
            |    val (state, row) = selectableJoinState(this, query)
            |    return JoinSource2<T1, T2>(state).block(this, row)
            |}
            |
            |inline fun <reified T1 : KPojo, reified T2 : KPojo, R : JoinResult> KSelectable<T1>.join(
            |    table: T2,
            |    block: JoinSource2<T1, T2>.(T1, T2) -> R
            |): R {
            |    val (state, row, right) = selectableLeftJoinState(this, table)
            |    return JoinSource2<T1, T2>(state).block(row, right)
            |}
            |
            |inline fun <reified T1 : KPojo, reified T2 : KPojo, R : JoinResult> KSelectable<T1>.join(
            |    query: KSelectable<T2>,
            |    block: JoinSource2<T1, T2>.(T1, T2) -> R
            |): R {
            |    val state = selectableSelectableJoinState(this, query)
            |    return JoinSource2<T1, T2>(state).block(state.sourceAt(0), state.sourceAt(1))
            |}
        """.trimMargin()

        val overloads = buildList {
            add(pairOverloads)
            for (arity in joinRange) {
                add(directTableJoin(arity))
            }
            for (rawArity in 2 until maxJoinNum) {
                add(tableRawJoin(rawArity, selectableLeft = false))
                add(tableRawJoin(rawArity, selectableLeft = true))
                add(rawSingleJoin(rawArity, selectableRight = false))
                add(rawSingleJoin(rawArity, selectableRight = true))
            }
            for (leftArity in 2 until maxJoinNum) {
                for (rightArity in 2..(maxJoinNum - leftArity)) {
                    add(rawRawJoin(leftArity, rightArity))
                }
            }
        }.joinToString("\n\n")

        val patch = """
            |/*
            | * Copyright 2022-2026 kronos-orm
            | *
            | * Licensed under the Apache License, Version 2.0 (the "License");
            | * you may not use this file except in compliance with the License.
            | */
            |
            |@file:Suppress("MagicNumber", "TooManyFunctions")
            |
            |package com.kotlinorm.orm.join
            |
            |import com.kotlinorm.beans.dsl.KSelectable
            |import com.kotlinorm.interfaces.KPojo
            |
            |// Generated by build-logic/scripts/codegen.gradle.kts.
            |
            |$overloads
        """.trimMargin().trimEnd() + "\n"

        val joinSources = """
            |/*
            | * Copyright 2022-2026 kronos-orm
            | *
            | * Licensed under the Apache License, Version 2.0 (the "License");
            | * you may not use this file except in compliance with the License.
            | */
            |
            |package com.kotlinorm.orm.join
            |
            |import com.kotlinorm.interfaces.KPojo
            |
            |// Generated by build-logic/scripts/codegen.gradle.kts.
            |
            |${joinRange.joinToString("\n\n") { arity ->
                val range = 1..arity
                """
                    |class ${sourceType(arity).substringBefore('<')}<${range.joinToString(", ") { "T$it : KPojo" }}> @PublishedApi internal constructor(
                    |    state: JoinSourceState<T1>
                    |) : JoinSource<T1, ${sourceType(arity)}>(state) {
                    |    @PublishedApi
                    |    internal override fun recreate(state: JoinSourceState<T1>): ${sourceType(arity)} =
                    |        ${sourceType(arity).substringBefore('<')}(state)
                    |}
                """.trimMargin()
            }}
        """.trimMargin().trimEnd() + "\n"

        joinDir.listFiles()?.forEach { file ->
            if (file.name.matches(Regex("SelectFrom\\d+\\.kt"))) file.delete()
        }
        File(joinDir, "Patch.kt").writeText(patch)
        File(joinDir, "JoinSources.kt").writeText(joinSources)

        println("Generated JOIN sources for arity 2..$maxJoinNum in $joinDir")
    }
}
