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

package com.kotlinorm.interfaces

import com.kotlinorm.Kronos
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueCodecOrigin
import com.kotlinorm.enums.ValueStorage
import com.kotlinorm.utils.StaticTypeInfo
import com.kotlinorm.utils.StaticTypeMetadataLock
import com.kotlinorm.utils.StaticTypeRef
import com.kotlinorm.utils.structuralSignature
import com.kotlinorm.utils.withoutTopLevelNullability
import kotlin.time.Instant
import kotlin.coroutines.cancellation.CancellationException

/** Storage representation used for enum values at the physical boundary. */
enum class StaticEnumStorage {
    NAME,
    ORDINAL
}

/**
 * Platform-neutral information supplied to a [StaticValueCodec].
 *
 * Type information is compiler-generated and represented by [StaticTypeRef]
 * and [StaticTypeInfo]. JVM code may attach an internal compatibility context,
 * but common codecs never require `KType` or platform reflection.
 */
data class StaticValueCodecContext(
    val direction: ValueCodecDirection,
    val origin: ValueCodecOrigin,
    val sourceType: StaticTypeRef? = null,
    val targetType: StaticTypeRef,
    val field: ValueCodecField? = null,
    val storage: ValueStorage = if (field?.serializable == true) ValueStorage.SERIALIZED else ValueStorage.NONE,
    val sourceTypeInfo: StaticTypeInfo? = null,
    val targetTypeInfo: StaticTypeInfo? = field?.typeInfo,
    val enumStorage: StaticEnumStorage = field.enumStorage(),
    /** IANA zone id used by automatic instant/local temporal conversion. */
    val timeZone: String? = Kronos.timeZone
) {
    internal var platformContext: Any? = null

    init {
        if (field != null) {
            val fieldStorage = if (field.serializable) ValueStorage.SERIALIZED else ValueStorage.NONE
            require(storage == fieldStorage) {
                "Value storage $storage conflicts with field '${field.name}' storage $fieldStorage"
            }
        }
    }
}

/**
 * A non-reflective user conversion extension shared by common, Native and JVM
 * runtime paths. Values are non-null when [supports] and [convert] are called.
 */
interface StaticValueCodec {
    /** Text used in conversion failures. */
    val description: String
        get() = "user StaticValueCodec"

    fun supports(value: Any, context: StaticValueCodecContext): Boolean

    fun convert(value: Any, context: StaticValueCodecContext): Any?
}

/** Idempotent handle for one common codec or enum-decoder registration. */
interface StaticValueCodecRegistration {
    fun close()
}

/**
 * Compiler- or application-supplied enum decoder for a concrete static type.
 * The entry list is declaration ordered and is used for ordinal validation.
 */
interface StaticEnumValueCodec {
    val type: StaticTypeRef
    val entryNames: List<String>

    fun decode(name: String): Enum<*>?
}

/**
 * Error raised by the common physical-value conversion boundary.
 */
class StaticValueCodecException(
    val direction: ValueCodecDirection,
    val origin: ValueCodecOrigin,
    val targetType: StaticTypeRef,
    val fieldName: String?,
    val columnName: String?,
    val detail: String,
    cause: Throwable? = null
) : IllegalArgumentException(
    buildString {
        append("Value codec failed for ")
        append(direction)
        append('/')
        append(origin)
        fieldName?.let { append(" field '").append(it).append('\'') }
        columnName?.let { append(" column '").append(it).append('\'') }
        append(" -> ")
        append(targetType.structuralSignature())
        append(": ")
        append(detail)
    },
    cause
)

/**
 * The single registration ledger for descriptor-based user codecs.
 *
 * Native callers use [register], [encode], and [decode] directly. The JVM
 * `Kronos.registerValueCodec` entry adapts its KType-based codec into this
 * ledger, so later registrations have one shared priority order on every
 * platform. Java temporal and JDBC conversions deliberately remain outside
 * this common boundary.
 */
object KronosValueCodecs {
    private data class CodecEntry(val id: Long, val codec: StaticValueCodec)
    private data class EnumEntry(val id: Long, val codec: StaticEnumValueCodec)

    private val lock = StaticTypeMetadataLock()
    private var nextId = 0L
    private val codecs = mutableListOf<CodecEntry>()
    private val enumCodecs = mutableListOf<EnumEntry>()

    /** Registers a user codec at the highest current priority. */
    fun register(codec: StaticValueCodec): StaticValueCodecRegistration = lock.withLock {
        val entry = CodecEntry(nextRegistrationId(), codec)
        codecs.add(0, entry)
        closingRegistration { codecs.removeAll { it.id == entry.id } }
    }

    /**
     * Registers an enum decoder at the highest current priority for its type.
     * A decoder is explicit because a [StaticTypeInfo] identifies an enum but
     * cannot construct one on Native by itself.
     */
    fun registerEnum(codec: StaticEnumValueCodec): StaticValueCodecRegistration = lock.withLock {
        require(codec.entryNames.isNotEmpty()) { "Enum codec entries must not be empty" }
        require(codec.entryNames.distinct().size == codec.entryNames.size) {
            "Enum codec entries must be unique"
        }
        val entry = EnumEntry(nextRegistrationId(), codec)
        enumCodecs.add(0, entry)
        closingRegistration { enumCodecs.removeAll { it.id == entry.id } }
    }

    /**
     * Encodes one logical value to a physical database value.
     */
    fun encode(value: Any?, context: StaticValueCodecContext): KronosPhysicalValue {
        require(context.direction == ValueCodecDirection.ENCODE) {
            "StaticValueCodecContext.direction must be ENCODE"
        }
        if (value == null) {
            requireNullable(context)
            return KronosPhysicalValue.Null
        }

        val codec = findMatchingCodec(value, context)
        if (codec != null) {
            val converted = codec.convertSafely(value, context)
            if (context.storage == ValueStorage.SERIALIZED && converted !is String) {
                throw context.failure("SERIALIZED encode must produce kotlin.String")
            }
            return converted.toPhysical(context)
        }
        if (context.storage == ValueStorage.SERIALIZED) {
            throw context.failure("no registered codec matched SERIALIZED storage")
        }
        return value.toPhysical(context)
    }

    /**
     * Decodes one physical database value to its logical scalar or registered
     * enum/serialized representation.
     */
    fun decode(value: KronosPhysicalValue, context: StaticValueCodecContext): Any? {
        require(context.direction == ValueCodecDirection.DECODE) {
            "StaticValueCodecContext.direction must be DECODE"
        }
        if (value == KronosPhysicalValue.Null) {
            requireNullable(context)
            return null
        }
        val raw = value.rawValue()
        val codec = findMatchingCodec(raw, context)
        if (codec != null) {
            val converted = codec.convertSafely(raw, context)
            requireNonNullResult(converted, context)
            return converted
        }
        if (context.storage == ValueStorage.SERIALIZED) {
            throw context.failure("no registered codec matched SERIALIZED storage")
        }

        enumCodec(context)?.let { codec ->
            return decodeEnum(raw, codec, context)
        }
        decodePlatformTemporal(value, context)?.let { return it }
        return raw.toLogicalScalar(context)
    }

    /**
     * JVM's legacy registry uses this selection result before its JVM-only
     * temporal and JDBC built-ins. It intentionally does not apply scalars.
     */
    internal fun findMatchingCodec(value: Any, context: StaticValueCodecContext): StaticValueCodec? {
        val snapshot = lock.withLock { codecs.toList() }
        for (entry in snapshot) {
            val supported = try {
                entry.codec.supports(value, context)
            } catch (cause: StaticValueCodecException) {
                throw cause
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: Error) {
                throw cause
            } catch (cause: Throwable) {
                throw context.failure("${entry.codec.description} failed in supports", cause)
            }
            if (supported) {
                return entry.codec
            }
        }
        return null
    }

    private fun enumCodec(context: StaticValueCodecContext): StaticEnumValueCodec? {
        if (context.targetTypeInfo?.enumKey == null) return null
        val target = context.targetType.withoutTopLevelNullability()
        return lock.withLock {
            enumCodecs.firstOrNull {
                it.codec.type.withoutTopLevelNullability() == target
            }?.codec
        }
    }

    private fun nextRegistrationId(): Long {
        check(nextId != Long.MAX_VALUE) { "Static value codec registration ids are exhausted" }
        nextId += 1
        return nextId
    }

    private fun closingRegistration(remove: () -> Unit): StaticValueCodecRegistration =
        object : StaticValueCodecRegistration {
            private var closed = false

            override fun close() {
                lock.withLock {
                    if (!closed) {
                        closed = true
                        remove()
                    }
                }
            }
        }
}

/** Creates one descriptor-based codec from matching and conversion functions. */
fun staticValueCodec(
    supports: (value: Any, context: StaticValueCodecContext) -> Boolean,
    convert: (value: Any, context: StaticValueCodecContext) -> Any?
): StaticValueCodec = object : StaticValueCodec {
    override fun supports(value: Any, context: StaticValueCodecContext): Boolean = supports(value, context)

    override fun convert(value: Any, context: StaticValueCodecContext): Any? = convert(value, context)
}

/**
 * Creates a codec for fields stored as serialized TEXT. The encode callback
 * always produces one text value; decode is considered only for physical TEXT.
 */
fun serializedTextValueCodec(
    encode: (value: Any, type: StaticTypeRef) -> String,
    decode: (text: String, type: StaticTypeRef) -> Any?
): StaticValueCodec = staticValueCodec(
    supports = { value, context ->
        context.storage == ValueStorage.SERIALIZED && when (context.direction) {
            ValueCodecDirection.ENCODE -> true
            ValueCodecDirection.DECODE -> value is String && context.origin != ValueCodecOrigin.PARAMETER
        }
    },
    convert = { value, context ->
        when (context.direction) {
            ValueCodecDirection.ENCODE -> encode(value, context.targetType)
            ValueCodecDirection.DECODE -> decode(value as String, context.targetType)
        }
    }
)

private fun ValueCodecField?.enumStorage(): StaticEnumStorage = when (this?.type) {
    KColumnType.TINYINT,
    KColumnType.SMALLINT,
    KColumnType.INT,
    KColumnType.MEDIUMINT,
    KColumnType.SERIAL,
    KColumnType.BIGINT -> StaticEnumStorage.ORDINAL

    else -> StaticEnumStorage.NAME
}

private fun KronosPhysicalValue.rawValue(): Any = when (this) {
    is KronosPhysicalValue.Integer -> value
    is KronosPhysicalValue.Real -> value
    is KronosPhysicalValue.Text -> value
    is KronosPhysicalValue.Blob -> bytes
    KronosPhysicalValue.Null -> error("NULL is handled before raw conversion")
}

private fun Any?.toPhysical(context: StaticValueCodecContext): KronosPhysicalValue = when (this) {
    null -> {
        requireNullable(context)
        KronosPhysicalValue.Null
    }
    is KronosPhysicalValue -> this
    is Boolean -> KronosPhysicalValue.Integer(if (this) 1 else 0)
    is Byte -> KronosPhysicalValue.Integer(toLong())
    is Short -> KronosPhysicalValue.Integer(toLong())
    is Int -> KronosPhysicalValue.Integer(toLong())
    is Long -> KronosPhysicalValue.Integer(this)
    is Float -> KronosPhysicalValue.Real(toDouble())
    is Double -> KronosPhysicalValue.Real(this)
    is Char -> KronosPhysicalValue.Text(toString())
    is String -> KronosPhysicalValue.Text(this)
    is ByteArray -> KronosPhysicalValue.Blob(this)
    is Instant -> KronosPhysicalValue.Text(toString())
    is Enum<*> -> when (context.enumStorage) {
        StaticEnumStorage.NAME -> KronosPhysicalValue.Text(name)
        StaticEnumStorage.ORDINAL -> KronosPhysicalValue.Integer(ordinal.toLong())
    }
    else -> encodePlatformTemporal(this, context)
        ?: throw context.failure("unsupported scalar ${this::class}")
}

private fun Any.toLogicalScalar(context: StaticValueCodecContext): Any = when (
    context.targetType.withoutTopLevelNullability().key
) {
    "kotlin.Boolean" -> toBooleanScalar(context)
    "kotlin.Byte" -> toLongScalar(context).toByteChecked(context)
    "kotlin.Short" -> toLongScalar(context).toShortChecked(context)
    "kotlin.Int" -> toLongScalar(context).toIntChecked(context)
    "kotlin.Long" -> toLongScalar(context)
    "kotlin.Float" -> toDoubleScalar(context).toFloat()
    "kotlin.Double" -> toDoubleScalar(context)
    "kotlin.Char" -> toCharScalar(context)
    "kotlin.String" -> toString()
    "kotlin.ByteArray" -> this as? ByteArray
        ?: throw context.failure("BYTE_ARRAY decoding requires BLOB storage")
    "kotlin.time.Instant" -> toKotlinInstant(context)
    else -> throw context.failure("no scalar codec for ${context.targetType.key}")
}

private fun decodeEnum(
    value: Any,
    codec: StaticEnumValueCodec,
    context: StaticValueCodecContext
): Enum<*> {
    val name = when (context.enumStorage) {
        StaticEnumStorage.NAME -> value as? String
            ?: throw context.failure("enum name decoding requires TEXT storage")
        StaticEnumStorage.ORDINAL -> {
            val ordinal = value.toLongScalar(context).toIntChecked(context)
            codec.entryNames.getOrNull(ordinal)
                ?: throw context.failure("invalid enum ordinal $ordinal")
        }
    }
    return codec.decode(name)
        ?.takeIf { it.name == name }
        ?: throw context.failure("unknown enum name '$name'")
}

private fun Any.toBooleanScalar(context: StaticValueCodecContext): Boolean = when (this) {
    is Boolean -> this
    is Number -> toDouble() != 0.0
    is String -> when (trim()) {
        "1" -> true
        "0", "" -> false
        "true", "TRUE" -> true
        "false", "FALSE" -> false
        else -> throw context.failure("cannot decode '$this' as kotlin.Boolean")
    }
    else -> throw context.failure("cannot decode $this as kotlin.Boolean")
}

private fun Any.toLongScalar(context: StaticValueCodecContext): Long = when (this) {
    is Byte -> toLong()
    is Short -> toLong()
    is Int -> toLong()
    is Long -> this
    is Float -> toDouble().toLongExact(context)
    is Double -> toLongExact(context)
    is String -> toLongOrNull() ?: throw context.failure("cannot decode '$this' as kotlin.Long")
    else -> throw context.failure("cannot decode $this as kotlin.Long")
}

private fun Any.toDoubleScalar(context: StaticValueCodecContext): Double = when (this) {
    is Number -> toDouble()
    is String -> toDoubleOrNull() ?: throw context.failure("cannot decode '$this' as kotlin.Double")
    else -> throw context.failure("cannot decode $this as kotlin.Double")
}

private fun Any.toCharScalar(context: StaticValueCodecContext): Char = when (this) {
    is Char -> this
    is String -> singleOrNull() ?: throw context.failure("cannot decode '$this' as kotlin.Char")
    else -> toLongScalar(context).toIntChecked(context).toChar()
}

private fun Any.toKotlinInstant(context: StaticValueCodecContext): Instant = when (this) {
    is Long -> Instant.fromEpochMilliseconds(this)
    is Int -> Instant.fromEpochMilliseconds(toLong())
    is Double -> Instant.fromEpochMilliseconds(toLongExact(context))
    is Float -> Instant.fromEpochMilliseconds(toDouble().toLongExact(context))
    is String -> try {
        Instant.parse(this)
    } catch (instantCause: Throwable) {
        try {
            decodePlatformTemporal(KronosPhysicalValue.Text(this), context) as? Instant
                ?: throw instantCause
        } catch (localCause: Throwable) {
            throw context.failure("cannot decode '$this' as kotlin.time.Instant", localCause).also {
                it.addSuppressed(instantCause)
            }
        }
    }
    else -> throw context.failure("cannot decode $this as kotlin.time.Instant")
}

private fun Double.toLongExact(context: StaticValueCodecContext): Long {
    if (!isFinite() || this < Long.MIN_VALUE.toDouble() || this > Long.MAX_VALUE.toDouble() || toLong().toDouble() != this) {
        throw context.failure("cannot decode $this as an integral value")
    }
    return toLong()
}

private fun Long.toByteChecked(context: StaticValueCodecContext): Byte {
    if (this !in Byte.MIN_VALUE.toLong()..Byte.MAX_VALUE.toLong()) {
        throw context.failure("$this is outside kotlin.Byte range")
    }
    return toByte()
}

private fun Long.toShortChecked(context: StaticValueCodecContext): Short {
    if (this !in Short.MIN_VALUE.toLong()..Short.MAX_VALUE.toLong()) {
        throw context.failure("$this is outside kotlin.Short range")
    }
    return toShort()
}

private fun Long.toIntChecked(context: StaticValueCodecContext): Int {
    if (this !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
        throw context.failure("$this is outside kotlin.Int range")
    }
    return toInt()
}

private fun requireNullable(context: StaticValueCodecContext) {
    if (!context.targetType.nullable) {
        throw context.failure("conversion produced null for a non-null target")
    }
}

private fun requireNonNullResult(value: Any?, context: StaticValueCodecContext) {
    if (value == null) requireNullable(context)
}

private fun StaticValueCodec.convertSafely(value: Any, context: StaticValueCodecContext): Any? = try {
    convert(value, context)
} catch (cause: StaticValueCodecException) {
    throw cause
} catch (cause: CancellationException) {
    throw cause
} catch (cause: Error) {
    throw cause
} catch (cause: Throwable) {
    throw context.failure("$description failed in convert", cause)
}

private fun StaticValueCodecContext.failure(
    detail: String,
    cause: Throwable? = null
): StaticValueCodecException = StaticValueCodecException(
    direction,
    origin,
    targetType,
    field?.name,
    field?.columnName,
    detail,
    cause
)
