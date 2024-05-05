package com.kotoframework.interfaces

interface KBatchTask {
    val paramMap: Map<String, Any?>
    val paramMapArr: Array<Map<String, Any?>>?
}