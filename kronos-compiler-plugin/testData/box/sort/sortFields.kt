import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForSort.Companion.afterSort
import com.kotlinorm.enums.SortType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToSort

@Table(name = "tb_sort_user")
data class SortUser(
    var id: Int? = null,
    var name: String? = null,
    @Column("created_at")
    var createdAt: String? = null,
) : KPojo {
    fun column(name: String): Field {
        return kronosColumns().single { it.name == name }
    }
}

fun SortUser.collectSort(block: ToSort<SortUser, Any?>): List<Pair<Field, SortType>> {
    var result: List<Pair<Field, SortType>>? = null
    afterSort {
        block!!(it)
        result = sortedFields.toList()
    }
    return result ?: error("sort block did not run")
}

fun box(): String {
    Kronos.init {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = SortUser()
    val result = user.collectSort { [it.id, it.name.desc(), it.createdAt.asc()] }

    return when {
        result != listOf(
            user.column("id") to SortType.ASC,
            user.column("name") to SortType.DESC,
            user.column("createdAt") to SortType.ASC,
        ) -> "Fail: sort result was $result"
        result[2].first.columnName != "created_at" -> "Fail: columnName was ${result[2].first.columnName}"
        else -> "OK"
    }
}
