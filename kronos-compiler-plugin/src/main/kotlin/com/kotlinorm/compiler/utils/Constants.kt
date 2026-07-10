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

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * FQN constants for Kronos classes and interfaces
 */

// Core interfaces
val KPojoFqName = FqName("com.kotlinorm.interfaces.KPojo")

// DSL classes
val FieldFqName = FqName("com.kotlinorm.beans.dsl.Field")
val KronosFunctionExprFqName = FqName("com.kotlinorm.beans.dsl.KronosFunctionExpr")
val KronosFunctionExpressionsFqName = FqName("com.kotlinorm.functions.KronosFunctionExpressions")
val KCascadeFqName = FqName("com.kotlinorm.beans.dsl.KCascade")
val KTableForSelectFqName = FqName("com.kotlinorm.beans.dsl.KTableForSelect")
val KTableForInsertSelectFqName = FqName("com.kotlinorm.beans.dsl.KTableForInsertSelect")
val KTableForSetFqName = FqName("com.kotlinorm.beans.dsl.KTableForSet")
val KTableForConditionFqName = FqName("com.kotlinorm.beans.dsl.KTableForCondition")
val KTableForSortFqName = FqName("com.kotlinorm.beans.dsl.KTableForSort")
val KTableForReferenceFqName = FqName("com.kotlinorm.beans.dsl.KTableForReference")
val KSelectableFqName = FqName("com.kotlinorm.beans.dsl.KSelectable")
val SyntaxExprPackageFqName = FqName("com.kotlinorm.syntax.expr")
val SyntaxSqlExprFqName = FqName("com.kotlinorm.syntax.expr.SqlExpr")
val SyntaxSqlExprColumnFqName = FqName("com.kotlinorm.syntax.expr.SqlExpr.Column")
val SqlWindowFqName = FqName("com.kotlinorm.syntax.expr.SqlWindow")
val SqlOrderingItemFqName = FqName("com.kotlinorm.syntax.order.SqlOrderingItem")
val SqlOrderingFqName = FqName("com.kotlinorm.syntax.order.SqlOrdering")

val PairFqName = FqName("kotlin.Pair")
val StringFqName = FqName("kotlin.String")
val DslCollectionFunctionNames = setOf("get", "of", "listOf", "mutableListOf", "setOf", "arrayOf")
val FunctionHandlerFqName = FqName("com.kotlinorm.functions.FunctionHandler")
val KronosFunctionAnnotationFqName = FqName("com.kotlinorm.annotations.KronosFunction")

val InsertFunctionName = Name.identifier("insert")
val InsertClauseFqName = FqName("com.kotlinorm.orm.insert.InsertClause")

// Select operation and generated projection support
val SelectPackageFqName = FqName("com.kotlinorm.orm.select")
val JoinPackageFqName = FqName("com.kotlinorm.orm.join")
val SelectFunctionName = Name.identifier("select")
val SelectAliasFunctionName = "alias"
val SelectAliasFunctionNameIdentifier = Name.identifier(SelectAliasFunctionName)
val SelectLimitFunctionName = Name.identifier("limit")
val SelectGroupByFunctionName = Name.identifier("groupBy")
val SelectGeneratedProjectionFunctionName = Name.identifier("selectGeneratedProjection")
val SelectFunctionFqName = FqName("com.kotlinorm.orm.select.select")
val SelectGeneratedProjectionCallableId = CallableId(
    SelectPackageFqName,
    null,
    SelectGeneratedProjectionFunctionName
)
val JoinSelectGeneratedProjectionCallableId = CallableId(
    JoinPackageFqName,
    null,
    SelectGeneratedProjectionFunctionName
)
val SelectClauseFqName = FqName("com.kotlinorm.orm.select.SelectClause")
val SelectFromFqName = FqName("com.kotlinorm.orm.join.SelectFrom")
val GeneratedProjectionPackageFqName = FqName("com.kotlinorm.generated.projection")
val GeneratedFactoryPackageFqName = FqName("com.kotlinorm.generated.factory")
val GeneratedProjectionClassPrefix = "KronosSelectResult_"
val GeneratedContextClassPrefix = "KronosSelectContext_"
val GeneratedProjectionFieldIdentifierRegex = Regex("[A-Za-z_][A-Za-z0-9_]*")
val ToListFunctionName = "toList"
val FirstFunctionName = "first"
val FirstOrNullFunctionName = "firstOrNull"
val SelectQueryFunctionNames = setOf(ToListFunctionName, FirstFunctionName, FirstOrNullFunctionName)
val CompareToFunctionName = Name.identifier("compareTo")
val ContainsFunctionName = Name.identifier("contains")
val EqualsFunctionName = Name.identifier("equals")
val SortAscendingFunctionName = Name.identifier("asc")
val SortDescendingFunctionName = Name.identifier("desc")
val WindowOverFunctionName = "over"
val WindowPartitionByFunctionName = "partitionBy"
val WindowOrderByFunctionName = "orderBy"
val PlainAggregateFunctionNames = setOf("count", "sum", "avg", "min", "max")
val SubqueryQuantifierFunctionNames = setOf(
    Name.identifier("any"),
    Name.identifier("some"),
    Name.identifier("all")
)
val KPojoTableNamePropertyName = Name.identifier("__tableName")
val KPojoTableCommentPropertyName = Name.identifier("__tableComment")
// Enums
val KColumnTypeFqName = FqName("com.kotlinorm.enums.KColumnType")

// Annotations
val TableAnnotationFqName = FqName("com.kotlinorm.annotations.Table")
val ColumnAnnotationFqName = FqName("com.kotlinorm.annotations.Column")
val ColumnTypeAnnotationFqName = FqName("com.kotlinorm.annotations.ColumnType")
val IgnoreAnnotationFqName = FqName("com.kotlinorm.annotations.Ignore")
val PrimaryKeyAnnotationFqName = FqName("com.kotlinorm.annotations.PrimaryKey")
val CascadeAnnotationFqName = FqName("com.kotlinorm.annotations.Cascade")
val DateTimeFormatAnnotationFqName = FqName("com.kotlinorm.annotations.DateTimeFormat")
val DefaultValueAnnotationFqName = FqName("com.kotlinorm.annotations.Default")
val NonNullAnnotationFqName = FqName("com.kotlinorm.annotations.NonNull")
val SerializeAnnotationFqName = FqName("com.kotlinorm.annotations.Serialize")
val KronosCommonStrategyFqName = FqName("com.kotlinorm.beans.config.KronosCommonStrategy")
val KronosObjectFqName = FqName("com.kotlinorm.Kronos")
val KPojoFactoryProviderFqName = FqName("com.kotlinorm.utils.KPojoFactoryProvider")

// Annotation FqNames grouped for class transformer
object AnnotationFqNames {
    val Table = TableAnnotationFqName
    val Column = ColumnAnnotationFqName
    val ColumnType = ColumnTypeAnnotationFqName
    val Ignore = IgnoreAnnotationFqName
    val PrimaryKey = PrimaryKeyAnnotationFqName
    val Cascade = CascadeAnnotationFqName
    val DateTimeFormat = DateTimeFormatAnnotationFqName
    val Default = DefaultValueAnnotationFqName
    val NonNull = NonNullAnnotationFqName
    val Serialize = SerializeAnnotationFqName
    val CreateTime = FqName("com.kotlinorm.annotations.CreateTime")
    val UpdateTime = FqName("com.kotlinorm.annotations.UpdateTime")
    val LogicDelete = FqName("com.kotlinorm.annotations.LogicDelete")
    val Version = FqName("com.kotlinorm.annotations.Version")
    val TableIndex = FqName("com.kotlinorm.annotations.TableIndex")
}

/**
 * ClassId constants for class references
 */

// Core interfaces
val KPojoClassId = ClassId.topLevel(KPojoFqName)

// DSL classes
val FieldClassId = ClassId.topLevel(FieldFqName)
val KronosFunctionExprClassId = ClassId.topLevel(KronosFunctionExprFqName)
val KronosFunctionExpressionsClassId = ClassId.topLevel(KronosFunctionExpressionsFqName)
val KCascadeClassId = ClassId.topLevel(KCascadeFqName)
val KTableForSelectClassId = ClassId.topLevel(KTableForSelectFqName)
val KTableForInsertSelectClassId = ClassId.topLevel(KTableForInsertSelectFqName)
val KTableForSetClassId = ClassId.topLevel(KTableForSetFqName)
val KTableForConditionClassId = ClassId.topLevel(KTableForConditionFqName)
val KTableForSortClassId = ClassId.topLevel(KTableForSortFqName)
val KTableForReferenceClassId = ClassId.topLevel(KTableForReferenceFqName)
val KSelectableClassId = ClassId.topLevel(KSelectableFqName)
val InsertClauseClassId = ClassId.topLevel(InsertClauseFqName)
val SyntaxSqlExprClassId = ClassId.topLevel(SyntaxSqlExprFqName)
val SyntaxSqlExprColumnClassId = ClassId(SyntaxExprPackageFqName, FqName("SqlExpr.Column"), isLocal = false)
val SqlWindowClassId = ClassId.topLevel(SqlWindowFqName)
val SqlOrderingItemClassId = ClassId.topLevel(SqlOrderingItemFqName)
val SqlOrderingClassId = ClassId.topLevel(SqlOrderingFqName)

// Kotlin standard library
val PairClassId = ClassId.topLevel(PairFqName)
val StringClassId = ClassId.topLevel(StringFqName)

// Enums
val KColumnTypeClassId = ClassId.topLevel(KColumnTypeFqName)

// Annotation class ids
val CascadeAnnotationClassId = ClassId.topLevel(CascadeAnnotationFqName)
val IgnoreAnnotationClassId = ClassId.topLevel(IgnoreAnnotationFqName)
val PrimaryKeyAnnotationClassId = ClassId.topLevel(PrimaryKeyAnnotationFqName)
val SerializeAnnotationClassId = ClassId.topLevel(SerializeAnnotationFqName)

// Config
val KronosCommonStrategyClassId = ClassId.topLevel(KronosCommonStrategyFqName)
val KronosObjectClassId = ClassId.topLevel(KronosObjectFqName)

// Select operation and generated projection support
val SelectClauseClassId = ClassId.topLevel(SelectClauseFqName)
val SelectFromClassId = ClassId.topLevel(SelectFromFqName)
