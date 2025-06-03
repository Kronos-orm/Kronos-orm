/**
 * Copyright 2022-2025 kronos-orm
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
package com.kotlinorm.enums

import com.kotlinorm.interfaces.PrintCode

enum class PrintBackground(override val code: Int) : PrintCode {
        BLACK_BACKGROUND(40),
        RED_BACKGROUND(41),
        GREEN_BACKGROUND(42),
        YELLOW_BACKGROUND(43),
        BLUE_BACKGROUND(44),
        MAGENTA_BACKGROUND(45),
        CYAN_BACKGROUND(46),
        GREY_BACKGROUND(47),
    }