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

package com.kotlinorm.orm.beans.sample.cascade.oneToMany


import com.kotlinorm.annotations.Cascade
import com.kotlinorm.annotations.CreateTime
import com.kotlinorm.annotations.DateTimeFormat
import com.kotlinorm.annotations.LogicDelete
import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.UpdateTime
import com.kotlinorm.annotations.Version
import com.kotlinorm.interfaces.KPojo
import java.time.LocalDateTime

@Table("tb_chapter")
data class Chapter(
    val id: Int? = null,
    val bookId: Int? = null, // 外键，关联到 Book'
    @Cascade(["bookId"], ["id"]) val book: Book? = null, // 一对多级联
    val title: String? = null, // 章节标题
    val content: String? = null, // 章节内容
    val chapterNumber: Int? = null, // 章节编号
    @UpdateTime
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val updateTime: LocalDateTime? = null,
    @CreateTime
    val createTime: LocalDateTime? = null,
    @Version
    val version: Int? = null,
    @LogicDelete
    val deleted: Boolean? = null,
) : KPojo