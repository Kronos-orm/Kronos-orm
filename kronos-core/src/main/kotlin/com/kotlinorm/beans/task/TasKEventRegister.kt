/**
 * Copyright 2022-2024 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotlinorm.beans.task

import com.kotlinorm.beans.task.ActionEvent.afterActionEvents
import com.kotlinorm.beans.task.ActionEvent.beforeActionEvents
import com.kotlinorm.beans.task.QueryEvent.afterQueryEvents
import com.kotlinorm.beans.task.QueryEvent.beforeQueryEvents
import com.kotlinorm.interfaces.TaskEventPlugin
import com.kotlinorm.types.ActionTaskEventList
import com.kotlinorm.types.QueryTaskEventList

/**
 * Task Event Register
 *
 * Object to register task events
 *
 * @author OUSC
 */
object QueryEvent {
    val beforeQueryEvents: QueryTaskEventList = mutableListOf()
    val afterQueryEvents: QueryTaskEventList = mutableListOf()
}

/**
 * Action Event Register
 *
 * Object to register action events
 *
 * @author OUSC
 */
object ActionEvent {
    val beforeActionEvents: ActionTaskEventList = mutableListOf()
    val afterActionEvents: ActionTaskEventList = mutableListOf()
}

/**
 * Register Task Event Plugin
 *
 * Function to register task event plugin
 *
 * @param plugin the task event plugin
 */
fun registerTaskEventPlugin(plugin: TaskEventPlugin) {
    plugin.doBeforeQuery?.let { beforeQueryEvents.add(it) }
    plugin.doAfterQuery?.let { afterQueryEvents.add(it) }
    plugin.doBeforeAction?.let { beforeActionEvents.add(it) }
    plugin.doAfterAction?.let { afterActionEvents.add(it) }
}

fun unregisterTaskEventPlugin(plugin: TaskEventPlugin) {
    plugin.doBeforeQuery?.let { beforeQueryEvents.remove(it) }
    plugin.doAfterQuery?.let { afterQueryEvents.remove(it) }
    plugin.doBeforeAction?.let { beforeActionEvents.remove(it) }
    plugin.doAfterAction?.let { afterActionEvents.remove(it) }
}