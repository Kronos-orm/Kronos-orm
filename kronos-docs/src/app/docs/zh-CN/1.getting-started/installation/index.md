{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 运行时和编译插件

添加 `kronos-core` 作为 ORM 运行时，并启用 Kronos 编译插件来获得编译期 DSL 支持。

可复制依赖片段使用 `{{ $.kronosVersion() }}`。只有在测试未发布源码改动时才使用 snapshot 版本。

```kotlin group="Gradle" name="build.gradle.kts" icon="gradlekts"
plugins {
    kotlin("jvm") version "2.4.0"
    id("com.kotlinorm.kronos-gradle-plugin") version "{{ $.kronosVersion() }}"
}

dependencies {
    implementation("com.kotlinorm:kronos-core:{{ $.kronosVersion() }}")
}
```

```groovy group="Gradle" name="build.gradle" icon="gradle"
plugins {
    id 'org.jetbrains.kotlin.jvm' version '2.4.0'
    id 'com.kotlinorm.kronos-gradle-plugin' version '{{ $.kronosVersion() }}'
}

dependencies {
    implementation 'com.kotlinorm:kronos-core:{{ $.kronosVersion() }}'
}
```

```xml group="Maven" name="pom.xml" icon="maven"
<project>
    <properties>
        <kotlin.version>2.4.0</kotlin.version>
        <kronos.version>{{ $.kronosVersion() }}</kronos.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.kotlinorm</groupId>
            <artifactId>kronos-core</artifactId>
            <version>${kronos.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.jetbrains.kotlin</groupId>
                <artifactId>kotlin-maven-plugin</artifactId>
                <version>${kotlin.version}</version>
                <extensions>true</extensions>
                <configuration>
                    <compilerPlugins>
                        <plugin>kronos-maven-plugin</plugin>
                    </compilerPlugins>
                </configuration>
                <dependencies>
                    <dependency>
                        <groupId>com.kotlinorm</groupId>
                        <artifactId>kronos-maven-plugin</artifactId>
                        <version>${kronos.version}</version>
                    </dependency>
                </dependencies>
            </plugin>
        </plugins>
    </build>
</project>
```

## JDBC wrapper

应用直接使用 JDBC `DataSource` 时，添加 `kronos-jdbc-wrapper`。它负责执行生成 SQL，并把查询结果映射回 KPojo 对象。

```kotlin group="JDBC wrapper" name="Gradle" icon="gradlekts"
dependencies {
    implementation("com.kotlinorm:kronos-jdbc-wrapper:{{ $.kronosVersion() }}")
}
```

```xml group="JDBC wrapper" name="Maven" icon="maven"
<dependency>
    <groupId>com.kotlinorm</groupId>
    <artifactId>kronos-jdbc-wrapper</artifactId>
    <version>${kronos.version}</version>
</dependency>
```

连接配置见 {{ $.keyword("database/connect-to-db", ["连接到数据库"]) }}。Spring JDBC 或其他框架的接入见 {{ $.keyword("database/custom-wrapper", ["自定义 Wrapper"]) }}。

## 数据库驱动和连接池

JDBC Driver 和连接池版本需要与数据库服务端、JDK 匹配。下面的 `<latest-stable>` 占位符表示第三方依赖按运行环境选择当前稳定版。

```kotlin group="Driver" name="MySQL example" icon="gradlekts"
dependencies {
    implementation("org.apache.commons:commons-dbcp2:<latest-stable>")
    implementation("com.mysql:mysql-connector-j:<latest-stable>")
}
```

## 最小环境

- JDK 8 或更高版本。
- Kotlin 2.4.0 或更高版本。
- Maven 3.9+，或 Kotlin 2.4.0 支持的 Gradle 版本。

> **Warning**
> 请确保 IDE 使用的 Kotlin 插件支持 Kotlin 2.4.0 或更高版本。

下一步按 {{ $.keyword("getting-started/first-query", ["第一次查询"]) }} 完成最小模型、建表、插入和查询流程。
