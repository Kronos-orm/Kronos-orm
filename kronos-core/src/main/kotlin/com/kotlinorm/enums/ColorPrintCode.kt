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

package com.kotlinorm.enums


/**
 * Formatting parameters for console messages.
 */
enum class ColorPrintCode(val code: Int) {
    /**
     * Black color.
     */
    BLACK(30),

    /**
     * Black background.
     */
    BLACK_BACKGROUND(40),

    /**
     * Red color.
     */
    RED(31),

    /**
     * Red background.
     */
    RED_BACKGROUND(41),

    /**
     * Green color.
     */
    GREEN(32),

    /**
     * Green background.
     */
    GREEN_BACKGROUND(42),

    /**
     * Yellow color.
     */
    YELLOW(33),

    /**
     * Yellow background.
     */
    YELLOW_BACKGROUND(43),

    /**
     * Blue color.
     */
    BLUE(34),

    /**
     * Blue background.
     */
    BLUE_BACKGROUND(44),

    /**
     * Magenta (Fuchsia) color.
     */
    MAGENTA(35),

    /**
     * Magenta background.
     */
    MAGENTA_BACKGROUND(45),

    /**
     * Cyan color.
     */
    CYAN(36),

    /**
     * Cyan background.
     */
    CYAN_BACKGROUND(46),

    /**
     * Grey color.
     */
    GREY(37),

    /**
     * Grey background.
     */
    GREY_BACKGROUND(47),

    /**
     * Bold text.
     */
    BOLD(1),

    /**
     * Italic text.
     */
    ITALIC(3),

    /**
     * Underline text.
     */
    UNDERLINE(4);

    /**
     * Converts the current ColorPrintCode object to an array of ColorPrintCode objects.
     *
     * @return An array containing the current ColorPrintCode object.
     */
    fun toArray(): Array<ColorPrintCode> {
        return arrayOf(this)
    }

    companion object {
        val Green = GREEN
        val Red = RED
        val Blue = BLUE
        val Yellow = YELLOW
        val Magenta = MAGENTA
        val Cyan = CYAN
        val Grey = GREY
        val Black = BLACK
        val Bold = BOLD
        val Italic = ITALIC
        val Underline = UNDERLINE
    }
}