package com.kotlinorm.utils

import com.kotlinorm.interfaces.KPojo
import kotlin.reflect.KClass

var kClassCreator: (KClass<out KPojo>) -> KPojo? = { null }
var kClassCreatorCustom: (KClass<out KPojo>) -> KPojo? = { null }

@Suppress("UNCHECKED_CAST")
fun <T : KPojo> KClass<T>.createInstance(): T {
    return kClassCreator(this) as T? ?: kClassCreatorCustom(this) as T? ?: throw NullPointerException(
        "KClass ${this.simpleName} instantiation failed \n1.Please check if the data class has a no-argument constructor. \n2.if you did not add `Kronos.init{}` to your code, please add it" + " to the code, this is a necessary step to use the ORM framework \n3.if you have added it, Maybe you're referencing some third-party library and using a KPojo class defined by them，" + "please add a custom instantiation method to the `kClassCreatorCustom` like this: \nkClassCreatorCustom = { kClass -> \n\twhen (kClass) {\n\t\tYourKPojo::class -> YourKPojo()\n\t\telse -> null\n\t}\n}"
    )
}