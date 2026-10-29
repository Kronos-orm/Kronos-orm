@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package example

import com.kotlinorm.Kronos
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.SelectClause
import com.kotlinorm.utils.initializeKronosMetadata
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

data class TestPerson(var enabled: Boolean = true) : KPojo

private inline fun <reified T : KPojo> materialize(query: SelectClause<*, T, *>, values: Map<String, Any?>): T =
    (Kronos.createKPojo(typeOf<T>()) as T).fromMapData<T>(values)

class ProjectionTest {
    @Test
    fun mainAndTestFactoriesCreateIndependentTypedValues() {
        initializeKronosMetadata()
        val first = Kronos.createKPojo(typeOf<TestPerson>()) as TestPerson
        val second = Kronos.createKPojo(typeOf<TestPerson>()) as TestPerson
        assertEquals(TestPerson(), first)
        assertNotSame(first, second)
        assertEquals(Person(), Kronos.createKPojo(typeOf<Person>()))

        val projected = materialize(personProjection(), mapOf("id" to 9, "displayName" to "Grace"))
        val id: Int = projected.id
        val name: String = projected.displayName
        assertEquals(9, id)
        assertEquals("Grace", name)
    }
}
