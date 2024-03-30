package test

import com.kotoframework.interfaces.KPojo
import com.kotoframework.orm.delete.delete
import com.kotoframework.orm.insert.insert
import com.kotoframework.orm.join.join
import com.kotoframework.orm.select.select
import com.kotoframework.orm.update.update
import com.kotoframework.orm.update.updateExcept
import com.kotoframework.orm.upsert.upsert
import com.kotoframework.orm.upsert.upsertExcept
import org.junit.jupiter.api.Test

class Main {

    @Test
    fun testUpdate() {
        data class User(var id: Int? = null, var username: String? = null, var gender: Int? = null) : KPojo

        val user = User(1)
        val testUser = User(1, "test")

        user.update()
            .set { it.username = "123" }
            .by { it.id }
            .execute()

        // update tb_user set username = '123' where id = 1

        testUser.update { it.username }
            .by { it.id }
            .execute()

        // update tb_user set username = 'test' where id = 1

        testUser.updateExcept { it.username }
            .by { it.id }
            .execute()

        // update tb_user set username = 'test' where id = 1

        user.update()
            .set { it.gender = 1 }
            .by { it.id }
            .execute()

        // update tb_user set gender = 1 where id = 1

        testUser.update { it.id + it.username }
            .where { it.id < 1 && it.id > 0 }
            .execute()

        // update tb_user set id = 1, username = 1 where id < 1 and id > 0
    }

    @Test
    fun testUpsert() {
        data class User(var id: Int? = null, var username: String? = null, var gender: Int? = null) : KPojo

        val user = User(1)
        val testUser = User(1, "test")

        user.upsert()
            .set { it.username = "123" }
            .by { it.id }
            .execute()

        // update tb_user set username = '123' where id = 1

        testUser.upsert { it.username }
            .by { it.id }
            .execute()

        // update tb_user set username = 'test' where id = 1

        testUser.upsertExcept { it.username }
            .by { it.id }
            .execute()

        // update tb_user set username = 'test' where id = 1

        user.upsert()
            .set { it.gender = 1 }
            .by { it.id }
            .execute()

        // update tb_user set gender = 1 where id = 1

        testUser.upsert { it.id + it.username }
            .where { it.id < 1 && it.id > 0 }
            .execute()


        testUser.upsert { it.username }
            .onDuplicateKey()
            .execute()
    }

    @Test
    fun testInsert() {
        data class User(var id: Int? = null, var username: String? = null, var gender: Int? = null) : KPojo

        val user = User(1)
        user.insert().execute()
    }

    @Test
    fun testDelete() {
        data class User(var id: Int? = null, var username: String? = null, var gender: Int? = null) : KPojo

        val user = User(1)
        user.delete().by { it.id }.execute()
        //delete from tb_user where id = 1

        user.delete().logic().where().execute()
        //delete from tb_user where id = 1

        user.delete().logic().where {
            it.id > 10 && it.id < 100 && it.username > 100
        }.execute()
        //delete from tb_user where id > 10 and id < 100
    }

    @Test
    fun testSelect() {
        data class User(var id: Int? = null, var username: String? = null, var gender: Int? = null) : KPojo

        val user = User(1)
        val list = user.select { it.id + it.username + it.gender }.query()

        val (total, listOfUser) = user.select { it.id }.page(1, 10).withTotal().query()

        val count = user.select().where {
            it.id > 10 && it.id < 100
        }.count()

        val result = User()
            .select { it.username }
            .where { it.id > 10 }
            .distinct()
            .groupBy { it.id }
            .orderBy { it.id.desc }
            .having { it.id.eq }
            .query()
    }

    @Test
    fun testJoin() {
        data class User(var id: Int? = null, var username: String? = null, var gender: Int? = null) : KPojo
        data class UserRelation(
            var id: Int? = null,
            var username: String? = null,
            var gender: Int? = null,
            var id2: Int? = null
        ) : KPojo

        User(1)
            .join(
                UserRelation(1, "123", 1, 1)
            ) { user, relation ->
                leftJoin(relation) { user.id == relation.id2 && user.gender == relation.gender }
                select { user + relation.gender }
                where { user.id == 1 }
                orderBy { user.id.desc }
                page(1, 10)
            }
            .withTotal()
            .query()
    }
}