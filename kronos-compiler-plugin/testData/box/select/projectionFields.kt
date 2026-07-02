import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToSelect

@Table(name = "tb_select_user")
data class SelectUser(
    var id: Int? = null,
    var name: String? = null,
    @Column("phone_number")
    var phone: String? = null,
    var age: Int? = null,
) : KPojo {
    fun column(name: String): Field {
        return kronosColumns().single { it.name == name }
    }
}

fun SelectUser.collectSelect(block: ToSelect<SelectUser, Any?>): List<Field> {
    val result = mutableListOf<Field>()
    afterSelect {
        block!!(it)
        result += fields
    }
    return result
}

fun box(): String {
    Kronos.init {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = SelectUser()
    val fields = user.collectSelect { [it.id, it.name, it.phone.alias("mobile"), "1"] }

    return when {
        fields.size != 4 -> "Fail: size was ${fields.size}"
        fields[0] != user.column("id") -> "Fail: first field was ${fields[0].name}"
        fields[1] != user.column("name") -> "Fail: second field was ${fields[1].name}"
        fields[2].name != "mobile" -> "Fail: alias was ${fields[2].name}"
        fields[2].columnName != "phone_number" -> "Fail: columnName was ${fields[2].columnName}"
        fields[3].type != KColumnType.CUSTOM_CRITERIA_SQL -> "Fail: custom type was ${fields[3].type}"
        else -> "OK"
    }
}
