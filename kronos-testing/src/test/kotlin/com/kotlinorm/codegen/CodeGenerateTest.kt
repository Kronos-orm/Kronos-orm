package com.kotlinorm.codegen

import com.kotlinorm.Kronos
import com.kotlinorm.beans.sample.codegen.CgStudent
import com.kotlinorm.beans.sample.codegen.CgUser
import com.kotlinorm.codegen.KronosConfig.Companion.write
import com.kotlinorm.codegen.TemplateConfig.Companion.template
import com.kotlinorm.orm.ddl.table
import org.intellij.lang.annotations.Language
import java.io.File
import java.time.LocalDateTime
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CodeGenerateTest {

    private lateinit var tempDir: File
    private lateinit var configPath: String

    @BeforeTest
    fun createTempDir() {
        tempDir = createTempDirectory("kotlinFileTest").toFile()
        configPath = File(tempDir, "testConfig.toml").apply {
            parentFile.mkdirs()
            writeTomlContent(
                """
                [[table]]
                name = "cg_user"
                className = "CgUser"
                
                [[table]]
                name = "cg_student"
                className = "CgStudent"
                
                [strategy]
                tableNamingStrategy = "lineHumpNamingStrategy"
                fieldNamingStrategy = "lineHumpNamingStrategy"
                createTimeStrategy = "create_time"
                updateTimeStrategy = "update_time"
                logicDeleteStrategy = "deleted"

                [output]
                targetDir = "$tempDir"
                packageName = "com.kotlinorm.orm.table"
                tableCommentLineWords = 80

                [dataSource]
                dataSourceClassName = "org.apache.commons.dbcp2.BasicDataSource"
                wrapperClassName = "com.kotlinorm.KronosBasicWrapper"
                url = "jdbc:mysql://localhost:3306/kronos_testing?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowMultiQueries=true&allowPublicKeyRetrieval=true&useServerPrepStmts=false&rewriteBatchedStatements=true"
                username = "${System.getenv("MYSQL_USERNAME")}"
                password = "${System.getenv("MYSQL_PASSWORD")}"
                driverClassName = "com.mysql.cj.jdbc.Driver"
                initialSize = 5
                maxActive = 10
                """.trimIndent()
            )
        }.path
    }

    @AfterTest
    fun cleanupTempDir() {
        tempDir.deleteRecursively()
    }

    fun File.writeTomlContent(@Language("toml") content: String) {
        this.parentFile.mkdirs()
        this.writeText(
            content.trimIndent()
        )
    }

    @Test
    fun testCodegen() {
        val now = LocalDateTime.now()
        init(configPath)
        Kronos.dataSource.table.syncTable(CgStudent())
        Kronos.dataSource.table.syncTable(CgUser())

        template {
            +"package $packageName"
            +""
            +imports.joinToString("\n") { "import $it" }
            +""
            +formatedComment
            +"// @author: Kronos-Codegen"
            +"// @date: $now"
            +""
            +"@Table(name = \"$tableName\")"
            +indexes.toAnnotations()
            +"data class $className("
            fields.forEach { field ->
                field.annotations().forEach { annotation ->
                    +"${indent(4)}$annotation"
                }
                +"${indent(4)}var ${field.name}: ${field.kotlinType}? = null,"
            }
            +"): KPojo"
        }.write()

        val tbUser = codeGenConfig?.table[0]!!
        var lines = File(tempDir, "${tbUser.className}.kt").readLines()
        print(lines)
        assertEquals("package com.kotlinorm.orm.table", lines[0])
        assertEquals("", lines[1])
        assertEquals("import com.kotlinorm.annotations.Table", lines[2])
        assertEquals("import com.kotlinorm.interfaces.KPojo", lines[3])
        assertEquals("import com.kotlinorm.annotations.PrimaryKey", lines[4])
        assertEquals("import com.kotlinorm.annotations.ColumnType", lines[5])
        assertEquals("import com.kotlinorm.enums.KColumnType", lines[6])
        assertEquals("import com.kotlinorm.annotations.Default", lines[7])
        assertEquals("import com.kotlinorm.annotations.Necessary", lines[8])
        assertEquals("import com.kotlinorm.annotations.CreateTime", lines[9])
        assertEquals("import com.kotlinorm.annotations.UpdateTime", lines[10])
        assertEquals("import com.kotlinorm.annotations.LogicDelete", lines[11])
        assertEquals("import com.kotlinorm.annotations.TableIndex", lines[12])
        assertEquals("", lines[13])
        assertEquals("// Kotlin Data Class for MysqlUser", lines[14])
        assertEquals("// @author: Kronos-Codegen", lines[15])
        assertEquals("// @date: $now", lines[16])
        assertEquals("", lines[17])
        assertEquals("@Table(name = \"cg_user\")", lines[18])
        assertEquals(
            "@TableIndex(name = \"idx_multi\", columns = [\"id\", \"username\"], type = \"UNIQUE\", method = \"BTREE\")",
            lines[19]
        )
        assertEquals(
            "@TableIndex(name = \"idx_username\", columns = [\"username\"], type = \"NORMAL\", method = \"BTREE\")",
            lines[20]
        )
        assertEquals("data class CgUser(", lines[21])
        assertEquals("    @PrimaryKey(identity = true)", lines[22])
        assertEquals("    var id: Int? = null,", lines[23])
        assertEquals("    @ColumnType(type = KColumnType.VARCHAR, length = 254)", lines[24])
        assertEquals("    var username: String? = null,", lines[25])
        assertEquals("    var score: Int? = null,", lines[26])
        assertEquals("    @Default(\"0\")", lines[27])
        assertEquals("    var gender: Boolean? = null,", lines[28])
        assertEquals("    @Necessary", lines[29])
        assertEquals("    @CreateTime", lines[30])
        assertEquals("    var createTime: String? = null,", lines[31])
        assertEquals("    @Necessary", lines[32])
        assertEquals("    @UpdateTime", lines[33])
        assertEquals("    var updateTime: java.time.LocalDateTime? = null,", lines[34])
        assertEquals("    @Necessary", lines[35])
        assertEquals("    @Default(\"0\")", lines[36])
        assertEquals("    @LogicDelete", lines[37])
        assertEquals("    var deleted: Boolean? = null,", lines[38])
        assertEquals("): KPojo", lines[39])

        val student = codeGenConfig?.table[1]!!
        lines = File(tempDir, "${student.className}.kt").readLines()
        print(lines)

        assertEquals("package com.kotlinorm.orm.table", lines[0])
        assertEquals("", lines[1])
        assertEquals("import com.kotlinorm.annotations.Table", lines[2])
        assertEquals("import com.kotlinorm.interfaces.KPojo", lines[3])
        assertEquals("import com.kotlinorm.annotations.PrimaryKey", lines[4])
        assertEquals("import com.kotlinorm.annotations.CreateTime", lines[5])
        assertEquals("", lines[6])
        assertEquals("", lines[7])
        assertEquals("// @author: Kronos-Codegen", lines[8])
        assertEquals("// @date: $now", lines[9])
        assertEquals("", lines[10])
        assertEquals("@Table(name = \"cg_student\")", lines[11])
        assertEquals("data class CgStudent(", lines[12])
        assertEquals("    @PrimaryKey(identity = true)", lines[13])
        assertEquals("    var id: Int? = null,", lines[14])
        assertEquals("    var name: String? = null,", lines[15])
        assertEquals("    var studentNo: String? = null,", lines[16])
        assertEquals("    var schoolName: String? = null,", lines[17])
        assertEquals("    var groupClassName: String? = null,", lines[18])
        assertEquals("    @CreateTime", lines[19])
        assertEquals("    var createTime: String? = null,", lines[20])
        assertEquals("): KPojo", lines[21])
    }
}