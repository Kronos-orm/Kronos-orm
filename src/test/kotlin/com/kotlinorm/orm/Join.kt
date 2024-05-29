//package com.kotlinorm.orm//package tests
//
//import com.kotlinorm.Kronos
//import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
//import com.kotlinorm.orm.beans.User
//import com.kotlinorm.orm.beans.UserRelation
//import com.kotlinorm.orm.join.join
//import org.junit.jupiter.api.Test
//import kotlin.test.assertEquals
//
//class Join {
//    init {
//        Kronos.apply {
//            fieldNamingStrategy = LineHumpNamingStrategy
//            tableNamingStrategy = LineHumpNamingStrategy
//        }
//    }
//
//    @Test
//    fun testJoin() {
//        val (sql, paramMap) =
//            User(1).join(
//                UserRelation(1, "123", 1, 1),
//            ) { user, relation ->
//                leftJoin(relation) { user.id == relation.id2 && user.gender == relation.gender }
//                select {
//                    user.id + relation.gender
//                }
//                where { user.id == 1 }
//                orderBy { user.id.desc() }
//            }.build()
//
//        assertEquals(
//            "Select `user`.`id` as `id`, `relation`.`gender` as `gender`, (Select `gender` from `user` where `user`.`id` = 1 limit 1) as col from `user` left join `user_relation` as `relation` on `user`.`id` = `relation`.`id2` and `user`.`gender` = `relation`.`gender` where `user`.`id` = ? order by `user`.`id` desc limit 0, 10",
//            sql
//        )
//        assertEquals(mapOf("id" to 1), paramMap)
//    }
//}