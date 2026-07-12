import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForSort
import com.kotlinorm.beans.dsl.KTableForSort.Companion.afterSort
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.types.ToSort

@Table(name = "tb_sort_user")
data class SortUser(
    var id: Int? = null,
    var name: String? = null,
    @Column("created_at")
    var createdAt: String? = null,
) : KPojo {
    fun column(name: String): Field {
        return __columns.single { it.name == name }
    }
}

fun SortUser.collectSort(block: ToSort<SortUser, Any?>): List<Pair<Field, SqlOrdering>> {
    var result: List<Pair<Field, SqlOrdering>>? = null
    afterSort {
        block!!(it)
        result = sortedItems.map { item ->
            val fieldItem = item as? KTableForSort.SortItem.FieldItem
                ?: error("Expected field sort item, got $item")
            fieldItem.field to fieldItem.ordering
        }
    }
    return result ?: error("sort block did not run")
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = SortUser()
    val result = user.collectSort { [it.id, it.name.desc(), it.createdAt.asc()] }

    return when {
        result != listOf(
            user.column("id") to SqlOrdering.Asc,
            user.column("name") to SqlOrdering.Desc,
            user.column("createdAt") to SqlOrdering.Asc,
        ) -> "Fail: sort result was $result"
        result[2].first.columnName != "created_at" -> "Fail: columnName was ${result[2].first.columnName}"
        else -> "OK"
    }
}
