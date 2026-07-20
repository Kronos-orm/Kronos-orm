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

package com.kotlinorm.beans.dsl

import com.kotlinorm.syntax.statement.SqlSelectItem

/**
 * Assigns stable result names while preserving explicitly requested names.
 * The first occurrence keeps its name and later occurrences use `_1`, `_2`, ... .
 */
internal fun List<KTableForSelect.ProjectionItem>.withUniqueOutputNames(): List<KTableForSelect.ProjectionItem> {
    val requestedNames = map { it.requestedOutputName() }
    val allocatedNames = allocateProjectionNames(requestedNames)
    return mapIndexed { index, item -> item.withOutputName(allocatedNames[index]) }
}

internal fun allocateProjectionNames(requestedNames: List<String?>): List<String?> {
    val reservedNames = requestedNames.filterNotNull().filterTo(linkedSetOf()) { it.isNotBlank() }
    val usedNames = linkedSetOf<String>()
    val nextSuffixByName = mutableMapOf<String, Int>()

    return requestedNames.map { requestedName ->
        if (requestedName.isNullOrBlank() || usedNames.add(requestedName)) {
            requestedName
        } else {
            var suffix = nextSuffixByName.getOrDefault(requestedName, 1)
            var candidate: String
            do {
                candidate = "${requestedName}_${suffix++}"
            } while (candidate in reservedNames || candidate in usedNames)
            nextSuffixByName[requestedName] = suffix
            usedNames += candidate
            candidate
        }
    }
}

private fun KTableForSelect.ProjectionItem.requestedOutputName(): String? =
    when (this) {
        is KTableForSelect.ProjectionItem.FieldItem -> field.name.ifBlank { field.columnName }
        is KTableForSelect.ProjectionItem.SelectItemValue -> (item as? SqlSelectItem.Expr)
            ?.let { it.metadata?.outputName ?: it.alias }
        is KTableForSelect.ProjectionItem.ScalarSubqueryValue -> alias
    }

private fun KTableForSelect.ProjectionItem.withOutputName(
    outputName: String?
): KTableForSelect.ProjectionItem =
    when (this) {
        is KTableForSelect.ProjectionItem.FieldItem -> copy(outputName = outputName)
        is KTableForSelect.ProjectionItem.SelectItemValue -> copy(
            item = item.withOutputName(outputName)
        )
        is KTableForSelect.ProjectionItem.ScalarSubqueryValue -> {
            val resolvedName = outputName ?: alias
            copy(alias = resolvedName, item = item.withOutputName(resolvedName))
        }
    }

private fun SqlSelectItem.withOutputName(outputName: String?): SqlSelectItem =
    when {
        this !is SqlSelectItem.Expr || outputName == null -> this
        metadata?.outputName == outputName && alias == outputName -> this
        else -> copy(
            alias = outputName,
            metadata = metadata?.copy(outputName = outputName)
        )
    }
