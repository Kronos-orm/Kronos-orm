import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForSort
import com.kotlinorm.beans.dsl.KTableForSort.Companion.afterSort
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.types.ToSort

@Table(name = "tb_sort_custom")
data class CustomSortUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo {
    fun column(name: String): Field = __columns.single { it.name == name }
}

fun CustomSortUser.collectSort(block: ToSort<CustomSortUser, Any?>): List<Pair<Field, SqlOrdering>> {
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

    val user = CustomSortUser()
    val normal = user.collectSort { [it.id.asc(), it.name.desc()] }
    val single = user.collectSort { it.name.asc() }

    return when {
        normal != listOf(user.column("id") to SqlOrdering.Asc, user.column("name") to SqlOrdering.Desc) -> "Fail: normal sort was $normal"
        single != listOf(user.column("name") to SqlOrdering.Asc) -> "Fail: single sort was $single"
        else -> "OK"
    }
}
