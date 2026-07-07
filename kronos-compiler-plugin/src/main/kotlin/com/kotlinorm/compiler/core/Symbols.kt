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

@file:Suppress("DEPRECATION")

package com.kotlinorm.compiler.core

import com.kotlinorm.compiler.utils.CascadeAnnotationFqName
import com.kotlinorm.compiler.utils.FieldClassId
import com.kotlinorm.compiler.utils.FieldFqName
import com.kotlinorm.compiler.utils.IgnoreAnnotationFqName
import com.kotlinorm.compiler.utils.KCascadeClassId
import com.kotlinorm.compiler.utils.KCascadeFqName
import com.kotlinorm.compiler.utils.KColumnTypeClassId
import com.kotlinorm.compiler.utils.KColumnTypeFqName
import com.kotlinorm.compiler.utils.KPojoClassId
import com.kotlinorm.compiler.utils.KPojoFactoryProviderFqName
import com.kotlinorm.compiler.utils.KPojoFqName
import com.kotlinorm.compiler.utils.KronosCommonStrategyClassId
import com.kotlinorm.compiler.utils.KronosFunctionExprClassId
import com.kotlinorm.compiler.utils.KronosFunctionExprFqName
import com.kotlinorm.compiler.utils.KronosFunctionExpressionsClassId
import com.kotlinorm.compiler.utils.KronosObjectClassId
import com.kotlinorm.compiler.utils.KTableForConditionClassId
import com.kotlinorm.compiler.utils.KTableForConditionFqName
import com.kotlinorm.compiler.utils.KTableForInsertSelectClassId
import com.kotlinorm.compiler.utils.KTableForInsertSelectFqName
import com.kotlinorm.compiler.utils.KTableForReferenceClassId
import com.kotlinorm.compiler.utils.KTableForReferenceFqName
import com.kotlinorm.compiler.utils.KTableForSelectClassId
import com.kotlinorm.compiler.utils.KTableForSelectFqName
import com.kotlinorm.compiler.utils.KTableForSetClassId
import com.kotlinorm.compiler.utils.KTableForSetFqName
import com.kotlinorm.compiler.utils.KTableForSortClassId
import com.kotlinorm.compiler.utils.KTableForSortFqName
import com.kotlinorm.compiler.utils.PairClassId
import com.kotlinorm.compiler.utils.PairFqName
import com.kotlinorm.compiler.utils.SerializeAnnotationFqName
import com.kotlinorm.compiler.utils.SqlOrderingClassId
import com.kotlinorm.compiler.utils.SqlOrderingItemClassId
import com.kotlinorm.compiler.utils.SqlWindowClassId
import com.kotlinorm.compiler.utils.SyntaxSqlExprColumnClassId
import com.kotlinorm.compiler.utils.SyntaxSqlExprColumnFqName
import com.kotlinorm.compiler.utils.StringClassId
import com.kotlinorm.compiler.utils.SyntaxSqlExprFqName
import com.kotlinorm.compiler.utils.SyntaxSqlExprClassId
import com.kotlinorm.compiler.utils.valueParameters
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getPropertySetter
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Symbol references for Kronos compiler plugin
 *
 * Uses context receivers and top-level properties to provide access to commonly used symbols
 */

context(context: IrPluginContext)
private fun requiredClass(classId: ClassId, message: String): IrClassSymbol =
    context.referenceClass(classId) ?: error(message)

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrClassSymbol.requiredConstructor(message: String): IrConstructorSymbol =
    constructors.firstOrNull() ?: error(message)

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrClassSymbol.requiredFunction(name: String, message: String): IrSimpleFunctionSymbol =
    getSimpleFunction(name) ?: error(message)

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrClassSymbol.requiredGetter(name: String, message: String): IrSimpleFunctionSymbol =
    getPropertyGetter(name) ?: error(message)

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrClassSymbol.requiredSetter(name: String, message: String): IrSimpleFunctionSymbol =
    getPropertySetter(name) ?: error(message)

private fun Collection<IrSimpleFunctionSymbol>.firstRequired(message: String): IrSimpleFunctionSymbol =
    firstOrNull() ?: error(message)

/**
 * KPojo interface symbol
 */
context(context: IrPluginContext)
val kPojoClassSymbol: IrClassSymbol
    get() = requiredClass(KPojoClassId, "KPojo interface not found: ${KPojoFqName.asString()}")

/**
 * KPojoFactoryProvider interface symbol
 */
context(context: IrPluginContext)
val kPojoFactoryProviderSymbol: IrClassSymbol
    get() = requiredClass(
        ClassId.topLevel(KPojoFactoryProviderFqName),
        "KPojoFactoryProvider interface not found: ${KPojoFactoryProviderFqName.asString()}"
    )

/**
 * Field class symbol
 */
context(context: IrPluginContext)
val fieldClassSymbol: IrClassSymbol
    get() = requiredClass(FieldClassId, "Field class not found: ${FieldFqName.asString()}")

/**
 * Field constructor symbol
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val fieldConstructorSymbol: IrConstructorSymbol
    get() = fieldClassSymbol.requiredConstructor("Field constructor not found")

/**
 * KCascade class symbol
 */
context(context: IrPluginContext)
val kCascadeClassSymbol: IrClassSymbol
    get() = requiredClass(KCascadeClassId, "KCascade class not found: ${KCascadeFqName.asString()}")

/**
 * KCascade constructor symbol
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val kCascadeConstructorSymbol: IrConstructorSymbol
    get() = kCascadeClassSymbol.requiredConstructor("KCascade constructor not found")

context(context: IrPluginContext)
val kronosFunctionExprClassSymbol: IrClassSymbol
    get() = requiredClass(KronosFunctionExprClassId, "KronosFunctionExpr class not found: ${KronosFunctionExprFqName.asString()}")

context(context: IrPluginContext)
val kronosFunctionExpressionsSymbol: IrClassSymbol
    get() = requiredClass(KronosFunctionExpressionsClassId, "KronosFunctionExpressions object not found")

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val kronosFunctionCallWindowArgsSymbol: IrSimpleFunctionSymbol
    get() = kronosFunctionExpressionsSymbol.requiredFunction("callWindowArgs", "KronosFunctionExpressions.callWindowArgs method not found")

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val kronosFunctionAliasSymbol: IrSimpleFunctionSymbol
    get() = kronosFunctionExprClassSymbol.requiredFunction("alias", "KronosFunctionExpr.alias method not found")

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val kronosFunctionExprGetterSymbol: IrSimpleFunctionSymbol
    get() = kronosFunctionExprClassSymbol.requiredGetter("expr", "KronosFunctionExpr.expr getter not found")

/**
 * Expression sealed interface symbol.
 */
context(context: IrPluginContext)
val expressionClassSymbol: IrClassSymbol
    get() = requiredClass(SyntaxSqlExprClassId, "SqlExpr interface not found: ${SyntaxSqlExprFqName.asString()}")

context(context: IrPluginContext)
val sqlExprColumnClassSymbol: IrClassSymbol
    get() = requiredClass(SyntaxSqlExprColumnClassId, "SqlExpr.Column class not found: ${SyntaxSqlExprColumnFqName.asString()}")

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val sqlExprColumnConstructorSymbol: IrConstructorSymbol
    get() = sqlExprColumnClassSymbol.requiredConstructor("SqlExpr.Column constructor not found")

context(context: IrPluginContext)
val sqlWindowClassSymbol: IrClassSymbol
    get() = requiredClass(SqlWindowClassId, "SqlWindow class not found")

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val sqlWindowConstructorSymbol: IrConstructorSymbol
    get() = sqlWindowClassSymbol.requiredConstructor("SqlWindow constructor not found")

context(context: IrPluginContext)
val sqlOrderingItemClassSymbol: IrClassSymbol
    get() = requiredClass(SqlOrderingItemClassId, "SqlOrderingItem class not found")

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val sqlOrderingItemConstructorSymbol: IrConstructorSymbol
    get() = sqlOrderingItemClassSymbol.requiredConstructor("SqlOrderingItem constructor not found")

context(context: IrPluginContext)
val sqlOrderingClassSymbol: IrClassSymbol
    get() = requiredClass(SqlOrderingClassId, "SqlOrdering enum not found")

/**
 * Pair class symbol (kotlin.Pair)
 */
context(context: IrPluginContext)
val pairClassSymbol: IrClassSymbol
    get() = requiredClass(PairClassId, "Pair class not found: ${PairFqName.asString()}")

/**
 * Pair constructor symbol
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val pairConstructorSymbol: IrConstructorSymbol
    get() = pairClassSymbol.requiredConstructor("Pair constructor not found")

/**
 * KronosCommonStrategy class symbol
 */
context(context: IrPluginContext)
val kronosCommonStrategyClassSymbol: IrClassSymbol
    get() = requiredClass(KronosCommonStrategyClassId, "KronosCommonStrategy class not found")

/**
 * KronosCommonStrategy constructor symbol
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val kronosCommonStrategyConstructorSymbol: IrConstructorSymbol
    get() = kronosCommonStrategyClassSymbol.requiredConstructor("KronosCommonStrategy constructor not found")

/**
 * Kronos object symbol
 */
context(context: IrPluginContext)
val kronosObjectSymbol: IrClassSymbol
    get() = requiredClass(KronosObjectClassId, "Kronos object not found")

/**
 * Kronos.fieldNamingStrategy property getter symbol
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val fieldNamingStrategyGetterSymbol: IrSimpleFunctionSymbol
    get() = kronosObjectSymbol.owner.declarations
        .filterIsInstance<IrProperty>()
        .first { it.name.asString() == "fieldNamingStrategy" }
        .getter!!.symbol

/**
 * Kronos.tableNamingStrategy property getter symbol
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val tableNamingStrategyGetterSymbol: IrSimpleFunctionSymbol
    get() = kronosObjectSymbol.owner.declarations
        .filterIsInstance<IrProperty>()
        .first { it.name.asString() == "tableNamingStrategy" }
        .getter!!.symbol

/**
 * KronosNamingStrategy.k2db function symbol
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val k2dbFunctionSymbol: IrSimpleFunctionSymbol
    get() {
        val namingStrategyClassId = ClassId.topLevel(FqName("com.kotlinorm.interfaces.KronosNamingStrategy"))
        val cls = context.referenceClass(namingStrategyClassId) ?: error("KronosNamingStrategy not found")
        return cls.owner.declarations
            .filterIsInstance<org.jetbrains.kotlin.ir.declarations.IrSimpleFunction>()
            .first { it.name.asString() == "k2db" }
            .symbol
    }

/**
 * listOf function symbol (kotlin.collections.listOf with vararg)
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val listOfFunctionSymbol: IrSimpleFunctionSymbol
    get() {
        val functions = context.referenceFunctions(
            CallableId(
                FqName("kotlin.collections"),
                null,
                Name.identifier("listOf")
            )
        )
        return functions.first { 
            it.owner.parameters.valueParameters.singleOrNull()?.isVararg == true 
        }
    }

/**
 * KColumnType enum symbol
 */
context(context: IrPluginContext)
val kColumnTypeSymbol: IrClassSymbol
    get() = requiredClass(KColumnTypeClassId, "KColumnType enum not found: ${KColumnTypeFqName.asString()}")

/**
 * KTableForSelect class symbol
 */
context(context: IrPluginContext)
val kTableForSelectSymbol: IrClassSymbol
    get() = requiredClass(KTableForSelectClassId, "KTableForSelect class not found: ${KTableForSelectFqName.asString()}")

/**
 * KTableForInsertSelect class symbol.
 */
context(context: IrPluginContext)
val kTableForInsertSelectSymbol: IrClassSymbol
    get() = requiredClass(KTableForInsertSelectClassId, "KTableForInsertSelect class not found: ${KTableForInsertSelectFqName.asString()}")

/**
 * KTableForSet class symbol
 */
context(context: IrPluginContext)
val kTableForSetSymbol: IrClassSymbol
    get() = requiredClass(KTableForSetClassId, "KTableForSet class not found: ${KTableForSetFqName.asString()}")

/**
 * KTableForCondition class symbol
 */
context(context: IrPluginContext)
val kTableForConditionSymbol: IrClassSymbol
    get() = requiredClass(KTableForConditionClassId, "KTableForCondition class not found: ${KTableForConditionFqName.asString()}")

/**
 * KTableForSort class symbol
 */
context(context: IrPluginContext)
val kTableForSortSymbol: IrClassSymbol
    get() = requiredClass(KTableForSortClassId, "KTableForSort class not found: ${KTableForSortFqName.asString()}")

/**
 * KTableForReference class symbol
 */
context(context: IrPluginContext)
val kTableForReferenceSymbol: IrClassSymbol
    get() = requiredClass(KTableForReferenceClassId, "KTableForReference class not found: ${KTableForReferenceFqName.asString()}")

/**
 * addField method symbol from KTableForReference
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val addRefFieldSymbol: IrSimpleFunctionSymbol
    get() = kTableForReferenceSymbol.requiredFunction("addField", "addField method not found in KTableForReference")

// ============================================================================
// Method Symbols
// ============================================================================

/**
 * addField method symbol from KTableForSelect
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val addFieldMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForSelectSymbol.requiredFunction("addField", "addField method not found in KTableForSelect")

/**
 * addValue method symbol from KTableForInsertSelect.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val addInsertSelectValueMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForInsertSelectSymbol.requiredFunction("addValue", "addValue method not found in KTableForInsertSelect")

/**
 * addScalarSubquery method symbol from KTableForSelect.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val addScalarSubqueryMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForSelectSymbol.requiredFunction("addScalarSubquery", "addScalarSubquery method not found in KTableForSelect")

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val addFunctionMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForSelectSymbol.requiredFunction("addFunction", "addFunction method not found in KTableForSelect")

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val addRawSqlMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForSelectSymbol.requiredFunction("addRawSql", "addRawSql method not found in KTableForSelect")

/**
 * setValue method symbol from KTableForSet
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val setValueMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForSetSymbol.requiredFunction("setValue", "setValue method not found in KTableForSet")

/**
 * setAssign method symbol from KTableForSet
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val setAssignMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForSetSymbol.requiredFunction("setAssign", "setAssign method not found in KTableForSet")

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val setConditionSqlExprSymbol: IrSimpleFunctionSymbol
    get() = kTableForConditionSymbol.requiredSetter("sqlExpr", "sqlExpr property setter not found in KTableForCondition")

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val rawConditionExprMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForConditionSymbol.requiredFunction("rawConditionExpr", "rawConditionExpr method not found in KTableForCondition")

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val isNullConditionExprMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForConditionSymbol.requiredFunction("isNullConditionExpr", "isNullConditionExpr method not found in KTableForCondition")

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val equalConditionExprMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForConditionSymbol.requiredFunction("equalConditionExpr", "equalConditionExpr method not found in KTableForCondition")

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val greaterThanConditionExprMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForConditionSymbol.requiredFunction("greaterThanConditionExpr", "greaterThanConditionExpr method not found in KTableForCondition")

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val greaterThanOrEqualConditionExprMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForConditionSymbol.requiredFunction("greaterThanOrEqualConditionExpr", "greaterThanOrEqualConditionExpr method not found in KTableForCondition")

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val lessThanConditionExprMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForConditionSymbol.requiredFunction("lessThanConditionExpr", "lessThanConditionExpr method not found in KTableForCondition")

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val lessThanOrEqualConditionExprMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForConditionSymbol.requiredFunction("lessThanOrEqualConditionExpr", "lessThanOrEqualConditionExpr method not found in KTableForCondition")

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val likeConditionExprMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForConditionSymbol.requiredFunction("likeConditionExpr", "likeConditionExpr method not found in KTableForCondition")

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val regexpConditionExprMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForConditionSymbol.requiredFunction("regexpConditionExpr", "regexpConditionExpr method not found in KTableForCondition")

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val inConditionExprMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForConditionSymbol.requiredFunction("inConditionExpr", "inConditionExpr method not found in KTableForCondition")

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val betweenConditionExprMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForConditionSymbol.requiredFunction("betweenConditionExpr", "betweenConditionExpr method not found in KTableForCondition")

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val andExprMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForConditionSymbol.requiredFunction("andExpr", "andExpr method not found in KTableForCondition")

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val orExprMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForConditionSymbol.requiredFunction("orExpr", "orExpr method not found in KTableForCondition")

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val existsExprMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForConditionSymbol.requiredFunction("existsExpr", "existsExpr method not found in KTableForCondition")

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val tupleExprMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForConditionSymbol.requiredFunction("tupleExpr", "tupleExpr method not found in KTableForCondition")

/**
 * sourceValueByFieldName method symbol from KTableForCondition
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val sourceValueByFieldNameMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForConditionSymbol.requiredFunction("sourceValueByFieldName", "sourceValueByFieldName method not found in KTableForCondition")

/**
 * addSortField method symbol from KTableForSort
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val addSortFieldMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForSortSymbol.requiredFunction("addSortField", "addSortField method not found in KTableForSort")

/**
 * asc method symbol from KTableForSort
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val ascMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForSortSymbol.requiredFunction("asc", "asc method not found in KTableForSort")

/**
 * desc method symbol from KTableForSort
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val descMethodSymbol: IrSimpleFunctionSymbol
    get() = kTableForSortSymbol.requiredFunction("desc", "desc method not found in KTableForSort")

/**
 * KClass class symbol (kotlin.reflect.KClass)
 */
context(context: IrPluginContext)
val kClassSymbol: IrClassSymbol
    get() = requiredClass(ClassId.topLevel(FqName("kotlin.reflect.KClass")), "kotlin.reflect.KClass not found in classpath")

/**
 * String class symbol
 */
context(context: IrPluginContext)
val stringClassSymbol: IrClassSymbol
    get() = requiredClass(StringClassId, "kotlin.String not found in classpath")

/**
 * String.plus function symbol
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
val stringPlusSymbol: IrSimpleFunctionSymbol
    get() = stringClassSymbol.requiredFunction("plus", "String.plus not found in classpath")

/**
 * getSafeValue function symbol from com.kotlinorm.utils.CommonUtil
 */
context(context: IrPluginContext)
val getSafeValueSymbol: IrSimpleFunctionSymbol
    get() = context.referenceFunctions(
        CallableId(FqName("com.kotlinorm.utils"), null, Name.identifier("getSafeValue"))
    ).firstRequired("getSafeValue function not found in com.kotlinorm.utils")

/**
 * registerKPojoFactory function symbol from com.kotlinorm.utils.KClassFactory
 */
context(context: IrPluginContext)
val registerKPojoFactorySymbol: IrSimpleFunctionSymbol
    get() = context.referenceFunctions(
        CallableId(FqName("com.kotlinorm.utils"), null, Name.identifier("registerKPojoFactory"))
    ).firstRequired("registerKPojoFactory function not found in com.kotlinorm.utils")

// ============================================================================
// Type Judgment Extension Functions
// ============================================================================

/**
 * Checks if this type is a KPojo type
 *
 * @return true if this type is or implements KPojo interface
 */
context(context: IrPluginContext)
fun IrType.isKPojoType(): Boolean {
    return classFqName == KPojoFqName || superTypes().any { it.classFqName == KPojoFqName }
}

/**
 * Gets the first type argument of this type (for generic types like List<T>)
 *
 * @return the first type argument, or null if not available
 */
fun IrType.firstTypeArgument(): IrType? {
    val simpleType = this as? IrSimpleType ?: return null
    val projection = simpleType.arguments.firstOrNull() as? IrTypeProjection ?: return null
    return projection.type
}

/**
 * Checks if this property represents a database column
 *
 * A property is considered a column if:
 * - It doesn't have @Ignore annotation
 * - It doesn't have @Cascade annotation (relationships are not columns)
 * - Its type is not a KPojo or collection of KPojo (relationships are not columns)
 * - Exception: @Serialize annotation forces it to be a column (serialized as JSON/text)
 *
 * @return true if this property represents a column field
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext)
fun IrProperty.isColumnType(): Boolean {
    // @Ignore means not a column
    if (hasAnnotation(IgnoreAnnotationFqName)) return false

    // @Cascade means it's a relationship, not a column
    if (hasAnnotation(CascadeAnnotationFqName)) {
        return false
    }
    
    // @Serialize forces it to be a column (will be serialized to JSON/text)
    if (hasAnnotation(SerializeAnnotationFqName)) {
        return true
    }
    
    // Get the property type
    val propertyType = backingField?.type ?: context.irBuiltIns.anyNType
    
    // If the type is KPojo, it's a relationship, not a column
    if (propertyType.isKPojoType()) {
        return false
    }
    
    // If it's a collection/array of KPojo, it's also a relationship
    val elementType = propertyType.firstTypeArgument()
    if (elementType?.isKPojoType() == true) {
        return false
    }
    
    // Otherwise, it's a regular column
    return true
}
