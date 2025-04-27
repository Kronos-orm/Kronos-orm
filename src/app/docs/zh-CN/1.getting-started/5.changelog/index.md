{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

`Kronos` 严格遵循 Semantic Versioning 2.0.0 语义化版本规范。

- **当前版本:** `0.0.2`

## 📝 更新日志

### 0.0.1

- Kronos的第一个版本发布，提供了ORM的所有功能。
- 提供`Kronos-core`、`Kronos-compiler-plugin`、`Kronos-jdbc-wrapper`、`Kronos-logging`、`Kronos-gradle-plugin`、
  `Kronos-maven-plugin`等官方库，可直接使用。
- 最低支持的Kotlin版本为 `2.1.0`。

### 0.0.2

- 🐛修复`@CreateTime`、`@Update`表级注解参数`enable`为`false`时不生效的问题，
  修复全局表创建时间、更新时间、逻辑删除字段设置，使其仅在KPojo包含该字段时生效的问题。（[#92](https://github.com/Kronos-orm/Kronos-orm/pull/92)）
- 💪优化实例化性能，取消转换时使用`map`作为中间变量（[#94](https://github.com/Kronos-orm/Kronos-orm/pull/92)）。
- 💪添加默认数据源的事务创建函数
  `fun transact(wrapper: KronosDataSourceWrapper? = null, block: () -> Any?)`（[#94](https://github.com/Kronos-orm/Kronos-orm/pull/95)）
-💪为insert语句添加缓存支持，性能提升20%，详细性能数据请查看kronos-benchmark（[#100](https://github.com/Kronos-orm/Kronos-orm/pull/100)）
- 为 `Field` 添加 `scale` 属性，用于指定小数类型的精度，并重构从 kotlin 类型到 Column 类型的默认映射，将 `java.math.BigDecimal` 更改为 `decimal`（原为 `NUMERIC`），将 `kotlin.ByteArray` 更改为 `BLOB`（原为`BINARY`） ([#106](https://github.com/Kronos-orm/Kronos-orm/pull/106))

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
    id 'kronos-gradle-plugin' version '0.0.3-SNAPSHOT'
}

dependencies {
    implementation "com.kotlinorm:kronos-core:0.0.3-SNAPSHOT"
    implementation "com.kotlinorm:kronos-jdbc-wrapper:0.0.3-SNAPSHOT"
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
    id("kronos-gradle-plugin") version "0.0.3-SNAPSHOT"
}

dependencies {
    implementation("com.kotlinorm:kronos-core:0.0.3-SNAPSHOT")
    implementation("com.kotlinorm:kronos-jdbc-wrapper:0.0.3-SNAPSHOT")
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
            <version>0.0.3-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>com.kotlinorm</groupId>
            <artifactId>kronos-jdbc-wrapper</artifactId>
            <version>0.0.3-SNAPSHOT</version>
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
