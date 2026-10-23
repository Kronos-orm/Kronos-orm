package com.kotlinorm.utils.codec
import com.kotlinorm.Kronos
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueCodecOrigin
import com.kotlinorm.enums.ValueStorage
import com.kotlinorm.utils.StaticTypeInfo
import com.kotlinorm.syntax.render.SqlDialect
import com.kotlinorm.interfaces.ValueCodec
import com.kotlinorm.interfaces.ValueCodecRegistration
import kotlin.reflect.KType


/**
 * Marks whether a caller supplied a final database value or a temporal value
 * that still needs one registry encoding pass.
 */
internal enum class PreparedValueKind {
    /**
     * The value is already JDBC-bindable and must bypass every codec.
     */
    READY_DATABASE_VALUE,
    /**
     * The value came from a time strategy and must enter temporal conversion once.
     */
    STRATEGY_TEMPORAL
}

/**
 * Carries an explicitly prepared source value across SQL rendering boundaries.
 *
 * @property value source value, including SQL `NULL`
 * @property sourceType complete declared source type when the caller knows it
 * @property dateFormat explicit text format, which takes precedence over field and global formats
 * @property kind whether conversion is complete or still requires temporal encoding
 * @property sourceTypeInfo compiler-generated static metadata for [sourceType], when available
 */
internal data class PreparedValue(
    val value: Any?,
    val sourceType: KType?,
    val dateFormat: String? = null,
    val kind: PreparedValueKind,
    val sourceTypeInfo: StaticTypeInfo? = null
)

/**
 * Complete internal request carried from a safe mapping boundary to the registry.
 *
 * [targetType] always describes the logical Kotlin field/result type. A JDBC
 * [physicalTargetType] may constrain ENCODE output but never replaces that
 * logical type in public codec matching. [storage] is derived from [field] when
 * one is present and conflicting explicit metadata is rejected immediately.
 *
 * @property value raw input value before any semantic conversion
 * @property direction ENCODE for parameters/delegate writes, DECODE for safe Map/JDBC/delegate reads
 * @property origin the boundary creating this request
 * @property targetType complete logical Kotlin target type, including generic arguments and nullability
 * @property sourceType complete declared source type when known
 * @property field optional mapped field metadata such as serialization and date format
 * @property storage storage protocol gate; serialized requests never fall through to built-in scalar codecs
 * @property physicalTargetType optional JDBC binding type used only to validate/shape encoded results
 * @property dialect optional SQL dialect included in temporal behavior and error context
 * @property dateFormat explicit caller format before field/global fallback
 * @property effectiveDateFormat resolved explicit, field or global date format in priority order
 * @property timeZone zone used for instant/local temporal conversion
 * @property strict whether implicit basic/temporal DECODE coercion is disabled
 * @property batchIndex source index included in conversion failures
 * @property valueName parameter or result label included in conversion failures
 * @property targetTypeInfo compiler-generated metadata for [targetType], when available
 * @property sourceTypeInfo compiler-generated metadata for [sourceType], when available
 * @property physicalTargetTypeInfo compiler-generated metadata for [physicalTargetType], when available
 */
internal data class ValueConversionRequest(
    val value: Any?,
    val direction: ValueCodecDirection,
    val origin: ValueCodecOrigin,
    val targetType: KType,
    val sourceType: KType? = null,
    val field: Field? = null,
    val storage: ValueStorage = if (field?.serializable == true) ValueStorage.SERIALIZED else ValueStorage.NONE,
    val physicalTargetType: KType? = null,
    val dialect: SqlDialect? = null,
    val dateFormat: String? = null,
    val timeZone: String? = Kronos.timeZone,
    val strict: Boolean = Kronos.strictSetValue,
    val batchIndex: Int? = null,
    val valueName: String? = null,
    val targetTypeInfo: StaticTypeInfo? = field?.typeInfo,
    val sourceTypeInfo: StaticTypeInfo? = null,
    val physicalTargetTypeInfo: StaticTypeInfo? = null
) {
    val effectiveDateFormat: String
        get() = dateFormat ?: this.field?.dateFormat ?: Kronos.defaultDateFormat

    init {
        if (field != null) {
            val fieldStorage = if (field.serializable) ValueStorage.SERIALIZED else ValueStorage.NONE
            require(storage == fieldStorage) {
                "Value storage $storage conflicts with field '${field.name}' storage $fieldStorage"
            }
        }
    }
}

internal expect object ValueCodecRegistry {
    fun register(codec: ValueCodec): ValueCodecRegistration
    fun convert(request: ValueConversionRequest): Any?
    internal fun convert(request: ValueConversionRequest, generatedTypes: com.kotlinorm.utils.GeneratedTypeMetadataSnapshot?): Any?
    fun acceptPrepared(request: ValueConversionRequest): Any?
}

internal expect fun Any.isTemporalRuntimeValue(): Boolean
