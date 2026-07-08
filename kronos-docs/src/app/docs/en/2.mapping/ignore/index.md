{% import "../../../macros/macros-en.njk" as $ %}

## {{ $.annotation("Ignore") }} and {{ $.title("IgnoreAction") }}

Use `@Ignore` on a `KPojo` property when the property is local state, a computed value, a one-way mapping field, or a cascade relationship that should be skipped by default.

`@Ignore` is equivalent to `@Ignore([IgnoreAction.ALL])`. Pass `targets` when the property should be ignored only in specific operations.

{{ $.members([
    ["ALL", "Treat the property as a non-column property and skip map conversion.", "IgnoreAction"],
    ["SELECT", "Skip the property in the default `select()` field list.", "IgnoreAction"],
    ["CASCADE_SELECT", "Skip the relationship property during automatic cascade select.", "IgnoreAction"],
    ["TO_MAP", "Skip the property in `KPojo.toDataMap()`.", "IgnoreAction"],
    ["FROM_MAP", "Skip the property in `KPojo.fromMapData()` and `KPojo.safeFromMapData()`.", "IgnoreAction"]
])}}

## Ignore a database column completely

Use `@Ignore` for a property that belongs to the Kotlin object and should stay out of Kronos column metadata and map conversion.

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
val columnNames = user.kronosColumns().map { it.name }
val data = user.toDataMap()
val mapped = User().fromMapData<User>(
    mapOf("id" to 2, "name" to "Grace", "displayName" to "Grace Hopper")
)
```

The observable result is:

```kotlin group="IgnoreAction 1 2" name="all result" icon="kotlin"
columnNames == listOf("id", "name")
data == mutableMapOf("id" to 1, "name" to "Ada")
mapped.displayName == null
```

## Skip the default select field list

Use `IgnoreAction.SELECT` when a property value can still be used by the object, while the default database select stays focused on table columns.

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
val users = User(searchText = "Ada").select().where().queryList()
```

`toDataMap()` still contains the local property value:

```kotlin group="IgnoreAction 2 2" name="select result" icon="kotlin"
data["searchText"] == "Ada"
```

The SQL shape of `User(searchText = "Ada").select().where()` skips `searchText` in the selected columns and query-by-example condition:

```sql group="IgnoreAction 2 3" name="select sql" icon="mysql"
SELECT `id`, `name`
FROM `tb_user`
```

## Skip automatic cascade select

Use `IgnoreAction.CASCADE_SELECT` on a relationship property that should stay out of automatic cascade loading.

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

val companies = Company().select().queryList()
```

The root query still reads the company table:

```sql group="IgnoreAction 3 2" name="cascade root sql" icon="mysql"
SELECT `id`, `name`
FROM `tb_company`
```

The cascade relationship is skipped during automatic cascade select, so loaded rows keep the default relationship value:

```kotlin group="IgnoreAction 3 3" name="cascade result" icon="kotlin"
companies.first().employees == null
```

> **Note**
> Explicit relationship selection has higher priority. A query can still request the relationship with `cascade { [it.employees] }`.

## Skip {{ $.title("toDataMap") }}

Use `IgnoreAction.TO_MAP` for values that should stay on the object and stay out of the map passed into SQL parameter preparation or custom mapping code.

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

The observable result is:

```kotlin group="IgnoreAction 4 2" name="to map result" icon="kotlin"
data == mutableMapOf("id" to 1, "name" to "Ada")
```

## Skip {{ $.title("fromMapData") }} and {{ $.title("safeFromMapData") }}

Use `IgnoreAction.FROM_MAP` for local values that should keep their current value when a map is applied to the object.

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

The observable result is:

```kotlin group="IgnoreAction 5 2" name="from map result" icon="kotlin"
user.id == 1
user.name == "Ada"
user.localState == "kept"

safeUser.id == 2
safeUser.name == "Grace"
safeUser.localState == "kept"
```

See {{ $.keyword("mapping/annotations", ["@Ignore annotation"]) }} for the annotation parameter list, and {{ $.keyword("advanced/cascade", ["Cascade Relationship Definition"]) }} for cascade relationship mapping.

Relationship-field mapping examples are collected in {{ $.keyword("mapping/cascade-mapping", ["Cascade Mapping"]) }}.
