package com.kotlinorm.enums

enum class IgnoreAction {
    ALL, // set `isColumn` to false
//    INSERT,
//    UPDATE,
//    DELETE,
    SELECT,
    CASCADE_SELECT;
}