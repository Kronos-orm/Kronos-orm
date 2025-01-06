package com.kotlinorm.compiler.plugin.utils

import com.kotlinorm.compiler.helpers.applyIrCall
import com.kotlinorm.compiler.helpers.createKClassExpr
import com.kotlinorm.compiler.helpers.dispatchBy
import com.kotlinorm.compiler.helpers.findByFqName
import com.kotlinorm.compiler.helpers.irEnum
import com.kotlinorm.compiler.helpers.irListOf
import com.kotlinorm.compiler.helpers.irPairOf
import com.kotlinorm.compiler.helpers.nType
import com.kotlinorm.compiler.helpers.pairSymbol
import com.kotlinorm.compiler.helpers.referenceClass
import com.kotlinorm.compiler.helpers.subType
import com.kotlinorm.compiler.helpers.valueArguments
import com.kotlinorm.compiler.plugin.beans.FieldIR
import com.kotlinorm.compiler.plugin.utils.context.KotlinBuilderContext
import com.kotlinorm.compiler.plugin.utils.context.withContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.name.FqName

internal val IrPluginContext.fieldSymbol
    get() = referenceClass("com.kotlinorm.beans.dsl.Field")!!

internal val IrPluginContext.functionSymbol
    get() = referenceClass("com.kotlinorm.beans.dsl.FunctionField")!!

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val IrPluginContext.k2dbSymbol
    get() = referenceClass("com.kotlinorm.interfaces.KronosNamingStrategy")!!.getSimpleFunction("k2db")!!

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val IrPluginContext.fieldNamingStrategySymbol
    get() =
        KronosSymbol.getPropertyGetter("fieldNamingStrategy")!!

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val IrPluginContext.tableNamingStrategySymbol
    get() =
        KronosSymbol.getPropertyGetter("tableNamingStrategy")!!

internal val IrPluginContext.kReferenceSymbol
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


/**
 * Returns the column name of the given IrExpression.
 *
 * @param expression the [IrExpression] to get the column name from
 * @return the `IrExpression` representing the column name
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun KotlinBuilderContext.getColumnName(expression: IrExpression): IrExpression {
    with(pluginContext){
        with(builder){
            if (!expression.isKronosColumn()) {
                return expression
            }
            return when (expression) {
                is IrCall -> {
                    val propertyName = withContext{ expression.correspondingName!!.asString() }
                    val irProperty =
                        expression.dispatchReceiver!!.type.getClass()!!.properties.first { it.name.asString() == propertyName }
                    getColumnName(irProperty, propertyName)
                }

                is IrPropertyReference -> {
                    val propertyName = expression.symbol.owner.name.asString()
                    val irProperty = expression.symbol.owner
                    getColumnName(irProperty, propertyName)
                }

                else -> applyIrCall(fieldSymbol.constructors.first(), irString(""), irString(""))
            }
        }
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
fun KotlinBuilderContext.getFunctionName(expression: IrExpression): IrExpression {
    with(pluginContext){
        with(builder){
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

                else -> {
                    throw IllegalStateException("Unexpected expression type: $expression")
                }
            }
        }
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
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun KotlinBuilderContext.getColumnName(
    irProperty: IrProperty, propertyName: String = irProperty.name.asString()
): IrExpression {
    with(pluginContext) {
        with(builder) {
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
            val columnType =
                columnTypeAnnotation?.getValueArgument(0) ?: irEnum(kColumnTypeSymbol, kotlinTypeToKColumnType(propertyType))
            val tableName = getTableName(parent)
            val propKClass = irPropertyType.getClass()
            val cascadeIsArrayOrCollection = irPropertyType.superTypes().any { it.classFqName in ARRAY_OR_COLLECTION_FQ_NAMES }
            val kClass = if (irProperty.isDelegated) {
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
                    kReferenceSymbol.constructors.first(), *cascadeAnnotation.valueArguments.toTypedArray()
                )
            } else {
                irNull()
            }

            val primaryKey = when {
                primaryKeyAnnotation == null -> "not"
                (primaryKeyAnnotation.getValueArgument(0) as? IrConstImpl)?.value == true -> "identity"
                (primaryKeyAnnotation.getValueArgument(1) as? IrConstImpl)?.value == true -> "uuid"
                (primaryKeyAnnotation.getValueArgument(2) as? IrConstImpl)?.value == true -> "snowflake"
                (primaryKeyAnnotation.getValueArgument(3) as? IrConstImpl)?.value == true -> "custom"
                else -> "default"
            }

            return FieldIR(
                columnName = columnName,
                name = propertyName,
                type = columnType,
                primaryKey = primaryKey,
                dateTimeFormat = dateTimeFormatAnnotation?.getValueArgument(0),
                tableName = tableName,
                cascade = kCascade,
                cascadeIsArrayOrCollection = cascadeIsArrayOrCollection,
                kClass = kClass,
                ignore = ignoreAnnotation?.getValueArgument(0),
                isColumn = irProperty.isColumn(irPropertyType),
                columnTypeLength = columnTypeAnnotation?.getValueArgument(1),
                columnDefaultValue = defaultValueAnnotation?.getValueArgument(0),
                nullable = notNullAnnotation == null && primaryKeyAnnotation == null,
                serializable = serializableAnnotation != null,
                kDoc = irProperty.getKDocString()
            ).build()
        }
    }
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
fun KotlinBuilderContext.getColumnOrValue(expression: IrExpression?): IrExpression? {
    if (expression == null) return null
    val (type, expr) = expression.columnValueGetter()
    return when (type) {
        KronosColumnValueType.Value -> expr
        KronosColumnValueType.ColumnName -> getColumnName(expr)
        KronosColumnValueType.Function -> getFunctionName(expr)
    }
}

/**
 * Returns the table name associated with the given IrExpression.
 *
 * @param expression the [IrExpression] to retrieve the table name from
 * @return the `IrExpression` representing the table name
 * @throws IllegalStateException if the expression type is unexpected
 */
fun KotlinBuilderContext.getTableName(expression: IrExpression): IrExpression {
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
fun KotlinBuilderContext.getTableName(irClass: IrClass): IrExpression {
    with(pluginContext){
        with(builder){
            val tableAnnotation = irClass.annotations.findByFqName(TableAnnotationsFqName)
            return tableAnnotation?.getValueArgument(0) ?: applyIrCall(
                k2dbSymbol, irString(irClass.name.asString())
            ) {
                dispatchBy(applyIrCall(tableNamingStrategySymbol) { dispatchBy(irGetObject(KronosSymbol)) })
            }
        }
    }
}