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
import org.jetbrains.kotlin.name.FqName

/**
 * FQN constants for Kronos classes and interfaces
 */

// Core interfaces
val KPojoFqName = FqName("com.kotlinorm.interfaces.KPojo")

// DSL classes
val FieldFqName = FqName("com.kotlinorm.beans.dsl.Field")
val FunctionFieldFqName = FqName("com.kotlinorm.beans.dsl.FunctionField")
val CriteriaFqName = FqName("com.kotlinorm.beans.dsl.Criteria")
val KTableForSelectFqName = FqName("com.kotlinorm.beans.dsl.KTableForSelect")
val KTableForSetFqName = FqName("com.kotlinorm.beans.dsl.KTableForSet")
val KTableForConditionFqName = FqName("com.kotlinorm.beans.dsl.KTableForCondition")
val KTableForSortFqName = FqName("com.kotlinorm.beans.dsl.KTableForSort")
val KTableForReferenceFqName = FqName("com.kotlinorm.beans.dsl.KTableForReference")

// Kotlin standard library
val PairFqName = FqName("kotlin.Pair")
val FunctionHandlerFqName = FqName("com.kotlinorm.functions.FunctionHandler")

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
val NecessaryAnnotationFqName = FqName("com.kotlinorm.annotations.Necessary")
val SerializeAnnotationFqName = FqName("com.kotlinorm.annotations.Serialize")
val KronosInitAnnotationFqName = FqName("com.kotlinorm.annotations.KronosInit")

/**
 * ClassId constants for class references
 */

// Core interfaces
val KPojoClassId = ClassId.topLevel(KPojoFqName)

// DSL classes
val FieldClassId = ClassId.topLevel(FieldFqName)
val FunctionFieldClassId = ClassId.topLevel(FunctionFieldFqName)
val CriteriaClassId = ClassId.topLevel(CriteriaFqName)
val KTableForSelectClassId = ClassId.topLevel(KTableForSelectFqName)
val KTableForSetClassId = ClassId.topLevel(KTableForSetFqName)
val KTableForConditionClassId = ClassId.topLevel(KTableForConditionFqName)
val KTableForSortClassId = ClassId.topLevel(KTableForSortFqName)
val KTableForReferenceClassId = ClassId.topLevel(KTableForReferenceFqName)

// Kotlin standard library
val PairClassId = ClassId.topLevel(PairFqName)

// Enums
val KColumnTypeClassId = ClassId.topLevel(KColumnTypeFqName)
