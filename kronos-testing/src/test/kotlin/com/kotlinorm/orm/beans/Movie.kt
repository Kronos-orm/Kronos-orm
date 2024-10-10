package com.kotlinorm.orm.beans

import com.kotlinorm.annotations.LogicDelete
import com.kotlinorm.annotations.UpdateTime
import com.kotlinorm.interfaces.KPojo

data class Movie(
    val id: Long? = null, // 主键
    val name: String? = null, // 名称
    val year: Int? = null, // 年代
    val director: String? = null, // 导演
    val actor: String? = null, // 演员
    val type: String? = null, // 类型
    val country: String? = null, // 国家
    val language: String? = null, // 语言
    val description: String? = null, // 简介
    val poster: String? = null, // 海报
    val video: String? = null, // 视频
    val summary: String? = null, // 简介
    val tags: String? = null, // 标签
    val score: Double? = null, // 评分
    val vote: Int? = null, // 评分人数
    val favorite: Int? = null, // 收藏人数
    @LogicDelete val deleted: Boolean? = null, // 逻辑删除
    @UpdateTime val updateTime: String? = null, // 逻辑删除
) : KPojo