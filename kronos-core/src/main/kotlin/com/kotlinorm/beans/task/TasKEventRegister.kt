package com.kotlinorm.beans.task

import com.kotlinorm.beans.task.QueryEvent.afterQueryEvents
import com.kotlinorm.beans.task.QueryEvent.beforeQueryEvents
import com.kotlinorm.interfaces.TaskEventPlugin
import com.kotlinorm.types.ActionTaskEventList
import com.kotlinorm.types.QueryTaskEventList

object QueryEvent {
    val beforeQueryEvents: QueryTaskEventList = mutableListOf()
    val afterQueryEvents: QueryTaskEventList = mutableListOf()
}

object ActionEvent {
    val beforeActionEvents: ActionTaskEventList = mutableListOf()
    val afterActionEvents: ActionTaskEventList = mutableListOf()
}

fun registerTaskEventPlugin(plugin: TaskEventPlugin) {
    plugin.doBeforeQuery?.let { beforeQueryEvents.add(it) }
    plugin.doAfterQuery?.let { afterQueryEvents.add(it) }
    plugin.doBeforeAction?.let { ActionEvent.beforeActionEvents.add(it) }
    plugin.doAfterAction?.let { ActionEvent.afterActionEvents.add(it) }
}