package com.kotlinorm.utils.lruCache

class Node(var key: String?, var value: TableObject?) {
    var prev: Node? = null
    var next: Node? = null
}