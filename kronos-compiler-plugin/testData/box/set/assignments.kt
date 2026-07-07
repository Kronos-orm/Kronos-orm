import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForSet.Companion.afterSet
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToSet

@Table(name = "tb_set_user")
data class SetUser(
    var id: Int? = null,
    var name: String? = null,
    var age: Int? = null,
    var version: Int? = null,
) : KPojo {
    fun column(name: String): Field {
        return kronosColumns().single { it.name == name }
    }
}

data class SetSnapshot(
    val fields: List<Field>,
    val fieldParamMap: Map<Field, Any?>,
    val plusAssignFields: List<Pair<Field, Number>>,
    val minusAssignFields: List<Pair<Field, Number>>,
)

fun SetUser.collectSet(block: ToSet<SetUser, Unit>): SetSnapshot {
    var result: SetSnapshot? = null
    afterSet {
        block!!(it)
        result = SetSnapshot(fields.toList(), fieldParamMap.toMap(), plusAssignFields.toList(), minusAssignFields.toList())
    }
    return result ?: error("set block did not run")
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = SetUser()
    val result = user.collectSet {
        it.id = 1
        it.name = "Ada"
        it.age -= 2
        it.version += 1
    }

    return when {
        result.fields.map { it.name } != listOf("id", "name", "age", "version") -> "Fail: fields were ${result.fields.map { it.name }}"
        result.fieldParamMap[user.column("id")] != 1 -> "Fail: id value was ${result.fieldParamMap[user.column("id")]}"
        result.fieldParamMap[user.column("name")] != "Ada" -> "Fail: name value was ${result.fieldParamMap[user.column("name")]}"
        result.minusAssignFields != listOf(user.column("age") to 2) -> "Fail: minus was ${result.minusAssignFields}"
        result.plusAssignFields != listOf(user.column("version") to 1) -> "Fail: plus was ${result.plusAssignFields}"
        else -> "OK"
    }
}
