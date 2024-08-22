# {{ NgDocPage.title }}

{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Add dependencies:

You can use Kronos in your project by simply importing the `kronos-core` module and the `kronos-compiler-plugin` plugin.

```kotlin group="import" name="gradle(kts)" icon="gradlekts"
    dependencies {
        implementation("com.kotlinorm.kronos-core:2.0.0") // for the basic ORM feature
    }
    
    plugins {
        id("com.kotlinorm.kronos-gradle-plugin") version "2.0.0" // for the compile-time support
    }
```

```groovy group="import" name="gradle(groovy)" icon="gradle"
    dependencies {
        implementation 'com.kotlinorm:kronos-core:2.0.0' // for the basic ORM feature
    }
    
    plugins {
        id 'com.kotlinorm.kronos-gradle-plugin' version '2.0.0' // for the compile-time support
    }
```

```xml group="import" name="maven" icon="maven"
<!--Add the plugin in your pom.xml file:-->
<!--Please refer to the [https://kotlinlang.org/docs/all-open-plugin.html#maven] for the detailed information.-->
<project>
  <dependencies>
    <!--kronos-core provides the basic ORM feature-->
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
                    <!--kronos-maven-plugin provides the compile-time support-->
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

At the same time, we provide a variety of optional dependencies such as logging (`kronos-logging`), database operation driver wrapper (`kronos-jvm-driver-wrapper`).

`kronos-jvm-driver-wrapper` is an optional module that provides a driver wrapper based on JDBC for the jvm platform. Of course, you can use other official driver wrappers or write your own wrapper class to easily use with third-party frameworks (such as SpringData, Mybatis, Hibernate, Jdbi, etc.) (refer to [this article](/documentation/en/plugin/datasource-wrapper-and-third-part-framework)).

You can find some examples of how to start a project [here](https://github.com/Kronos-orm?tab=repositories).

## Configuring the Database:

Here, we will only discuss the usage of the `kronos-jvm-driver-wrapper` module with MySQL. The usage for other modules is similar. For specifics, please refer to [Connecting to the Database](/documentation/en/database/connect-to-db).

Dependencies such as `commons-dbcp2` and `mysql-connector-java` need to be included.

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
When using other databases or non-JVM platforms, the corresponding driver and configuration need to be used.

## Writing Entity Classes:

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

## Using Kronos:

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
```
