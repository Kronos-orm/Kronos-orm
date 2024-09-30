package com.kotlinorm.plugins.utils

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irNull
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
 * This function iterates through the source code lines starting from the given offset and skips
 * lines that are annotations, single-line comments, or multi-line comments. It returns the first
 * line that is not a comment or annotation.
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

private val sourceFileCache: LRUCache<String, List<String>> = LRUCache(128)

/**
 * Retrieves the KDoc string for the current IR declaration.
 *
 * This function attempts to extract the KDoc comment associated with the current IR declaration.
 * It uses the source offsets to locate the relevant lines in the source file and then extracts
 * the comment content.
 *
 * @receiver The IR declaration for which to retrieve the KDoc string.
 * @return An IR expression containing the KDoc string, or null if no KDoc comment is found.
 */
context(IrBuilderWithScope, IrPluginContext)
fun IrDeclaration.getKDocString(): IrExpression {
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

        val comment =
            when (this) {
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
            return irString(comment)
        }
    }
    return irNull()
}

/**
 * Extract the comment content within the specified range.
 *
 * This function will extract single-line or multi-line comments within the specified range from the given list of lines.
 * If no comments are found within the specified range, the function will search upwards to the beginning of the file to find possible comments.
 *
 * 提取指定范围内的注释内容。
 *
 * 此函数会从给定的行列表中提取位于指定范围内的单行或多行注释。
 * 如果在指定范围内没有找到注释，函数将向上查找直到文件的开头，以获取可能的注释。
 *
 * @param lines a list of code lines 包含代码行的列表
 * @param range the range of lines to check 指定要检查的行范围
 * @return the extracted comment content, or null if no comment is found 找到的注释内容，如果没有找到则返回 null
 */
fun extractDeclarationComment(lines: List<String>, range: IntRange): String? {
    val startIndex = range.first
    val endIndex = range.last

    var comment: String? = null

    // Find single-line or multi-line comments within the specified range
    // 在指定范围内查找单行或多行注释
    for (i in startIndex..endIndex) {
        val line = lines.getOrNull(i)?.trim() ?: continue

        // Extract single-line comments
        // 提取单行注释
        val singleLineComment = line.substringAfter("//", "").substringBefore("//").trim()
        // Extract multi-line comments
        // 查找多行注释的起始和结束位置
        val multiLineCommentStart = line.indexOf("/*")
        val multiLineCommentEnd = line.indexOf("*/")

        // If a single-line comment is found
        // 如果找到单行注释
        if (singleLineComment.isNotEmpty()) {
            comment = singleLineComment
            break
        }

        // If a multi-line comment is found
        // 如果找到多行注释
        else if (multiLineCommentStart != -1 && multiLineCommentEnd != -1) {
            comment = line.substring(multiLineCommentStart + 2, multiLineCommentEnd).trim { it == '*' || it == ' ' }
            break
        }
    }

    // If no comments are found within the specified range, search upwards
    // 如果在指定范围内没有找到注释，向上查找
    if (comment == null) {
        var multiLineCommentFlag = false
        var singleLineCommentFlag = false
        for (i in (startIndex - 1) downTo 0) {
            val line = lines.getOrNull(i)?.trim() ?: continue
            if (line.startsWith("@")) continue
            val singleLineComment = line.substringAfter("//", "").trim()
            val multiLineCommentStart = line.indexOf("/*")
            val multiLineCommentEnd = line.indexOf("*/")

            // Handle single-line comments
            // 处理单行注释
            if (line.startsWith("//") && singleLineComment.isNotEmpty()) {
                if (singleLineCommentFlag) {
                    comment = singleLineComment + comment
                } else {
                    comment = singleLineComment
                    singleLineCommentFlag = true
                }
                continue
            }
            // Handle multi-line comments
            // 处理多行注释
            else if (multiLineCommentStart != -1 && multiLineCommentEnd != -1) {
                comment = line.substring(multiLineCommentStart + 2, multiLineCommentEnd).trim { it == '*' || it == ' ' }
                break
            }
            // Handle multi-line comments that start but do not end
            // 处理多行注释开始但未结束的情况
            else if (multiLineCommentStart != -1 && multiLineCommentFlag) {
                comment = line.substring(multiLineCommentStart + 2).trim { it == '*' || it == ' ' } + comment
                multiLineCommentFlag = true
                continue
            }
            // Handle multi-line comments that end but do not start
            // 处理多行注释结束的情况
            else if (multiLineCommentEnd != -1) {
                comment = line.substring(0, multiLineCommentEnd).trim { it == '*' || it == ' ' }
                multiLineCommentFlag = true
                continue
            }
            // Handle non-empty lines
            // 处理非空行
            else if (line.isNotBlank()) {
                if (multiLineCommentFlag) {
                    comment = line.trim { it == '*' || it == ' ' } + comment
                    continue
                }
                if (line.startsWith("@")) { // 如果是注解
                    continue
                }
                break
            }
        }
    }
    return comment
}