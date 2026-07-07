import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.TableIndex
import com.kotlinorm.interfaces.KPojo

@Table("tb_indexed_user")
@TableIndex("idx_name", ["name"], "UNIQUE")
@TableIndex(name = "idx_age_name", columns = ["age", "name"], method = "BTREE")
data class IndexedUser(
    var id: Int? = null,
    var name: String? = null,
    var age: Int? = null,
) : KPojo

fun box(): String {
    val user = IndexedUser()
    val indexes = user.kronosTableIndex()
    val unique = indexes.singleOrNull { it.name == "idx_name" }
    val compound = indexes.singleOrNull { it.name == "idx_age_name" }

    return when {
        user.__tableName != "tb_indexed_user" -> "Fail: table name was ${user.__tableName}"
        indexes.size != 2 -> "Fail: index count was ${indexes.size}"
        unique == null -> "Fail: missing idx_name"
        !unique.columns.contentEquals(arrayOf("name")) -> "Fail: idx_name columns were ${unique.columns.joinToString()}"
        unique.type != "UNIQUE" -> "Fail: idx_name type was ${unique.type}"
        compound == null -> "Fail: missing idx_age_name"
        !compound.columns.contentEquals(arrayOf("age", "name")) -> "Fail: compound columns were ${compound.columns.joinToString()}"
        compound.method != "BTREE" -> "Fail: compound method was ${compound.method}"
        else -> "OK"
    }
}
