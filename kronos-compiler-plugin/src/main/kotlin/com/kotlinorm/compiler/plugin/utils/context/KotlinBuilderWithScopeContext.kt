package com.kotlinorm.compiler.plugin.utils.context

import com.kotlinorm.compiler.helpers.Receivers
import com.kotlinorm.compiler.helpers.applyIrCall
import com.kotlinorm.compiler.helpers.irEnum
import com.kotlinorm.compiler.helpers.subType
import com.kotlinorm.compiler.helpers.valueArguments
import com.kotlinorm.compiler.plugin.beans.CriteriaIR
import com.kotlinorm.compiler.plugin.beans.FieldIR
import com.kotlinorm.compiler.plugin.beans.primaryKeyTypeSymbol
import com.kotlinorm.compiler.plugin.utils.CascadeAnnotationsFqName
import com.kotlinorm.compiler.plugin.utils.IgnoreAnnotationsFqName
import com.kotlinorm.compiler.plugin.utils.KPojoFqName
import com.kotlinorm.compiler.plugin.utils.KronosColumnValueType
import com.kotlinorm.compiler.plugin.utils.SerializeAnnotationsFqName
import com.kotlinorm.compiler.plugin.utils.extractDeclarationComment
import com.kotlinorm.compiler.plugin.utils.fieldSymbol
import com.kotlinorm.compiler.plugin.utils.getColumnName
import com.kotlinorm.compiler.plugin.utils.kTableForCondition.createCriteria
import com.kotlinorm.compiler.plugin.utils.realStartOffset
import com.kotlinorm.compiler.plugin.utils.sourceFileCache
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.sourceElement
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.name.FqName
import java.io.File
import kotlin.text.Charsets.UTF_8

open class KotlinBuilderWithScopeContext<out T : IrBuilderWithScope>(
    open val pluginContext: IrPluginContext,
    open val builder: T
) {
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
    fun IrExpression?.isKPojo(): Boolean {
        if (this == null) return false
        return (this is IrCallImpl && this.symbol.owner.correspondingPropertySymbol?.owner is IrProperty && this.let {
            val propertyName = pluginContext.withContext{ correspondingName!!.asString() }
            (dispatchReceiver!!.type.getClass()!!.properties.first { it.name.asString() == propertyName }.parent as IrClass).isKPojo()
        }) || this is IrPropertyReference && this.symbol.owner.parent is IrClass && (this.symbol.owner.parent as IrClass).isKPojo()
    }

    fun IrType.isKPojo(): Boolean {
        return superTypes().any { it.classFqName == pluginContext.KPojoFqName }
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
    fun IrExpression.findKronosColumn(): IrExpression? {
        if (this is IrBlock && origin == IrStatementOrigin.SAFE_CALL) return null
        if (this !is IrCall) return this
        if (isKPojo()) {
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

    fun IrExpression?.isKronosFunction(): Boolean {
        if (this == null) return false
        return this is IrCallImpl && this.extensionReceiver?.type?.classFqName == FqName("com.kotlinorm.functions.FunctionHandler")
    }

    fun IrExpression.setValue(property: IrProperty, value: IrExpression): IrExpression? {
        with(builder) {
            if (property.isDelegated) return null
            return if (property.setter != null) {
                applyIrCall(
                    property.setter!!.symbol, value
                ) {
                    dispatchReceiver = this@setValue
                }
            } else {
                irSetField(
                    this@setValue, property.backingField!!, value
                )
            }
        }
    }

    fun IrExpression.getValue(property: IrProperty): IrExpression {
        with(builder) {
            return if (property.getter != null) {
                applyIrCall(
                    property.getter!!.symbol
                ) {
                    dispatchReceiver = this@getValue
                }
            } else {
                irGetField(
                    this@getValue, property.backingField!!
                )
            }
        }
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
    fun IrProperty.isColumn(
        irPropertyType: IrType = this.backingField?.type ?: pluginContext.irBuiltIns.anyNType,
        ignored: IrConstructorCall? = ignoreAnnotationValue()
    ): Boolean {
        if (ignored.ignore("all")) return false
        if (hasAnnotation(CascadeAnnotationsFqName)) return false
        if (hasAnnotation(SerializeAnnotationsFqName)) return true
        if (irPropertyType.isKPojo() || irPropertyType.subType()?.isKPojo() == true) return false
        return true
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun IrProperty.ignoreAnnotationValue(): IrConstructorCall? {
        return annotations.find { it.symbol.owner.returnType.getClass()!!.fqNameWhenAvailable == IgnoreAnnotationsFqName }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun IrConstructorCall?.ignore(name: String): Boolean {
        if (this == null) return false
        val action = this.getValueArgument(0)
        if(action == null) return true
        return (action is IrVarargImpl &&
                action.elements.isNotEmpty() &&
                (action.elements.first() is IrGetEnumValueImpl) &&
                (action.elements.first() as IrGetEnumValueImpl).symbol.owner.name.asString() == name.uppercase()
                )
    }

    fun IrClass.isKPojo(): Boolean {
        with(pluginContext) {
            with(builder) {
                return superTypes.any { it.classFqName == KPojoFqName }
            }
        }
    }

    fun IrExpression.funcName(setNot: Boolean = false) = pluginContext.withContext{
        funcName(setNot = setNot)
    }

    /**
     * Retrieves the KDoc string for the current IR declaration.
     *
     * This function attempts to extract the KDoc comment associated with the current IR declaration.
     * It uses the source offsets to locate the relevant lines in the source file and then extracts
     * the comment content.
     *
     * @receiver The IR declaration for which to retrieve the KDoc string.
     * @return An IR expression containing the KDoc string, or null if no KDoc comment is found.
     */
    fun IrDeclaration.getKDocString(): IrExpression {
        val declaration = this
        with(pluginContext){
            with(builder){
                val sourceOffsets = sourceElement()
                if (sourceOffsets != null) {
                    val startOffset = sourceOffsets.startOffset
                    val endOffset = sourceOffsets.endOffset
                    val fileEntry = file.fileEntry
                    val sourceRange = fileEntry.getSourceRangeInfo(startOffset, endOffset)
                    val source = sourceFileCache.getOrPut(fileEntry.name) {
                        File(sourceRange.filePath).readLines(UTF_8)
                    }
                    val realStartOffset = realStartOffset(source, sourceRange.startLineNumber)
                    val comment = when (declaration) {
                            is IrProperty -> extractDeclarationComment(
                                source,
                                realStartOffset..sourceRange.endLineNumber
                            )

                            is IrClass -> extractDeclarationComment(
                                source,
                                sourceRange.startLineNumber..realStartOffset
                            )

                            else -> null
                        }

                    if (comment != null) {
                        return irString(comment)
                    }
                }
                return irString("")
            }
        }
    }

    internal fun IrSimpleFunctionSymbol.invoke(
        vararg values: IrExpression?,
        typeArguments: Array<IrType> = emptyArray(),
        setReceivers: Receivers.() -> Unit = { }
    ): IrFunctionAccessExpression {
        return builder.applyIrCall(this, *values, typeArguments = typeArguments, setReceivers = setReceivers)
    }

    fun IrExpression?.irFieldOrNull(): IrExpression {
        return if (this != null && this.isKPojo()) getColumnName(this) else builder.irNull()
    }

    /**
     * Converts the current object to an IrVariable by creating a criteria using the provided propertyeters.
     *
     * @return an IrVariable representing the created criteria
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun FieldIR.build(): IrExpression {
        with(pluginContext){
            with(builder){
                return applyIrCall(
                    fieldSymbol.constructors.first(),
                    columnName,
                    irString(name),
                    type,
                    irEnum(primaryKeyTypeSymbol, primaryKey),
                    dateTimeFormat ?: irNull(),
                    tableName,
                    cascade,
                    irBoolean(cascadeIsArrayOrCollection),
                    kClass,
                    ignore ?: irNull(),
                    irBoolean(isColumn),
                    columnTypeLength ?: irInt(0),
                    columnDefaultValue ?: irNull(),
                    irBoolean(nullable),
                    irBoolean(serializable),
                    kDoc
                )
            }
        }
    }
}

typealias KotlinBuilderContext = KotlinBuilderWithScopeContext<IrBuilderWithScope>

class KotlinBlockBuilderContext(
    override var pluginContext: IrPluginContext,
    override var builder: IrBlockBuilder
) : KotlinBuilderWithScopeContext<IrBlockBuilder>(pluginContext, builder){
    /**
     * Converts the current object to an IrVariable by creating a criteria using the provided parameters.
     *
     * @return an IrVariable representing the created criteria
     */
    fun CriteriaIR.toIrVariable(): IrVariable {
        return createCriteria(parameterName, type, not, value, children, tableName, noValueStrategyType)
    }
}

fun <R> IrBuilderWithScope.withBuilder(
    pluginContext: IrPluginContext,
    action: KotlinBuilderContext.() -> R
): R = action(
    KotlinBuilderContext(pluginContext, this)
)

fun <R> IrBlockBuilder.withBlock(
    pluginContext: IrPluginContext,
    action: KotlinBlockBuilderContext.() -> R
): R = action(
    KotlinBlockBuilderContext(pluginContext, this)
)