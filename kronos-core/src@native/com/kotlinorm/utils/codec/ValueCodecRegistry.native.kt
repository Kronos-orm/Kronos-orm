@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm.utils.codec

import com.kotlinorm.Kronos
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.exceptions.ValueMappingException
import com.kotlinorm.interfaces.ValueCodec
import com.kotlinorm.interfaces.ValueCodecRegistration
import com.kotlinorm.interfaces.ValueCodecContext
import com.kotlinorm.interfaces.asStaticValueCodec
import com.kotlinorm.interfaces.asValueCodecRegistration
import com.kotlinorm.interfaces.StaticValueCodecContext
import com.kotlinorm.interfaces.LegacyValueCodecPlatformContext
import com.kotlinorm.interfaces.KronosValueCodecs
import com.kotlinorm.interfaces.KronosPhysicalValue
import com.kotlinorm.interfaces.toStaticTypeRef
import com.kotlinorm.interfaces.decodePlatformTemporal
import com.kotlinorm.interfaces.toLogicalScalar
import com.kotlinorm.interfaces.StaticEnumValueCodec
import com.kotlinorm.interfaces.StaticValueCodecException
import com.kotlinorm.utils.GeneratedTypeMetadataSnapshot
import com.kotlinorm.utils.generatedTypeMetadata
import com.kotlinorm.utils.isStructurallyAssignableTo
import com.kotlinorm.utils.platformIsEnumClass
import kotlin.reflect.KClass
import kotlin.coroutines.cancellation.CancellationException

internal actual object ValueCodecRegistry {
    actual fun register(codec: ValueCodec): ValueCodecRegistration =
        KronosValueCodecs.register(codec.asStaticValueCodec()).asValueCodecRegistration()
    actual fun convert(request: ValueConversionRequest): Any? = convert(request, null)

    @Suppress("TooGenericExceptionCaught")
    internal actual fun convert(request: ValueConversionRequest, generatedTypes: GeneratedTypeMetadataSnapshot?): Any? {
        val context = StaticValueCodecContext(
            request.direction, request.origin,
            request.sourceType?.toStaticTypeRef(request.sourceTypeInfo),
            request.targetType.toStaticTypeRef(request.targetTypeInfo),
            request.field, request.storage, request.sourceTypeInfo, request.targetTypeInfo,
            timeZone = request.timeZone
        ).also { it.platformContext = LegacyValueCodecPlatformContext(ValueCodecContext(
            request.direction, request.origin, request.sourceType, request.targetType,
            request.field, request.storage, request.sourceTypeInfo, request.targetTypeInfo
        )) }
        try {
            if (request.direction == ValueCodecDirection.ENCODE) {
                val input = request.value
                val logical = if (input == null || (request.targetType.acceptsNative(input) && request.declaredSourceAssignable())) input else {
                    require(request.storage == com.kotlinorm.enums.ValueStorage.NONE) {
                        "Encode input is not assignable to its logical target"
                    }
                    if (input is kotlin.time.Instant && context.targetType.key == "kotlin.Long") {
                        input.toEpochMilliseconds()
                    } else {
                        val physical = when (input) {
                            is kotlin.time.Instant, is kotlinx.datetime.LocalDate,
                            is kotlinx.datetime.LocalDateTime, is kotlinx.datetime.LocalTime -> KronosPhysicalValue.Text(input.toString())
                            else -> input.toPhysicalValue()
                        }
                        decodePlatformTemporal(physical, context) ?: input.toLogicalScalar(context)
                    }
                }
                return KronosValueCodecs.encode(logical, context).toRuntimeValue()
            }
            if (request.origin == com.kotlinorm.enums.ValueCodecOrigin.MAP && request.value != null &&
                request.targetType.acceptsNative(request.value)) return request.value
            val targetClass = request.targetType.classifier as? KClass<*>
            val enumMetadata = if (request.targetTypeInfo?.enumKey != null ||
                targetClass?.let(::platformIsEnumClass) == true) {
                (generatedTypes ?: generatedTypeMetadata()).enumMetadata(request.targetType)
            } else null
            val fallbackEnum = enumMetadata?.let { metadata -> object : StaticEnumValueCodec {
                override val type = context.targetType
                override val entryNames = metadata.entryNames
                override fun decode(name: String): Enum<*>? = metadata.factory.create(name)
            } }
            val numericTarget = request.targetType.classifier in setOf(Byte::class, Short::class, Int::class,
                Long::class, Float::class, Double::class)
            val allowBuiltInCoercion = !request.strict || request.targetType.acceptsNative(request.value) ||
                (request.value is Number && numericTarget)
            val decoded = KronosValueCodecs.decode(request.value.toPhysicalValue(), context, fallbackEnum, allowBuiltInCoercion)
            require(request.targetType.acceptsNative(decoded)) { "Codec output is not assignable to its complete logical target" }
            return decoded
        } catch (cause: StaticValueCodecException) {
            if (cause.detail == "missing generated enum metadata") {
                throw com.kotlinorm.exceptions.MissingEnumMetadata(request.direction, request.origin,
                    request.sourceType, null, request.targetType, request.field?.name,
                    request.field?.columnName, request.valueName, request.dialect?.family?.name, request.batchIndex)
            }
            if (request.storage == com.kotlinorm.enums.ValueStorage.SERIALIZED &&
                cause.detail == "no registered codec matched SERIALIZED storage") {
                throw com.kotlinorm.exceptions.MissingSerializedCodec(request.direction, request.origin,
                    request.sourceType, null, request.targetType, request.field?.name,
                    request.field?.columnName, request.valueName, request.dialect?.family?.name, request.batchIndex)
            }
            throw request.nativeFailure(cause.detail, cause)
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Error) {
            throw cause
        } catch (cause: ValueMappingException) {
            throw cause
        } catch (cause: Throwable) {
            throw request.nativeFailure(cause.message ?: "Native value conversion failed", cause)
        }
    }
    actual fun acceptPrepared(request: ValueConversionRequest): Any? {
        if (request.value == null && !request.targetType.isMarkedNullable) {
            throw request.nativeFailure("NULL is not assignable to a non-null target")
        }
        request.value.toPhysicalValue()
        return request.value
    }
}
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
private fun ValueConversionRequest.nativeFailure(detail: String, cause: Throwable? = null) = ValueMappingException(
    direction, origin, sourceType, null, targetType, field?.name, field?.columnName,
    valueName, dialect?.family?.name, batchIndex, detail, cause
)
internal actual fun Any.isTemporalRuntimeValue(): Boolean = this is kotlin.time.Instant ||
    this is kotlinx.datetime.LocalDate || this is kotlinx.datetime.LocalTime || this is kotlinx.datetime.LocalDateTime

private fun kotlin.reflect.KType.acceptsNative(value: Any?): Boolean {
    if (value == null) return isMarkedNullable
    if ((classifier as? KClass<*>)?.isInstance(value) != true) return false
    return when (value) {
        is Map<*, *> -> value.all { (key, element) ->
            arguments.getOrNull(0)?.type?.acceptsNative(key) != false &&
                arguments.getOrNull(1)?.type?.acceptsNative(element) != false
        }
        is Iterable<*> -> arguments.firstOrNull()?.type?.let { element -> value.all(element::acceptsNative) } ?: true
        is Array<*> -> arguments.firstOrNull()?.type?.let { element -> value.all(element::acceptsNative) } ?: true
        else -> true
    }
}

private fun ValueConversionRequest.declaredSourceAssignable(): Boolean {
    val declaredSource = sourceType ?: return true
    if (declaredSource == targetType) return true
    return if (sourceTypeInfo != null && targetTypeInfo != null) {
        sourceTypeInfo.isStructurallyAssignableTo(targetTypeInfo)
    } else {
        declaredSource.isStructurallyAssignableTo(targetType)
    }
}
