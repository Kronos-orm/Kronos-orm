import com.kotlinorm.beans.task.ActionEvent
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.orm.ddl.DDLInfo
import com.kotlinorm.orm.delete.DeleteClauseInfo
import com.kotlinorm.orm.update.UpdateClauseInfo
import com.kotlinorm.plugins.DataGuardPlugin
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse

class DataGuardPluginTest {

    // Mock数据源包装器（根据实际情况调整）
    private val mockWrapper = SampleMysqlJdbcWrapper()

    @BeforeTest
    fun setup() {
        DataGuardPlugin.enable()
    }

    @AfterTest
    fun tearDown() {
        DataGuardPlugin.disable()
    }

    private fun createActionTask(
        operation: KOperationType,
        sql: String,
        tableName: String? = null,
        whereClause: String? = null
    ): KronosAtomicActionTask {
        val actionInfo = when (operation) {
            KOperationType.DELETE -> DeleteClauseInfo(
                tableName = tableName ?: parseTableName(sql),
                whereClause = whereClause
            )

            KOperationType.UPDATE -> UpdateClauseInfo(
                tableName = tableName ?: parseTableName(sql),
                whereClause = whereClause
            )

            KOperationType.TRUNCATE, KOperationType.DROP, KOperationType.ALTER ->
                DDLInfo(
                    tableName = tableName ?: parseTableName(sql),
                )

            else -> throw IllegalArgumentException("Unsupported operation type")
        }

        return KronosAtomicActionTask(
            sql = sql,
            operationType = operation,
            actionInfo = actionInfo
        )
    }

    private fun parseTableName(sql: String): String {
        val cleanedSql = sql.replace("\\s+".toRegex(), " ")
            .replace(";", "")
            .trim()
            .replace(Regex("(\\s)(?i)WHERE\\b.*"), "") // 移除 WHERE 子句干扰

        val patterns = listOf(
            // 优先级 1: DELETE FROM [schema.]table
            Regex("(?i)DELETE\\s+FROM\\s+([`\"\\[\\]]?)([\\w.]+)\\1"),
            // 优先级 2: UPDATE [schema.]table
            Regex("(?i)UPDATE\\s+([`\"\\[\\]]?)([\\w.]+)\\1"),
            // 优先级 3: TRUNCATE TABLE [schema.]table
            Regex("(?i)TRUNCATE\\s+TABLE\\s+([`\"\\[\\]]?)([\\w.]+)\\1"),
            // 优先级 4: DROP TABLE [schema.]table
            Regex("(?i)DROP\\s+TABLE\\s+([`\"\\[\\]]?)([\\w.]+)\\1"),
            // 优先级 5: ALTER TABLE [schema.]table
            Regex("(?i)ALTER\\s+TABLE\\s+([`\"\\[\\]]?)([\\w.]+)\\1"),
            // 后备匹配模式
            Regex("(?i)FROM\\s+([`\"\\[\\]]?)([\\w.]+)\\1"),
            Regex("(?i)INTO\\s+([`\"\\[\\]]?)([\\w.]+)\\1")
        )

        patterns.forEach { regex ->
            regex.find(cleanedSql)?.let {
                return it.groupValues[2].substringAfterLast('.') // 处理 schema.table 情况
            }
        }

        return cleanedSql.substringAfterLast(" ")
            .takeIf { it.isNotEmpty() } ?: throw IllegalArgumentException("Unable to parse table name from SQL: $sql")
    }

    @Test
    fun `should correctly parse table name from SQL`() {
        val sql = "DELETE FROM users WHERE id = 1"
        val tableName = parseTableName(sql)
        assertEquals("users", tableName)

        val sql2 = "UPDATE orders SET status = 'shipped' WHERE id = 123"
        val tableName2 = parseTableName(sql2)
        assertEquals("orders", tableName2)

        val sql3 = "TRUNCATE TABLE products"
        val tableName3 = parseTableName(sql3)
        assertEquals("products", tableName3)

        val sql4 = "DROP TABLE customers"
        val tableName4 = parseTableName(sql4)
        assertEquals("customers", tableName4)

        val sql5 = "ALTER TABLE employees ADD COLUMN age INT"
        val tableName5 = parseTableName(sql5)
        assertEquals("employees", tableName5)
    }

    @Test
    fun `should block truncate by default`() {
        val task = createActionTask(
            operation = KOperationType.TRUNCATE,
            sql = "TRUNCATE TABLE users"
        )

        assertFails("Truncate operation is not allowed.") {
            DataGuardPlugin.doBeforeAction.invoke(task, mockWrapper)
        }
    }

    @Test
    fun `should allow whitelisted truncate`() {
        DataGuardPlugin.enable {
            truncate {
                allow {
                    databaseName = "kronos"
                    tableName = "logs"
                }
            }
        }

        val task = createActionTask(
            operation = KOperationType.TRUNCATE,
            sql = "TRUNCATE TABLE logs",
            tableName = "logs"
        )

        // 正常执行不应抛出异常
        DataGuardPlugin.doBeforeAction.invoke(task, mockWrapper)
    }

    @Test
    fun `should block delete all by default`() {
        val task = createActionTask(
            operation = KOperationType.DELETE,
            sql = "DELETE FROM products",
            whereClause = null
        )

        assertFails("Delete operation is not allowed.") {
            DataGuardPlugin.doBeforeAction.invoke(task, mockWrapper)
        }
    }

    @Test
    fun `should allow delete with where clause`() {
        val task = createActionTask(
            operation = KOperationType.DELETE,
            sql = "DELETE FROM orders WHERE id = 123",
            whereClause = "id = 123"
        )

        DataGuardPlugin.doBeforeAction.invoke(task, mockWrapper) // 应正常执行
    }

    @Test
    fun `should handle drop operations`() {
        DataGuardPlugin.enable {
            drop {
                allowAll()
                deny {
                    tableName = "sensitive_%"
                }
            }
        }

        val blockedTask = createActionTask(
            operation = KOperationType.DROP,
            sql = "DROP TABLE sensitive_data"
        )

        assertFails("Drop operation is not allowed.") {
            DataGuardPlugin.doBeforeAction.invoke(blockedTask, mockWrapper)
        }

        val allowedTask = createActionTask(
            operation = KOperationType.DROP,
            sql = "DROP TABLE normal_table"
        )

        DataGuardPlugin.doBeforeAction.invoke(allowedTask, mockWrapper) // 应正常执行
    }

    @Test
    fun `should handle update operations`() {
        DataGuardPlugin.enable {
            updateAll {
                allow {
                    tableName = "allowed_updates"
                }
            }
        }

        val blockedTask = createActionTask(
            operation = KOperationType.UPDATE,
            sql = "UPDATE other_table SET value = 1",
            whereClause = null
        )

        assertFails("UPDATE operation is not allowed.") {
            DataGuardPlugin.doBeforeAction.invoke(blockedTask, mockWrapper)
        }

        val allowedTask = createActionTask(
            operation = KOperationType.UPDATE,
            sql = "UPDATE allowed_updates SET value = 1",
            tableName = "allowed_updates"
        )

        DataGuardPlugin.doBeforeAction.invoke(allowedTask, mockWrapper) // 应正常执行
    }

    @Test
    fun `should disable protection when plugin off`() {
        DataGuardPlugin.disable()
        assertFalse(ActionEvent.beforeActionEvents.contains(DataGuardPlugin.doBeforeAction))
    }

    @Test
    fun `should enable protection when plugin on`() {
        DataGuardPlugin.enable()
        assert(ActionEvent.beforeActionEvents.contains(DataGuardPlugin.doBeforeAction))
    }

    @Test
    fun `should handle database matching`() {
        DataGuardPlugin.enable {
            deleteAll {
                allow {
                    databaseName = "kronos"
                    tableName = "temp_%"
                }
            }
        }

        val validTask = createActionTask(
            operation = KOperationType.DELETE,
            sql = "DELETE FROM temp_logs"
        )

        DataGuardPlugin.doBeforeAction.invoke(validTask, mockWrapper)

        val mockWrapper2 = SampleMysqlJdbcWrapper().apply {
            url = url.replace("kronos", "test_db")
        } // Mock a different database

        val invalidTask = createActionTask(
            operation = KOperationType.DELETE,
            sql = "DELETE FROM temp_logs"
        )

        assertFails("Delete operation is not allowed.") {
            DataGuardPlugin.doBeforeAction.invoke(invalidTask, mockWrapper2)
        }
    }
}