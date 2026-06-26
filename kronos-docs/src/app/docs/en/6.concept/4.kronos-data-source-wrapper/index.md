{% import "../../../macros/macros-en.njk" as $ %}

`KronosDataSourceWrapper` is an interface that encapsulates database operations. It does not concern itself with the specific details of database connections and is platform-independent; it only focuses on the logic of database operations.

## **Properties**
 
{{ $.members([
        ['dbType', 'Database type', 'DbType'],
        ['url', 'Database connection address', 'String'],
        ['username', 'Database username', 'String']
    ]) }}

## **Methods**

### 1. {{ $.title("forList(task, kClass)")}} Execute query

Execute a query and return the result as a list of maps.


- **Function declaration**

### 2. {{ $.title("forList(task, kClass)")}} Execute query(with type conversion)

Execute a query and return the result as a list of objects.

- **Function declaration**
    
    ```kotlin
    fun forList(task: KAtomicQueryTask, kClass: KClass<*>): List<Any>
    ```
  
- **Usage example**
    
    ```kotlin
    val list = wrapper.forList(KAtomicQueryTask(
        "SELECT * FROM user WHERE id = :id",
        mapOf("id" to 1)
    ), User::class)
    ```
  
- **Parameters**
    
    {{ $.params([
        ['task', 'Query task', 'KAtomicQueryTask'],
        ['kClass', 'Entity object type', 'KClass<*>']
    ]) }}

- **Return value**

`List<Any>` Query result

{{ $.hr() }}

### 3. {{ $.title("forMap(task)")}} Execute query

Execute a query and return the result as a map.

- **Function declaration**
    
    ```kotlin
    fun forMap(task: KAtomicQueryTask): Map<String, Any>?
    ```
  
- **Usage example**
    
    ```kotlin
    val map = wrapper.forMap(KAtomicQueryTask(
        "SELECT * FROM user WHERE id = :id",
        mapOf("id" to 1)
    ))
    ```
  
- **Parameters**

    {{ $.params([
        ['task', 'Query task', 'KAtomicQueryTask']
    ]) }}

- **Return value**

`Map<String, Any>?` Query result

{{ $.hr() }}

### 4. {{ $.title("forObject(task, kClass)")}} Execute query(with type conversion)

Execute a query and return the result as an object.

- **Function declaration**
    
    ```kotlin
    fun forObject(task: KAtomicQueryTask, kClass: KClass<*>): Any?
    ```
  
- **Usage example**
    
    ```kotlin
    val user = wrapper.forObject(KAtomicQueryTask(
        "SELECT * FROM user WHERE id = :id",
        mapOf("id" to 1)
    ), User::class)
    ```
  
- **Parameters**

    {{ $.params([
        ['task', 'Query task', 'KAtomicQueryTask'],
        ['kClass', 'Entity object type', 'KClass<*>']
    ]) }}

- **Return value**

`Any?` Query result

{{ $.hr() }}

### 5. {{ $.title("update(task)")}} Execute update

Execute an update operation.

- **Function declaration**
    
    ```kotlin
    fun update(task: KAtomicActionTask): Int
    ```
  
- **Usage example**
    
    ```kotlin
    val affectedRows = wrapper.update(KAtomicActionTask(
        "UPDATE user SET name = :name WHERE id = :id",
        mapOf("name" to "Alice", "id" to 1)
    ))
    ```
  
- **Parameters**

    {{ $.params([
        ['task', 'Update task', 'KAtomicActionTask']
    ]) }}

- **Return value**

`Int` Number of affected rows

{{ $.hr() }}

### 6. {{ $.title("batchUpdate(task)")}} Batch update

Execute a batch update operation.

- **Function declaration**
    
    ```kotlin
    fun batchUpdate(task: KAtomicBatchTask): Int
    ```
  
- **Usage example**
    
    ```kotlin
    val affectedRows = wrapper.batchUpdate(KAtomicBatchTask(
        "UPDATE user SET name = :name WHERE id = :id",
        listOf(
            mapOf("name" to "Alice", "id" to 1),
            mapOf("name" to "Bob", "id" to 2)
        )
    ))
    ```
  
- **Parameters**
    
    {{ $.params([
        ['task', 'Batch update task', 'KAtomicBatchTask']
    ]) }}


- **Return value**

    `Int` Number of affected rows

{{ $.hr() }}

### 7. {{ $.title("transact(task)")}} Transaction

Execute a transaction.

- **Function declaration**

    ```kotlin
    fun transact(task: (DataSource) -> Any?): Any?
    ```
  
- **Usage example**
    
    ```kotlin
    val result = wrapper.transact { dataSource ->
        wrapper.update(KAtomicActionTask(
            "UPDATE user SET name = :name WHERE id = :id",
            mapOf("name" to "Alice", "id" to 1)
        ))
    }
    ```

- **Parameters**

    {{ $.params([
        ['task', 'Transaction task', '(DataSource) -> Any?']
    ]) }}

- **Return value**

    `Any?` Transaction result