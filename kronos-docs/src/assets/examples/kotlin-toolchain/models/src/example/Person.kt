package example

import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select

const val SAMPLE_PERSON_ID = 7
const val SAMPLE_PERSON_NAME = "Ada"

data class Person(var id: Int = 0, var name: String = SAMPLE_PERSON_NAME) : KPojo

// Both applications consume this compiled library's inferred projection type.
fun personProjection() = Person(id = SAMPLE_PERSON_ID, name = SAMPLE_PERSON_NAME).select {
    [it.id, it.name.alias("displayName")]
}
