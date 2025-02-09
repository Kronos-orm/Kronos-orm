/**
 * Copyright 2022-2024 kronos-orm
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

package com.kotlinorm.compiler.plugin.utils

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

internal const val SOURCE_FILE_CACHE_SIZE = 128
internal val sourceFileCache: LRUCache<String, List<String>> = LRUCache(SOURCE_FILE_CACHE_SIZE)

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
    val commentIgnore = { char: Char -> char == '*' || char == ' ' }

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
            comment = line.substring(multiLineCommentStart + 2, multiLineCommentEnd).trim(commentIgnore)
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
                comment = line.substring(multiLineCommentStart + 2, multiLineCommentEnd).trim(commentIgnore)
                break
            }
            // Handle multi-line comments that start but do not end
            // 处理多行注释开始但未结束的情况
            else if (multiLineCommentStart != -1 && multiLineCommentFlag) {
                comment = line.substring(multiLineCommentStart + 2).trim(commentIgnore) + comment
                break
            }
            // Handle multi-line comments that end but do not start
            // 处理多行注释结束的情况
            else if (multiLineCommentEnd != -1) {
                comment = line.substring(0, multiLineCommentEnd).trim(commentIgnore)
                multiLineCommentFlag = true
                continue
            }
            // Handle non-empty lines
            // 处理非空行
            else if (line.isNotBlank()) {
                if (multiLineCommentFlag) {
                    comment = line.trim(commentIgnore) + comment
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