package com.kotoframework.enums;


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
}