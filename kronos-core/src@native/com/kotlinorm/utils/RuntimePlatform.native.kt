package com.kotlinorm.utils

import kotlin.reflect.KClass
import kotlin.native.identityHashCode

@kotlin.native.concurrent.ThreadLocal
private object ThreadValues {
    val values = mutableMapOf<Any, Any?>()
}
internal actual class PlatformThreadLocal<T> actual constructor(private val initial: () -> T) {
    @Suppress("UNCHECKED_CAST")
    actual fun get(): T = ThreadValues.values.getOrPut(this) { initial() } as T
    actual fun set(value: T) { ThreadValues.values[this] = value }
    actual fun remove() { ThreadValues.values.remove(this) }
}
internal actual fun platformIsKPojoClass(declaration: KClass<*>): Boolean =
    declaration == com.kotlinorm.interfaces.KPojo::class || typeLock.withLock { declaration in pojoClasses }
internal actual fun platformIsEnumClass(declaration: KClass<*>): Boolean =
    typeLock.withLock { declaration in enumClasses }
internal actual fun platformClassAssignable(source: KClass<*>, target: KClass<*>): Boolean =
    source == target || target == Any::class
internal actual fun platformIsCollectionClass(declaration: KClass<*>): Boolean =
    declaration in setOf(List::class, MutableList::class, Set::class, MutableSet::class,
        Collection::class, MutableCollection::class, Iterable::class, arrayOf<Any>()::class,
        BooleanArray::class, ByteArray::class, CharArray::class, DoubleArray::class,
        FloatArray::class, IntArray::class, LongArray::class, ShortArray::class)

private val typeLock = StaticTypeMetadataLock()
private val pojoClasses = mutableSetOf<KClass<*>>()
private val enumClasses = mutableSetOf<KClass<*>>()
internal actual fun registerPlatformGeneratedType(type: kotlin.reflect.KType, isKPojo: Boolean, isEnum: Boolean) {
    val classifier = type.classifier as? KClass<*> ?: return
    typeLock.withLock {
        if (isKPojo) pojoClasses += classifier
        if (isEnum) enumClasses += classifier
    }
}

internal actual fun platformValidateKPojoFactoryType(type: kotlin.reflect.KType) = Unit

internal actual fun platformNamingLowercase(value: String): String = value.lowercase()

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
internal actual fun platformIdentityHashCode(value: Any): Int = value.identityHashCode()

internal actual fun platformIsUnavailableTypeMetadata(failure: Error): Boolean = false
