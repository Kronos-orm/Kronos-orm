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

import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.types.IrType

/**
 * DebugLogger 存根
 * 
 * 此类已被废弃，所有调用都是空操作。
 * 请使用 IrTestFramework 进行测试。
 */
object DebugLogger {
    enum class OutputFormat { JSON }
    
    fun enable(path: String, format: OutputFormat) {}
    fun disable() {}
    fun logInfo(message: String) {}
    fun logSymbolResolution(name: String, found: Boolean, fqName: String?) {}
    fun logTypeJudgment(type: IrType, functionName: String, result: Boolean, reason: String) {}
    fun logPropertyJudgment(property: IrProperty, functionName: String, result: Boolean, reason: String) {}
    fun getStats(): String = "DebugLogger is deprecated"
}
