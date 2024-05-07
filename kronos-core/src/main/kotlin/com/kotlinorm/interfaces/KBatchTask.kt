package com.kotlinorm.interfaces

interface KBatchTask {
    val paramMap: Map<String, Any?>
    val paramMapArr: Array<Map<String, Any?>>?
}