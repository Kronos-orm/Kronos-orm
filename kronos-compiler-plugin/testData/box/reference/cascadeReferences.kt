/**
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Verifies cascade reference DSL extraction from class-qualified recursive references.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Cascade
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForReference.Companion.afterReference
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToReference
import kotlin.reflect.KClass

@Table(name = "tb_ref_carrier")
data class RefCarrier(
    var id: Int? = null,
    @Cascade(["id"], ["carrierId"])
    var cars: List<RefCar>? = null,
) : KPojo

@Table(name = "tb_ref_car")
data class RefCar(
    var id: Int? = null,
    var carrierId: Int? = null,
    @Cascade(["id"], ["carId"])
    var door: RefDoor? = null,
) : KPojo

@Table(name = "tb_ref_door")
data class RefDoor(
    var id: Int? = null,
    var carId: Int? = null,
    @Cascade(["id"], ["doorId"])
    var lock: RefLock? = null,
) : KPojo

@Table(name = "tb_ref_lock")
data class RefLock(
    var id: Int? = null,
    var doorId: Int? = null,
) : KPojo

fun RefCarrier.collectReference(block: ToReference<RefCarrier, Any?>): List<Field> {
    val result = mutableListOf<Field>()
    afterReference {
        block!!(it)
        result += fields
    }
    return result
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val result = RefCarrier().collectReference { [RefCarrier::cars, RefCar::door, RefDoor::lock] }
    val names = result.map { it.name }
    val tables = result.map { it.tableName }
    val cascadeColumns = result.map { it.isColumn }
    val carsKType = result[0].kType
    val carsClassifier = carsKType?.classifier as? KClass<*>
    val carsElementType = carsKType?.arguments?.singleOrNull()?.type
    val carsElementClassifier = carsElementType?.classifier as? KClass<*>
    val carsDerivedElementType = result[0].elementKType
    val carsDerivedElementClassifier = carsDerivedElementType?.classifier as? KClass<*>

    return when {
        names != listOf("cars", "door", "lock") -> "Fail: refs were $names"
        tables != listOf("tb_ref_carrier", "tb_ref_car", "tb_ref_door") -> "Fail: tables were $tables"
        cascadeColumns != listOf(false, false, false) -> "Fail: cascade column flags were $cascadeColumns"
        !result[0].cascadeIsCollectionOrArray -> "Fail: cars should be marked as collection cascade"
        carsClassifier != List::class -> "Fail: cars kType classifier was $carsClassifier"
        carsElementClassifier != RefCar::class -> "Fail: cars kType element was $carsElementType"
        carsDerivedElementType != carsElementType -> "Fail: cars elementKType was $carsDerivedElementType, expected $carsElementType"
        carsDerivedElementClassifier != RefCar::class -> "Fail: cars elementKType classifier was $carsDerivedElementClassifier"
        carsDerivedElementType?.arguments?.isNotEmpty() == true -> "Fail: cars elementKType arguments were ${carsDerivedElementType.arguments}"
        else -> "OK"
    }
}
