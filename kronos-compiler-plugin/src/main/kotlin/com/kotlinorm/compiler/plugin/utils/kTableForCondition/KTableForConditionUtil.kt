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

package com.kotlinorm.compiler.plugin.utils.kTableForCondition

import com.kotlinorm.compiler.helpers.dispatchReceiverArgument
import com.kotlinorm.compiler.helpers.extensionReceiver
import com.kotlinorm.compiler.helpers.extensionReceiverArgument
import com.kotlinorm.compiler.helpers.invoke
import com.kotlinorm.compiler.helpers.irCast
import com.kotlinorm.compiler.helpers.sub
import com.kotlinorm.compiler.helpers.valueArguments
import com.kotlinorm.compiler.plugin.beans.CriteriaIR
import com.kotlinorm.compiler.plugin.utils.ARRAY_OR_COLLECTION_FQ_NAMES
import com.kotlinorm.compiler.plugin.utils.CascadeAnnotationsFqName
import com.kotlinorm.compiler.plugin.utils.IgnoreAnnotationsFqName
import com.kotlinorm.compiler.plugin.utils.KPojoFqName
import com.kotlinorm.compiler.plugin.utils.KronosColumnValueType
import com.kotlinorm.compiler.plugin.utils.SerializeAnnotationsFqName
import com.kotlinorm.compiler.plugin.utils.getColumnOrValue
import com.kotlinorm.compiler.plugin.utils.getTableName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrWhenImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.name.FqName

/**
 * Generates IR for setting simple criteria.
 *
 * This function applies an IR call to set the criteria using the criteriaSetterSymbol.
 * It takes the result of building the criteria using the body of the current function.
 * The criteria are then dispatched by getting the extension receiver parameter.
 *
 * @return the IR expression for setting the criteria
 * @author OUSC
 */

context(_: IrPluginContext, builder: IrBlockBuilder)
fun updateCriteriaIr(irFunction: IrFunction) = criteriaSetterSymbol(
    builder.irGet(irFunction.parameters.extensionReceiver!!),
    builder.irGet(buildCriteria(irFunction, irFunction.body!!)!!)
)

/**
 * Builds a criteria IR variable based on the given element.
 *
 * This function recursively builds a criteria IR variable based on the given element and its children.
 * It handles different types of elements such as IrBlockBody, IrIfThenElseImpl, IrCall, IrReturn, and IrConstImpl.
 * The criteria are built based on the function name and the value arguments of the IrCall element.
 * The criteria type and not flag are determined based on the function name.
 * The column name, table name, value, and children are extracted from the element and its arguments.
 * The criteria IR variable is then returned.
 *
 * @param element The element to build the criteria IR from.
 * @param setNot Whether to set the not flag. Default is false.
 * @param noValueStrategyType The strategy to use when there is no value. Default is null.
 * @return The built criteria IR variable, or null if the element is a constant.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext, builder: IrBlockBuilder)
fun buildCriteria(
    irFunction: IrFunction,
    element: IrElement,
    setNot: Boolean = false,
    noValueStrategyType: IrExpression? = null
): IrVariable? {
    with(builder) {
        var paramName: IrExpression? = null
        var type = "ROOT"
        var not = setNot
        var value: IrExpression? = null
        val children: MutableList<IrVariable?> = mutableListOf()
        var tableName: IrExpression? = null
        var strategy = noValueStrategyType

        when (element) {
            is IrBlockBody -> {
                element.statements.forEach { statement ->
                    children.add(buildCriteria(irFunction, statement))
                }
            }

            is IrWhenImpl -> {
                type = element.funcName(setNot)
                element.branches.forEach {
                    children.add(buildCriteria(irFunction, it.condition, setNot))
                    children.add(buildCriteria(irFunction, it.result, setNot))
                }
            }

            is IrCall -> {
                val funcName = element.funcName()
                var args = element.valueArguments
                val dispatchReceiver = element.dispatchReceiverArgument
                val extensionReceiver = element.extensionReceiverArgument
                if (args.isEmpty() && dispatchReceiver != null && dispatchReceiver is IrCall) {
                    args = dispatchReceiver.arguments
                }

                if ("not" == funcName) {
                    return buildCriteria(irFunction, dispatchReceiver!!, !not)
                }

                val (conditionType, isNot) = parseConditionType(funcName)
                type = conditionType
                not = not xor isNot

                when (funcName) {
                    "isNull", "notNull" -> {
                        paramName = getColumnOrValue(extensionReceiver!!)
                        // 形如 it.<property>.isNull的写法
                        // Write like it.<property>.isNull
                        tableName = getTableName(dispatchReceiver!!.type.sub()!!.getClass()!!)
                    }

                    "lt", "gt", "le", "ge" -> {
                        if (args.isEmpty()) {
                            // 形如it.<property>.lt的写法
                            // Write like it.<property>.lt with no arguments
                            paramName = getColumnOrValue(extensionReceiver!!)
                            tableName = getTableName(dispatchReceiver!!.type.sub()!!.getClass()!!)
                            value = getValueByFieldNameSymbol(
                                irGet(irFunction.parameters.extensionReceiver!!),
                                irString(extensionReceiver.irCast<IrCall>().funcName())
                            )
                        } else {
                            // it.xxx > xx 或 xx > it.xxx
                            // it.xxx > xx or xx > it.xxx
                            val irCall = args.first()!!.irCast<IrCall>()
                            // 提供fun(a, b)形式和A.B.C形式的函数调用支持(!!属于fun(a, b))
                            // Provides support for function calls of the form fun(a, b)
                            // and of the form A.B.C (!!!). belongs to fun(a, b))
                            val (left, operator, right) = runExpressionAnalysis(
                                irCall.extensionReceiverArgument,
                                funcName,
                                irCall.valueArguments.firstOrNull() ?: args.getOrNull(1)
                            )
                            paramName = left
                            type = operator
                            value = right
                            tableName =
                                getTableName(irCall.findKronosColumn()!!.irCast<IrCall>().dispatchReceiverArgument!!)
                        }
                    }

                    "equal" -> {
                        not = not xor element.valueArguments.isEmpty()
                        val index = when {
                            args[0].isKPojo() || args[0].isKronosFunction() -> 0
                            args[1].isKPojo() || args[0].isKronosFunction() -> 1
                            else -> {
                                type = "sql"
                                value = element
                                -1
                            }
                        }
                        if (index != -1) {
                            val irCall = args[index]!!.irCast<IrCall>()
                            val (left, _, right) = runExpressionAnalysis(
                                irCall,
                                funcName,
                                args[1 - index]
                            )
                            paramName = left
                            value = right
                            tableName = getTableName(irCall.dispatchReceiverArgument!!)
                        }
                    }

                    "eq", "neq" -> {
                        if (extensionReceiver != null && extensionReceiver.isKPojo()) {
                            paramName = getColumnOrValue(extensionReceiver)
                            value = getValueByFieldNameSymbol(
                                irGet(irFunction.parameters.extensionReceiver!!),
                                irString(
                                    extensionReceiver.irCast<IrCall>().correspondingName!!.asString()
                                )
                            )
                            tableName = getTableName(extensionReceiver.irCast<IrCall>().dispatchReceiverArgument!!)
                        } else if (extensionReceiver != null) {
                            val irClass = element.extensionReceiverArgument?.type?.getClass()

                            fun generateEq(kPojo: IrClass, receiver: IrExpression, excludes: List<String>) {
                                kPojo.properties.forEach { prop ->
                                    if (prop.isColumn() && prop.name.asString() !in excludes) {
                                        children.add(
                                            buildCriteria(
                                                irFunction,
                                                ComparableEq.getter!!.symbol(
                                                    extensionReceiver,
                                                    irGet(
                                                        prop.backingField!!.type, receiver,
                                                        prop.getter!!.symbol
                                                    )
                                                ),
                                                setNot
                                            )
                                        )
                                    }
                                }
                            }

                            if (irClass?.kotlinFqName == KPojoFqName) {
                                if (extensionReceiver is IrCallImpl && extensionReceiver.irCast<IrCall>().origin == IrStatementOrigin.MINUS) {
                                    type = "AND"
                                    val (kPojoClass, kPojo, excludes) = analyzeMinusExpression(extensionReceiver.irCast<IrCall>())
                                    generateEq(kPojoClass, kPojo, excludes)
                                } else if (irClass.superTypes.any { it.classFqName == KPojoFqName }) {
                                    type = "AND"
                                    generateEq(irClass, extensionReceiver, listOf())
                                }
                            }
                        }
                    }

                    "between", "like", "regexp", "notBetween", "notLike", "notRegexp" -> {
                        paramName = getColumnOrValue(extensionReceiver!!)
                        value = if (args.isEmpty()) {
                            getValueByFieldNameSymbol(
                                irGet(irFunction.parameters.extensionReceiver!!),
                                irString(extensionReceiver.irCast<IrCall>().correspondingName!!.asString()),
                            )
                        } else {
                            getColumnOrValue(args.first())
                        }
                        tableName = getTableName(element.dispatchReceiver!!.type.sub()!!.getClass()!!)
                    }

                    "startsWith" -> {
                        val str = if (args.isEmpty()) {
                            getValueByFieldNameSymbol(
                                irGet(irFunction.parameters.extensionReceiver!!),
                                irString(extensionReceiver!!.irCast<IrCall>().correspondingName!!.asString())
                            )
                        } else {
                            getColumnOrValue(args.first())
                        }
                        paramName = getColumnOrValue(extensionReceiver!!)
                        value = stringPlusSymbol(str, irString("%"))
                        tableName = getTableName(element.dispatchReceiver!!.type.sub()!!.getClass()!!)
                    }

                    "endsWith" -> {
                        val str = if (args.isEmpty()) {
                            getValueByFieldNameSymbol(
                                irGet(irFunction.parameters.extensionReceiver!!),
                                irString(extensionReceiver!!.irCast<IrCall>().correspondingName!!.asString())
                            )
                        } else {
                            getColumnOrValue(args.first())
                        }
                        paramName = getColumnOrValue(extensionReceiver!!)
                        value = stringPlusSymbol(irString("%"), str)
                        tableName = getTableName(dispatchReceiver!!.type.sub()!!.getClass()!!)
                    }

                    "contains" -> {
                        val left = extensionReceiver ?: dispatchReceiver
                        if (left!!.type.classFqName in ARRAY_OR_COLLECTION_FQ_NAMES || left.type.superTypes()
                                .any { it.classFqName in ARRAY_OR_COLLECTION_FQ_NAMES }
                        ) {
                            tableName =
                                getTableName(args.first()!!.irCast<IrCall>().dispatchReceiver!!.type.getClass()!!)
                            // 形如 it.<property> in [1, 2, 3]的写法
                            // Write like it.<property> in listOf(1, 2, 3)
                            paramName = getColumnOrValue(args.first()!!)
                            value = left
                        } else {
                            tableName = getTableName(left.irCast<IrCall>().dispatchReceiver!!.type.getClass()!!)
                            type = "like"
                            val str = if (args.isEmpty()) {
                                paramName = getColumnOrValue(left)
                                // 形如 it.<property>.contains后面不加参数的写法
                                // Write it as it.<property>.contains with no arguments after it
                                getValueByFieldNameSymbol(
                                    irGet(irFunction.parameters.extensionReceiver!!),
                                    irString(left.irCast<IrCall>().correspondingName!!.asString())
                                )
                            } else {
                                paramName = getColumnOrValue(left)
                                // 形如 it.<property>.contains("xx")的写法
                                // Writes like it.<property>.contains("xx") or "xx" in it.<property>
                                getColumnOrValue(args.first())
                            }

                            value = if (str is IrConstImpl && str.value is String) {
                                irString("%${str.value}%")
                            } else {
                                buildContainsStrSymbol(irGet(irFunction.parameters.extensionReceiver!!), str)
                            }
                        }
                    }

                    "asSql" -> {
                        value = extensionReceiver
                    }

                    "ifNoValue" -> {
                        strategy = args.first()
                        return buildCriteria(irFunction, extensionReceiver!!, not, strategy)
                    }
                }
            }

            is IrReturn -> {
                return buildCriteria(irFunction, element.value)
            }

            is IrConstImpl -> {
                if (element.type == context.irBuiltIns.stringType) {
                    type = "sql"
                    value = element
                } else {
                    null
                }
            }

        }

        return CriteriaIR(
            paramName, type, not, value, children.filterNotNull(), tableName, strategy
        ).toIrVariable()
    }
}

context(_: IrPluginContext)
fun analyzeMinusExpression(irCall: IrCall): Triple<IrClass, IrExpression, List<String>> {
    val (kPojo, properties) = getIrMinusParent(irCall)
    return Triple(
        kPojo.type.getClass()!!,
        kPojo,
        properties
    )
}

context(_: IrPluginContext)
fun getIrMinusParent(irCall: IrCall): Pair<IrExpression, List<String>> {
    val property = listOfNotNull(
            irCall.valueArguments.find { it is IrCallImpl && it.origin == IrStatementOrigin.GET_PROPERTY }?.funcName()
        )
    val extensionReceiver = irCall.extensionReceiverArgument
    val (kPojo, properties) = if (extensionReceiver is IrCallImpl) {
        getIrMinusParent(extensionReceiver.irCast<IrCall>())
    } else {
        extensionReceiver!! to listOf()
    }
    return kPojo to (properties + property)
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val IrCall.correspondingName
    get() = symbol.owner.correspondingPropertySymbol?.owner?.name

/**
 * Returns a string representing the function name based on the IrExpression type and origin, with optional logic for setNot parameter.
 *
 * @param setNot a boolean value indicating whether to add the "not" prefix to the function name
 * @return a string representing the function name
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrExpression.funcName(setNot: Boolean = false): String {
    return when (this) {
        is IrCall -> when (origin) {
            IrStatementOrigin.EQEQ, IrStatementOrigin.EXCLEQ -> "equal"
            IrStatementOrigin.GT -> "gt"
            IrStatementOrigin.LT -> "lt"
            IrStatementOrigin.GTEQ -> "ge"
            IrStatementOrigin.LTEQ -> "le"
            else -> correspondingName?.asString() ?: symbol.owner.name.asString()
        }

        is IrWhen -> when {
            (origin == IrStatementOrigin.OROR && !setNot) || (origin == IrStatementOrigin.ANDAND && setNot) -> "OR"
            (origin == IrStatementOrigin.ANDAND && !setNot) || (origin == IrStatementOrigin.OROR && setNot) -> "AND"
            else -> origin.toString()
        }

        else -> ""
    }
}

/**
 * Finds a Kronos Column in the given IrExpression.
 *
 * This function checks if the given IrExpression is a Kronos Column. If it is, it returns the expression itself.
 * If the expression is an instance of IrBlock and its origin is SAFE_CALL, it returns null.
 * If the expression is not an instance of IrCall, it returns the expression itself.
 * If the extension receiver or the dispatch receiver of the expression is an instance of IrCall, it recursively calls this function with the receiver.
 * If none of the above conditions are met, it iterates over the value arguments of the expression. If it finds an argument that is an instance of IrCall, it recursively calls this function with the argument.
 * If no Kronos Column is found, it returns the expression itself.
 *
 * @receiver the `IrExpression` to find the Kronos Column in.
 * @return returns the found Kronos Column `IrExpression`, or null if no Kronos Column is found.
 */
context(_: IrPluginContext)
fun IrExpression.findKronosColumn(): IrExpression? {
    if (this is IrBlock && origin == IrStatementOrigin.SAFE_CALL) return null
    if (this !is IrCall) return this
    if (isKPojo()) {
        return this
    } else if (extensionReceiverArgument is IrCall) {
        return extensionReceiverArgument!!.findKronosColumn()
    } else if (dispatchReceiverArgument is IrCall) {
        return dispatchReceiverArgument!!.findKronosColumn()
    } else {
        for (arg in valueArguments) {
            if (arg is IrCall) {
                return arg.findKronosColumn()
            }
        }
        return this
    }
}

/**
 * Checks if the given IrExpression is a Kronos Column.
 *
 * This function checks if the given IrExpression is an instance of IrCallImpl and if its origin is either GET_PROPERTY or EQ.
 * If these conditions are met, it retrieves the property name from the IrExpression and finds the corresponding property in the class.
 * It then checks if any of the super types of the parent class of the property is "com.kotlinorm.interfaces.KPojo".
 *
 * @receiver the `IrExpression` to check. It can be null.
 * @return returns true if the IrExpression is a Kronos Column, false otherwise.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext)
fun IrExpression?.isKPojo(): Boolean {
    if (this == null) return false
    return (
            this is IrCallImpl &&
                    this.symbol.owner.correspondingPropertySymbol?.owner is IrProperty &&
                    this.let {
                        val propertyName = correspondingName!!.asString()
                        (
                                dispatchReceiver!!.type.getClass()!!
                                    .properties
                                    .first { it.name.asString() == propertyName }
                                    .parent as IrClass
                                ).isKPojo()
                    }
            ) || (
            this is IrPropertyReference &&
                    this.symbol.owner.parent is IrClass &&
                    (this.symbol.owner.parent as IrClass)
                        .isKPojo()
            )
}

context(_: IrPluginContext)
fun IrType.isKPojo(): Boolean {
    return superTypes().any { it.classFqName == KPojoFqName }
}

context(_: IrPluginContext)
fun IrClass.isKPojo(): Boolean {
    return superTypes.any { it.classFqName == KPojoFqName }
}

/**
 * Determines the type and value of a Kronos Column.
 *
 * This function checks if the given IrExpression is a Kronos Column. If it is, it returns a pair with the type as ColumnName and the expression itself.
 * If the function name of the expression is "value", it returns a pair with the type as Value and the expression itself.
 * Otherwise, it tries to find a Kronos Column in the expression and returns a pair with the type as ColumnName and the found Kronos Column.
 * If no Kronos Column is found, it throws an IllegalStateException.
 *
 * @receiver the `IrExpression` to check.
 * @return returns a pair with the type and value of the Kronos Column.
 * @throws IllegalStateException if no Kronos Column is found in the expression, and the function name is not "value".
 */
context(_: IrPluginContext)
fun IrExpression.columnValueGetter(): Pair<KronosColumnValueType, IrExpression> {
    return if (this.isKPojo()) {
        KronosColumnValueType.ColumnName to this
    } else if (this.funcName() == "value") {
        KronosColumnValueType.Value to this
    } else if (this.isKronosFunction()) {
        KronosColumnValueType.Function to this
    } else {
        KronosColumnValueType.ColumnName to (findKronosColumn()
            ?: throw IllegalStateException("`?.` is not supported in CriteriaBuilder. Unless using `.value to get the real expression value."))
    }
}

/**
 * Checks if the given IrExpression is a Kronos function.
 * This function checks if the IrExpression is an instance of IrCallImpl and if its extension receiver argument's type has the fully qualified name "com.kotlinorm.functions.FunctionHandler".
 * "com.kotlinorm.functions.FunctionHandler" is the marker interface for all Kronos functions.
 *
 * @return true if the IrExpression is a Kronos function, false otherwise.
 */
context(_: IrPluginContext)
fun IrExpression?.isKronosFunction(): Boolean {
    if (this == null) return false
    return this is IrCallImpl && this.extensionReceiverArgument?.type?.classFqName == FqName("com.kotlinorm.functions.FunctionHandler")
}

/**
 * For properties that are not columns, we need to check if :
 * 1. the field using @Ignore annotation
 * 2. the type is a KPojo or its super types are KPojo
 * 3. is a Collection of KPojo, such as List<KPojo>
 * 4. has Annotation `@Cascade`
 *
 * Specially, if the property is using `@Serialize` annotation, it will be treated as a column.
 * but the priority of `@Serialize` is lower than `Ignore` annotation and `@Cascade` annotation.
 */
context(context: IrPluginContext)
fun IrProperty.isColumn(
    irPropertyType: IrType = this.backingField?.type ?: context.irBuiltIns.anyNType,
    ignored: IrConstructorCall? = ignoreAnnotationValue()
): Boolean {
    if (ignored.ignore("all")) return false
    if (hasAnnotation(CascadeAnnotationsFqName)) return false
    if (hasAnnotation(SerializeAnnotationsFqName)) return true
    if (irPropertyType.isKPojo() || irPropertyType.sub()?.isKPojo() == true) return false
    return true
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrProperty.ignoreAnnotationValue(): IrConstructorCall? {
    return annotations.find { it.symbol.owner.returnType.getClass()!!.fqNameWhenAvailable == IgnoreAnnotationsFqName }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrConstructorCall?.ignore(name: String): Boolean {
    if (this == null) return false
    val action = valueArguments[0]
    if (action == null) return true
    return (
            action is IrVarargImpl &&
                    action.elements.isNotEmpty() &&
                    (action.elements.first() is IrGetEnumValueImpl) &&
                    (action.elements.first() as IrGetEnumValueImpl).symbol.owner.name.asString() == name.uppercase()
            )
}