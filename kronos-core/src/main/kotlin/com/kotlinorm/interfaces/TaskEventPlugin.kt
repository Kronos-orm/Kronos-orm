package com.kotlinorm.interfaces

import com.kotlinorm.types.ActionTaskEvent
import com.kotlinorm.types.QueryTaskEvent

interface TaskEventPlugin {
    val doBeforeQuery: QueryTaskEvent?
    val doAfterQuery: QueryTaskEvent?
    val doBeforeAction: ActionTaskEvent?
    val doAfterAction: ActionTaskEvent?
}