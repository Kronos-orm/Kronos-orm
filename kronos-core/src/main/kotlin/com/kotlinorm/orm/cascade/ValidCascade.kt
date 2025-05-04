/**
 * Copyright 2022-2024 kronos-orm
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

package com.kotlinorm.orm.cascade

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KCascade
import com.kotlinorm.cache.kPojoAllFieldsCache
import com.kotlinorm.enums.IgnoreAction.CASCADE_SELECT
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.utils.createInstance
import kotlin.reflect.KClass

/**
 * Represents a valid cascade within the context of ORM operations.
 *
 * This data class encapsulates a field, its corresponding cascade, and the cascaded POJO. It is primarily used to
 * determine the cascade relationships between this property and other entities and the specifics of the cascade,
 * such as which fields are used to relate, maintain the relationship, where the cascade information is declared, etc.
 *
 * ORM 操作上下文中的有效引用。
 *
 * 此数据类封装了一个KPojo的属性及其关联关系信息。它主要用于在获取该属性与其他实体之间存在的引用关系及具体的引用细节，如通过哪些字段关联、维护端、关联信息声明在哪个表等。
 *
 * @property field The [Field] instance representing the field in the POJO that holds the cascade.
 * @property kCascade The [KCascade] instance representing the cascade details such as the target table and columns.
 * @property refPojo The [KPojo] instance of the cascaded POJO, providing access to its properties and methods.
 * @property tableName The [tableName] that this cascade be announced.
 */
data class ValidCascade(
    val field: Field,
    val kCascade: KCascade,
    val refPojo: KPojo,
    val tableName: String,
    val mapperByThis: Boolean = true
)

/**
 * Identifies and constructs a list of valid cascades for ORM operations based on the provided columns and operation type.
 *
 * This function filters through a list of [Field] objects to find those that are not directly mapped to database columns but have associated cascades.
 * It then uses reflection to instantiate the cascaded POJOs. Depending on whether the field has a cascade mapping and is applicable for the specified operation type,
 * it either returns the direct cascades or searches through the cascaded POJO's columns for any that have a cascade mapping back to the original table and are applicable for the operation.
 * This process constructs a list of [ValidCascade] objects, each encapsulating a field, its cascades, and the instantiated cascaded POJO, which are essential for operations like cascading deletes.
 *
 * This function is a core part of the ORM's cascading operations, used to extract valid, usable cascades in different cascade operations to build a tree-like structure of cascading operations.
 *
 * 根据提供的列和操作类型识别并构建 ORM 操作的有效引用列表。
 *
 * 此函数通过 [Field] 对象列表进行筛选，以查找未直接映射到数据库列但具有相关引用的对象。
 * 然后，它使用反射来实例化引用的 POJO。根据字段是否具有级联映射以及是否适用于指定的操作类型，
 * 它要么返回直接引用，要么在引用的 POJO 的列中搜索任何具有级联映射回原始表并适用于该操作的对象。
 * 此过程构造一个 [ValidCascade] 对象列表，每个对象都封装一个字段、其引用和实例化的引用 POJO，这些对象对于级联删除等操作至关重要。
 *
 * 此函数是 ORM 级联操作的核心部分，它用于在不同的级联操作中提取出有效的、可用的引用，以便构建级联操作的树形结构。
 *
 *
 * @param columns A list of [Field] objects representing the columns of a POJO, including those that are not directly mapped to database columns but have associated references.
 * @param operationType The [KOperationType] indicating the type of ORM operation (e.g., DELETE) for which the cascades are being validated.
 * @param allowed A set of strings representing the names of columns that are allowed for the specified operation type.
 * @param allowAll A boolean flag indicating whether not specifying any allowed columns means all columns are allowed.
 * @return A list of [ValidCascade] objects representing valid cascades for the specified operation type.
 */
fun findValidRefs(
    kClass: KClass<out KPojo>, columns: Collection<Field>, operationType: KOperationType, allowed: Set<String>?, allowAll: Boolean
): List<ValidCascade> {
    //columns 为的非数据库列、有关联注解且用于删除操作的Field
    return columns.asSequence().filter { !it.isColumn && (allowed == null || it.name in allowed || allowAll) && it.kClass != null }.map { col ->
        //如果是Select并且该列有Ignore[cascadeSelect] ，且没有明确指定允许当前列，直接返回空
        if (col.ignore?.contains(CASCADE_SELECT) == true && allowAll && operationType == KOperationType.SELECT) {
            return@map listOf()
        }

        //否则首先判断该列是否是维护级联映射的，如果是，直接返回引用 / SELECT时不区分是否为维护端，需要用户手动指定Ignore或者cascade的属性
        return@map if ((col.cascade != null && col.refUseFor(operationType)) || (operationType == KOperationType.SELECT && col.cascade != null)) {
            if (operationType == KOperationType.DELETE) return@map listOf() // 插入操作不允许子级向上级级联
            val ref =
                col.kClass!!.createInstance() // 通过反射创建引用的类的POJO，支持类型为KPojo/Collections<KPojo>
            listOf(
                ValidCascade(col, col.cascade, ref, col.tableName)
            ) // 若有级联映射，返回引用
        } else {
            val ref =
                col.kClass!!.createInstance() // 通过反射创建引用的类的POJO，支持类型为KPojo/Collections<KPojo>
            val tableName = ref.kronosTableName() // 获取引用所在的表名
            kPojoAllFieldsCache[col.kClass]!!.asSequence().filter {
                it.cascade != null && it.tableName == tableName && it.refUseFor(operationType) && it.kClass == kClass
            }.map {
                ValidCascade(col, it.cascade!!, ref, tableName, false)
            }.toList() // 若没有级联映射，返回引用的所有关于本表级联映射
        }
    }.toList().flatten()
}