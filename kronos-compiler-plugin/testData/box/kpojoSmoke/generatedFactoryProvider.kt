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

// Verifies that the generated KPojo factory provider registers direct constructor factories.

import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.KPojoFactoryProvider
import com.kotlinorm.utils.createInstance

data class FactoryProviderUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

data class FactoryProviderRequiredUser(
    var id: Int,
) : KPojo

fun box(): String {
    val providerClass = Class.forName("com.kotlinorm.generated.factory.KronosGeneratedKPojoFactoryProvider")
    val provider = providerClass.getDeclaredConstructor().newInstance() as KPojoFactoryProvider
    provider.register()

    val user = FactoryProviderUser::class.createInstance()
    val requiredFailure = runCatching { FactoryProviderRequiredUser::class.createInstance() }.exceptionOrNull()
    return when {
        user.id != null -> "Fail: id was ${user.id}"
        user.name != null -> "Fail: name was ${user.name}"
        requiredFailure !is NullPointerException -> "Fail: required constructor failure was $requiredFailure"
        else -> "OK"
    }
}
