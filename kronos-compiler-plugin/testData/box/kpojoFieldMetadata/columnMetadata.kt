import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.DateTimeFormat
import com.kotlinorm.annotations.Default
import com.kotlinorm.annotations.NonNull
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.interfaces.KPojo

data class ColumnUser(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    @Column("user_name")
    @NonNull
    @Default("guest")
    var name: String? = null,
    @ColumnType(KColumnType.VARCHAR, length = 64)
    var status: String? = null,
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    var createdAt: String? = null,
    @Serialize
    var tags: List<String>? = null,
) : KPojo

fun box(): String {
    val columns = ColumnUser().kronosColumns()
    fun column(name: String) = columns.singleOrNull { it.name == name }
        ?: error("missing column $name")

    val id = column("id")
    val name = column("name")
    val status = column("status")
    val createdAt = column("createdAt")
    val tags = column("tags")

    return when {
        id.primaryKey != PrimaryKeyType.IDENTITY -> "Fail: id primary key was ${id.primaryKey}"
        name.columnName != "user_name" -> "Fail: name columnName was ${name.columnName}"
        name.nullable -> "Fail: name should be non-nullable"
        name.defaultValue != "guest" -> "Fail: name default was ${name.defaultValue}"
        status.type != KColumnType.VARCHAR -> "Fail: status type was ${status.type}"
        status.length != 64 -> "Fail: status length was ${status.length}"
        createdAt.dateFormat != "yyyy-MM-dd HH:mm:ss" -> "Fail: date format was ${createdAt.dateFormat}"
        !tags.serializable -> "Fail: tags should be serializable"
        else -> "OK"
    }
}
