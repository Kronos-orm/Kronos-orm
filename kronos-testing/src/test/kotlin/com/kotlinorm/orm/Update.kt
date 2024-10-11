package com.kotlinorm.orm

import com.kotlinorm.Kronos
import com.kotlinorm.KronosBasicWrapper
import com.kotlinorm.beans.strategies.LineHumpNamingStrategy
import com.kotlinorm.enums.NoValueStrategyType
import com.kotlinorm.orm.beans.Movie
import com.kotlinorm.orm.update.UpdateClause.Companion.build
import com.kotlinorm.orm.update.UpdateClause.Companion.by
import com.kotlinorm.orm.update.UpdateClause.Companion.set
import com.kotlinorm.orm.update.UpdateClause.Companion.where
import com.kotlinorm.orm.update.update
import com.kotlinorm.orm.utils.GsonResolver
import com.kotlinorm.tableOperation.beans.MysqlUser
import com.kotlinorm.utils.Extensions.mapperTo
import org.apache.commons.dbcp2.BasicDataSource
import kotlin.test.Test
import kotlin.test.assertEquals

class Update {
    private val ds = BasicDataSource().apply {
        driverClassName = "com.mysql.cj.jdbc.Driver"
        url = "jdbc:mysql://localhost:3306/test"
        username = "root"
        password = "rootroot"
    }

    init {
        Kronos.apply {
            fieldNamingStrategy = LineHumpNamingStrategy
            tableNamingStrategy = LineHumpNamingStrategy
            dataSource = { KronosBasicWrapper(ds) }
            serializeResolver = GsonResolver
        }
    }


    private val user = MysqlUser(1)
    private val testUser = MysqlUser(1, "test")
    private val testUser1 = MysqlUser(1, "test", 1)

    @Test
    fun testUpdateWithSetAndBy() {
        val (sql, paramMap) =
            user.update()
                .set {
                    it.username = "123"
                    it.gender += 10
                    it.gender = 1
                }
                .by { it.id }
                .build()


        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `username` = :usernameNew, `update_time` = :updateTimeNew, `gender1` = `gender1` + :gender2PlusNew WHERE `id` = :id AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "id" to 1,
                "usernameNew" to "123",
                "gender2PlusNew" to 10,
                "updateTimeNew" to paramMap["updateTimeNew"]
            ), paramMap
        )
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
            "UPDATE `tb_user` SET `username` = :usernameNew, `gender1` = :genderNew, `update_time` = :updateTimeNew WHERE `id` = :id AND `username` = :username AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "id" to 1,
                "usernameNew" to "123",
                "genderNew" to 1,
                "username" to "test",
                "updateTimeNew" to paramMap["updateTimeNew"]
            ), paramMap
        )
        // Update tb_user set username = '123' where id = 1

    }

    @Test
    fun testUpdateMultiFieldsWithSetAndBy() {
        val (sql, paramMap) = user.update { it.username + it.gender }
            .by { it.id }
            .build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `username` = :usernameNew, `gender1` = :genderNew, `update_time` = :updateTimeNew WHERE `id` = :id AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "id" to 1,
                "usernameNew" to null,
                "genderNew" to null,
                "updateTimeNew" to paramMap["updateTimeNew"]
            ), paramMap
        )

    }

    @Test
    fun testUpdateExceptWithSetAndBy() {
        val (sql, paramMap) = testUser.update { it - it.username }
            .by { it.id }
            .build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `gender1` = :genderNew, `update_time` = :updateTimeNew WHERE `id` = :id AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "idNew" to 1,
                "genderNew" to null,
                "id" to 1,
                "updateTimeNew" to paramMap["updateTimeNew"]
            ), paramMap
        )
        // Update tb_user set username = 'test' where id = 1
    }

    @Test
    fun testUpdateWithSetAndWhere() {
        val (sql, paramMap) = user.update()
            .set { it.gender = 1 }
            .where { it.id == 1 }
            .build()

        assertEquals(
            "UPDATE `tb_user` SET `gender1` = :genderNew, `update_time` = :updateTimeNew WHERE `id` = :id AND `deleted` = 0",
            sql
        )
        assertEquals(mapOf("id" to 1, "genderNew" to 1, "updateTimeNew" to paramMap["updateTimeNew"]), paramMap)
        // Update tb_user set gender = 1 where id = 1
    }

    @Test
    fun testUpdateWhereComparable() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.id < 1 && it.id > 0 }.build()
        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew, `update_time` = :updateTimeNew WHERE `id` < :idMax AND `id` > :idMin AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "idNew" to 1,
                "usernameNew" to "test",
                "idMin" to 0,
                "idMax" to 1,
                "updateTimeNew" to paramMap["updateTimeNew"]
            ), paramMap
        )
        // Update tb_user set id = 1, username = 1 where id < 1 and id > 0
    }

    @Test
    fun testUpdateWhereNeq() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.id.neq }.build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew, `update_time` = :updateTimeNew WHERE `id` != :id AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "id" to 1,
                "idNew" to 1,
                "usernameNew" to "test",
                "updateTimeNew" to paramMap["updateTimeNew"]
            ), paramMap
        )
    }

    @Test
    fun testUpdateWhereNotEqual() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.id != 1 }.build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew, `update_time` = :updateTimeNew WHERE `id` != :id AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "id" to 1,
                "idNew" to 1,
                "usernameNew" to "test",
                "updateTimeNew" to paramMap["updateTimeNew"]
            ), paramMap
        )
    }

    @Test
    fun testUpdateWhereLikeInfix() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.username like "%t" }.build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew, `update_time` = :updateTimeNew WHERE `username` LIKE :username AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "idNew" to 1,
                "usernameNew" to "test",
                "username" to "%t",
                "updateTimeNew" to paramMap["updateTimeNew"]
            ), paramMap
        )
    }

    @Test
    fun testUpdateWhereLikeBracket() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.username.like("%t") }.build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew, `update_time` = :updateTimeNew WHERE `username` LIKE :username AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "idNew" to 1,
                "usernameNew" to "test",
                "username" to "%t",
                "updateTimeNew" to paramMap["updateTimeNew"]
            ), paramMap
        )
    }

    @Test
    fun testUpdateWhereNotLikeInfix() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.username notLike "%t" }.build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew, `update_time` = :updateTimeNew WHERE `username` NOT LIKE :username AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "idNew" to 1,
                "usernameNew" to "test",
                "username" to "%t",
                "updateTimeNew" to paramMap["updateTimeNew"]
            ), paramMap
        )
    }

    @Test
    fun testUpdateWhereNotLikeBracket() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.username.notLike("%t") }.build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew, `update_time` = :updateTimeNew WHERE `username` NOT LIKE :username AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "idNew" to 1,
                "usernameNew" to "test",
                "username" to "%t",
                "updateTimeNew" to paramMap["updateTimeNew"]
            ), paramMap
        )
    }

    @Test
    fun testUpdateWhereBetweenInfix() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.id between (1..2) }.build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew, `update_time` = :updateTimeNew WHERE `id` BETWEEN :idMin AND :idMax AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "idNew" to 1,
                "usernameNew" to "test",
                "idMin" to 1,
                "idMax" to 2,
                "updateTimeNew" to paramMap["updateTimeNew"]
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
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew, `update_time` = :updateTimeNew WHERE `id` NOT BETWEEN :idMin AND :idMax AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "idNew" to 1,
                "usernameNew" to "test",
                "idMin" to 1,
                "idMax" to 2,
                "updateTimeNew" to paramMap["updateTimeNew"]
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

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew, `update_time` = :updateTimeNew WHERE `id` IS NULL AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf("idNew" to 1, "usernameNew" to "test", "updateTimeNew" to paramMap["updateTimeNew"]),
            paramMap
        )
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
            "UPDATE `movie` SET `id` = :idNew, `name` = :nameNew, `year` = :yearNew, `director` = :directorNew, `actor` = :actorNew, `type` = :typeNew, `country` = :countryNew, `language` = :languageNew, `description` = :descriptionNew, `poster` = :posterNew, `video` = :videoNew, `summary` = :summaryNew, `tags` = :tagsNew, `score` = :scoreNew, `vote` = :voteNew, `favorite` = :favoriteNew, `update_time` = :updateTimeNew WHERE xxxxxxx AND `id` = :id AND `name` LIKE :name AND `score` BETWEEN :scoreMin AND :scoreMax AND `tags` = :tags AND `description` LIKE :description AND (`year` IN (:yearList) OR `vote` < :voteMax OR `favorite` = :favorite) AND `director` = :director AND `actor` = :actor AND (`country` NOT IN (:countryList) OR `language` = :language OR (`poster` IS NOT NULL AND `video` IS NOT NULL AND `summary` NOT LIKE :summary)) AND `deleted` = 0",
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

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew, `update_time` = :updateTimeNew WHERE `id` IS NOT NULL AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf("idNew" to 1, "usernameNew" to "test", "updateTimeNew" to paramMap["updateTimeNew"]),
            paramMap
        )
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

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew, `update_time` = :updateTimeNew WHERE `id` IN (:idList) AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "idNew" to 1,
                "usernameNew" to "test",
                "idList" to listOf(1, 2, 3),
                "updateTimeNew" to paramMap["updateTimeNew"]
            ), paramMap
        )
    }

    @Test
    fun testUpdateWhereNotIn() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.id !in listOf(1, 2, 3) }.build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew, `update_time` = :updateTimeNew WHERE `id` NOT IN (:idList) AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "idNew" to 1,
                "usernameNew" to "test",
                "idList" to listOf(1, 2, 3),
                "updateTimeNew" to paramMap["updateTimeNew"]
            ), paramMap
        )
    }

    @Test
    fun testUpdateWhereContains() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { listOf(1, 2, 3).contains(it.id) }.build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew, `update_time` = :updateTimeNew WHERE `id` IN (:idList) AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "idNew" to 1,
                "usernameNew" to "test",
                "idList" to listOf(1, 2, 3),
                "updateTimeNew" to paramMap["updateTimeNew"]
            ), paramMap
        )
    }

    @Test
    fun testAutoWhere() {
        val (sql, paramMap) = testUser.update().set {
            it.username = "ZhangSan"
        }.build()
        assertEquals(
            "UPDATE `tb_user` SET `username` = :usernameNew, `update_time` = :updateTimeNew WHERE `id` = :id AND `username` = :username AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "id" to 1,
                "username" to "test",
                "usernameNew" to "ZhangSan",
                "updateTimeNew" to paramMap["updateTimeNew"]
            ), paramMap
        )
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

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew, `update_time` = :updateTimeNew WHERE `id` LIKE :id AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "idNew" to 1,
                "usernameNew" to "test",
                "id" to "1%",
                "updateTimeNew" to paramMap["updateTimeNew"]
            ), paramMap
        )
    }

    @Test
    fun testUpdateWhereMatchRightInfix() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.id matchRight "1" }.build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew, `update_time` = :updateTimeNew WHERE `id` LIKE :id AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "idNew" to 1,
                "usernameNew" to "test",
                "id" to "%1",
                "updateTimeNew" to paramMap["updateTimeNew"]
            ), paramMap
        )
    }

    @Test
    fun testUpdateWhereMatchBothInfix() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.id matchBoth "1" }.build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew, `update_time` = :updateTimeNew WHERE `id` LIKE :id AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "idNew" to 1,
                "usernameNew" to "test",
                "id" to "%1%",
                "updateTimeNew" to paramMap["updateTimeNew"]
            ), paramMap
        )
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
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew, `update_time` = :updateTimeNew WHERE `username` LIKE :username AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "idNew" to 1,
                "usernameNew" to "test",
                "username" to "test",
                "updateTimeNew" to paramMap["updateTimeNew"]
            ), paramMap
        )
    }

    @Test
    fun testUpdateWhereNotLike() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.username.notLike }.build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew, `update_time` = :updateTimeNew WHERE `username` NOT LIKE :username AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "idNew" to 1,
                "usernameNew" to "test",
                "username" to "test",
                "updateTimeNew" to paramMap["updateTimeNew"]
            ), paramMap
        )
    }

    @Test
    fun testUpdateWhereMatchLeft() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.username.matchLeft }.build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew, `update_time` = :updateTimeNew WHERE `username` LIKE :username AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "idNew" to 1,
                "usernameNew" to "test",
                "username" to "test%",
                "updateTimeNew" to paramMap["updateTimeNew"]
            ), paramMap
        )
    }

    @Test
    fun testUpdateWhereMatchRight() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.username.matchRight }.build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew, `update_time` = :updateTimeNew WHERE `username` LIKE :username AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "idNew" to 1,
                "usernameNew" to "test",
                "username" to "%test",
                "updateTimeNew" to paramMap["updateTimeNew"]
            ), paramMap
        )
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

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew, `update_time` = :updateTimeNew WHERE `id` < :idMax AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "idNew" to 1,
                "usernameNew" to "test",
                "idMax" to 1,
                "updateTimeNew" to paramMap["updateTimeNew"]
            ), paramMap
        )
    }

    @Test
    fun testUpdateWhereGt() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.id.gt }.build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew, `update_time` = :updateTimeNew WHERE `id` > :idMin AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "idNew" to 1,
                "usernameNew" to "test",
                "idMin" to 1,
                "updateTimeNew" to paramMap["updateTimeNew"]
            ), paramMap
        )
    }

    @Test
    fun testUpdateWhereLe() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.id.le }.build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew, `update_time` = :updateTimeNew WHERE `id` <= :idMax AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "idNew" to 1,
                "usernameNew" to "test",
                "idMax" to 1,
                "updateTimeNew" to paramMap["updateTimeNew"]
            ), paramMap
        )
    }

    @Test
    fun testUpdateWhereGe() {
        val (sql, paramMap) = testUser.update { it.id + it.username }
            .where { it.id.ge }.build()

        println(sql)
        println(paramMap)
        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `username` = :usernameNew, `update_time` = :updateTimeNew WHERE `id` >= :idMin AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "idNew" to 1,
                "usernameNew" to "test",
                "idMin" to 1,
                "updateTimeNew" to paramMap["updateTimeNew"]
            ), paramMap
        )
    }

    @Test
    fun testUpdateExceptWithSet() {
        val (sql, paramMap) = testUser.update { it - it.username }
            .set { it.id = 1 }
            .by { it.id }
            .build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `gender1` = :genderNew, `update_time` = :updateTimeNew WHERE `id` = :id AND `deleted` = 0",
            sql
        )
        assertEquals(
            mapOf(
                "idNew" to 1,
                "genderNew" to null,
                "id" to 1,
                "updateTimeNew" to paramMap["updateTimeNew"]
            ), paramMap
        )
        // Update tb_user set username = 'test' where id = 1
    }

    @Test
    fun testUpdateWhereNull() {
        val (sql, paramMap) = testUser.update()
            .set { it.id = 1 }
            .where { it.gender.eq.ifNoValue(NoValueStrategyType.Ignore) }
            .build()

        println(sql)
        println(paramMap)

        assertEquals("UPDATE `tb_user` SET `id` = :idNew, `update_time` = :updateTimeNew WHERE `deleted` = 0", sql)
        assertEquals(mapOf("idNew" to 1, "updateTimeNew" to paramMap["updateTimeNew"]), paramMap)
        assertEquals(testUser.mapperTo<MysqlUser>().equals(testUser), true)
        // Update tb_user set username = 'test' where id = 1
    }

    @Test
    fun testUpdateWhereNoNull() {
        val (sql, paramMap) = testUser1.update()
            .set { it.id = 1 }
            .where { it.gender.eq.ifNoValue(NoValueStrategyType.Ignore) }
            .build()

        println(sql)
        println(paramMap)

        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `update_time` = :updateTimeNew WHERE `gender1` = :gender AND `deleted` = 0",
            sql
        )
        assertEquals(mapOf("idNew" to 1, "gender" to 1, "updateTimeNew" to paramMap["updateTimeNew"]), paramMap)
        // Update tb_user set username = 'test' where id = 1
    }

    @Test
    fun testBatchUpdateBy() {
        val (sql, _, list) = arrayOf(user, testUser).update { it.username }.by { it.id }.build()
        assertEquals(
            "UPDATE `tb_user` SET `username` = :usernameNew, `update_time` = :updateTimeNew WHERE `id` = :id AND `deleted` = 0",
            sql
        )
        assertEquals(
            arrayOf(
                mapOf(
                    "usernameNew" to null, "id" to 1, "updateTimeNew" to (list[0].paramMap["updateTimeNew"] ?: "")
                ),
                mapOf(
                    "usernameNew" to "test", "id" to 1, "updateTimeNew" to (list[1].paramMap["updateTimeNew"] ?: "")
                )
            ).toList(), list.map { it.paramMap }
        )
    }

    @Test
    fun testBatchUpdateExceptBy() {
        val (sql, _, list) = arrayOf(user, testUser).update { it - it.username }.by { it.id }.build()
        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `gender1` = :genderNew, `update_time` = :updateTimeNew WHERE `id` = :id AND `deleted` = 0",
            sql
        )
        assertEquals(
            arrayOf(
                mapOf(
                    "id" to 1,
                    "idNew" to 1,
                    "genderNew" to null,
                    "updateTimeNew" to (list[0].paramMap["updateTimeNew"] ?: "")
                ),
                mapOf(
                    "id" to 1,
                    "idNew" to 1,
                    "genderNew" to null,
                    "updateTimeNew" to (list[1].paramMap["updateTimeNew"] ?: "")
                )
            ).toList(), list.map { it.paramMap }
        )
    }

    @Test
    fun testBatchUpdateWhere() {
        val (sql, _, list) = arrayOf(user, testUser).update().set { it.gender = 2 }.where { it.id.eq }.build()
        assertEquals(
            "UPDATE `tb_user` SET `gender1` = :genderNew, `update_time` = :updateTimeNew WHERE `id` = :id AND `deleted` = 0",
            sql
        )
        assertEquals(
            arrayOf(
                mapOf(
                    "genderNew" to 2, "id" to 1, "updateTimeNew" to (list[0].paramMap
                        .get("updateTimeNew") ?: "")
                ),
                mapOf(
                    "genderNew" to 2, "id" to 1, "updateTimeNew" to (list[1].paramMap
                        .get("updateTimeNew") ?: "")
                )
            ).toList(), list.map { it.paramMap.toMap() }
        )
    }

    @Test
    fun testBatchUpdateIterWhere() {
        val (sql, _, list) = listOf(user, testUser).update { it - it.username }.where { it.id.eq }.build()
        assertEquals(
            "UPDATE `tb_user` SET `id` = :idNew, `gender1` = :genderNew, `update_time` = :updateTimeNew WHERE `id` = :id AND `deleted` = 0",
            sql
        )
        assertEquals(
            arrayOf(
                mapOf(
                    "id" to 1,
                    "idNew" to 1,
                    "genderNew" to null,
                    "updateTimeNew" to (list[0].paramMap["updateTimeNew"] ?: "")
                ),
                mapOf(
                    "id" to 1,
                    "idNew" to 1,
                    "genderNew" to null,
                    "updateTimeNew" to (list[1].paramMap["updateTimeNew"] ?: "")
                )
            ).toList(), list.map { it.paramMap }
        )
    }

    @Test
    fun testBatchUpdateExceptIterWhere() {
        val (sql, _, list) = listOf(user, testUser).update { it - it.id }.where { it.id.eq }.build()
        assertEquals(
            "UPDATE `tb_user` SET `username` = :usernameNew, `gender1` = :genderNew, `update_time` = :updateTimeNew WHERE `id` = :id AND `deleted` = 0",
            sql
        )
        assertEquals(
            arrayOf(
                mapOf(
                    "genderNew" to null,
                    "id" to 1,
                    "usernameNew" to null,
                    "updateTimeNew" to (list[0].paramMap["updateTimeNew"] ?: "")
                ),
                mapOf(
                    "genderNew" to null,
                    "id" to 1,
                    "usernameNew" to "test",
                    "updateTimeNew" to (list[1].paramMap["updateTimeNew"] ?: "")
                )
            ).toList(), list.map { it.paramMap }
        )
    }
}