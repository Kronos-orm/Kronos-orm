import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForReference.Companion.afterReference
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToReference

@Table(name = "tb_reference_user")
data class ReferenceUser(
    var id: Int? = null,
    var name: String? = null,
    @Column("phone_number")
    var phone: String? = null,
) : KPojo {
    fun column(name: String): Field {
        return kronosColumns().single { it.name == name }
    }
}

fun ReferenceUser.collectReference(block: ToReference<ReferenceUser, Any?>): List<Field> {
    val result = mutableListOf<Field>()
    afterReference {
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

    val user = ReferenceUser()
    val result = user.collectReference { [it::id, it::name, it::phone] }

    return when {
        result != listOf(user.column("id"), user.column("name"), user.column("phone")) -> "Fail: references were ${result.map { it.name }}"
        result[2].columnName != "phone_number" -> "Fail: columnName was ${result[2].columnName}"
        else -> "OK"
    }
}
