package com.kotlinorm.utils

import kotlin.reflect.KClass
import kotlin.jvm.javaObjectType

internal actual class PlatformThreadLocal<T> actual constructor(initial: () -> T) {
    private val local = ThreadLocal.withInitial(initial)
    actual fun get(): T = local.get()
    actual fun set(value: T) = local.set(value)
    actual fun remove() = local.remove()
}
internal actual fun platformIsKPojoClass(declaration: KClass<*>): Boolean =
    com.kotlinorm.interfaces.KPojo::class.java.isAssignableFrom(declaration.java)
internal actual fun platformIsEnumClass(declaration: KClass<*>): Boolean = declaration.java.isEnum
internal actual fun platformClassAssignable(source: KClass<*>, target: KClass<*>): Boolean =
    target.javaObjectType.isAssignableFrom(source.javaObjectType)
internal actual fun platformIsCollectionClass(declaration: KClass<*>): Boolean =
    declaration.java.isArray || Iterable::class.java.isAssignableFrom(declaration.java)

internal actual fun registerPlatformGeneratedType(type: kotlin.reflect.KType, isKPojo: Boolean, isEnum: Boolean) = Unit

internal actual fun platformValidateKPojoFactoryType(type: kotlin.reflect.KType) { type.normalizedKPojoType() }

internal actual fun platformNamingLowercase(value: String): String = value.lowercase(java.util.Locale.getDefault())

internal actual fun platformIdentityHashCode(value: Any): Int = System.identityHashCode(value)

internal actual fun platformIsUnavailableTypeMetadata(failure: Error): Boolean = failure is LinkageError
