@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm.utils.codec

import com.kotlinorm.enums.ValueCodecOrigin
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.exceptions.MissingEnumMetadata
import com.kotlinorm.interfaces.KronosPhysicalValue
import com.kotlinorm.interfaces.KronosValueCodecs
import com.kotlinorm.interfaces.StaticEnumValueCodec
import com.kotlinorm.interfaces.StaticValueCodecContext
import com.kotlinorm.interfaces.StaticValueCodecException
import com.kotlinorm.interfaces.ValueCodecContext
import com.kotlinorm.interfaces.toLogicalScalar
import com.kotlinorm.interfaces.ScalarCoercionMode
import com.kotlinorm.interfaces.toStaticTypeRef
import com.kotlinorm.utils.GeneratedTypeMetadataSnapshot
import com.kotlinorm.utils.generatedTypeMetadata
import com.kotlinorm.utils.platformIsEnumClass
import kotlin.reflect.KClass

internal actual val builtInCodecs: List<RegistryCodec> = listOf(PortableTemporalValueCodec, NativeValueCodec)
internal actual val builtInEncodeCoercionCodecs: List<RegistryCodec> = listOf(PortableTemporalValueCodec, NativeEncodeCoercionCodec)

/** Native physical values use the shared scalar/temporal/enum codecs after common dispatch. */
private object NativeValueCodec : RegistryCodec {
    override val description = "Native built-in ValueCodec"

    override fun supports(value: Any, request: ValueConversionRequest, context: ValueCodecContext): Boolean {
        if (request.direction == ValueCodecDirection.ENCODE) {
            return value is Enum<*> || value is KronosPhysicalValue ||
                (request.effectiveTargetType().classifier in scalarClasses && value.isBindableScalar())
        }
        if (request.targetType.accepts(value)) return false
        if (request.isEnumTarget()) return true
        return request.targetType.classifier in scalarClasses &&
            (!request.strict || (request.origin == ValueCodecOrigin.DATABASE &&
                value is Number && request.targetType.classifier in numericClasses))
    }

    override fun convert(value: Any, request: ValueConversionRequest, context: ValueCodecContext,
        generatedTypes: GeneratedTypeMetadataSnapshot?): Any? {
        val static = request.staticContext(context)
        return if (request.direction == ValueCodecDirection.ENCODE) {
            encode(value, request, static)
        } else {
            decode(value, request, static, generatedTypes)
        }
    }

    private fun encode(value: Any, request: ValueConversionRequest, context: StaticValueCodecContext): Any? {
        val logical = when {
            request.targetType.accepts(value) -> value
            else -> value.toLogicalScalar(context, ScalarCoercionMode.COERCING)
        }
        val encoded = KronosValueCodecs.encodeBuiltIn(logical, context).toRuntimeValue()
        val physical = request.physicalTargetType
        if (encoded == null || physical == null || physical.accepts(encoded)) return encoded
        return encoded.toLogicalScalar(context.copy(targetType = physical.toStaticTypeRef(request.physicalTargetTypeInfo), targetTypeInfo = request.physicalTargetTypeInfo), ScalarCoercionMode.COERCING)
    }

    private fun decode(value: Any, request: ValueConversionRequest, context: StaticValueCodecContext,
        generatedTypes: GeneratedTypeMetadataSnapshot?): Any? {
        val enum = if (request.isEnumTarget()) {
            (generatedTypes ?: generatedTypeMetadata()).enumMetadata(request.targetType)?.let { metadata ->
                object : StaticEnumValueCodec {
                    override val type = context.targetType
                    override val entryNames = metadata.entryNames
                    override fun decode(name: String): Enum<*>? = metadata.factory.create(name)
                }
            } ?: throw MissingEnumMetadata(request.direction, request.origin, request.sourceType, null,
                request.targetType, request.field?.name, request.field?.columnName,
                request.valueName, request.dialect?.family?.name, request.batchIndex)
        } else null
        if (enum == null && request.targetType.classifier in scalarClasses) {
            return value.toLogicalScalar(context,
                if (request.strict) ScalarCoercionMode.EXACT else ScalarCoercionMode.COERCING)
        }
        return try {
            KronosValueCodecs.decode(value.toPhysicalValue(), context, enum, dispatchUserCodecs = false)
        } catch (cause: StaticValueCodecException) {
            throw request.failure(cause.detail, cause)
        }
    }
}

/** Input coercion never broadens arbitrary logical objects or serialized values. */
private object NativeEncodeCoercionCodec : RegistryCodec by NativeValueCodec {
    override fun supports(value: Any, request: ValueConversionRequest, context: ValueCodecContext): Boolean =
        request.effectiveTargetType().classifier in scalarClasses &&
            value.isBindableScalar()
}

private val numericClasses = setOf(Byte::class, Short::class, Int::class, Long::class, Float::class, Double::class)
private val scalarClasses = numericClasses + setOf(Boolean::class, Char::class, String::class, ByteArray::class)

private fun ValueConversionRequest.isEnumTarget(): Boolean = targetTypeInfo?.enumKey != null ||
    (targetType.classifier as? KClass<*>)?.let(::platformIsEnumClass) == true

internal fun KronosPhysicalValue.toRuntimeValue(): Any? = when (this) {
    KronosPhysicalValue.Null -> null
    is KronosPhysicalValue.Integer -> value
    is KronosPhysicalValue.Real -> value
    is KronosPhysicalValue.Text -> value
    is KronosPhysicalValue.Blob -> bytes
}

internal fun Any?.toPhysicalValue(): KronosPhysicalValue = when (this) {
    null -> KronosPhysicalValue.Null
    is KronosPhysicalValue -> this
    is Boolean -> KronosPhysicalValue.Integer(if (this) 1 else 0)
    is Byte -> KronosPhysicalValue.Integer(toLong())
    is Short -> KronosPhysicalValue.Integer(toLong())
    is Int -> KronosPhysicalValue.Integer(toLong())
    is Long -> KronosPhysicalValue.Integer(this)
    is Float -> KronosPhysicalValue.Real(toDouble())
    is Double -> KronosPhysicalValue.Real(this)
    is String -> KronosPhysicalValue.Text(this)
    is Char -> KronosPhysicalValue.Text(toString())
    is ByteArray -> KronosPhysicalValue.Blob(this)
    else -> error("Unsupported physical database value ${this::class.simpleName}")
}

internal actual fun Any.isBindableScalar(): Boolean = this is Boolean || this is Byte || this is Short ||
    this is Int || this is Long || this is Float || this is Double || this is Char || this is String || this is ByteArray

internal actual fun Any.isTemporalRuntimeValue(): Boolean = isPortableTemporalRuntimeValue()
