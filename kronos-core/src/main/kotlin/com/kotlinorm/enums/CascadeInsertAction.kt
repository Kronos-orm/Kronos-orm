package com.kotlinorm.enums

/**
 * Enum class for cascade actions
 */
@Suppress("UNUSED_PARAMETER")
enum class CascadeInsertAction(name: String) {
    CASCADE("CASCADE"),
    NO_ACTION("NO ACTION"),
}