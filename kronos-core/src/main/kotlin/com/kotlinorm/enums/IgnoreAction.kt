package com.kotlinorm.enums

enum class IgnoreAction {
    ALL, // set `isColumn` to false
    FROM_MAP, // from map to pojo will ignore this field
    TO_MAP, // from pojo to map will ignore this field
//    INSERT,
//    UPDATE,
//    DELETE,
    SELECT,
    CASCADE_SELECT;
}