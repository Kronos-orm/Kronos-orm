{% import "../../../macros/macros-en.njk" as $ %}

## Member Functions:

### 1. {{ $.title("db2k(name)")}} Database Name -> Kotlin Name

Convert database table/column names to kotlin class/property names.

- **Function Declaration**

   ```kotlin
      fun db2k(name: String): String
   ```

- **Usage Example**
  
   ```kotlin
      val kName = db2k("user_info")
   ```

- **Reception Parameters**

{{ $.params([['name', 'Database table/column names', 'String']]) }}

- **Return**

  `String` - kotlin class name/property name

{{ $.hr() }}

### 2. {{ $.title("k2db(name)")}} Kotlin Name -> Database Name

Convert Kotlin class names/property names to database table/column names.

- **Function Declaration**

  ```kotlin
      fun k2db(name: String): String
  ```

- **Usage Example**

  ```kotlin
      val dbName = k2db("UserInfo")
  ```

- **Reception Parameters**

{{ $.params([['name', 'Kotlin class name/property name', 'String']]) }}

- **Return**

  `String` - Database table/column names