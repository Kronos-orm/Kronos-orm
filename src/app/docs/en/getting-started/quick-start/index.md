# {{ NgDocPage.title }}

{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Adding Dependencies:

Kronos-orm is a multi-module project where we provide multiple modules for developers to choose from. Developers can select the modules according to their requirements.

The modules are as follows:

1. `kronos-core` is a **mandatory module** that provides basic ORM functionalities.
2. `kronos-logging` is an optional module that offers logging functionalities across multiple platforms.
3. `kronos-jvm-driver-wrapper` is an optional module that provides a JVM driver wrapper. (You can easily use other official driver wrappers or write your own wrapper classes to work with third-party frameworks like SpringData, Mybatis, Hibernate, Jdbi, etc.)
4. The `kronos-compiler-plugin` plugin is a **mandatory module** that provides compile-time support for Kronos ORM functionalities.

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

```xml group="import" name="maven(NOT SUPPORT NOW)" icon="maven"
<project>
  <dependencies>
    <dependency>
      <groupId>com.kotlinorm</groupId>
      <artifactId>kronos-core</artifactId>
      <version>1.0.0</version>
    </dependency>
    <dependency>
      <groupId>com.kotlinorm</groupId>
      <artifactId>kronos-logging</artifactId>
      <version>1.0.0</version>
    </dependency>
    <dependency>
      <groupId>com.kotlinorm</groupId>
      <artifactId>kronos-jvm-driver-wrapper</artifactId>
      <version>1.0.0</version>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>com.kotlinorm</groupId>
        <artifactId>kronos-compiler-plugin</artifactId>
        <version>1.0.0</version>
      </plugin>
    </plugins>
  </build>
</project>
```

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
