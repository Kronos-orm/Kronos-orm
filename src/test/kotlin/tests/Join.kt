//package tests
//
//import com.kotlinorm.Kronos
//import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
//import com.kotlinorm.orm.join.join
//import com.kotlinorm.orm.select.select
//import org.junit.jupiter.api.Test
//import tests.beans.User
//import tests.beans.UserRelation
//import kotlin.test.assertEquals
//
//class Join {
//    init {
//        Kronos.apply {
//            LineHumpNamingStrategy().let {
//                fieldNamingStrategy = it
//                tableNamingStrategy = it
//            }
//        }
//    }
//
//    @Test
//    fun testJoin() {
//        val (sql, paramMap) = User(1)
//            .join(
//                UserRelation(1, "123", 1, 1)
//            ) { user, relation ->
//                leftJoin(relation) { user.id == relation.id2 && user.gender == relation.gender }
//                select {
//                    user.id + relation.gender + user.select { it.gender }.where { it.id == 1 }.limit(1).alias("col")
//                }
//                where { user.id == 1 }
//                orderBy { user.id.desc }
//                page(1, 10)
//            }
//            .withTotal()
//
//        assertEquals("Select `user`.`id` as `id`, `relation`.`gender` as `gender`, (Select `gender` from `user` where `user`.`id` = 1 limit 1) as col from `user` left join `user_relation` as `relation` on `user`.`id` = `relation`.`id2` and `user`.`gender` = `relation`.`gender` where `user`.`id` = ? order by `user`.`id` desc limit 0, 10", sql)
//        assertEquals(mapOf("id" to 1), paramMap)
//    }
//}