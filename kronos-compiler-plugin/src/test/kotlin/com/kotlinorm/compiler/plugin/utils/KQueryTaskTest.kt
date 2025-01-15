package com.kotlinorm.compiler.plugin.utils

import com.kotlinorm.compiler.plugin.KotlinSourceDynamicCompiler.compile
import com.kotlinorm.compiler.plugin.testBaseName
import com.tschuchort.compiletesting.KotlinCompilation
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals

class KQueryTaskTest {
    @Test
    fun `KronosQueryTask_queryOne Test`() {
        "KronosQueryTaskQueryOne" testCompile
         """
            fun test(){
                assertEquals(1, KronosQueryTask(
                    KronosAtomicQueryTask(
                        "SELECT id FROM user where id = 1"
                    )
                ).queryOne<Int>())
            }
        """
    }
    @Test
    fun `KronosQueryTask_queryOneOrNull Test`() {
        "KronosQueryTaskQueryOneOrNull" testCompile
                """
            fun test(){
                assertEquals(1, KronosQueryTask(
                    KronosAtomicQueryTask(
                        "SELECT id FROM user where id = 1"
                    )
                ).queryOneOrNull<Int>())
            }
        """
    }

    @Test
    fun `KronosQueryTask_queryList Test`() {
        "KronosQueryTaskQueryList" testCompile """
            fun test(){
                assertEquals(listOf(1), KronosQueryTask(
                    KronosAtomicQueryTask(
                        "SELECT id FROM user"
                    )
                ).queryList<Int>())
            }
        """
    }

    @Test
    fun `SelectClause_queryList Test`() {
        "SelectClauseQueryList" testCompile
                """
            fun test(){
                assertEquals(listOf(1), User(1).select().queryList<Int>())
            }
        """
    }

    @Test
    fun `SelectClause_queryOne Test`() {
        "SelectClauseQueryOne" testCompile
                """
            fun test(){
                assertEquals(1, User(1).select().queryOne<Int>())
            }
        """
    }

    @Test
    fun `SelectClause_queryOneOrNull Test`() {
        "SelectClauseQueryOneOrNull" testCompile
                """
            fun test(){
                assertEquals(1, User(1).select().queryOneOrNull<Int>())
            }
        """
    }

    @Test
    fun `SelectFrom_queryOneOrNull Test`() {
        "SelectFormQueryOneOrNull" testCompile
                """
            fun test(){
                assertEquals(1, User().join(Email()){ user, email->
                    select{ email.id }
                    on { user.id == email.userId }
                }.queryOneOrNull<Int>())
            }
        """
    }

    @Test
    fun `SelectFrom_queryOne Test`() {
        "SelectFormQueryOne" testCompile
                """
            fun test(){
                assertEquals(1, User().join(Email()){ user, email->
                    select{ email.id }
                    on { user.id == email.userId }
                }.queryOne<Int>())
            }
        """
    }

    @Test
    fun `SelectFrom_queryList Test`() {
        "SelectFormQueryList" testCompile
                """
            fun test(){
                assertEquals(listOf(1), User().join(Email()){ user, email->
                    select{ email.id }
                    on { user.id == email.userId }
                }.queryList<Int>())
            }
        """
    }

    @Test
    fun `QueryHandler_queryList Test`() {
        "QueryHandlerQueryList" testCompile
                """
            fun test(){
                assertEquals(listOf(1), TestWrapper.queryList<Int>("select id from user where id = 1"))
            }
        """
    }

    @Test
    fun `QueryHandler_queryOne Test`() {
        "QueryHandlerQueryOne" testCompile
                """
            fun test(){
                assertEquals(1, TestWrapper.queryOne<Int>("select id from user where id = 1"))
            }
        """
    }

    @Test
    fun `QueryHandler_queryOneOrNull Test`() {
        "QueryHandlerQueryOneOrNull" testCompile
                """
            fun test(){
                assertEquals(1, TestWrapper.queryOneOrNull<Int>("select id from user where id = 1"))
            }
        """
    }

    @Language("kotlin")
    private val mainKt = """
            import com.kotlinorm.beans.task.KronosAtomicBatchTask
            import com.kotlinorm.enums.DBType
            import com.kotlinorm.interfaces.KAtomicActionTask
            import com.kotlinorm.interfaces.KAtomicQueryTask
            import com.kotlinorm.interfaces.KronosDataSourceWrapper
            import javax.sql.DataSource
            import kotlin.reflect.KClass

            import com.kotlinorm.Kronos
            import com.kotlinorm.Kronos.init
            import com.kotlinorm.annotations.*
            import com.kotlinorm.interfaces.KPojo
            import com.kotlinorm.orm.select.select
            import com.kotlinorm.orm.join.join
            import com.kotlinorm.enums.KColumnType.TINYINT
            import com.kotlinorm.utils.createInstance
            import java.time.LocalDateTime
            import kotlin.test.assertEquals
            import kotlin.test.assertNotNull
            import com.kotlinorm.enums.DBType.Mysql
        
            import com.kotlinorm.beans.task.KronosAtomicQueryTask
            import com.kotlinorm.beans.task.KronosQueryTask

            import com.kotlinorm.database.SqlHandler.queryList
            import com.kotlinorm.database.SqlHandler.queryOne
            import com.kotlinorm.database.SqlHandler.queryOneOrNull

            object TestWrapper : KronosDataSourceWrapper {
                override val url: String
                    get() = TODO("Not yet implemented")
                override val userName: String
                    get() = TODO("Not yet implemented")
                override val dbType: DBType
                    get() = Mysql
        
                override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> {
                    TODO("Not yet implemented")
                }
        
                override fun forList(
                    task: KAtomicQueryTask,
                    kClass: KClass<*>,
                    isKPojo: Boolean,
                    superTypes: List<String>
                ): List<Any> {
                    assertEquals(isKPojo, false)
                    assertEquals(superTypes, listOf("kotlin.Int", "kotlin.Number", "kotlin.Comparable", "java.io.Serializable"))
                    return listOf(1)
                }
        
                override fun forMap(task: KAtomicQueryTask): Map<String, Any>? {
                    TODO("Not yet implemented")
                }
        
                override fun forObject(
                    task: KAtomicQueryTask,
                    kClass: KClass<*>,
                    isKPojo: Boolean,
                    superTypes: List<String>
                ): Any? {
                    assertEquals(isKPojo, false)
                    assertEquals(superTypes, listOf("kotlin.Int", "kotlin.Number", "kotlin.Comparable", "java.io.Serializable"))
                    return 1
                }
        
                override fun update(task: KAtomicActionTask): Int {
                    TODO("Not yet implemented")
                }
        
                override fun batchUpdate(task: KronosAtomicBatchTask): IntArray {
                    TODO("Not yet implemented")
                }
        
                override fun transact(block: (DataSource) -> Any?): Any? {
                    TODO("Not yet implemented")
                }
        
            }

            data class User(
                val id: Int? = null
            ): KPojo

            data class Email(
                val id: Int? = null,
                val userId: Int? = null,
            ): KPojo
                
            fun main() {
                Kronos.init {
                    fieldNamingStrategy = lineHumpNamingStrategy
                    tableNamingStrategy = lineHumpNamingStrategy
                    dataSource = { TestWrapper }
                }
                
                //inject
                
                test()
            }
        """

    @OptIn(ExperimentalCompilerApi::class)
    infix fun String.testCompile(@Language("kotlin") code: String) {
        if (this.any { !it.isLetter() } || this.first().isLowerCase()) {
            throw IllegalArgumentException("测试名称必须全部都是字母，且首字母必须大写")
        }
        val result =
            compile(mainKt.replace("//inject", code), this@KQueryTaskTest.testBaseName + this)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val ktClazz =
            result.classLoader.loadClass("${this@KQueryTaskTest.testBaseName + this}Kt")
        val main = ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
        main.invoke(null)
    }
}