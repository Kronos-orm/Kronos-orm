import com.kotlinorm.annotations.Ignore
import com.kotlinorm.enums.IgnoreAction
import com.kotlinorm.interfaces.KPojo

data class MapUser(
    var id: Int? = null,
    var name: String? = null,
    var age: Int? = null,
    @Ignore([IgnoreAction.TO_MAP])
    var internalNote: String? = null,
) : KPojo

fun box(): String {
    val map = MapUser(1, "Ada", 36, "hidden").toDataMap()

    return when {
        map["id"] != 1 -> "Fail: id was ${map["id"]}"
        map["name"] != "Ada" -> "Fail: name was ${map["name"]}"
        map["age"] != 36 -> "Fail: age was ${map["age"]}"
        "internalNote" in map -> "Fail: internalNote should be ignored"
        else -> "OK"
    }
}
