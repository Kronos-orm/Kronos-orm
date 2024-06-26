package com.kotlinorm.enums

/**
 * Enum class for cascade actions
 */
@Suppress("UNUSED_PARAMETER")
enum class CascadeDeleteAction(name: String) {
    CASCADE("CASCADE"),
    RESTRICT("RESTRICT"),
    SET_NULL("SET NULL"),
    NO_ACTION("NO ACTION"),
    SET_DEFAULT("SET DEFAULT");
}