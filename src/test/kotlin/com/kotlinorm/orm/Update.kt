package com.kotlinorm.orm

import com.kotlinorm.Kronos
import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
import com.kotlinorm.enums.NoValueStrategy
import com.kotlinorm.orm.beans.Movie
import com.kotlinorm.orm.beans.User
import com.kotlinorm.orm.update.UpdateClause.Companion.build
import com.kotlinorm.orm.update.UpdateClause.Companion.by
import com.kotlinorm.orm.update.UpdateClause.Companion.set
import com.kotlinorm.orm.update.UpdateClause.Companion.where
import com.kotlinorm.orm.update.update
import com.kotlinorm.orm.update.updateExcept
import com.kotlinorm.utils.execute
import com.kotlinorm.utils.toAsyncTask
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class Update {

    init {
        Kronos.apply {
            fieldNamingStrategy = LineHumpNamingStrategy
            tableNamingStrategy = LineHumpNamingStrategy
        }
    }


    private val user = User(1)
    private val testUser = User(1, "test")

    @Test
    fun testUpdateWithSetAndBy() {
        val (sql, paramMap) =
            user.update()
                .set {
                    // Field("username").setValue("123")
                    it.username = "123"
                    it.gender = 1
                }
                .by { it.id }
                .build()


        println(sql)
        println(paramMap)

        assertEquals("UPDATE `tb_user` SET `username` = :usernameNew, `gender` = :genderNew WHERE `id` = :id", sql)
        assertEquals(mapOf("id" to 1, "usernameNew" to "123", "genderNew" to 1), paramMap)
        // Update tb_user set username = '123' where id = 1

    }

    @Test
    fun testUpdateWithSetAndByMultiFields() {
        val (sql, paramMap) = testUser.update()
            .set {
                it.username = "123"
                it.gender = 1
            }
            .by { it.id + it.username }
            .build()


        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `username` = :usernameNew, `gender` = :genderNew WHERE `id` = :id AND `username` = :username",
            sql
        )
        assertEquals(mapOf("id" to 1, "usernameNew" to "123", "genderNew" to 1, "username" to "test"), paramMap)
        // Update tb_user set username = '123' where id = 1

    }

    @Test
    fun testUpdateMultiFieldsWithSetAndBy() {
        val (sql, paramMap) = user.update { it.username + it.gender }
            .by { it.id }
            .build()

        println(sql)
        println(paramMap)

        assertEquals("UPDATE `tb_user` SET `username` = :usernameNew, `gender` = :genderNew WHERE `id` = :id", sql)
        assertEquals(mapOf("id" to 1, "usernameNew" to null, "genderNew" to null), paramMap)

    }

    @Test
    fun testUpdateExceptWithSetAndBy() {
        val (sql, paramMap) = testUser.updateExcept { it.username }
            .by { it.id }
            .build()

        println(sql)
        println(paramMap)

        assertEquals("UPDATE `tb_user` SET `gender` = :genderNew, `id` = :idNew WHERE `id` = :id", sql)
        assertEquals(mapOf("idNew" to 1, "genderNew" to null, "usernameNew" to "test", "id" to 1), paramMap)
        // Update tb_user set username = 'test' where id = 1
    }

    @Test
    fun testUpdateWithSetAndWhere() {
        val (sql, paramMap) = user.update()
            .set { it.gender = 1 }
            .where { it.id == 1 }
            .build()

        assertEquals("UPDATE `tb_user` SET `gender` = :genderNew WHERE `id` = :id", sql)
        assertEquals(mapOf("id" to 1, "genderNew" to 1), paramMap)
        // Update tb_user set gender = 1 where id = 1
    }

    @Test
    fun testUpdateWhereComparable() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.id < 1 && it.id > 0 }.build()
        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew WHERE `id` < :idMax AND `id` > :idMin", sql
        )
        assertEquals(mapOf("idNew" to 1, "usernameNew" to "test", "idMin" to 0, "idMax" to 1), paramMap)
        // Update tb_user set id = 1, username = 1 where id < 1 and id > 0
    }

    @Test
    fun testUpdateWhereNeq() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.id.neq }.build()

        println(sql)
        println(paramMap)

        assertEquals("UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew WHERE `id` != :id", sql)
        assertEquals(mapOf("id" to 1, "idNew" to 1, "usernameNew" to "test"), paramMap)
    }

    @Test
    fun testUpdateWhereNotEqual() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.id != 1 }.build()

        println(sql)
        println(paramMap)

        assertEquals("UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew WHERE `id` != :id", sql)
        assertEquals(mapOf("id" to 1, "idNew" to 1, "usernameNew" to "test"), paramMap)
    }

    @Test
    fun testUpdateWhereLikeInfix() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.username like "%t" }.build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew WHERE `username` LIKE :username",
            sql
        )
        assertEquals(mapOf("idNew" to 1, "usernameNew" to "test", "username" to "%t"), paramMap)
    }

    @Test
    fun testUpdateWhereLikeBracket() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.username.like("%t") }.build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew WHERE `username` LIKE :username",
            sql
        )
        assertEquals(mapOf("idNew" to 1, "usernameNew" to "test", "username" to "%t"), paramMap)
    }

    @Test
    fun testUpdateWhereNotLikeInfix() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.username notLike "%t" }.build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew WHERE `username` NOT LIKE :username",
            sql
        )
        assertEquals(mapOf("idNew" to 1, "usernameNew" to "test", "username" to "%t"), paramMap)
    }

    @Test
    fun testUpdateWhereNotLikeBracket() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.username.notLike("%t") }.build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew WHERE `username` NOT LIKE :username",
            sql
        )
        assertEquals(mapOf("idNew" to 1, "usernameNew" to "test", "username" to "%t"), paramMap)
    }

    @Test
    fun testUpdateWhereBetweenInfix() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.id between (1..2) }.build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew WHERE `id` BETWEEN :idMin AND :idMax", sql
        )
        assertEquals(
            mapOf(
                "idNew" to 1,
                "usernameNew" to "test",
                "idMin" to 1,
                "idMax" to 2
            ), paramMap
        )
    }

    @Test
    fun testUpdateWhereNotBetweenInfix() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.id notBetween (1..2) }.build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew WHERE `id` NOT BETWEEN :idMin AND :idMax",
            sql
        )
        assertEquals(
            mapOf(
                "idNew" to 1,
                "usernameNew" to "test",
                "idMin" to 1,
                "idMax" to 2
            ),
            paramMap
        )
    }

    // isNull
    //it.<property>.isNull
    //notNull
    //it.<property>.notNull
    @Test
    fun testUpdateWhereIsNull() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.id.isNull }.build()

        println(sql)
        println(paramMap)

        assertEquals("UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew WHERE `id` IS NULL", sql)
        assertEquals(mapOf("idNew" to 1, "usernameNew" to "test"), paramMap)
    }

    @Test
    fun testUpdateWhereAndOr() {
        val movie = Movie(
            id = 1,
            name = "testMovie",
            year = 2021,
            director = "testDirector",
            actor = "testActor",
            type = "Comedy",
            country = "America",
            language = "English",
            description = "A test movie",
            poster = "https://test.com/test.jpg",
            video = "https://test.com/test.mp4",
            summary = "This is a test movie",
            tags = "test, testMovie",
            score = 1.5,
            vote = 1,
            favorite = 1
        )

        val (sql, paramMap) = movie.update().where {
            "xxxxxxx".asSql() && it.id.eq && it.name.matchBoth && it.score.between(1..2) && it.tags.eq && it.description.matchBoth &&
                    (it.year in listOf(2021, 2022) || it.vote < 10 || it.favorite == 1) &&
                    it.director.eq && it.actor.eq &&
                    (it.country !in listOf("China", "Japan") || it.language.eq ||
                            (it.poster.notNull && it.video.notNull && it.summary.notLike("%test%"))
                            )
        }.build()

        assertEquals(
            "UPDATE `movie` SET `actor` = :actorNew, `country` = :countryNew, `description` = :descriptionNew, `director` = :directorNew, `favorite` = :favoriteNew, `id` = :idNew, `language` = :languageNew, `name` = :nameNew, `poster` = :posterNew, `score` = :scoreNew, `summary` = :summaryNew, `tags` = :tagsNew, `type` = :typeNew, `update_time` = :updateTimeNew, `video` = :videoNew, `vote` = :voteNew, `year` = :yearNew WHERE xxxxxxx AND `id` = :id AND `name` LIKE :name AND `score` BETWEEN :scoreMin AND :scoreMax AND `tags` = :tags AND `description` LIKE :description AND (`year` IN (:yearList) OR `vote` < :voteMax OR `favorite` = :favorite) AND `director` = :director AND `actor` = :actor AND (`country` NOT IN (:countryList) OR `language` = :language OR (`poster` IS NOT NULL AND `video` IS NOT NULL AND `summary` NOT LIKE :summary)) AND `deleted` = 0",
            sql
        )
        val expectedMap = mapOf(
            "id" to 1L,
            "name" to "%testMovie%",
            "scoreMin" to 1,
            "scoreMax" to 2,
            "tags" to "test, testMovie",
            "description" to "%A test movie%",
            "yearList" to listOf(2021, 2022),
            "voteMax" to 10,
            "favorite" to 1,
            "director" to "testDirector",
            "actor" to "testActor",
            "countryList" to listOf("China", "Japan"),
            "language" to "English",
            "summary" to "%test%",
            "actorNew" to "testActor",
            "countryNew" to "America",
            "descriptionNew" to "A test movie",
            "directorNew" to "testDirector",
            "favoriteNew" to 1,
            "idNew" to 1L,
            "languageNew" to "English",
            "nameNew" to "testMovie",
            "posterNew" to "https://test.com/test.jpg",
            "scoreNew" to 1.5,
            "summaryNew" to "This is a test movie",
            "tagsNew" to "test, testMovie",
            "typeNew" to "Comedy",
            "videoNew" to "https://test.com/test.mp4",
            "voteNew" to 1,
            "yearNew" to 2021,
            "updateTimeNew" to paramMap["updateTimeNew"]
        )
        assertEquals(
            expectedMap, paramMap
        )
    }

    @Test
    fun testUpdateWhereNotNull() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.id.notNull }.build()

        println(sql)
        println(paramMap)

        assertEquals("UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew WHERE `id` IS NOT NULL", sql)
        assertEquals(mapOf("idNew" to 1, "usernameNew" to "test"), paramMap)
    }

    // in
    //it.<property> in listOf(1,2,3)
    //Iterable类型的contains函数
    //IN
    //
    //listOf(1, 2, 3).contains(it.<property>)
    //
    //
    //notIn
    //it.<property> notIn listOf(1,2,3)
    //Any类型的notIn方法
    @Test
    fun testUpdateWhereIn() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.id in listOf(1, 2, 3) }.build()

        println(sql)
        println(paramMap)

        assertEquals("UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew WHERE `id` IN (:idList)", sql)
        assertEquals(
            mapOf(
                "idNew" to 1,
                "usernameNew" to "test",
                "idList" to listOf(1, 2, 3)
            ), paramMap
        )
    }

    @Test
    fun testUpdateWhereNotIn() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.id !in listOf(1, 2, 3) }.build()

        println(sql)
        println(paramMap)

        assertEquals("UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew WHERE `id` NOT IN (:idList)", sql)
        assertEquals(
            mapOf(
                "idNew" to 1,
                "usernameNew" to "test",
                "idList" to listOf(1, 2, 3)
            ), paramMap
        )
    }

    @Test
    fun testUpdateWhereContains() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { listOf(1, 2, 3).contains(it.id) }.build()

        println(sql)
        println(paramMap)

        assertEquals("UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew WHERE `id` IN (:idList)", sql)
        assertEquals(
            mapOf(
                "idNew" to 1,
                "usernameNew" to "test",
                "idList" to listOf(1, 2, 3)
            ), paramMap
        )
    }

    @Test
    fun testAutoWhere() {
        val (sql, paramMap) = testUser.update().set {
            it.username = "ZhangSan"
        }.where().build()
        assertEquals(
            "UPDATE `tb_user` SET `username` = :usernameNew WHERE `id` = :id AND `username` = :username",
            sql
        )
        assertEquals(mapOf("id" to 1, "username" to "test", "usernameNew" to "ZhangSan"), paramMap)
    }

    //    infix fun Comparable<*>?.matchLeft(@Suppress("UNUSED_PARAMETER") other: String?): Boolean = true
    //    infix fun Comparable<*>?.matchRight(@Suppress("UNUSED_PARAMETER") other: String?): Boolean = true
    //    infix fun Comparable<*>?.matchBoth(@Suppress("UNUSED_PARAMETER") other: String?): Boolean = true

    @Test
    fun testUpdateWhereMatchLeftInfix() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.id matchLeft "1" }.build()

        println(sql)
        println(paramMap)

        assertEquals("UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew WHERE `id` LIKE :id", sql)
        assertEquals(mapOf("idNew" to 1, "usernameNew" to "test", "id" to "1%"), paramMap)
    }

    @Test
    fun testUpdateWhereMatchRightInfix() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.id matchRight "1" }.build()

        println(sql)
        println(paramMap)

        assertEquals("UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew WHERE `id` LIKE :id", sql)
        assertEquals(mapOf("idNew" to 1, "usernameNew" to "test", "id" to "%1"), paramMap)
    }

    @Test
    fun testUpdateWhereMatchBothInfix() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.id matchBoth "1" }.build()

        println(sql)
        println(paramMap)

        assertEquals("UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew WHERE `id` LIKE :id", sql)
        assertEquals(mapOf("idNew" to 1, "usernameNew" to "test", "id" to "%1%"), paramMap)
    }

    //  val Comparable<*>?.like get() = true
    //    val Comparable<*>?.notLike get() = true
    //    val Comparable<*>?.matchLeft get() = true
    //    val Comparable<*>?.matchRight get() = true
    @Test
    fun testUpdateWhereLike() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.username.like }.build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew WHERE `username` LIKE :username",
            sql
        )
        assertEquals(mapOf("idNew" to 1, "usernameNew" to "test", "username" to "test"), paramMap)
    }

    @Test
    fun testUpdateWhereNotLike() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.username.notLike }.build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew WHERE `username` NOT LIKE :username",
            sql
        )
        assertEquals(mapOf("idNew" to 1, "usernameNew" to "test", "username" to "test"), paramMap)
    }

    @Test
    fun testUpdateWhereMatchLeft() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.username.matchLeft }.build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew WHERE `username` LIKE :username",
            sql
        )
        assertEquals(mapOf("idNew" to 1, "usernameNew" to "test", "username" to "test%"), paramMap)
    }

    @Test
    fun testUpdateWhereMatchRight() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.username.matchRight }.build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew WHERE `username` LIKE :username",
            sql
        )
        assertEquals(mapOf("idNew" to 1, "usernameNew" to "test", "username" to "%test"), paramMap)
    }

    //     val Comparable<*>?.lt get() = true
    //    val Comparable<*>?.gt get() = true
    //    val Comparable<*>?.le get() = true
    //    val Comparable<*>?.ge get() = true
    @Test
    fun testUpdateWhereLt() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.id.lt }.build()

        println(sql)
        println(paramMap)

        assertEquals("UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew WHERE `id` < :idMax", sql)
        assertEquals(mapOf("idNew" to 1, "usernameNew" to "test", "idMax" to 1), paramMap)
    }

    @Test
    fun testUpdateWhereGt() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.id.gt }.build()

        println(sql)
        println(paramMap)

        assertEquals("UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew WHERE `id` > :idMin", sql)
        assertEquals(mapOf("idNew" to 1, "usernameNew" to "test", "idMin" to 1), paramMap)
    }

    @Test
    fun testUpdateWhereLe() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.id.le }.build()

        println(sql)
        println(paramMap)

        assertEquals("UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew WHERE `id` <= :idMax", sql)
        assertEquals(mapOf("idNew" to 1, "usernameNew" to "test", "idMax" to 1), paramMap)
    }

    @Test
    fun testUpdateWhereGe() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.id.ge }.build()

        println(sql)
        println(paramMap)
        assertEquals("UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew WHERE `id` >= :idMin", sql)
        assertEquals(mapOf("idNew" to 1, "usernameNew" to "test", "idMin" to 1), paramMap)
    }

    @Test
    fun testUpdateExceptWithSet() {
        val (sql, paramMap) = testUser.updateExcept { it.username }
            .set { it.id = 1 }
            .by { it.id }
            .build()

        println(sql)
        println(paramMap)

        assertEquals("UPDATE `tb_user` SET `gender` = :genderNew, `id` = :idNew WHERE `id` = :id", sql)
        assertEquals(mapOf("idNew" to 1, "genderNew" to null, "usernameNew" to "test", "id" to 1), paramMap)
        // Update tb_user set username = 'test' where id = 1
    }

    @Test
    fun testUpdateWhereNull() {
        val (sql, paramMap) = testUser.update()
            .set { it.id = 1 }
            .where { it.gender.eq.ifNoValue(NoValueStrategy.Ignore) }
            .build()

        println(sql)
        println(paramMap)

        assertEquals("UPDATE `tb_user` SET `id` = :idNew", sql)
        assertEquals(mapOf("idNew" to 1), paramMap)
        // Update tb_user set username = 'test' where id = 1
    }

    @Test
    fun testBatchUpdateBy() {
        val (sql, paramMapArr) = arrayOf(user, testUser).update { it.username }.by { it.id }.build()
        assertEquals("UPDATE `tb_user` SET `username` = :usernameNew WHERE `id` = :id", sql)
        assertEquals(
            arrayOf(
                mapOf("usernameNew" to null, "id" to 1),
                mapOf("usernameNew" to "test", "id" to 1)
            ).toList(), paramMapArr!!.toList()
        )
    }

    @Test
    fun testBatchUpdateWhere() {
        val (sql, paramMapArr) = arrayOf(user, testUser).update().set { it.gender = 2 }.where { it.id.eq }.build()
        assertEquals("UPDATE `tb_user` SET `gender` = :genderNew WHERE `id` = :id", sql)
        assertEquals(
            arrayOf(
                mapOf("genderNew" to 2, "id" to 1),
                mapOf("genderNew" to 2, "id" to 1)
            ).toList(), paramMapArr!!.toList()
        )
    }

    fun testAsyncUpdate() {
        listOf(
            listOf(user, testUser).update { it.id + it.username }
                .where { listOf(1, 2, 3).contains(it.id) }.build(),
            testUser.update { it.id + it.username }
                .where { listOf(1, 2, 3).contains(it.id) }.build(),
            testUser.update { it.id + it.username }
                .where { listOf(1, 2, 3).contains(it.id) }.build(),
            testUser.update { it.id + it.username }
                .where { listOf(1, 2, 3).contains(it.id) }.build()
        )
            .toAsyncTask() // 还没实现 后面再拆
            .execute()
    }
}