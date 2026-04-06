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

package com.kotlinorm.compiler.utils

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.sourceElement
import java.io.File
import kotlin.text.Charsets.UTF_8

/**
 * Calculates the real start offset of the source code, skipping over comments and annotations.
 *
 * @param source The list of source code lines.
 * @param startOffset The initial offset to start checking from.
 * @return The real start offset after skipping comments and annotations.
 */
fun realStartOffset(source: List<String>, startOffset: Int): Int {
    var realStartOffset = startOffset
    var multiLineCommentFlag = false
    while (
        source[realStartOffset].trimStart().startsWith("@") ||
        source[realStartOffset].trimStart().startsWith("//") ||
        source[realStartOffset].trimStart().startsWith("/*") ||
        multiLineCommentFlag
    ) {
        if (source[realStartOffset].trimStart().startsWith("/*")) {
            multiLineCommentFlag = true
        }
        if (source[realStartOffset].trimStart().endsWith("*/")) {
            multiLineCommentFlag = false
        }
        realStartOffset++
    }
    return realStartOffset
}

internal const val SOURCE_FILE_CACHE_SIZE = 128
internal val sourceFileCache: LRUCache<String, List<String>> = LRUCache(SOURCE_FILE_CACHE_SIZE)

/**
 * Extract the comment content within the specified range.
 *
 * @param lines a list of code lines
 * @param range the range of lines to check
 * @return the extracted comment content, or null if no comment is found
 */
fun extractDeclarationComment(lines: List<String>, range: IntRange): String? {
    val startIndex = range.first
    val endIndex = range.last

    var comment: String? = null
    val commentIgnore = { char: Char -> char == '*' || char == ' ' }

    for (i in startIndex..endIndex) {
        val line = lines.getOrNull(i)?.trim() ?: continue

        val singleLineComment = line.substringAfter("//", "").substringBefore("//").trim()
        val multiLineCommentStart = line.indexOf("/*")
        val multiLineCommentEnd = line.indexOf("*/")

        if (singleLineComment.isNotEmpty()) {
            comment = singleLineComment
            break
        } else if (multiLineCommentStart != -1 && multiLineCommentEnd != -1) {
            comment = line.substring(multiLineCommentStart + 2, multiLineCommentEnd).trim(commentIgnore)
            break
        }
    }

    // If no comments are found within the specified range, search upwards
    if (comment == null) {
        var multiLineCommentFlag = false
        var singleLineCommentFlag = false
        for (i in (startIndex - 1) downTo 0) {
            val line = lines.getOrNull(i)?.trim() ?: continue
            if (line.startsWith("@")) continue
            val singleLineComment = line.substringAfter("//", "").trim()
            val multiLineCommentStart = line.indexOf("/*")
            val multiLineCommentEnd = line.indexOf("*/")

            if (line.startsWith("//") && singleLineComment.isNotEmpty()) {
                if (singleLineCommentFlag) {
                    comment = singleLineComment + comment
                } else {
                    comment = singleLineComment
                    singleLineCommentFlag = true
                }
                continue
            } else if (multiLineCommentStart != -1 && multiLineCommentEnd != -1) {
                comment = line.substring(multiLineCommentStart + 2, multiLineCommentEnd).trim(commentIgnore)
                break
            } else if (multiLineCommentStart != -1 && multiLineCommentFlag) {
                comment = line.substring(multiLineCommentStart + 2).trim(commentIgnore) + comment
                break
            } else if (multiLineCommentEnd != -1) {
                comment = line.substring(0, multiLineCommentEnd).trim(commentIgnore)
                multiLineCommentFlag = true
                continue
            } else if (line.isNotBlank()) {
                if (multiLineCommentFlag) {
                    comment = line.trim(commentIgnore) + comment
                    continue
                }
                if (line.startsWith("@")) {
                    continue
                }
                break
            }
        }
    }
    return comment
}

/**
 * Retrieves the KDoc string for the current IR declaration.
 *
 * @receiver The IR declaration for which to retrieve the KDoc string.
 * @return An IR expression containing the KDoc string, or an empty string if not found.
 */
context(builder: IrBuilderWithScope)
fun IrDeclaration.getKDocString(): IrExpression {
    val declaration = this
    val sourceOffsets = sourceElement()
    if (sourceOffsets != null) {
        val startOffset = sourceOffsets.startOffset
        val endOffset = sourceOffsets.endOffset
        val fileEntry = file.fileEntry
        val sourceRange = fileEntry.getSourceRangeInfo(startOffset, endOffset)
        val source = sourceFileCache.getOrPut(fileEntry.name) {
            File(sourceRange.filePath).readLines(UTF_8)
        }
        val realStartOffset = realStartOffset(source, sourceRange.startLineNumber)
        val comment = when (declaration) {
            is IrProperty -> extractDeclarationComment(
                source,
                realStartOffset..sourceRange.endLineNumber
            )
            is IrClass -> extractDeclarationComment(
                source,
                sourceRange.startLineNumber..realStartOffset
            )
            else -> null
        }

        if (comment != null) {
            return builder.irString(comment)
        }
    }
    return builder.irString("")
}
