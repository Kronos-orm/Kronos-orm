package com.kotlinorm.plugins.utils

import com.kotlinorm.plugins.helpers.applyIrCall
import com.kotlinorm.plugins.helpers.findByFqName
import com.kotlinorm.plugins.helpers.referenceClass
import com.kotlinorm.plugins.helpers.referenceFunctions
import com.kotlinorm.plugins.helpers.subType
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import java.io.File
import kotlin.text.Charsets.UTF_8

context(IrPluginContext)
internal val fieldSymbol
    get() = referenceClass("com.kotlinorm.beans.dsl.Field")!!

context(IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val IrCall.correspondingName
    get() = symbol.owner.correspondingPropertySymbol?.owner?.name

context(IrPluginContext)
internal val fieldK2dbSymbol
    get() = referenceFunctions("com.kotlinorm.utils", "fieldK2db").first()

context(IrPluginContext)
internal val tableK2dbSymbol
    get() = referenceFunctions("com.kotlinorm.utils", "tableK2db").first()

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
val CascadeSelectIgnoreAnnotationsFqName = FqName("com.kotlinorm.annotations.CascadeSelectIgnore")
val SerializableAnnotationsFqName = FqName("com.kotlinorm.annotations.Serializable")
val DefaultValueAnnotationsFqName = FqName("com.kotlinorm.annotations.Default")
val NotNullAnnotationsFqName = FqName("com.kotlinorm.annotations.NotNull")


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
    irProperty: IrProperty,
    propertyName: String = irProperty.name.asString()
): IrExpression {
    val parent = irProperty.parent as IrClass
    val columnAnnotation =
        irProperty.annotations.findByFqName(ColumnAnnotationsFqName)
    val columnName =
        columnAnnotation?.getValueArgument(0) ?: applyIrCall(fieldK2dbSymbol, irString(propertyName))

    val columnTypeAnnotation =
        irProperty.annotations.findByFqName(ColumnTypeAnnotationsFqName)
    val irPropertyType = irProperty.backingField?.type ?: irBuiltIns.anyNType
    val propertyType = irPropertyType.classFqName!!.asString()
    val columnType =
        columnTypeAnnotation?.getValueArgument(0) ?: getKColumnType(propertyType)
    val columnTypeLength =
        columnTypeAnnotation?.getValueArgument(1) ?: irInt(0)
    val columnDefaultValue =
        irProperty.annotations.findByFqName(DefaultValueAnnotationsFqName)?.getValueArgument(0) ?: irNull()
    val tableName = getTableName(parent)
    val selectIgnoreAnnotation = irProperty.annotations.findByFqName(CascadeSelectIgnoreAnnotationsFqName)
    val cascadeAnnotation = irProperty.annotations.findByFqName(CascadeAnnotationsFqName)
    var cascadeTypeKClassName = irPropertyType.getClass()!!.classId!!.asFqNameString()
    if (cascadeTypeKClassName.startsWith("kotlin.collections")) {
        cascadeTypeKClassName = irPropertyType.subType()!!.getClass()!!.classId!!.asFqNameString()
    }
    if (irProperty.isDelegated) {
        cascadeTypeKClassName = ""
    }
    val kCascade = if (cascadeAnnotation != null) {
        applyIrCall(
            kReferenceSymbol.constructors.first(),
            *cascadeAnnotation.valueArguments.toTypedArray()
        )
    } else {
        irNull()
    }

    val primaryKeyAnnotation =
        irProperty.annotations.findByFqName(PrimaryKeyAnnotationsFqName)
    val identity = primaryKeyAnnotation?.getValueArgument(0) ?: irBoolean(false)
    val isColumn = irBoolean(irProperty.isColumn(irPropertyType))

    val columnNotNull =
        irBoolean(null == irProperty.annotations.findByFqName(NotNullAnnotationsFqName) && null == primaryKeyAnnotation)

    return applyIrCall(
        fieldSymbol.constructors.first(),
        columnName,
        irString(propertyName),
        columnType,
        irBoolean(primaryKeyAnnotation != null),
        irProperty.annotations.findByFqName(DateTimeFormatAnnotationsFqName)?.getValueArgument(0),
        when (tableName) {
            is IrCall -> applyIrCall(
                fieldK2dbSymbol,
                irString((tableName.valueArguments[0] as IrConst<*>).value.toString())
            )

            else -> irString((tableName as IrConst<*>).value.toString())
        },
        kCascade,
        irString(cascadeTypeKClassName),
        irBoolean(selectIgnoreAnnotation != null),
        isColumn,
        columnTypeLength,
        columnDefaultValue,
        identity,
        columnNotNull,
        irBoolean(irProperty.hasAnnotation(SerializableAnnotationsFqName)),
        irProperty.getKDocString()
    )
}

/**
 * Kronos Column Value Type
 *
 * Enum class for the kronos column value type
 */
enum class KronosColumnValueType {
    Value, ColumnName
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
    } else {
        KronosColumnValueType.ColumnName to
                (findKronosColumn()
                    ?: throw IllegalStateException("`?.` is not supported in CriteriaBuilder. Unless using `.value to get the real expression value."))
    }
}

/**
 * Checks if the given IrExpression is a Kronos Column.
 *
 * This function checks if the given IrExpression is an instance of IrCallImpl and if its origin is either GET_PROPERTY or EQ.
 * If these conditions are met, it retrieves the property name from the IrExpression and finds the corresponding property in the class.
 * It then checks if any of the super types of the parent class of the property is "com.kotlinorm.beans.dsl.KPojo".
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
        is IrGetValue, is IrCall -> expression.type.getClass()
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
        tableK2dbSymbol, irString(
            irClass.name.asString()
        )
    )
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
    return hasAnnotation(SerializableAnnotationsFqName) ||
            (!hasAnnotation(CascadeAnnotationsFqName) &&
                    !irPropertyType.isKronosColumn() && irPropertyType.subType()?.isKronosColumn() != true)
}

private val sourceFileCache: LRUCache<String, List<String>> = LRUCache(128)
context(IrBuilderWithScope, IrPluginContext)
fun IrProperty.getKDocString(): IrExpression {
    val sourceOffsets = sourceElement()
    if (sourceOffsets != null) {
        val startOffset = sourceOffsets.startOffset
        val endOffset = sourceOffsets.endOffset
        val fileEntry = file.fileEntry
        val sourceRange = fileEntry.getSourceRangeInfo(startOffset, endOffset)
        val source = sourceFileCache.getOrPut(fileEntry.name) {
            File(sourceRange.filePath).readLines(UTF_8)
        }
        val comment =
            extractPropertyComment(source, sourceRange.startLineNumber..sourceRange.endLineNumber)
        if (comment != null) {
            return irString(comment)
        }
    }
    return irNull()
}

/**
 * Extract the comment content within the specified range.
 *
 * This function will extract single-line or multi-line comments within the specified range from the given list of lines.
 * If no comments are found within the specified range, the function will search upwards to the beginning of the file to find possible comments.
 *
 * 提取指定范围内的注释内容。
 *
 * 此函数会从给定的行列表中提取位于指定范围内的单行或多行注释。
 * 如果在指定范围内没有找到注释，函数将向上查找直到文件的开头，以获取可能的注释。
 *
 * @param lines a list of code lines 包含代码行的列表
 * @param range the range of lines to check 指定要检查的行范围
 * @return the extracted comment content, or null if no comment is found 找到的注释内容，如果没有找到则返回 null
 */
fun extractPropertyComment(lines: List<String>, range: IntRange): String? {
    val startIndex = range.first
    val endIndex = range.last

    var comment: String? = null

    // Find single-line or multi-line comments within the specified range
    // 在指定范围内查找单行或多行注释
    for (i in startIndex..endIndex) {
        val line = lines.getOrNull(i)?.trim() ?: continue

        // Extract single-line comments
        // 提取单行注释
        val singleLineComment = line.substringAfter("//", "").substringBefore("//").trim()
        // Extract multi-line comments
        // 查找多行注释的起始和结束位置
        val multiLineCommentStart = line.indexOf("/*")
        val multiLineCommentEnd = line.indexOf("*/")

        // If a single-line comment is found
        // 如果找到单行注释
        if (singleLineComment.isNotEmpty()) {
            comment = singleLineComment
            break
        }

        // If a multi-line comment is found
        // 如果找到多行注释
        else if (multiLineCommentStart != -1 && multiLineCommentEnd != -1) {
            comment = line.substring(multiLineCommentStart + 2, multiLineCommentEnd).trim { it == '*' || it == ' ' }
            break
        }
    }

    // If no comments are found within the specified range, search upwards
    // 如果在指定范围内没有找到注释，向上查找
    if (comment == null) {
        var multiLineCommentFlag = false
        var singleLineCommentFlag = false
        for (i in (startIndex - 1) downTo 0) {
            val line = lines.getOrNull(i)?.trim() ?: continue
            val singleLineComment = line.substringAfter("//", "").trim()
            val multiLineCommentStart = line.indexOf("/*")
            val multiLineCommentEnd = line.indexOf("*/")

            // Handle single-line comments
            // 处理单行注释
            if (singleLineComment.isNotEmpty()) {
                if (singleLineCommentFlag) {
                    comment = singleLineComment + comment
                } else {
                    comment = singleLineComment
                    singleLineCommentFlag = true
                }
                continue
            }
            // Handle multi-line comments
            // 处理多行注释
            else if (multiLineCommentStart != -1 && multiLineCommentEnd != -1) {
                comment = line.substring(multiLineCommentStart + 2, multiLineCommentEnd).trim { it == '*' || it == ' ' }
                break
            }
            // Handle multi-line comments that start but do not end
            // 处理多行注释开始但未结束的情况
            else if (multiLineCommentStart != -1 && multiLineCommentFlag) {
                comment = line.substring(multiLineCommentStart + 2).trim { it == '*' || it == ' ' } + comment
                multiLineCommentFlag = true
                continue
            }
            // Handle multi-line comments that end but do not start
            // 处理多行注释结束的情况
            else if (multiLineCommentEnd != -1) {
                comment = line.substring(0, multiLineCommentEnd).trim { it == '*' || it == ' ' }
                multiLineCommentFlag = true
                continue
            }
            // Handle non-empty lines
            // 处理非空行
            else if (line.isNotBlank()) {
                if (multiLineCommentFlag) {
                    comment = line.trim { it == '*' || it == ' ' } + comment
                    continue
                }
                break
            }
        }
    }
    return comment
}