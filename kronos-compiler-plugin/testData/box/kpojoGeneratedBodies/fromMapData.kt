import com.kotlinorm.annotations.Ignore
import com.kotlinorm.enums.IgnoreAction
import com.kotlinorm.interfaces.KPojo

data class MappedUser(
    var id: Int? = null,
    var name: String? = null,
    @Ignore([IgnoreAction.FROM_MAP])
    var readOnly: String? = "kept",
) : KPojo

fun box(): String {
    val source = mapOf<String, Any?>(
        "id" to 9,
        "name" to "Grace",
        "readOnly" to "changed",
        "unknown" to "ignored",
    )
    val user = MappedUser().fromMapData<MappedUser>(source)
    val safeUser = MappedUser().safeFromMapData<MappedUser>(mapOf("id" to 10, "name" to "Lin"))

    return when {
        user.id != 9 -> "Fail: id was ${user.id}"
        user.name != "Grace" -> "Fail: name was ${user.name}"
        user.readOnly != "kept" -> "Fail: readOnly was ${user.readOnly}"
        safeUser.id != 10 -> "Fail: safe id was ${safeUser.id}"
        safeUser.name != "Lin" -> "Fail: safe name was ${safeUser.name}"
        else -> "OK"
    }
}
