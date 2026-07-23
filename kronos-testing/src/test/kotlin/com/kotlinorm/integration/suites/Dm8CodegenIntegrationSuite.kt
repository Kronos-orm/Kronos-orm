package com.kotlinorm.integration.suites

import com.kotlinorm.codegen.KronosConfig.Companion.write
import com.kotlinorm.codegen.TemplateConfig
import com.kotlinorm.codegen.codeGenConfig
import com.kotlinorm.codegen.init
import com.kotlinorm.codegen.kotlinType
import com.kotlinorm.database.SqlExecutor.execute
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.integration.profiles.StandardIntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironments.dm8
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import org.apache.commons.dbcp2.BasicDataSource
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class Dm8CodegenIntegrationTest : IntegrationSuiteSupport(dm8, StandardIntegrationScenarioProfile) {
    @Test
    fun codegenReadsDm8MetadataAndWritesGeneratedFilesAgainstRealDatabase() {
        requireDatabaseAvailable()
        configureKronos()
        recreateCodegenTables()

        val tempDir = createTempDirectory("kronos-dm8-codegen-integration-").toFile()
        try {
            val outputDir = File(tempDir, "generated")
            val configFile = File(tempDir, "codegen.toml").apply {
                writeText(codegenConfigToml(outputDir))
            }

            init(configFile.absolutePath)

            val config = assertNotNull(codeGenConfig)
            assertEquals(DBType.DM8, config.wrapper.dbType)
            assertEquals(
                CodegenConfigShape(
                    tableNames = listOf("kt_codegen_dm8_user"),
                    classNames = listOf("Dm8CodegenUser"),
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
                        tableName = "kt_codegen_dm8_user",
                        className = "Dm8CodegenUser",
                        tableComment = "Generated DM8 user table",
                        fields = listOf(
                            CodegenFieldShape("id", "id", KColumnType.INT, false, PrimaryKeyType.IDENTITY, "identifier", "Int"),
                            CodegenFieldShape("username", "username", KColumnType.VARCHAR, false, PrimaryKeyType.NOT, "login name", "String"),
                            CodegenFieldShape("age", "age", KColumnType.INT, true, PrimaryKeyType.NOT, "age value", "Int"),
                            CodegenFieldShape("created_at", "createdAt", KColumnType.TIMESTAMP, true, PrimaryKeyType.NOT, "created time", "java.time.Instant"),
                        ),
                        indexes = listOf(
                            CodegenIndexShape("idx_codegen_dm8_user_age", listOf("age"), "NORMAL", ""),
                            CodegenIndexShape("uk_codegen_dm8_user_username", listOf("username"), "UNIQUE", ""),
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
                                nullable = field.nullable,
                                primaryKey = field.primaryKey,
                                kDoc = field.kDoc,
                                kotlinType = field.kotlinType,
                            )
                        },
                        indexes = template.indexes
                            .map { index ->
                                CodegenIndexShape(
                                    name = index.name.lowercase(),
                                    columns = index.columns.map(String::lowercase),
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
                    +"// field=${field.columnName},${field.name},${field.type},${field.nullable},${field.primaryKey},${field.kDoc},${field.kotlinType}"
                }
            }

            generated.write()
            assertEquals(
                listOf(File(outputDir, "Dm8CodegenUser.kt").absolutePath),
                outputDir.listFiles().orEmpty().map { it.absolutePath }.sorted(),
            )
            assertEquals(
                expectedGeneratedUser(),
                File(outputDir, "Dm8CodegenUser.kt").readText(),
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
            CREATE TABLE KT_CODEGEN_DM8_USER (
                ID INT IDENTITY(1, 1) NOT NULL,
                USERNAME VARCHAR(64) NOT NULL,
                AGE INT,
                CREATED_AT TIMESTAMP(6),
                CONSTRAINT PK_CODEGEN_DM8_USER PRIMARY KEY (ID),
                CONSTRAINT UK_CODEGEN_DM8_USER_USERNAME UNIQUE (USERNAME)
            )
            """.trimIndent(),
        )
        wrapper.execute("CREATE INDEX IDX_CODEGEN_DM8_USER_AGE ON KT_CODEGEN_DM8_USER (AGE)")
        wrapper.execute("COMMENT ON TABLE KT_CODEGEN_DM8_USER IS 'Generated DM8 user table'")
        wrapper.execute("COMMENT ON COLUMN KT_CODEGEN_DM8_USER.ID IS 'identifier'")
        wrapper.execute("COMMENT ON COLUMN KT_CODEGEN_DM8_USER.USERNAME IS 'login name'")
        wrapper.execute("COMMENT ON COLUMN KT_CODEGEN_DM8_USER.AGE IS 'age value'")
        wrapper.execute("COMMENT ON COLUMN KT_CODEGEN_DM8_USER.CREATED_AT IS 'created time'")
    }

    private fun dropCodegenTables() {
        wrapper.execute("DROP TABLE IF EXISTS KT_CODEGEN_DM8_USER")
    }

    private fun codegenConfigToml(outputDir: File): String {
        val targetDir = outputDir.absolutePath.replace("\\", "\\\\")
        val url = dm8.url.replace("\\", "\\\\")
        val username = dm8.username.orEmpty().replace("\\", "\\\\")
        return """
        [[table]]
        name = "kt_codegen_dm8_user"
        className = "Dm8CodegenUser"

        [strategy]
        tableNamingStrategy = "lineHumpNamingStrategy"
        fieldNamingStrategy = "lineHumpNamingStrategy"
        createTimeStrategy = "createdAt"

        [output]
        targetDir = "$targetDir"
        packageName = "com.kotlinorm.integration.generated"
        tableCommentLineWords = 80

        [dataSource]
        dataSourceClassName = "com.kotlinorm.integration.suites.Dm8CodegenEnvironmentDataSource"
        url = "$url"
        username = "$username"
        driverClassName = "${dm8.driverClassName}"
        maxTotal = 3
        maxIdle = 2
        """.trimIndent()
    }

    private fun expectedGeneratedUser(): String =
        """
        package com.kotlinorm.integration.generated

        // table=kt_codegen_dm8_user
        // class=Dm8CodegenUser
        // comment=Generated DM8 user table
        // indexes=@TableIndex(name = "IDX_CODEGEN_DM8_USER_AGE", columns = ["age"], type = "NORMAL") | @TableIndex(name = "UK_CODEGEN_DM8_USER_USERNAME", columns = ["username"], type = "UNIQUE")
        // field=id,id,INT,false,IDENTITY,identifier,Int
        // field=username,username,VARCHAR,false,NOT,login name,String
        // field=age,age,INT,true,NOT,age value,Int
        // field=created_at,createdAt,TIMESTAMP,true,NOT,created time,java.time.Instant
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
        val nullable: Boolean,
        val primaryKey: PrimaryKeyType,
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

/**
 * Supplies the DM8 password from the process environment so Codegen tests do not write it to TOML.
 */
class Dm8CodegenEnvironmentDataSource : BasicDataSource() {
    init {
        password = System.getenv("DM_PASSWORD").orEmpty().ifBlank { "SYSDBA" }
    }
}
