{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

`DataGuard`的使用场景主要是对一些危险操作进行保护，防止误操作导致数据丢失或错误修改。它可以在执行数据库操作前进行检查，确保操作的安全性。

支持的场景包括：

- **Alter操作**：禁止对表结构进行修改，防止数据库结构被意外更改及造成数据丢失。
- **Drop操作**：禁止删除表，防止数据丢失。
- **Truncate操作**：禁止清空表，防止数据丢失。
- **DeleteAll操作**：阻止全表删除，防止误删除数据。
- **UpdateAll操作**：阻止全表更新，防止误修改数据。

`DataGuard`插件默认关闭，需手动开启。

## 使用方法

在Kronos.init中进行配置：

```kotlin
DataGuardPlugin.enable{
    ... // 可以在这里配置DataGuard插件的使用场景
} // 开启DataGuard插件

DataGuardPlugin.disable() // 关闭DataGuard插件
```

## 黑名单模式

在黑名单模式下，`DataGuard`插件会禁止执行指定的操作。你可以通过配置来指定哪些操作需要被禁止。

```kotlin
DataGuardPlugin.enable{
    // 禁止执行Alter操作
    alter {
        allowAll()
        deny {
            tableName = "sensitive_%" // 禁止对敏感表进行Alter操作
            // 可以使用通配符%来匹配多个表名
        }
        deny {
            // 禁止对特定数据库的Alter操作
            databaseName = "sensitive_db"
            tableName = "sensitive_table"
        }
    }
}
```

## 白名单模式

在白名单模式下，`DataGuard`插件只允许执行指定的操作。你可以通过配置来指定哪些操作是被允许的。

```kotlin
DataGuardPlugin.enable{
    // 只允许对部分表允许updateAll操作
    updateAll {
        denyAll() // 禁止所有updateAll操作
        allow {
            tableName = "allowed_table" // 只允许对allowed_table进行updateAll操作
        }
    }
    deleteAll {
        denyAll() // 禁止所有deleteAll操作
        allow {
            tableName = "allowed_table" // 只允许对allowed_table进行deleteAll操作
        }
    }
    truncate {
        denyAll() // 禁止所有truncate操作
        allow {
            tableName = "allowed_table" // 只允许对allowed_table进行truncate操作
        }
    }
    drop {
        denyAll() // 禁止所有drop操作
        allow {
            tableName = "allowed_table" // 只允许对allowed_table进行drop操作
        }
    }
}
```

实际上，`DataGuard`插件默认使用白名单模式，即若使用以下形式的写法，将会默认禁止所有的操作，不需要额外配置`denyAll`：

```kotlin
DataGuardPlugin.enable{
    drop {
        // 默认禁止所有drop操作
        allow { tableName = "tmp_%" } // 只允许对tmp_开头的表进行drop操作
    }
}
```

{{ $.hr() }}

`DataGuard`插件的配置可以根据实际需求进行调整，以确保数据库操作的安全性和可靠性。通过合理配置，可以有效防止误操作导致的数据丢失或错误修改。
