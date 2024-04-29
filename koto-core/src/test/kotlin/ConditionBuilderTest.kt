import com.kotoframework.beans.dsl.Criteria
import com.kotoframework.beans.dsl.Field
import com.kotoframework.enums.ConditionType
import kotlin.test.Test
import kotlin.test.assertEquals

class ConditionBuilderTest {

    @Test
    fun test() {
        val condition = Criteria(
            field = Field("id"),
            type = ConditionType.EQUAL,
            value = 1
        )

        val expect = "id = :id"
        val paramMap = mapOf("id" to 1)
        val (sql, paramMap2) = condition.build()
        assertEquals(expect, sql)
        assertEquals(paramMap, paramMap2)
    }
}