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

// Verifies Ignore.ALL annotation targets omit scalar metadata while partial and empty targets keep diagnostics.

import com.kotlinorm.annotations.Ignore
import com.kotlinorm.enums.IgnoreAction
import com.kotlinorm.interfaces.KPojo

private enum class HiddenIgnoredStatus { READY }

internal class IgnoredTargetsEntity : KPojo {
    @Ignore
    private var defaultTargets: HiddenIgnoredStatus = HiddenIgnoredStatus.READY

    @Ignore([IgnoreAction.ALL])
    private var positionalTargets: HiddenIgnoredStatus = HiddenIgnoredStatus.READY

    @Ignore(targets = [IgnoreAction.ALL])
    private var namedTargets: HiddenIgnoredStatus = HiddenIgnoredStatus.READY

    @Ignore(targets = [IgnoreAction.SELECT, IgnoreAction.ALL])
    private var mixedTargets: HiddenIgnoredStatus = HiddenIgnoredStatus.READY

    <!KRONOS_INACCESSIBLE_ENUM_METADATA!>@Ignore(targets = [])
    private var emptyTargets: HiddenIgnoredStatus = HiddenIgnoredStatus.READY<!>

    <!KRONOS_INACCESSIBLE_ENUM_METADATA!>@Ignore(targets = [IgnoreAction.SELECT])
    private var partialTargets: HiddenIgnoredStatus = HiddenIgnoredStatus.READY<!>

    <!KRONOS_INACCESSIBLE_ENUM_METADATA!>@Ignore(targets = [IgnoreAction.FROM_MAP, IgnoreAction.TO_MAP])
    private var mappingTargets: HiddenIgnoredStatus = HiddenIgnoredStatus.READY<!>
}
