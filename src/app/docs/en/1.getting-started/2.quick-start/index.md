{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 🖥 JDK、Kotlin 版本及构建工具

- **JDK** 8+
- **Kotlin** 2.0.0+
- **Maven** 3.6.3+ 或 **Gradle** 6.8.3+

> **Warning**
> **请确保您使用IDE的的kotlin插件支持kotlin 2.0.0或更高版本**
>
> 如果您在Intellij IDEA（或Android Studio）中使用Maven构建失败，请尝试启用以下设置：
>
> `Settings` / `Build, Execution, Deployment` / `Build Tools` / `Maven` / `Runner` /
`Delegate IDE build/run actions to Maven`

## 📦 添加Kronos依赖

仅需引入`kronos-core`模块和`kronos-compiler-plugin`插件即可在您的项目中使用Kronos。

```kotlin group="import" name="gradle(kts)" icon="gradlekts"
dependencies {
    implementation("com.kotlinorm.kronos-core:0.0.1") // 供了基础的ORM功能
}

plugins {
    id("com.kotlinorm.kronos-gradle-plugin") version "0.0.1" // 提供了编译时支持
}
```

```groovy group="import" name="gradle(groovy)" icon="gradle"
dependencies {
    implementation 'com.kotlinorm:kronos-core:0.0.1' // 供了基础的ORM功能
}

plugins {
    id 'com.kotlinorm.kronos-gradle-plugin' version '0.0.1' // 提供了编译时支持
}
```

```xml group="import" name="maven" icon="maven"
<!--将插件添加到您的pom.xml文件中：-->
<!--有关详细信息，请参考[https://kotlinlang.org/docs/all-open-plugin.html#maven]。-->
<project>
    <!--kronos-core提供了基础的ORM功能-->
    <dependencies>
        <dependency>
            <groupId>com.kotlinorm</groupId>
            <artifactId>kronos-core</artifactId>
            <version>0.0.1</version>
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
                        <!--kronos-maven-plugin提供了编译时支持-->
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

### jdbc数据源包装器(kronos-jdbc-wrapper)

> **Note**
> kronos-jdbc-wrapper是可选模块，本文以它为实例进行创建数据库连接，它提供了jvm平台基于JDBC的数据源包装器，当然，您可以使用其他包装器插件或自己编写包装类，并搭配第三方框架（如SpringData、Mybatis、Hibernate、Jdbi等）使用

引入依赖：

```kotlin group="importDriver" name="gradle(kts)" icon="gradlekts"
dependencies {
    implementation("com.kotlinorm.kronos-jdbc-wrappere:0.0.1") // 提供了数据库操作的基础功能
}
```

```groovy group="importDriver" name="gradle(groovy)" icon="gradle"
dependencies {
    implementation 'com.kotlinorm:kronos-jdbc-wrapper:0.0.1' // 提供了数据库操作的基础功能
}
```

```xml group="importDriver" name="maven" icon="maven"
<!--将插件添加到您的pom.xml文件中：-->
<project>
    <!--kronos-jdbc-wrapper提供了jdbc数据源包装器-->
    <dependencies>
        <dependency>
            <groupId>com.kotlinorm</groupId>
            <artifactId>kronos-jdbc-wrapper</artifactId>
            <version>0.0.1</version>
        </dependency>
    </dependencies>
</project>
```

详细使用方法和自定义包装器请参考{{ $.keyword("plugin/datasource-wrapper-and-third-part-framework", ["数据源及三方框架扩展"]) }}。

## 🔗 配置数据库

Kronos支持多种数据库，本文以`Mysql数据库`搭配`commons-dbcp2`
连接池为例，更多信息请参考{{ $.keyword("database/connect-to-db", ["连接到数据库"]) }}。

### 引入相关依赖

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

### 配置数据库连接

```kotlin group="DataSourceConfig" name="Main.kt"
import com.kotlinorm.Kronos

fun main() {
    val ds by lazy {
        BasicDataSource().apply {
            driverClassName = "com.mysql.cj.jdbc.Driver"
            url =
                "jdbc:mysql://localhost:3306/kronos?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=UTC"
            username = "user"
            password = "******"
        }
    }
    
    Kronos.init{
        dataSource = { ds }
    }
}
```

## ⚙️ 全局设置

Kronos支持表名策略、字段名策略、创建时间、更新时间、逻辑删除等全局设置，以下仅列出部分，详细请参考{{ $.keyword("getting-started/global-config", ["全局设置"]) }}。

```kotlin group="KronosConfig" name="Main.kt"
import com.kotlinorm.Kronos
import java.time.ZoneId

fun main() {
    Kronos.init {
        // 表名策略
        tableNamingStrategy = LineHumpStrategy
        // 字段名策略
        fieldNamingStrategy = LineHumpStrategy
        // 时区
        timeZone = ZoneId.systemDefault()
        // 默认日期格式
        dateFormat = "yyyy-MM-dd HH:mm:ss"
        // 创建时间策略
        createTimeStrategy = KronosCommonStrategy(true, Field("createTime"))
        // 更新时间策略
        updateTimeStrategy = KronosCommonStrategy(true, Field("updateTime"))
        // 逻辑删除策略
        logicDeleteStrategy = KronosCommonStrategy(true, Field("deleted"))
        // 乐观锁策略
        optimisticLockStrategy = KronosCommonStrategy(true, Field("version"))
    }
}
```

当使用其他数据库或使用非jvm平台时，需要使用对应的驱动及配置。

## 🎨 编写实体类

通过Kronos，您可以使用Kotlin编写实体类，Kronos会自动根据实体类生成数据库表结构。

```kotlin group="KPojo" name="Director.kt"
data class Director(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var name: String? = "",
    var movies: List<Movie>? = emptyList(),
    @CreateTime
    var createTime: LocalDateTime? = null,
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
    var id: Int? = null,
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
    var createTime: LocalDateTime? = null,
    @updateTime
    var updateTime: Date? = null
) : KPojo
```

## 🚀 开始使用

恭喜你，您已经完成了Kronos的基本配置，现在可以开始使用Kronos了。

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
