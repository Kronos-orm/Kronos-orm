import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo

@Table("tb_official_user")
data class OfficialUser(
    @PrimaryKey
    var id: Int? = null,
    var name: String? = null,
) : KPojo

fun box(): String {
    val user = OfficialUser(7, "Ada")
    val map = user.toDataMap()
    val columnNames = user.kronosColumns().map { it.name }.toSet()

    return when {
        user.__tableName != "tb_official_user" -> "Fail: table name was ${user.__tableName}"
        map["id"] != 7 -> "Fail: id was ${map["id"]}"
        map["name"] != "Ada" -> "Fail: name was ${map["name"]}"
        "id" !in columnNames -> "Fail: missing id column"
        "name" !in columnNames -> "Fail: missing name column"
        else -> "OK"
    }
}
