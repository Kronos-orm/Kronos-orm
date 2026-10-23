package com.kotlinorm.utils

/** A per-thread stack holder; scopes must never bleed between concurrent queries. */
internal expect class PlatformThreadLocal<T>(initial: () -> T) {
    fun get(): T
    fun set(value: T)
    fun remove()
}

/** Small identity map; generated data-class equality must not merge distinct query sources. */
internal class IdentityHashMap<K : Any, V> : AbstractMutableMap<K, V>() {
    private val buckets = mutableMapOf<Int, MutableList<MutableMap.MutableEntry<K, V>>>()
    override val size: Int get() = buckets.values.sumOf { it.size }
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = object : AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
            override val size get() = this@IdentityHashMap.size
            override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> {
                val keys = buckets.values.flatMap { bucket -> bucket.map { it.key } }.iterator()
                var last: K? = null
                return object : MutableIterator<MutableMap.MutableEntry<K, V>> {
                    override fun hasNext() = keys.hasNext()
                    override fun next(): MutableMap.MutableEntry<K, V> {
                        val key = keys.next(); last = key
                        return buckets.getValue(platformIdentityHashCode(key)).first { it.key === key }
                    }
                    override fun remove() { this@IdentityHashMap.remove(last ?: error("Call next before remove")); last = null }
                }
            }
            override fun add(element: MutableMap.MutableEntry<K, V>): Boolean = error("Use put")
        }
    override fun get(key: K): V? = buckets[platformIdentityHashCode(key)]?.firstOrNull { it.key === key }?.value
    override fun containsKey(key: K): Boolean = buckets[platformIdentityHashCode(key)]?.any { it.key === key } == true
    override fun clear() = buckets.clear()
    override fun remove(key: K): V? {
        val hash = platformIdentityHashCode(key)
        val bucket = buckets[hash] ?: return null
        val index = bucket.indexOfFirst { it.key === key }
        if (index < 0) return null
        val result = bucket.removeAt(index).value
        if (bucket.isEmpty()) buckets.remove(hash)
        return result
    }
    override fun put(key: K, value: V): V? {
        val bucket = buckets.getOrPut(platformIdentityHashCode(key)) { mutableListOf() }
        val entry = bucket.firstOrNull { it.key === key }
        if (entry != null) return entry.setValue(value)
        bucket += object : MutableMap.MutableEntry<K, V> {
            override val key = key
            override var value = value
            override fun setValue(newValue: V): V = this.value.also { this.value = newValue }
        }
        return null
    }
}

internal expect fun platformIdentityHashCode(value: Any): Int

internal expect fun platformIsKPojoClass(declaration: kotlin.reflect.KClass<*>): Boolean
internal expect fun platformIsEnumClass(declaration: kotlin.reflect.KClass<*>): Boolean
internal expect fun platformClassAssignable(source: kotlin.reflect.KClass<*>, target: kotlin.reflect.KClass<*>): Boolean
internal expect fun platformIsCollectionClass(declaration: kotlin.reflect.KClass<*>): Boolean

internal expect fun registerPlatformGeneratedType(type: kotlin.reflect.KType, isKPojo: Boolean, isEnum: Boolean)

internal expect fun platformValidateKPojoFactoryType(type: kotlin.reflect.KType)

internal expect fun platformNamingLowercase(value: String): String

/** Distinguishes unavailable optional JVM types from cancellation and fatal runtime failures. */
internal expect fun platformIsUnavailableTypeMetadata(failure: Error): Boolean
