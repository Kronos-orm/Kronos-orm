import com.kotlinorm.annotations.Ignore
import com.kotlinorm.enums.IgnoreAction
import com.kotlinorm.interfaces.KPojo

data class IgnoredUser(
    var id: Int? = null,
    @Ignore([IgnoreAction.ALL])
    var secret: String? = null,
    @Ignore([IgnoreAction.TO_MAP])
    var writeOnly: String? = null,
    @Ignore([IgnoreAction.FROM_MAP])
    var readOnly: String? = "kept",
    var normal: String? = null,
) : KPojo

fun box(): String {
    val user = IgnoredUser(1, "secret", "write", "read", "normal")
    val map = user.toDataMap()
    val columns = user.kronosColumns().map { it.name }.toSet()
    val patched = IgnoredUser().fromMapData<IgnoredUser>(
        mapOf(
            "id" to 2,
            "secret" to "changed",
            "writeOnly" to "changed",
            "readOnly" to "changed",
            "normal" to "changed",
        )
    )

    return when {
        "secret" in columns -> "Fail: secret should not be a column"
        "writeOnly" !in columns -> "Fail: writeOnly should remain a column"
        "readOnly" !in columns -> "Fail: readOnly should remain a column"
        "secret" in map -> "Fail: secret should not be in map"
        "writeOnly" in map -> "Fail: writeOnly should not be in map"
        map["readOnly"] != "read" -> "Fail: readOnly map value was ${map["readOnly"]}"
        patched.secret != null -> "Fail: secret was ${patched.secret}"
        patched.readOnly != "kept" -> "Fail: readOnly was ${patched.readOnly}"
        patched.normal != "changed" -> "Fail: normal was ${patched.normal}"
        else -> "OK"
    }
}
