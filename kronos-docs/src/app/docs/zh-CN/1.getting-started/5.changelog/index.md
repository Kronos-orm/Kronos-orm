{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

`Kronos` 严格遵循 Semantic Versioning 2.0.0 语义化版本规范。

- **当前版本:** `0.0.7`

## 📝 更新日志

### 0.0.7

- 🔧 升级 Gradle 至 9.4.1，Kotlin 至 2.3.0 ([#201](https://github.com/Kronos-orm/Kronos-orm/pull/201))
- 🐛 修复 PostgreSQL 自增主键对 BIGINT 列使用 BIGSERIAL ([#201](https://github.com/Kronos-orm/Kronos-orm/pull/201))
- 💪 重构表名和表注释从函数改为属性（`__tableName`、`__tableComment`）([#198](https://github.com/Kronos-orm/Kronos-orm/pull/198))
- 🔧 修复 bump-version.sh 在 Linux CI 环境下的 sed 语法问题 ([#197](https://github.com/Kronos-orm/Kronos-orm/pull/197))

### 0.0.6

- ✨ 更新 Kotlin 版本至 `2.2.21`，添加对 `kotlin.time.Instant` 的支持 ([#191](https://github.com/Kronos-orm/Kronos-orm/pull/191))
- ✨ 为 select 子句添加 patch 功能 ([#192](https://github.com/Kronos-orm/Kronos-orm/pull/192))
- ✨ 添加 `takeIf` 条件支持，用于条件查询处理 ([#178](https://github.com/Kronos-orm/Kronos-orm/pull/178))
- 💪 增强 upsert 逻辑，支持空值处理和逻辑删除策略 ([#188](https://github.com/Kronos-orm/Kronos-orm/pull/188))
- 🐛 修复 join with on 子句时 criteria 丢失的问题 ([#194](https://github.com/Kronos-orm/Kronos-orm/pull/194))
- 🐛 修复 `java.sql.Date` 转换逻辑，并为 `LocalDate` 类型解析添加测试用例 ([#185](https://github.com/Kronos-orm/Kronos-orm/pull/185))
- 🐛 修复 jdbc wrapper `getObject(position)` 空指针异常 ([#184](https://github.com/Kronos-orm/Kronos-orm/pull/184))
- 🐛 处理列定义中的空默认值 ([#175](https://github.com/Kronos-orm/Kronos-orm/pull/175))

### 0.0.5

- ✨ 为 DeleteClauseInfo 和 InsertClauseInfo 添加 kClass 属性 ([#152](https://github.com/Kronos-orm/Kronos-orm/pull/152))
- 💪 优化查询参数处理，修复无效参数映射 ([#150](https://github.com/Kronos-orm/Kronos-orm/pull/150))

### 0.0.4

- ✨ 添加默认布尔值处理方法 `getDefaultBoolean` ([#147](https://github.com/Kronos-orm/Kronos-orm/pull/147))
- 💪 修改缓存实现，使用现有实例而不是每次创建新实例 ([#145](https://github.com/Kronos-orm/Kronos-orm/pull/145))
- 🐛 确保 PostgreSQL SQL 语句中的表名被正确引用 ([#142](https://github.com/Kronos-orm/Kronos-orm/pull/142))

### 0.0.3

- ✨ 添加新的 CodeGen 模块用于代码生成 ([#130](https://github.com/Kronos-orm/Kronos-orm/pull/130))
- ✨ 添加 DataGuardPlugin，防止对表进行删除、更新和清空操作
- ✨ 添加 LastInsertIdPlugin，支持获取最后插入的 ID
- ✨ 添加任务事件钩子支持，包括 QueryEvent 和 ActionEvent ([#123](https://github.com/Kronos-orm/Kronos-orm/pull/123))
- ✨ 添加缓存和标识配置 ([#122](https://github.com/Kronos-orm/Kronos-orm/pull/122))
- ✨ 添加命名 SQL 解析缓存 ([#116](https://github.com/Kronos-orm/Kronos-orm/pull/116))
- 💪 重构 jdbc wrapper ([#117](https://github.com/Kronos-orm/Kronos-orm/pull/117))
- 💪 重构 LastInsertIdPlugin 启用逻辑，使用插件注册/注销方式 ([#125](https://github.com/Kronos-orm/Kronos-orm/pull/125))
- 💪 更新日志 DSL ([#137](https://github.com/Kronos-orm/Kronos-orm/pull/137))
- 🐛 修复 InsertClause 中的空值处理逻辑以适配 SpringData
- 🐛 修复覆盖率计算中的字段索引 ([#133](https://github.com/Kronos-orm/Kronos-orm/pull/133))
- 🔧 移除 KronosKClassMapperTransformer ([#131](https://github.com/Kronos-orm/Kronos-orm/pull/131))

### 0.0.2

- 🐛 修复 `@CreateTime`、`@Update` 表级注解参数 `enable` 为 `false` 时不生效的问题，
  修复全局表创建时间、更新时间、逻辑删除字段设置，使其仅在 KPojo 包含该字段时生效。([#92](https://github.com/Kronos-orm/Kronos-orm/pull/92))
- 💪 优化实例化性能，取消转换时使用 `map` 作为中间变量。([#94](https://github.com/Kronos-orm/Kronos-orm/pull/92))
- 💪 添加默认数据源的事务创建函数
  `fun transact(wrapper: KronosDataSourceWrapper? = null, block: () -> Any?)`。([#94](https://github.com/Kronos-orm/Kronos-orm/pull/95))
- 💪 为 insert 语句添加缓存支持，性能提升 20%，详细性能数据请查看 kronos-benchmark。([#100](https://github.com/Kronos-orm/Kronos-orm/pull/100))
- ✨ 为 `Field` 添加 `scale` 属性，用于指定小数类型的精度，并重构从 kotlin 类型到 Column 类型的默认映射，将 `java.math.BigDecimal` 更改为 `decimal`（原为 `NUMERIC`），将 `kotlin.ByteArray` 更改为 `BLOB`（原为 `BINARY`）。([#106](https://github.com/Kronos-orm/Kronos-orm/pull/106))

### 0.0.1

- Kronos 的第一个版本发布，提供了 ORM 的所有功能。
- 提供 `Kronos-core`、`Kronos-compiler-plugin`、`Kronos-jdbc-wrapper`、`Kronos-logging`、`Kronos-gradle-plugin`、
  `Kronos-maven-plugin` 等官方库，可直接使用。
- 最低支持的 Kotlin 版本为 `2.2.0`。

> **Note**
> 当前版本为开发阶段，不保证向后兼容性。
>
> 若您有意愿，可以使用最新版本来测试。

```groovy name="gradle(groovy)" icon="gradle" group="dependency"
// settings.gradle
pluginManagement {
    repositories {
        mavenCentral()
        maven {
            url "https://central.sonatype.com/repository/maven-snapshots/"
        }
    }
}

// build.gradle
repositories {
    mavenCentral()
    maven {
        url "https://central.sonatype.com/repository/maven-snapshots/"
    }
}

plugins {
    id 'kronos-gradle-plugin' version '0.1.0-SNAPSHOT'
}

dependencies {
    implementation "com.kotlinorm:kronos-core:0.1.0-SNAPSHOT"
    implementation "com.kotlinorm:kronos-jdbc-wrapper:0.1.0-SNAPSHOT"
}
```

```groovy name="gradle(kts)" icon="gradlekts" group="dependency"
// settings.gradle.kts
pluginManagement {
    repositories {
        mavenCentral()
        maven {
            name = "Maven Central Snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
}

// build.gradle.kts
repositories {
    mavenCentral()
    maven {
        name = "Maven Central Snapshots"
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
    }
}

plugins {
    id("kronos-gradle-plugin") version "0.1.0-SNAPSHOT"
}

dependencies {
    implementation("com.kotlinorm:kronos-core:0.1.0-SNAPSHOT")
    implementation("com.kotlinorm:kronos-jdbc-wrapper:0.1.0-SNAPSHOT")
}
```

```xml name="maven" icon="maven" group="dependency"
<project>
    <repositories>
        <repository>
            <id>maven-center-snapshots</id>
            <url>https://central.sonatype.com/repository/maven-snapshots/</url>
            <releases>
                <enabled>false</enabled>
            </releases>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>com.kotlinorm</groupId>
            <artifactId>kronos-core</artifactId>
            <version>0.1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>com.kotlinorm</groupId>
            <artifactId>kronos-jdbc-wrapper</artifactId>
            <version>0.1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>

    <pluginRepositories>
        <pluginRepository>
            <id>central-portal-snapshots</id>
            <url>https://central.sonatype.com/repository/maven-snapshots/</url>
            <releases>
                <enabled>false</enabled>
            </releases>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </pluginRepository>
    </pluginRepositories>

    <plugins>
        <plugin>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-maven-plugin</artifactId>
            <extensions>true</extensions>
            <configuration>
                <compilerPlugins>
                    <plugin>all-open</plugin>
                    <plugin>kronos-maven-plugin</plugin>
                </compilerPlugins>
            </configuration>
            <dependencies>
                <dependency>
                    <groupId>org.jetbrains.kotlin</groupId>
                    <artifactId>kotlin-maven-allopen</artifactId>
                    <version>${kotlin.version}</version>
                </dependency>
                <dependency>
                    <groupId>com.kotlinorm</groupId>
                    <artifactId>kronos-maven-plugin</artifactId>
                    <version>${kronos.version}</version>
                </dependency>
            </dependencies>
        </plugin>
    </plugins>
</project>
```
