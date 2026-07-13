import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.interfaces.KPojo
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class TypeMappedUser(
    var boolValue: Boolean? = null,
    var byteValue: Byte? = null,
    var shortValue: Short? = null,
    var intValue: Int? = null,
    var longValue: Long? = null,
    var floatValue: Float? = null,
    var doubleValue: Double? = null,
    var decimalValue: BigDecimal? = null,
    var charValue: Char? = null,
    var stringValue: String? = null,
    var dateValue: LocalDate? = null,
    var timeValue: LocalTime? = null,
    var dateTimeValue: LocalDateTime? = null,
    var binaryValue: ByteArray? = null,
) : KPojo

fun box(): String {
    val columns = TypeMappedUser().__columns
    fun typeOf(name: String): KColumnType {
        return columns.singleOrNull { it.name == name }?.type
            ?: error("missing column $name")
    }

    val stringColumn = columns.single { it.name == "stringValue" }

    return when {
        columns.size != 14 -> "Fail: column count was ${columns.size}"
        typeOf("boolValue") != KColumnType.BIT -> "Fail: bool type was ${typeOf("boolValue")}"
        typeOf("byteValue") != KColumnType.TINYINT -> "Fail: byte type was ${typeOf("byteValue")}"
        typeOf("shortValue") != KColumnType.SMALLINT -> "Fail: short type was ${typeOf("shortValue")}"
        typeOf("intValue") != KColumnType.INT -> "Fail: int type was ${typeOf("intValue")}"
        typeOf("longValue") != KColumnType.BIGINT -> "Fail: long type was ${typeOf("longValue")}"
        typeOf("floatValue") != KColumnType.FLOAT -> "Fail: float type was ${typeOf("floatValue")}"
        typeOf("doubleValue") != KColumnType.DOUBLE -> "Fail: double type was ${typeOf("doubleValue")}"
        typeOf("decimalValue") != KColumnType.DECIMAL -> "Fail: decimal type was ${typeOf("decimalValue")}"
        typeOf("charValue") != KColumnType.CHAR -> "Fail: char type was ${typeOf("charValue")}"
        typeOf("stringValue") != KColumnType.VARCHAR -> "Fail: string type was ${typeOf("stringValue")}"
        typeOf("dateValue") != KColumnType.DATE -> "Fail: date type was ${typeOf("dateValue")}"
        typeOf("timeValue") != KColumnType.TIME -> "Fail: time type was ${typeOf("timeValue")}"
        typeOf("dateTimeValue") != KColumnType.DATETIME -> "Fail: dateTime type was ${typeOf("dateTimeValue")}"
        typeOf("binaryValue") != KColumnType.BLOB -> "Fail: binary type was ${typeOf("binaryValue")}"
        stringColumn.primaryKey != PrimaryKeyType.NOT -> "Fail: default primary key was ${stringColumn.primaryKey}"
        !stringColumn.nullable -> "Fail: unannotated column should be nullable"
        stringColumn.defaultValue != null -> "Fail: default value was ${stringColumn.defaultValue}"
        stringColumn.dateFormat != null -> "Fail: date format was ${stringColumn.dateFormat}"
        stringColumn.ignore != null -> "Fail: ignore was ${stringColumn.ignore?.joinToString()}"
        else -> "OK"
    }
}
