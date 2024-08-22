# {{ NgDocPage.title }}

{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 添加依赖：

kronos-orm是一个多模块的项目，我们提供了多个模块供开发者选择，开发者可以根据自己的需求选择对应的模块。

其中：

1. `kronos-core`是**必选模块**，它提供了基础的ORM功能
2. `kronos-logging`是可选模块，它提供了多平台的日志功能
3. `kronos-jvm-driver-wrapper`是可选模块，它提供了JVM驱动包装器。（您可以使用其他官方驱动包装器或自己编写包装类轻松地搭配第三方框架（如SpringData、Mybatis、Hibernate、Jdbi等）使用）
4. `kronos-compiler-plugin`插件是**必选模块**，它为Kronos的ORM功能提供了编译时支持

```kotlin group="import" name="gradle(kts)" icon="gradlekts"
    dependencies {
        implementation("com.kotlinorm.kronos-core:1.0.0")
        implementation("com.kotlinorm.kronos-logging:1.0.0")
        implementation("com.kotlinorm.kronos-jvm-driver-wrapper:1.0.0")
    }
    
    plugins {
        id("com.kotlinorm.kronos-compiler-plugin") version "1.0.0"
    }
```

```groovy group="import" name="gradle(groovy)" icon="gradle"
    dependencies {
        implementation 'com.kotlinorm:kronos-core:1.0.0'
        implementation 'com.kotlinorm:kronos-logging:1.0.0'
        implementation 'com.kotlinorm:kronos-jvm-driver-wrapper:1.0.0'
    }
    
    plugins {
        id 'com.kotlinorm.kronos-compiler-plugin' version '1.0.0'
    }
```


```xml group="import" name="maven" icon="maven"
<!--将插件添加到您的pom.xml文件中：-->
<!--有关详细信息，请参考[https://kotlinlang.org/docs/all-open-plugin.html#maven]。-->
<project>
    <dependencies>
        <dependency>
            <groupId>com.kotlinorm</groupId>
            <artifactId>kronos-core</artifactId>
            <version>1.0.0</version>
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

## 配置数据库：

这里仅介绍`kronos-jvm-driver-wrapper`模块Mysql下的使用，其他模块的使用方式类似，具体请参考[连接到数据库](/documentation/zh-CN/database/connect-to-db)。

需引入`commons-dbcp2`、`mysql-connector-java`等依赖。

```kotlin group="KronosConfig" name="KronosConfig.kt"
import com.kotlinorm.Kronos
Kronos.apply {
  dataSource = {
    BasicDataSource().apply {
        driverClassName = "com.mysql.cj.jdbc.Driver"
        url = "jdbc:mysql://localhost:3306/kronos?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=UTC"
        username = "root"
        password = "******"
    }
  }
```
当使用其他数据库或使用非jvm平台时，需要使用对应的驱动及配置。

## 编写实体类：

```kotlin group="KPojo" name="Director.kt"
data class Director(
    @PrimaryKey(identity = true)
    var id: Int? = 0,
    var name: String? = "",
    var age: Int? = 0,
    var movies: List<Movie>? = emptyList(),
    @CreateTime
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    var createTime: String? = "",
    @updateTime
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    var updateTime: String? = "",
    @LogicDelete
    var deleted: Boolean? = false
): KPojo
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
    @Reference(["director_id"], ["id"])
    var director: Director? = "",
    var releaseTime: String? = "",
    @LogicDelete
    @Default("0")
    var deleted: Boolean? = false,
    @CreateTime
    var createTime: LocalDateTime? = "",
    @updateTime
    var updateTime: Date? = ""
): KPojo
```

## 使用Kronos：

```kotlin group="Kronos" name="Kronos.kt"
val director: Director = Director(
    id = 1,
    name = "Kronos",
    age = 18
)

director.insert(director)

director.update().set { it.name = "Kronos ORM" }.where { it.id == 1 }.execute()

val directors: List<Director> = director.select().where { it.id == 1 }.queryList()

val movies: List<Movie> = Movie().select().where { it.director!!.id == director.id.value }.queryList()

