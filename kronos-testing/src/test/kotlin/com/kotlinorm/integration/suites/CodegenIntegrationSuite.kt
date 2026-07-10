package com.kotlinorm.integration.suites

import com.kotlinorm.codegen.KronosConfig.Companion.write
import com.kotlinorm.codegen.TemplateConfig
import com.kotlinorm.codegen.codeGenConfig
import com.kotlinorm.codegen.init
import com.kotlinorm.codegen.kotlinType
import com.kotlinorm.database.SqlExecutor.execute
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.integration.profiles.StandardIntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

abstract class CodegenIntegrationSuite(
    private val codegenEnvironment: IntegrationDatabaseEnvironment,
) : IntegrationSuiteSupport(codegenEnvironment, StandardIntegrationScenarioProfile) {
    @Test
    fun codegenReadsMysqlMetadataAndWritesGeneratedFilesAgainstRealDatabase() {
        assumeDatabaseAvailable()
        configureKronos()
        recreateCodegenTables()

        val tempDir = createTempDirectory("kronos-codegen-integration-").toFile()
        try {
            val outputDir = File(tempDir, "generated")
            val configFile = File(tempDir, "codegen.toml").apply {
                writeText(codegenConfigToml(outputDir))
            }

            init(configFile.absolutePath)

            val config = assertNotNull(codeGenConfig)
            assertEquals(
                CodegenConfigShape(
                    tableNames = listOf("kt_codegen_user", "kt_codegen_student"),
                    classNames = listOf("CodegenUser", "CodegenStudent"),
                    packageName = "com.kotlinorm.integration.generated",
                    targetDir = outputDir.absolutePath,
                ),
                CodegenConfigShape(
                    tableNames = config.tableNames,
                    classNames = config.classNames,
                    packageName = config.packageName,
                    targetDir = config.targetDir,
                ),
            )

            val templates = config.toKronosConfigs()
            assertEquals(
                listOf(
                    CodegenTableShape(
                        tableName = "kt_codegen_user",
                        className = "CodegenUser",
                        tableComment = "Generated user table",
                        fields = listOf(
                            CodegenFieldShape("id", "id", KColumnType.INT, 0, 0, false, PrimaryKeyType.IDENTITY, null, "identifier", "Int"),
                            CodegenFieldShape("username", "username", KColumnType.VARCHAR, 64, 0, false, PrimaryKeyType.NOT, null, "login name", "String"),
                            CodegenFieldShape("age", "age", KColumnType.INT, 0, 0, true, PrimaryKeyType.NOT, null, "age value", "Int"),
                            CodegenFieldShape("created_at", "createdAt", KColumnType.DATETIME, 0, 0, true, PrimaryKeyType.NOT, null, "created time", "java.time.LocalDateTime"),
                            CodegenFieldShape("deleted", "deleted", KColumnType.BIT, 1, 0, true, PrimaryKeyType.NOT, null, "logic flag", "Boolean"),
                        ),
                        indexes = listOf(
                            CodegenIndexShape("idx_codegen_user_age", listOf("age"), "NORMAL", "BTREE"),
                            CodegenIndexShape("uk_codegen_user_username", listOf("username"), "UNIQUE", "BTREE"),
                        ),
                    ),
                    CodegenTableShape(
                        tableName = "kt_codegen_student",
                        className = "CodegenStudent",
                        tableComment = "Generated student table",
                        fields = listOf(
                            CodegenFieldShape("student_id", "studentId", KColumnType.BIGINT, 0, 0, false, PrimaryKeyType.IDENTITY, null, "student identifier", "Long"),
                            CodegenFieldShape("user_id", "userId", KColumnType.INT, 0, 0, false, PrimaryKeyType.NOT, null, "owner user", "Int"),
                            CodegenFieldShape("nickname", "nickname", KColumnType.VARCHAR, 80, 0, true, PrimaryKeyType.NOT, null, "student nickname", "String"),
                        ),
                        indexes = listOf(
                            CodegenIndexShape("idx_codegen_student_user", listOf("user_id", "nickname"), "NORMAL", "BTREE"),
                        ),
                    ),
                ),
                templates.map { template ->
                    CodegenTableShape(
                        tableName = template.tableName,
                        className = template.className,
                        tableComment = template.tableComment,
                        fields = template.fields.map { field ->
                            CodegenFieldShape(
                                columnName = field.columnName,
                                name = field.name,
                                type = field.type,
                                length = field.length,
                                scale = field.scale,
                                nullable = field.nullable,
                                primaryKey = field.primaryKey,
                                defaultValue = field.defaultValue,
                                kDoc = field.kDoc,
                                kotlinType = field.kotlinType,
                            )
                        },
                        indexes = template.indexes
                            .map { index ->
                                CodegenIndexShape(
                                    name = index.name,
                                    columns = index.columns.toList(),
                                    type = index.type,
                                    method = index.method,
                                )
                            }
                            .sortedBy { it.name },
                    )
                },
            )

            val generated = TemplateConfig.template {
                +"package $packageName"
                +""
                +"// table=$tableName"
                +"// class=$className"
                +"// comment=$tableComment"
                +"// indexes=${indexes.sortedBy { it.name }.joinToString(" | ") { it.toAnnotations() }}"
                fields.forEach { field ->
                    +"// field=${field.columnName},${field.name},${field.type},${field.length},${field.scale},${field.nullable},${field.primaryKey},${field.defaultValue},${field.kDoc},${field.kotlinType}"
                }
            }

            generated.write()
            assertEquals(
                listOf(
                    File(outputDir, "CodegenStudent.kt").absolutePath,
                    File(outputDir, "CodegenUser.kt").absolutePath,
                ),
                outputDir.listFiles().orEmpty().map { it.absolutePath }.sorted(),
            )
            assertEquals(
                expectedGeneratedUser(),
                File(outputDir, "CodegenUser.kt").readText(),
            )
            assertEquals(
                expectedGeneratedStudent(),
                File(outputDir, "CodegenStudent.kt").readText(),
            )
        } finally {
            codeGenConfig = null
            tempDir.deleteRecursively()
            dropCodegenTables()
        }
    }

    private fun recreateCodegenTables() {
        dropCodegenTables()
        wrapper.execute(
            """
            CREATE TABLE `kt_codegen_user` (
                `id` INT NOT NULL AUTO_INCREMENT COMMENT 'identifier',
                `username` VARCHAR(64) NOT NULL COMMENT 'login name',
                `age` INT NULL COMMENT 'age value',
                `created_at` DATETIME NULL COMMENT 'created time',
                `deleted` BIT(1) NULL COMMENT 'logic flag',
                PRIMARY KEY (`id`),
                UNIQUE KEY `uk_codegen_user_username` (`username`),
                KEY `idx_codegen_user_age` (`age`)
            ) ENGINE=InnoDB COMMENT='Generated user table'
            """.trimIndent(),
        )
        wrapper.execute(
            """
            CREATE TABLE `kt_codegen_student` (
                `student_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'student identifier',
                `user_id` INT NOT NULL COMMENT 'owner user',
                `nickname` VARCHAR(80) NULL COMMENT 'student nickname',
                PRIMARY KEY (`student_id`),
                KEY `idx_codegen_student_user` (`user_id`, `nickname`)
            ) ENGINE=InnoDB COMMENT='Generated student table'
            """.trimIndent(),
        )
    }

    private fun dropCodegenTables() {
        wrapper.execute("DROP TABLE IF EXISTS `kt_codegen_student`")
        wrapper.execute("DROP TABLE IF EXISTS `kt_codegen_user`")
    }

    private fun codegenConfigToml(outputDir: File): String =
        """
        [[table]]
        name = "kt_codegen_user"
        className = "CodegenUser"

        [[table]]
        name = "kt_codegen_student"
        className = "CodegenStudent"

        [strategy]
        tableNamingStrategy = "lineHumpNamingStrategy"
        fieldNamingStrategy = "lineHumpNamingStrategy"
        createTimeStrategy = "createdAt"
        logicDeleteStrategy = "deleted"

        [output]
        targetDir = "${outputDir.absolutePath.replace("\\", "\\\\")}"
        packageName = "com.kotlinorm.integration.generated"
        tableCommentLineWords = 80

        [dataSource]
        dataSourceClassName = "org.apache.commons.dbcp2.BasicDataSource"
        url = "${codegenEnvironment.url.replace("\\", "\\\\")}"
        username = "${codegenEnvironment.username.orEmpty().replace("\\", "\\\\")}"
        password = "${codegenEnvironment.password.orEmpty().replace("\\", "\\\\")}"
        driverClassName = "${codegenEnvironment.driverClassName}"
        maxTotal = 3
        maxIdle = 2
        """.trimIndent()

    private fun expectedGeneratedUser(): String =
        """
        package com.kotlinorm.integration.generated

        // table=kt_codegen_user
        // class=CodegenUser
        // comment=Generated user table
        // indexes=@TableIndex(name = "idx_codegen_user_age", columns = ["age"], type = "NORMAL", method = "BTREE") | @TableIndex(name = "uk_codegen_user_username", columns = ["username"], type = "UNIQUE", method = "BTREE")
        // field=id,id,INT,0,0,false,IDENTITY,null,identifier,Int
        // field=username,username,VARCHAR,64,0,false,NOT,null,login name,String
        // field=age,age,INT,0,0,true,NOT,null,age value,Int
        // field=created_at,createdAt,DATETIME,0,0,true,NOT,null,created time,java.time.LocalDateTime
        // field=deleted,deleted,BIT,1,0,true,NOT,null,logic flag,Boolean
        """.trimIndent() + "\n"

    private fun expectedGeneratedStudent(): String =
        """
        package com.kotlinorm.integration.generated

        // table=kt_codegen_student
        // class=CodegenStudent
        // comment=Generated student table
        // indexes=@TableIndex(name = "idx_codegen_student_user", columns = ["user_id", "nickname"], type = "NORMAL", method = "BTREE")
        // field=student_id,studentId,BIGINT,0,0,false,IDENTITY,null,student identifier,Long
        // field=user_id,userId,INT,0,0,false,NOT,null,owner user,Int
        // field=nickname,nickname,VARCHAR,80,0,true,NOT,null,student nickname,String
        """.trimIndent() + "\n"

    private data class CodegenConfigShape(
        val tableNames: List<String>,
        val classNames: List<String>,
        val packageName: String,
        val targetDir: String,
    )

    private data class CodegenTableShape(
        val tableName: String,
        val className: String,
        val tableComment: String,
        val fields: List<CodegenFieldShape>,
        val indexes: List<CodegenIndexShape>,
    )

    private data class CodegenFieldShape(
        val columnName: String,
        val name: String,
        val type: KColumnType,
        val length: Int,
        val scale: Int,
        val nullable: Boolean,
        val primaryKey: PrimaryKeyType,
        val defaultValue: String?,
        val kDoc: String?,
        val kotlinType: String,
    )

    private data class CodegenIndexShape(
        val name: String,
        val columns: List<String>,
        val type: String,
        val method: String,
    )
}
