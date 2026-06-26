/**
 * Copyright 2022-2025 kronos-orm
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

import com.kotlinorm.compiler.helpers.irListOf
import com.kotlinorm.compiler.helpers.valueArguments
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.name.FqName

val fqNameOfTypedQuery =
    listOf(
        FqName("com.kotlinorm.beans.task.KronosQueryTask.queryList"),
        FqName("com.kotlinorm.beans.task.KronosQueryTask.queryOne"),
        FqName("com.kotlinorm.beans.task.KronosQueryTask.queryOneOrNull"),
        FqName("com.kotlinorm.orm.select.SelectClause.queryList"),
        FqName("com.kotlinorm.orm.select.SelectClause.queryOne"),
        FqName("com.kotlinorm.orm.select.SelectClause.queryOneOrNull"),
        FqName("com.kotlinorm.database.SqlHandler.queryList"),
        FqName("com.kotlinorm.database.SqlHandler.queryOne"),
        FqName("com.kotlinorm.database.SqlHandler.queryOneOrNull")
    )

val fqNameOfSelectFromsRegexes =
    listOf(
        "com.kotlinorm.orm.join.SelectFrom\\d.queryList",
        "com.kotlinorm.orm.join.SelectFrom\\d.queryOne",
        "com.kotlinorm.orm.join.SelectFrom\\d.queryOneOrNull"
    )

context(context: IrPluginContext, builder: IrBlockBuilder)
fun updateTypedQueryParameters(irCall: IrCall): IrCall {
    val queryType = irCall.typeArguments[0]!!
    val superTypes = (listOf(queryType) + queryType.superTypes()).map { it.classFqName!! }
    val irSuperTypes = irListOf(
        context.irBuiltIns.stringType,
        superTypes.map { builder.irString(it.asString()) }
    )
    val isKPojo = KPojoFqName in superTypes
    // 请务必保证上方函数的最后两个参数为 isKPojo 和 superTypes
    // 否则会导致 irCall 的参数顺序错乱
    irCall.arguments[irCall.arguments.size - 2] = builder.irBoolean(isKPojo)
    irCall.arguments[irCall.arguments.size - 1] = irSuperTypes
    return irCall
}
