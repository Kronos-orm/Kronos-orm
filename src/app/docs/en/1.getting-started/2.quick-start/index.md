{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 🖥 JDK, Kotlin versions and build tools

- **JDK** 8+
- **Kotlin** 2.0.0+
- **Maven** 3.6.3+ 或 **Gradle** 6.8.3+

> **Warning**
> **Please make sure that the kotlin plugin you use with your IDE supports kotlin 2.0.0 or later**.
>
> If you fail to build with Maven in Intellij IDEA (or Android Studio), try enabling the following settings:
>
> `Settings` / `Build, Execution, Deployment` / `Build Tools` / `Maven` / `Runner` /
`Delegate IDE build/run actions to Maven`

## 📦 Adding Kronos Dependencies

Simply introduce the `kronos-core` module and the `kronos-compiler-plugin` plugin to use Kronos in your project.

```kotlin group="import" name="gradle(kts)" icon="gradlekts"
dependencies {
    implementation("com.kotlinorm.kronos-core:2.0.0") // Provides basic ORM functionality
}

plugins {
    id("com.kotlinorm.kronos-gradle-plugin") version "2.0.0" // Compile-time support is provided
}
```

```groovy group="import" name="gradle(groovy)" icon="gradle"
dependencies {
    implementation 'com.kotlinorm:kronos-core:2.0.0' // Provides basic ORM functionality
}

plugins {
    id 'com.kotlinorm.kronos-gradle-plugin' version '2.0.0' // Compile-time support is provided
}
```

```xml group="import" name="maven" icon="maven"
<!--Add the plugin to your pom.xml file :-->
<!--有关详细信息，请参考[https://kotlinlang.org/docs/all-open-plugin.html#maven]。-->
<project>
    <!--kronos-core Provides basic ORM functionality-->
    <dependencies>
        <dependency>
            <groupId>com.kotlinorm</groupId>
            <artifactId>kronos-core</artifactId>
            <version>2.0.0</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.jetbrains.kotlin</groupId>
                <artifactId>kotlin-maven-plugin</artifactId>
                <extensions>true</extensions>
                <configuration>
                    <compilerPlugins>
                        <! --kronos-maven-plugin provides compile-time support -->
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

### jdbc data source wrapper (kronos-jdbc-wrapper)
> **Note**
> kronos-jdbc-wrapper is an optional module, this article uses it as an example to create a database connection, it provides a JDBC-based data source wrapper for the jvm platform, of course, you can use other wrapper plug-ins or write your own wrapper classes with third-party frameworks (such as SpringData, Mybatis, Hibernate, Jdbi , etc.) to use

Introducing dependency:

```kotlin group="importDriver" name="gradle(kts)" icon="gradlekts"
dependencies {
    implementation("com.kotlinorm.kronos-jdbc-wrappere:2.0.0") // Provides basic functions for database operations
}
```

```groovy group="importDriver" name="gradle(groovy)" icon="gradle"
dependencies {
    implementation 'com.kotlinorm:kronos-jdbc-wrapper:2.0.0' // Provides basic functions for database operations
}
```

```xml group="importDriver" name="maven" icon="maven"
<! -- Add the plugin to your pom.xml file: -->
<project>
    <! --kronos-jdbc-wrapper provides jdbc data source wrapper -->
    <dependencies>
        <dependency>
            <groupId>com.kotlinorm</groupId>
            <artifactId>kronos-jdbc-wrapper</artifactId>
            <version>2.0.0</version>
        </dependency>
    </dependencies>
</project>
```

See {{ $.keyword("plugin/datasource-wrapper-and-third-part-framework", ["datasource-and-third-part-framework-extensions"]) }} for details on usage and custom wrappers }}.

## 🔗 Configuration Database

Kronos supports a variety of databases, this article takes `Mysql database` with `commons-dbcp2`.
connection pool as an example, for more information please refer to {{ $.keyword("database/connect-to-db", ["connect to database"]) }}.

### Introduce relevant dependencies

```kotlin group="importRelatedPackages" name="gradle(kts)" icon="gradlekts"
dependencies {
    implementation("org.apache.commons:commons-dbcp2:2.8.0")
    implementation("mysql:mysql-connector-java:8.0.26")
}
```

```groovy group="importRelatedPackages" name="gradle(groovy)" icon="gradle"
dependencies {
    implementation 'org.apache.commons:commons-dbcp2:2.8.0'
    implementation 'mysql:mysql-connector-java:8.0.26'
}
```

```xml group="importRelatedPackages" name="maven" icon="maven"

<dependencies>
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-dbcp2</artifactId>
        <version>2.8.0</version>
    </dependency>
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.26</version>
    </dependency>
</dependencies>
```

### Configuring Database Connections

```kotlin group="DataSourceConfig" name="Main.kt"
import com.kotlinorm.Kronos

fun main() {
    val dataSource = BasicDataSource().apply {
        driverClassName = "com.mysql.cj.jdbc.Driver"
        url =
            "jdbc:mysql://localhost:3306/kronos?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=UTC"
        username = "user"
        password = "******"
    }
    Kronos.dataSource = { dataSource }
}
```

## ⚙️ global setting

Kronos supports global settings such as table name policy, field name policy, creation time, update time, logical deletion, etc. Below is only a partial list, please refer to {{ $.keyword("getting-started/global-config", ["Global Settings"]) }} for details.

```kotlin group="KronosConfig" name="Main.kt"
import com.kotlinorm.Kronos
import java.time.ZoneId

fun main() {
    Kronos.apply {
        // Table Name Strategy
        tableNamingStrategy = LineHumpStrategy
        // Field Name Strategy
        fieldNamingStrategy = LineHumpStrategy
        // time zones
        timeZone = ZoneId.systemDefault()
        // Default date format
        dateFormat = "yyyy-MM-dd HH:mm:ss"
        // Create a time strategy
        createTimeStrategy = KronosCommonStrategy(true, Field("createTime"))
        // Update Time Strategy
        updateTimeStrategy = KronosCommonStrategy(true, Field("updateTime"))
        // Logical Deletion Strategy
        logicDeleteStrategy = KronosCommonStrategy(true, Field("deleted"))
        // Optimistic Lock Strategy
        optimisticLockStrategy = KronosCommonStrategy(true, Field("version"))
    }
}
```

When using other databases or using non-jvm platforms, you need to use the corresponding driver and configuration.

## 🎨 Writing entity classes

With Kronos, you can write entity classes in Kotlin, and Kronos will automatically generate database table structures based on the entity classes.

```kotlin group="KPojo" name="Director.kt"
data class Director(
    @PrimaryKey(identity = true)
    var id: Int? = 0,
    var name: String? = "",
    var movies: List<Movie>? = emptyList(),
    @CreateTime
    var createTime: LocalDateTime? = "",
    @updateTime
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    var updateTime: String? = "",
    @LogicDelete
    var deleted: Boolean? = false
) : KPojo
```

```kotlin group="KPojo" name="Movie.kt"
@Table(name = "tb_movie")
@TableIndex("idx_name_director", ["name", "director_id"], Mysql.KIndexType.UNIQUE, Mysql.KIndexMethod.BTREE)
data class Movie(
    @PrimaryKey(identity = true)
    var id: Int? = 0,
    @Column("name")
    @ColumnType(CHAR)
    var name: String? = "",
    var directorId: Long? = 0,
    @Reference(["directorId"], ["id"])
    var director: Director? = null,
    @LogicDelete
    @Default("0")
    var deleted: Boolean? = false,
    @CreateTime
    var createTime: LocalDateTime? = "",
    @updateTime
    var updateTime: Date? = ""
) : KPojo
```

## 🚀 Start using Kronos

Congratulations, you have completed the basic configuration of Kronos and are now ready to start using Kronos.

```kotlin group="Kronos" name="Main.kt"
fun main() {
    val director = Director(
        id = 1,
        name = "Kronos",
        age = 18
    )

    director.insert().execute()

    director.update().set { it.name = "Kronos ORM" }.by { it.id }.execute()

    val directors: List<Director> = director.select().where { it.id == 1 }.queryList()

    val movies: List<Movie> = Movie().select().where { it.director!!.id == director.id.value }.queryList()
}
```