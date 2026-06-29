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
val FunctionFieldFqName = FqName("com.kotlinorm.beans.dsl.FunctionField")
val KCascadeFqName = FqName("com.kotlinorm.beans.dsl.KCascade")
val CriteriaFqName = FqName("com.kotlinorm.beans.dsl.Criteria")
val KTableForSelectFqName = FqName("com.kotlinorm.beans.dsl.KTableForSelect")
val KTableForSetFqName = FqName("com.kotlinorm.beans.dsl.KTableForSet")
val KTableForConditionFqName = FqName("com.kotlinorm.beans.dsl.KTableForCondition")
val KTableForSortFqName = FqName("com.kotlinorm.beans.dsl.KTableForSort")
val KTableForReferenceFqName = FqName("com.kotlinorm.beans.dsl.KTableForReference")

// Kotlin standard library
val BooleanFqName = FqName("kotlin.Boolean")
val ByteFqName = FqName("kotlin.Byte")
val ShortFqName = FqName("kotlin.Short")
val IntFqName = FqName("kotlin.Int")
val LongFqName = FqName("kotlin.Long")
val FloatFqName = FqName("kotlin.Float")
val DoubleFqName = FqName("kotlin.Double")
val CharFqName = FqName("kotlin.Char")
val PairFqName = FqName("kotlin.Pair")
val StringFqName = FqName("kotlin.String")
val FunctionHandlerFqName = FqName("com.kotlinorm.functions.FunctionHandler")

// Select operation and generated projection support
val SelectPackageFqName = FqName("com.kotlinorm.orm.select")
val SelectFunctionName = Name.identifier("select")
val SelectAliasFunctionName = "as_"
val SelectGeneratedProjectionFunctionName = Name.identifier("selectGeneratedProjection")
val SelectFunctionFqName = FqName("com.kotlinorm.orm.select.select")
val SelectGeneratedProjectionCallableId = CallableId(
    SelectPackageFqName,
    null,
    SelectGeneratedProjectionFunctionName
)
val SelectClauseFqName = FqName("com.kotlinorm.orm.select.SelectClause")
val GeneratedProjectionPackageFqName = FqName("com.kotlinorm.generated.projection")
val GeneratedProjectionClassPrefix = "KronosSelectResult_"
val GeneratedProjectionFieldIdentifierRegex = Regex("[A-Za-z_][A-Za-z0-9_]*")
val QueryListFunctionName = "queryList"
val QueryOneFunctionName = "queryOne"
val QueryOneOrNullFunctionName = "queryOneOrNull"
val SelectQueryFunctionNames = setOf(QueryListFunctionName, QueryOneFunctionName, QueryOneOrNullFunctionName)
val KPojoTableNamePropertyName = Name.identifier("__tableName")
val KPojoTableCommentPropertyName = Name.identifier("__tableComment")
val TypedQueryFunctionFqNames = setOf(
    FqName("com.kotlinorm.beans.task.KronosQueryTask.$QueryListFunctionName"),
    FqName("com.kotlinorm.beans.task.KronosQueryTask.$QueryOneFunctionName"),
    FqName("com.kotlinorm.beans.task.KronosQueryTask.$QueryOneOrNullFunctionName"),
    FqName("com.kotlinorm.orm.select.SelectClause.$QueryListFunctionName"),
    FqName("com.kotlinorm.orm.select.SelectClause.$QueryOneFunctionName"),
    FqName("com.kotlinorm.orm.select.SelectClause.$QueryOneOrNullFunctionName"),
    FqName("com.kotlinorm.database.SqlHandler.$QueryListFunctionName"),
    FqName("com.kotlinorm.database.SqlHandler.$QueryOneFunctionName"),
    FqName("com.kotlinorm.database.SqlHandler.$QueryOneOrNullFunctionName")
)
val SelectFromQueryFunctionRegexes = setOf(
    Regex("com\\.kotlinorm\\.orm\\.join\\.SelectFrom\\d\\.$QueryListFunctionName"),
    Regex("com\\.kotlinorm\\.orm\\.join\\.SelectFrom\\d\\.$QueryOneFunctionName"),
    Regex("com\\.kotlinorm\\.orm\\.join\\.SelectFrom\\d\\.$QueryOneOrNullFunctionName")
)

// Enums
val KColumnTypeFqName = FqName("com.kotlinorm.enums.KColumnType")
val ConditionTypeFqName = FqName("com.kotlinorm.enums.ConditionType")
val NoValueStrategyTypeFqName = FqName("com.kotlinorm.enums.NoValueStrategyType")

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
val KronosInitAnnotationFqName = FqName("com.kotlinorm.annotations.KronosInit")
val KronosCommonStrategyFqName = FqName("com.kotlinorm.beans.config.KronosCommonStrategy")
val KronosObjectFqName = FqName("com.kotlinorm.Kronos")

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
val FunctionFieldClassId = ClassId.topLevel(FunctionFieldFqName)
val KCascadeClassId = ClassId.topLevel(KCascadeFqName)
val CriteriaClassId = ClassId.topLevel(CriteriaFqName)
val KTableForSelectClassId = ClassId.topLevel(KTableForSelectFqName)
val KTableForSetClassId = ClassId.topLevel(KTableForSetFqName)
val KTableForConditionClassId = ClassId.topLevel(KTableForConditionFqName)
val KTableForSortClassId = ClassId.topLevel(KTableForSortFqName)
val KTableForReferenceClassId = ClassId.topLevel(KTableForReferenceFqName)

// Kotlin standard library
val PairClassId = ClassId.topLevel(PairFqName)
val StringClassId = ClassId.topLevel(StringFqName)

// Enums
val KColumnTypeClassId = ClassId.topLevel(KColumnTypeFqName)
val ConditionTypeClassId = ClassId.topLevel(ConditionTypeFqName)
val NoValueStrategyTypeClassId = ClassId.topLevel(NoValueStrategyTypeFqName)

// Config
val KronosCommonStrategyClassId = ClassId.topLevel(KronosCommonStrategyFqName)
val KronosObjectClassId = ClassId.topLevel(KronosObjectFqName)

// Select operation and generated projection support
val SelectClauseClassId = ClassId.topLevel(SelectClauseFqName)
