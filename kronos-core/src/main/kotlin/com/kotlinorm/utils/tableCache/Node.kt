package com.kotlinorm.utils.tableCache

class Node(var key: String?, var value: TableObject?) {
    var prev: Node? = null
    var next: Node? = null
}