@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package example

import com.kotlinorm.Kronos
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.SelectClause
import com.kotlinorm.utils.initializeKronosMetadata
import kotlin.reflect.typeOf

inline fun <reified T : KPojo> materialize(query: SelectClause<*, T, *>, values: Map<String, Any?>): T =
    (Kronos.createKPojo(typeOf<T>()) as T).fromMapData<T>(values)

fun main() {
    initializeKronosMetadata()
    val first = Kronos.createKPojo(typeOf<Person>()) as Person
    val second = Kronos.createKPojo(typeOf<Person>()) as Person
    check(first == Person() && first !== second)

    val query = personProjection().orderBy { it.displayName.desc() }
    val projected = materialize(query, mapOf("id" to SAMPLE_PERSON_ID, "displayName" to SAMPLE_PERSON_NAME))
    val id: Int = projected.id
    val name: String = projected.displayName
    check(id == SAMPLE_PERSON_ID && name == SAMPLE_PERSON_NAME)
    println("OK factory=fresh models=shared projection=$id/$name")
}
