{% import "../../../macros/macros-zh-CN.njk" as $ %}

## {{ $.annotation("Ignore") }} 和 {{ $.title("IgnoreAction") }}

当 `KPojo` 属性用于本地状态、计算值、单向 map 转换或默认跳过的级联关系时，可以在属性上使用 `@Ignore`。

`@Ignore` 等价于 `@Ignore([IgnoreAction.ALL])`。需要只在部分场景忽略时，通过 `targets` 传入具体的 `IgnoreAction`。

{{ $.members([
["ALL", "将属性作为非数据库列处理，并跳过 map 转换。", "IgnoreAction"],
["SELECT", "在默认 `select()` 字段列表中跳过该属性。", "IgnoreAction"],
["CASCADE_SELECT", "在自动级联查询中跳过该关系属性。", "IgnoreAction"],
["TO_MAP", "在 `KPojo.toDataMap()` 中跳过该属性。", "IgnoreAction"],
["FROM_MAP", "在 `KPojo.fromMapData()` 和 `KPojo.safeFromMapData()` 中跳过该属性。", "IgnoreAction"]
])}}

## 完全忽略数据库列

属性只属于 Kotlin 对象，不参与 Kronos 列元数据和 map 转换时，使用 `@Ignore`。

```kotlin group="IgnoreAction 1 1" name="all" icon="kotlin"
import com.kotlinorm.annotations.Ignore
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo

@Table("tb_user")
data class User(
    var id: Int? = null,
    var name: String? = null,
    @Ignore
    var displayName: String? = null
) : KPojo

val user = User(id = 1, name = "Ada", displayName = "Ada Lovelace")
val columnNames = user.__columns.map { it.name }
val data = user.toDataMap()
val mapped = User().fromMapData<User>(
    mapOf("id" to 2, "name" to "Grace", "displayName" to "Grace Hopper")
)
```

可观察结果如下：

```kotlin group="IgnoreAction 1 2" name="all result" icon="kotlin"
columnNames == listOf("id", "name")
data == mutableMapOf("id" to 1, "name" to "Ada")
mapped.displayName == null
```

## 跳过默认 select 字段列表

属性值仍需要保留在对象中，默认数据库查询只读取表字段时，使用 `IgnoreAction.SELECT`。

```kotlin group="IgnoreAction 2 1" name="select" icon="kotlin"
import com.kotlinorm.annotations.Ignore
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.IgnoreAction
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select

@Table("tb_user")
data class User(
    var id: Int? = null,
    var name: String? = null,
    @Ignore([IgnoreAction.SELECT])
    var searchText: String? = null
) : KPojo

val data = User(id = 1, name = "Ada", searchText = "Ada").toDataMap()
val users = User(searchText = "Ada").select().where().toList()
```

`toDataMap()` 仍保留本地属性值：

```kotlin group="IgnoreAction 2 2" name="select result" icon="kotlin"
data["searchText"] == "Ada"
```

`User(searchText = "Ada").select().where()` 的 SQL 形态会在查询字段和 query-by-example 条件中跳过 `searchText`：

```sql group="IgnoreAction 2 3" name="select sql" icon="mysql"
SELECT `id`, `name`
FROM `tb_user`
```

## 跳过自动级联查询

关系属性需要默认跳过自动级联加载时，使用 `IgnoreAction.CASCADE_SELECT`。

```kotlin group="IgnoreAction 3 1" name="cascade select" icon="kotlin"
import com.kotlinorm.annotations.Cascade
import com.kotlinorm.annotations.Ignore
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.IgnoreAction
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select

@Table("tb_company")
data class Company(
    var id: Int? = null,
    var name: String? = null,
    @Cascade(["id"], ["companyId"])
    @Ignore([IgnoreAction.CASCADE_SELECT])
    var employees: List<Employee>? = null
) : KPojo

@Table("tb_employee")
data class Employee(
    var id: Int? = null,
    var companyId: Int? = null,
    var name: String? = null
) : KPojo

val companies = Company().select().toList()
```

主查询仍读取公司表：

```sql group="IgnoreAction 3 2" name="cascade root sql" icon="mysql"
SELECT `id`, `name`
FROM `tb_company`
```

自动级联查询会跳过该关系，加载后的对象保留关系属性默认值：

```kotlin group="IgnoreAction 3 3" name="cascade result" icon="kotlin"
companies.first().employees == null
```

> **Note**
> 显式关系选择优先级更高。查询仍可通过 `cascade { [it.employees] }` 指定加载该关系。

## 跳过 {{ $.title("toDataMap") }}

属性值需要保留在对象中，并从 SQL 参数准备或自定义 map 代码使用的 map 中排除时，使用 `IgnoreAction.TO_MAP`。

```kotlin group="IgnoreAction 4 1" name="to map" icon="kotlin"
import com.kotlinorm.annotations.Ignore
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.IgnoreAction
import com.kotlinorm.interfaces.KPojo

@Table("tb_user")
data class User(
    var id: Int? = null,
    var name: String? = null,
    @Ignore([IgnoreAction.TO_MAP])
    var temporaryToken: String? = null
) : KPojo

val data = User(id = 1, name = "Ada", temporaryToken = "token").toDataMap()
```

可观察结果如下：

```kotlin group="IgnoreAction 4 2" name="to map result" icon="kotlin"
data == mutableMapOf("id" to 1, "name" to "Ada")
```

## 跳过 {{ $.title("fromMapData") }} 和 {{ $.title("safeFromMapData") }}

map 应用到对象时，本地值需要保持当前值的属性使用 `IgnoreAction.FROM_MAP`。

```kotlin group="IgnoreAction 5 1" name="from map" icon="kotlin"
import com.kotlinorm.annotations.Ignore
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.IgnoreAction
import com.kotlinorm.interfaces.KPojo

@Table("tb_user")
data class User(
    var id: Int? = null,
    var name: String? = null,
    @Ignore([IgnoreAction.FROM_MAP])
    var localState: String? = "kept"
) : KPojo

val user = User().fromMapData<User>(
    mapOf("id" to 1, "name" to "Ada", "localState" to "changed")
)

val safeUser = User().safeFromMapData<User>(
    mapOf("id" to 2, "name" to "Grace", "localState" to "changed")
)
```

可观察结果如下：

```kotlin group="IgnoreAction 5 2" name="from map result" icon="kotlin"
user.id == 1
user.name == "Ada"
user.localState == "kept"

safeUser.id == 2
safeUser.name == "Grace"
safeUser.localState == "kept"
```

注解参数列表见 {{ $.keyword("mapping/annotations", ["@Ignore 注解"]) }}，级联关系配置见 {{ $.keyword("advanced/cascade", ["级联关系定义"]) }}。

关系字段映射示例见 {{ $.keyword("mapping/cascade-mapping", ["级联映射"]) }}。
