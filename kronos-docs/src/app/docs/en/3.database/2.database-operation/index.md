{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

Please make sure that your KPojo has the correct annotations so that Kronos recognizes table names, fields, indexes, etc. correctly.

> **Note**
> Functions can be invoked via `Kronos.dataSource(() -> KronosDataSourceWrapper)` or a specific data source object `KronosDataSourceWrapper`.

## 1. {{ $.title("exists(tableName)")}} whether the table exists

Check if the table exists by table name.

- **Function declaration**

    ```kotlin
    fun exists(tableName: String): Boolean
    ```

- **Usage Example**

    ```kotlin
    val exists = wrapper.table.exists("user")
    ```

- **Parameters**

  {{ $.params([['tableName', 'Table name', 'String']]) }}

- **Return value**

  `Boolean` - Whether the table exists

{{ $.hr() }}

## 2. {{ $.title("exists<T>(KPojo)")}} whether the table exists

- **Generic parameters**： `<T>` Entity object type, inherited from `KPojo`

{{ $.hr(50) }}

Check if the table exists by KPojo.

- **Function declaration**
    
    ```kotlin
    fun <T : KPojo> exists(kPojo: T = new T()): Boolean
    ```

<small>_{{ $.keyword("concept/kpojo-dynamic-instantiate", ["How does Kronos implement instantiating KClass&lt;KPojo&gt; without relying on reflection?"])}} _</small

- **Usage Example**

    ```kotlin
    val exists = wrapper.table.exists(User())
    // or
    val exists = wrapper.table.exists<User>()
    ```

- **Parameters**

  {{ $.params([['kPojo', 'Entity object', 'T', 'new T()']]) }}

- **Return value**

  `Boolean` - Whether the table exists

{{ $.hr() }}

## 3. {{ $.title("createTable(KPojo)")}} Create a table

- **Generic parameters**： `<T>` Entity object type, inherited from `KPojo`

{{ $.hr(50) }}

Create a table by KPojo.

- **Function declaration**
    
    ```kotlin
    fun createTable<T: KPojo>(kPojo: T = new T())
    ```

<small>_{{ $.keyword("concept/kpojo-dynamic-instantiate", ["How does Kronos implement instantiating KClass&lt;KPojo&gt; without relying on reflection?"])}}_</small>

- **Usage Example**

    ```kotlin
    wrapper.table.createTable(User())
    // or
    wrapper.table.createTable<User>()
    ```

- **Parameters**

  {{ $.params([['kPojo', 'Entity object', 'T', 'new T()']]) }}

{{ $.hr() }}

## 4. {{ $.title("truncateTable(tableName, restartIdentity)")}} Clear the table

Clear the table by table name.

- **Function declaration**

    ```kotlin
    fun truncateTable(tableName: String, restartIdentity: Boolean = true)
    ```

- **Usage Example**

    ```kotlin
    wrapper.table.truncateTable("user")
    ```

- **Parameters**

  {{ $.params([['tableName', 'Table name', 'String'], ['restartIdentity', 'Whether to reset the auto-increment value, applicable to PostgreSQL and sqlite', 'Boolean', 'true']]) }}

{{ $.hr() }}

## 5. {{ $.title("truncateTable(KPojo, restartIdentity)")}} Clear the table

- **Generic parameters**： `<T>` Entity object type, inherited from `KPojo`

{{ $.hr(50) }}

Clear the table by KPojo.

- **Function declaration**

    ```kotlin
    fun <T: KPojo> truncateTable(kPojo: T = new T(), restartIdentity: Boolean = true)
    ```

<small>_{{ $.keyword("concept/kpojo-dynamic-instantiate", ["Kronos is how to instantiate KClass&lt;KPojo&gt; without relying on reflection?"])}}_</small>

- **Usage Example**

    ```kotlin
    wrapper.table.truncateTable(User())
    // or
    wrapper.table.truncateTable<User>()
    ```

- **Parameters**

  {{ $.params([['kPojo', 'Entity object', 'T', 'new T()'], ['restartIdentity', 'Whether to reset the auto-increment value, applicable to PostgreSQL and sqlite', 'Boolean', 'true']]) }}

{{ $.hr() }}

## 6. {{ $.title("dropTable(tableName)")}} Delete the table

Delete the table by table name.

- **Function declaration**

    ```kotlin
    fun dropTable(tableName: String)
    ```

- **Usage Example**

    ```kotlin
    wrapper.table.dropTable("user")
    ```

- **Parameters**

  {{ $.params([['tableName', 'Table name', 'String']]) }}

{{ $.hr() }}

## 7. {{ $.title("dropTable(KPojo)")}} Delete the table

- **Generic parameters**： `<T>` Entity object type, inherited from `KPojo`

{{ $.hr(50) }}

- **Function declaration**

    ```kotlin
    fun <T: KPojo> dropTable(kPojo: T = new T())
    ```

<small>_{{ $.keyword("concept/kpojo-dynamic-instantiate", ["Kronos is how to instantiate KClass&lt;KPojo&gt; without relying on reflection?"])}}_</small>

- **Usage Example**

    ```kotlin
    wrapper.table.dropTable(User())
    // or
    wrapper.table.dropTable<User>()
    ```

- **Parameters**
  
  {{ $.params([['kPojo', 'Entity object', 'T', 'new T()']]) }}

{{ $.hr() }}

## 8. {{ $.title("syncTable(KPojo)")}} Synchronize table structure

- **Generic parameters**： `<T>` Entity object type, inherited from `KPojo`

{{ $.hr(50) }}

- **Function declaration**
    
    ```kotlin
    fun syncTable<T: KPojo>(kPojo: T = new T())
    ```

<small>_{{ $.keyword("concept/kpojo-dynamic-instantiate", ["Kronos is how to instantiate KClass&lt;KPojo&gt; without relying on reflection?"])}}_</small>

- **Usage Example**

    ```kotlin
    wrapper.table.syncTable(User())
    // or
    wrapper.table.syncTable<User>()
    ```

- **Parameters**

  {{ $.params([['kPojo', 'Entity object', 'T', 'new T()']]) }}

{{ $.hr() }}

## 9. {{ $.title("getTableCreateSqlList")}} Dynamic table creation

In some cases, there is a need for dynamic table creation, in which case you can dynamically get the table creation statement and execute it through the `getTableCreateSqlList` method.

- **Function declaration**

    ```kotlin
    fun getTableCreateSqlList(
        dbType: DBType,
        tableName: String,
        fields: List<Field>,
        indexes: List<KTableIndex> = emptyList()
    ): List<String>
    ```

- **Usage Example**
    
    ```kotlin
    
    val listOfSql =
        getTableCreateSqlList(
            dbType = DBType.Mysql,
            tableName = "user",
            fields = listOf(
                Field(
                    name = "id",
                    type = KColumnType.fromString("INT"),
                    primaryKey = true,
                    identity = true
                ),
                Field(
                    name = "name",
                    type = KColumnType.fromString("VARCHAR"),
                    length = 255
                ),
                Field(
                    name = "age",
                    type = KColumnType.fromString("INT"),
                )
            ),
            indexes = listOf(
                KTableIndex(
                    name = "idx_name",
                    columns = listOf("name"),
                    type = "UNIQUE"
                )
            )
        )
        
    listOfSql.forEach { db.execute(it) }
    ```

- **Parameters**

  {{$.params([
      ['dbType', 'Database type', 'DBType'],
      ['tableName', 'Table name', 'String'],
      ['fields', 'Field list', 'List<Field>'],
      ['indexes', 'Index list', 'List<KTableIndex>', '[]']
  ])}}

{{ $.hr() }}

> **Warning**
> If you need to perform multiple database operations on the same entity object consecutively, it is recommended that you do not use the `createTable<KPojo>()` writeup, but instead use `createTable(kPojo)` to avoid creating the KPojo object multiple times, which would incur unnecessary overhead.
