{% import "../../../macros/macros-en.njk" as $ %}

## 成员函数：

### 1. {{ $.title("db2k(name)")}} 数据库->Kotlin

将数据库表/列名转为kotlin类名/属性名。

- **函数声明**

 ```kotlin
    fun db2k(name: String): String
 ```

- **使用示例**

 ```kotlin
    val kName = db2k("user_info")
 ```

- **接收参数**

{{ $.params([['name', '数据库表/列名', 'String']]) }}

- **返回值**

`String` - kotlin类名/属性名

{{ $.hr() }}

### 2. {{ $.title("k2db(name)")}} Kotlin->数据库

将kotlin类名/属性名转为数据库表/列名。

- **函数声明**

```kotlin
    fun k2db(name: String): String
```

- **使用示例**

```kotlin
    val dbName = k2db("UserInfo")
```

- **接收参数**

{{ $.params([['name', 'kotlin类名/属性名', 'String']]) }}

- **返回值**

`String` - 数据库表/列名