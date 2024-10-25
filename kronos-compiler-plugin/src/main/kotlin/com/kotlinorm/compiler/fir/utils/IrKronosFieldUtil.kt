package com.kotlinorm.compiler.fir.utils

import com.kotlinorm.compiler.fir.beans.FieldIR
import com.kotlinorm.compiler.fir.utils.kTableForSelect.irFieldOrNull
import com.kotlinorm.compiler.helpers.applyIrCall
import com.kotlinorm.compiler.helpers.createKClassExpr
import com.kotlinorm.compiler.helpers.dispatchBy
import com.kotlinorm.compiler.helpers.findByFqName
import com.kotlinorm.compiler.helpers.irListOf
import com.kotlinorm.compiler.helpers.irPairOf
import com.kotlinorm.compiler.helpers.nType
import com.kotlinorm.compiler.helpers.pairSymbol
import com.kotlinorm.compiler.helpers.referenceClass
import com.kotlinorm.compiler.helpers.subType
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.name.FqName

context(IrPluginContext)
internal val fieldSymbol
    get() = referenceClass("com.kotlinorm.beans.dsl.Field")!!

context(IrPluginContext)
internal val functionSymbol
    get() = referenceClass("com.kotlinorm.beans.dsl.FunctionField")!!

context(IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val IrCall.correspondingName
    get() = symbol.owner.correspondingPropertySymbol?.owner?.name

context(IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val k2dbSymbol
    get() = referenceClass("com.kotlinorm.interfaces.KronosNamingStrategy")!!.getSimpleFunction("k2db")!!

context(IrBuilderWithScope, IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val fieldNamingStrategySymbol
    get() =
        KronosSymbol.getPropertyGetter("fieldNamingStrategy")!!

context(IrBuilderWithScope, IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val tableNamingStrategySymbol
    get() =
        KronosSymbol.getPropertyGetter("tableNamingStrategy")!!

context(IrPluginContext)
internal val kReferenceSymbol
    get() = referenceClass("com.kotlinorm.beans.dsl.KCascade")!!

val TableAnnotationsFqName = FqName("com.kotlinorm.annotations.Table")
val TableIndexAnnotationsFqName = FqName("com.kotlinorm.annotations.TableIndex")
val PrimaryKeyAnnotationsFqName = FqName("com.kotlinorm.annotations.PrimaryKey")
val ColumnAnnotationsFqName = FqName("com.kotlinorm.annotations.Column")
val ColumnTypeAnnotationsFqName = FqName("com.kotlinorm.annotations.ColumnType")
val DateTimeFormatAnnotationsFqName = FqName("com.kotlinorm.annotations.DateTimeFormat")
val CascadeAnnotationsFqName = FqName("com.kotlinorm.annotations.Cascade")
val IgnoreAnnotationsFqName = FqName("com.kotlinorm.annotations.Ignore")
val SerializableAnnotationsFqName = FqName("com.kotlinorm.annotations.Serializable")
val DefaultValueAnnotationsFqName = FqName("com.kotlinorm.annotations.Default")
val NotNullAnnotationsFqName = FqName("com.kotlinorm.annotations.NotNull")
val KTableFunctionFqName = FqName("com.kotlinorm.beans.dsl.KSqlFunction")


/**
 * Returns the column name of the given IrExpression.
 *
 * @param expression the [IrExpression] to get the column name from
 * @return the `IrExpression` representing the column name
 */
context(IrBuilderWithScope, IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun getColumnName(expression: IrExpression): IrExpression {
    if (!expression.isKronosColumn()) {
        return expression
    }
    return when (expression) {
        is IrCall -> {
            val propertyName = expression.correspondingName!!.asString()
            val irProperty =
                expression.dispatchReceiver!!.type.getClass()!!.properties.first { it.name.asString() == propertyName }
            getColumnName(irProperty, propertyName)
        }

        else -> applyIrCall(fieldSymbol.constructors.first(), irString(""), irString(""))
    }
}

context(IrBuilderWithScope, IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun getFunctionName(expression: IrExpression): IrExpression {
    return when (expression) {
        is IrCall -> {
            val args = mutableListOf<IrExpression>()
            expression.valueArguments.forEach {
                if (it is IrVarargImpl) {
                    args.addAll(it.elements.map { element ->
                        irPairOf(
                            fieldSymbol.nType,
                            irBuiltIns.anyNType,
                            (element as IrExpression).irFieldOrNull() to element
                        )
                    })
                } else {
                    args.add(
                        irPairOf(
                            fieldSymbol.nType,
                            irBuiltIns.anyNType,
                            it.irFieldOrNull() to it
                        )
                    )
                }
            }
            applyIrCall(
                functionSymbol.constructors.first(),
                irString(expression.funcName()),
                irListOf(
                    pairSymbol.owner.returnType,
                    args
                ),
            )
        }

        else -> throw IllegalStateException("Unexpected expression type: $expression")
    }
}

val ARRAY_OR_COLLECTION_FQ_NAMES = arrayOf(
    FqName("kotlin.collections.Collection"),
    FqName("kotlin.collections.Iterator"),
    FqName("kotlin.Array"),
    FqName("kotlin.IntArray"),
    FqName("kotlin.LongArray"),
    FqName("kotlin.ShortArray"),
    FqName("kotlin.DoubleArray"),
    FqName("kotlin.FloatArray"),
    FqName("kotlin.CharArray"),
    FqName("kotlin.ByteArray"),
    FqName("kotlin.BooleanArray"),
)

/**
 * Returns the column name of the given IrProperty.
 *
 * @param irProperty the [IrProperty] to get the column name from
 * @param propertyName the name of the property (default: the name of the IrProperty)
 * @return the `IrExpression` representing the column name
 */
context(IrBuilderWithScope, IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun getColumnName(
    irProperty: IrProperty, propertyName: String = irProperty.name.asString()
): IrExpression {
    val parent = irProperty.parent as IrClass
    val annotations = irProperty.annotations

    // detect annotations
    var columnAnnotation: IrConstructorCall? = null // @Column
    var columnTypeAnnotation: IrConstructorCall? = null // @ColumnType
    var cascadeAnnotation: IrConstructorCall? = null // @Cascade
    var ignoreAnnotation: IrConstructorCall? = null // @Ignore
    var defaultValueAnnotation: IrConstructorCall? = null // @DefaultValue
    var primaryKeyAnnotation: IrConstructorCall? = null // @PrimaryKey
    var dateTimeFormatAnnotation: IrConstructorCall? = null // @DateTimeFormat
    var notNullAnnotation: IrConstructorCall? = null // @NotNull
    var serializableAnnotation: IrConstructorCall? = null // @Serializable

    annotations.forEach {
        when (it.symbol.owner.returnType.getClass()!!.fqNameWhenAvailable) {
            ColumnTypeAnnotationsFqName -> columnTypeAnnotation = it
            ColumnAnnotationsFqName -> columnAnnotation = it
            CascadeAnnotationsFqName -> cascadeAnnotation = it
            IgnoreAnnotationsFqName -> ignoreAnnotation = it
            DefaultValueAnnotationsFqName -> defaultValueAnnotation = it
            PrimaryKeyAnnotationsFqName -> primaryKeyAnnotation = it
            DateTimeFormatAnnotationsFqName -> dateTimeFormatAnnotation = it
            NotNullAnnotationsFqName -> notNullAnnotation = it
            SerializableAnnotationsFqName -> serializableAnnotation = it
        }
    }

    val columnName = columnAnnotation?.getValueArgument(0) ?: applyIrCall(
        k2dbSymbol, irString(propertyName)
    ) {
        dispatchBy(applyIrCall(fieldNamingStrategySymbol) { dispatchBy(irGetObject(KronosSymbol)) })
    }
    val irPropertyType = irProperty.backingField?.type ?: irBuiltIns.anyNType
    val propertyType = irPropertyType.classFqName!!.asString()
    val columnType = columnTypeAnnotation?.getValueArgument(0) ?: getKColumnType(propertyType)
    val tableName = getTableName(parent)
    val propKClass = irPropertyType.getClass()
    val cascadeIsArrayOrCollection = irPropertyType.superTypes().any { it.classFqName in ARRAY_OR_COLLECTION_FQ_NAMES }
    val cascadeTypeKClass = if (irProperty.isDelegated) {
        irNull()
    } else {
        createKClassExpr(
            (if (cascadeIsArrayOrCollection) {
                irPropertyType.subType()!!.getClass()
            } else {
                propKClass
            }!!).symbol
        )
    }

    val kCascade = if (cascadeAnnotation != null) {
        applyIrCall(
            kReferenceSymbol.constructors.first(), *cascadeAnnotation!!.valueArguments.toTypedArray()
        )
    } else {
        irNull()
    }

    return FieldIR(
        columnName = columnName,
        name = propertyName,
        type = columnType,
        primaryKey = primaryKeyAnnotation != null,
        dateTimeFormat = dateTimeFormatAnnotation?.getValueArgument(0),
        tableName = tableName,
        cascade = kCascade,
        cascadeIsArrayOrCollection = cascadeIsArrayOrCollection,
        cascadeTypeKClass = cascadeTypeKClass,
        ignore = ignoreAnnotation?.getValueArgument(0),
        isColumn = irProperty.isColumn(irPropertyType),
        columnTypeLength = columnTypeAnnotation?.getValueArgument(1),
        columnDefaultValue = defaultValueAnnotation?.getValueArgument(0),
        identity = (primaryKeyAnnotation?.getValueArgument(0) as? IrConstImpl<*>)?.value == true,
        nullable = notNullAnnotation == null && primaryKeyAnnotation == null,
        serializable = serializableAnnotation != null,
        kDoc = irProperty.getKDocString()
    ).build()
}

/**
 * Kronos Column Value Type
 *
 * Enum class for the kronos column value type
 */
enum class KronosColumnValueType {
    Value, ColumnName, Function
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
context(IrBlockBuilder, IrPluginContext)
fun IrExpression.findKronosColumn(): IrExpression? {
    if (this is IrBlock && origin == IrStatementOrigin.SAFE_CALL) return null
    if (this !is IrCall) return this
    if (isKronosColumn()) {
        return this
    } else if (extensionReceiver is IrCall) {
        return extensionReceiver!!.findKronosColumn()
    } else if (dispatchReceiver is IrCall) {
        return dispatchReceiver!!.findKronosColumn()
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
 * Determines the type and value of a Kronos Column.
 *
 * This function checks if the given IrExpression is a Kronos Column. If it is, it returns a pair with the type as ColumnName and the expression itself.
 * If the function name of the expression is "value", it returns a pair with the type as Value and the expression itself.
 * Otherwise, it tries to find a Kronos Column in the expression and returns a pair with the type as ColumnName and the found Kronos Column.
 * If no Kronos Column is found, it throws an IllegalStateException.
 *
 * @receiver the `IrExpression` to check.
 * @return returns a pair with the type and value of the Kronos Column.
 * @throws IllegalStateException if no Kronos Column is found in the expression and the function name is not "value".
 */
context(IrBlockBuilder, IrPluginContext)
fun IrExpression.columnValueGetter(): Pair<KronosColumnValueType, IrExpression> {
    return if (this.isKronosColumn()) {
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
 * Checks if the given IrExpression is a Kronos Column.
 *
 * This function checks if the given IrExpression is an instance of IrCallImpl and if its origin is either GET_PROPERTY or EQ.
 * If these conditions are met, it retrieves the property name from the IrExpression and finds the corresponding property in the class.
 * It then checks if any of the super types of the parent class of the property is "com.kotlinorm.interfaces.KPojo".
 *
 * @receiver the `IrExpression` to check. It can be null.
 * @return returns true if the IrExpression is a Kronos Column, false otherwise.
 */
context(IrBuilderWithScope, IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrExpression?.isKronosColumn(): Boolean {
    if (this == null) return false
    return this is IrCallImpl && this.origin in listOf(
        IrStatementOrigin.GET_PROPERTY, IrStatementOrigin.EQ
    ) && this.let {
        val propertyName = correspondingName!!.asString()
        (dispatchReceiver!!.type.getClass()!!.properties.first { it.name.asString() == propertyName }.parent as IrClass).isKronosColumn()
    }
}

context(IrBuilderWithScope, IrPluginContext)
fun IrExpression?.isKronosFunction(): Boolean {
    if (this == null) return false
    return this is IrCallImpl && this.extensionReceiver?.type?.classFqName == FqName("com.kotlinorm.functions.FunctionHandler")
}

context(IrBuilderWithScope, IrPluginContext)
fun IrClass.isKronosColumn(): Boolean {
    return superTypes.any { it.classFqName == KPojoFqName }
}

context(IrBuilderWithScope, IrPluginContext)
fun IrType.isKronosColumn(): Boolean {
    return superTypes().any { it.classFqName == KPojoFqName }
}

/**
 * Retrieves the column or value from the given IrExpression.
 *
 * This function checks if the given IrExpression is null. If it is, it returns null.
 * Otherwise, it determines the type and value of the IrExpression using the columnValueGetter function.
 * If the type is Value, it returns the expression itself.
 * If the type is ColumnName, it retrieves the column name from the expression using the getColumnName function.
 *
 * @param expression the IrExpression to retrieve the column or value from. It can be null.
 * @return returns the column or value from the `IrExpression`, or null if the IrExpression is null.
 */
context(IrBlockBuilder, IrPluginContext)
fun getColumnOrValue(expression: IrExpression?): IrExpression? {
    if (expression == null) return null
    val (type, expr) = expression.columnValueGetter()
    return when (type) {
        KronosColumnValueType.Value -> expr
        KronosColumnValueType.ColumnName -> getColumnName(expr)
        KronosColumnValueType.Function -> getFunctionName(expr)
    }
}

/**
 * Returns a string representing the function name based on the IrExpression type and origin, with optional logic for setNot parameter.
 *
 * @param setNot a boolean value indicating whether to add the "not" prefix to the function name
 * @return a string representing the function name
 */
context(IrPluginContext)
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
 * Returns the table name associated with the given IrExpression.
 *
 * @param expression the [IrExpression] to retrieve the table name from
 * @return the `IrExpression` representing the table name
 * @throws IllegalStateException if the expression type is unexpected
 */
context(IrBuilderWithScope, IrPluginContext)
fun getTableName(expression: IrExpression): IrExpression {
    val irClass = when (expression) {
        is IrGetValue, is IrCall, is IrGetObjectValue -> expression.type.getClass()
        else -> throw IllegalStateException("Unexpected expression type: $expression")
    }!!
    return getTableName(irClass)
}

/**
 * Returns the table name associated with the given IrClass.
 *
 * @param irClass the [IrClass] to retrieve the table name from
 * @return the `IrExpression` representing the table name
 * @throws IllegalStateException if the table annotation is not found
 */
context(IrBuilderWithScope, IrPluginContext)
fun getTableName(irClass: IrClass): IrExpression {
    val tableAnnotation = irClass.annotations.findByFqName(TableAnnotationsFqName)
    return tableAnnotation?.getValueArgument(0) ?: applyIrCall(
        k2dbSymbol, irString(irClass.name.asString())
    ) {
        dispatchBy(applyIrCall(tableNamingStrategySymbol) { dispatchBy(irGetObject(KronosSymbol)) })
    }
}

/**
 * for custom serialization, the property is a column if it has a `@Serializable` annotation
 * for properties that are not columns, we need to check if :
 * 1. the type is a KPojo
 * 2. has a KPojo in its super types
 * 3. is a Collection of KPojo
 * 4. has Annotation `@Cascade`
 */
context(IrBuilderWithScope, IrPluginContext)
fun IrProperty.isColumn(irPropertyType: IrType = this.backingField?.type ?: irBuiltIns.anyNType): Boolean {
    return hasAnnotation(SerializableAnnotationsFqName) || (!hasAnnotation(CascadeAnnotationsFqName) && !irPropertyType.isKronosColumn() && irPropertyType.subType()
        ?.isKronosColumn() != true)
}