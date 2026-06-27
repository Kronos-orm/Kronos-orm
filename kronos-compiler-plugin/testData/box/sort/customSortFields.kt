import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForSort.Companion.afterSort
import com.kotlinorm.enums.SortType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToSort

@Table(name = "tb_sort_custom")
data class CustomSortUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo {
    fun column(name: String): Field = kronosColumns().single { it.name == name }
}

fun CustomSortUser.collectSort(block: ToSort<CustomSortUser, Any?>): List<Pair<Field, SortType>> {
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

    val user = CustomSortUser()
    val normal = user.collectSort { [it.id.asc(), it.name.desc()] }
    val single = user.collectSort { it.name.asc() }

    return when {
        normal != listOf(user.column("id") to SortType.ASC, user.column("name") to SortType.DESC) -> "Fail: normal sort was $normal"
        single != listOf(user.column("name") to SortType.ASC) -> "Fail: single sort was $single"
        else -> "OK"
    }
}
