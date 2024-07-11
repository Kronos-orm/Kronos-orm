package com.kotlinorm.orm.cascade

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.dsl.KReference
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.utils.LRUCache
import kotlin.reflect.KFunction

/**
 * Represents a valid reference within the context of ORM operations.
 *
 * This data class encapsulates a field, its corresponding reference, and the referenced POJO. It is primarily used
 * in operations that require knowledge of the relationships between different entities in the ORM, such as cascading deletes.
 *
 * @property field The [Field] instance representing the field in the POJO that holds the reference.
 * @property reference The [KReference] instance representing the reference details such as the target table and columns.
 * @property refPojo The [KPojo] instance of the referenced POJO, providing access to its properties and methods.
 */
data class ValidRef(
    val field: Field,
    val reference: KReference,
    val refPojo: KPojo
)

/**
 * Identifies and constructs a list of valid references for ORM operations based on the provided columns and operation type.
 *
 * This function filters through a list of [Field] objects to find those that are not directly mapped to database columns but have associated references.
 * It then uses reflection to instantiate the referenced POJOs. Depending on whether the field has a cascade mapping and is applicable for the specified operation type,
 * it either returns the direct reference or searches through the referenced POJO's columns for any that have a cascade mapping back to the original table and are applicable for the operation.
 * This process constructs a list of [ValidRef] objects, each encapsulating a field, its reference, and the instantiated referenced POJO, which are essential for operations like cascading deletes.
 *
 * 根据提供的列和操作类型识别并构建 ORM 操作的有效引用列表。
 *
 * 此函数通过 [Field] 对象列表进行筛选，以查找未直接映射到数据库列但具有相关引用的对象。
 * 然后，它使用反射来实例化引用的 POJO。根据字段是否具有级联映射以及是否适用于指定的操作类型，
 * 它要么返回直接引用，要么在引用的 POJO 的列中搜索任何具有级联映射回原始表并适用于该操作的对象。
 * 此过程构造一个 [ValidRef] 对象列表，每个对象都封装一个字段、其引用和实例化的引用 POJO，这些对象对于级联删除等操作至关重要。
 *
 *
 * @param columns A list of [Field] objects representing the columns of a POJO, including those that are not directly mapped to database columns but have associated references.
 * @param operationType The [KOperationType] indicating the type of ORM operation (e.g., DELETE) for which the references are being validated.
 * @return A list of [ValidRef] objects representing valid references for the specified operation type.
 */
fun findValidRefs(columns: List<Field>, operationType: KOperationType): List<ValidRef> {
    //columns 为的非数据库列、有关联注解且用于删除操作的Field
    return columns.filter { !it.isColumn }.map { col ->
        val ref =
            col.referenceKClassName.kConstructor.callBy(emptyMap()) as KPojo // 通过反射创建引用的类的POJO，支持类型为KPojo/Collections<KPojo>
        if (col.cascadeMapperBy() && col.refUseFor(operationType)) {
            listOf(col.reference!!) // 若有级联映射，返回引用
        } else {
            ref.kronosColumns()
                .filter { it.cascadeMapperBy(col.tableName) && it.refUseFor(operationType) }
                .map { it.reference!! } // 若没有级联映射，返回引用的所有关于本表级联映射
        }.map { reference ->
            ValidRef(col, reference, ref)
        }
    }.flatten()
}

private val lruCacheOfConstructor = LRUCache<String, KFunction<*>>(128) // 用于存储实例化的对象

/**
 * Instantiates an object from a class name string, utilizing a cache to improve performance.
 *
 * This extension function for nullable String objects attempts to instantiate an object of the class specified by the string.
 * If the string is null or the class cannot be found, it throws an UnsupportedOperationException.
 * To optimize performance, especially for repeated instantiations of the same class, this function uses an LRU (Least Recently Used) cache.
 * If an instance of the specified class name already exists in the cache, it is returned directly to avoid redundant instantiation.
 * Otherwise, a new instance is created using reflection, added to the cache, and then returned.
 *
 *  从类名字符串实例化对象，利用缓存来提高性能。
 *
 * 此可空字符串对象的扩展函数尝试实例化字符串指定的类的对象。
 * 如果字符串为空或找不到该类，则会抛出 UnsupportedOperationException。
 * 为了优化性能，尤其是对于同一类的重复实例化，此函数使用 LRU（最近最少使用）缓存。
 * 如果缓存中已存在指定类名的实例，则直接返回以避免冗余实例化。
 * 否则，使用反射创建一个新实例，将其添加到缓存中，然后返回。
 *
 * @receiver A nullable String representing the fully qualified name of the class to instantiate.
 * @return The instantiated object of the specified class.
 * @throws UnsupportedOperationException if the class name is null or the class cannot be found.
 */
private val String?.kConstructor
    get(): KFunction<*> {
        this ?: throw UnsupportedOperationException("The reference class is not supported!")
        return lruCacheOfConstructor.getOrPut(this) {
            Class.forName(this).kotlin.constructors.first() //若没有无参构造函数，抛出异常
        }
    }