import com.kotlinorm.interfaces.KPojo

data class AccessUser(
    var id: Int? = null,
    var name: String? = null,
    val fixed: String? = "initial",
) : KPojo

fun box(): String {
    val user = AccessUser()

    user["id"] = 12
    user["name"] = "Katherine"
    user["fixed"] = "changed"

    return when {
        user["id"] != 12 -> "Fail: dynamic id was ${user["id"]}"
        user["name"] != "Katherine" -> "Fail: dynamic name was ${user["name"]}"
        user.id != 12 -> "Fail: id property was ${user.id}"
        user.name != "Katherine" -> "Fail: name property was ${user.name}"
        user.fixed != "initial" -> "Fail: val property was ${user.fixed}"
        user["missing"] != null -> "Fail: missing field returned ${user["missing"]}"
        else -> "OK"
    }
}
