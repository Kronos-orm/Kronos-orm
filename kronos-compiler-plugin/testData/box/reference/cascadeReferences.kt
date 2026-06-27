import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Cascade
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForReference.Companion.afterReference
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToReference

@Table(name = "tb_ref_parent")
data class RefParent(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

@Table(name = "tb_ref_child")
data class RefChild(
    var id: Int? = null,
    var parentId: Int? = null,
    @Cascade(["parentId"], ["id"])
    var parent: RefParent? = null,
) : KPojo {
    fun column(name: String): Field = kronosColumns().single { it.name == name }
}

fun RefChild.collectReference(block: ToReference<RefChild, Any?>): List<Field> {
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

    val child = RefChild()
    val result = child.collectReference { [it::id, it::parent] }

    return when {
        result.map { it.name } != listOf("id", "parent") -> "Fail: refs were ${result.map { it.name }}"
        result[1].isColumn -> "Fail: parent should be a cascade field, not a column"
        else -> "OK"
    }
}
