import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo

/** Regression table comment */
@Table("tb_documented_user")
data class DocumentedUser(
    var id: Int? = null,
) : KPojo

fun box(): String {
    val user = DocumentedUser()

    return when {
        user.__tableName != "tb_documented_user" -> "Fail: table name was ${user.__tableName}"
        user.__tableComment != "Regression table comment" -> "Fail: table comment was ${user.__tableComment}"
        else -> "OK"
    }
}
