{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 启用 DataGuard

`DataGuardPlugin` 会在 Kronos 写入操作和表操作执行前进行检查。应用需要默认拒绝全表写入和危险表操作时，在启动阶段启用它。

```kotlin group="Enable 1" name="kotlin" icon="kotlin"
import com.kotlinorm.plugins.DataGuardPlugin

DataGuardPlugin.enable()
```

同一进程需要临时关闭 DataGuard 检查时，调用 `disable()`。

```kotlin group="Enable 2" name="disable" icon="kotlin"
DataGuardPlugin.disable()
```

> **Note**
> DataGuard 会读取 Kronos DSL 和表操作 API 中的表名与条件信息。直接创建并执行原生 SQL 时，请在生成和执行原生 SQL 的边界增加业务侧保护。

## 拦截全表 delete

使用默认策略时，不带 `by` 或 `where` 的 delete 会被拒绝。

```kotlin group="DeleteAll 1 1" name="kotlin" icon="kotlin"
DataGuardPlugin.enable()

User()
    .delete()
    .execute()
```

操作会在进入数据源前停止。

```text group="DeleteAll 1 2" name="result"
UnsupportedOperationException: Delete operation is not allowed.
```

明确要删除的记录时，加入条件。

```kotlin group="DeleteAll 1 3" name="with where" icon="kotlin"
User()
    .delete()
    .where { it.id == 1 }
    .execute()
```

## 拦截全表 update

使用默认策略时，没有生成 `WHERE` 条件的 update 会被拒绝。

```kotlin group="UpdateAll 1" name="kotlin" icon="kotlin"
DataGuardPlugin.enable()

User()
    .update()
    .set { it.status = "LOCKED" }
    .execute()
```

操作会返回 update 保护异常。

```text group="UpdateAll 2" name="result"
UnsupportedOperationException: Update operation is not allowed.
```

需要对某张表执行计划内全表更新时，使用 `updateAll` 加入允许规则。

```kotlin group="UpdateAll 3" name="allow table" icon="kotlin"
DataGuardPlugin.enable {
    updateAll {
        allow {
            tableName = "user_archive"
        }
    }
}

UserArchive()
    .update()
    .set { it.status = "EXPIRED" }
    .execute()
```

## 允许维护 delete

维护任务需要清空某张固定表时，使用 `deleteAll` 加入允许规则。

```kotlin group="DeleteAll 2 1" name="allow table" icon="kotlin"
DataGuardPlugin.enable {
    deleteAll {
        allow {
            tableName = "tmp_import_error"
        }
    }
}

TmpImportError()
    .delete()
    .execute()
```

允许规则只作用于 `tmp_import_error`。其他全表 delete 仍会被拒绝。

```text group="DeleteAll 2 2" name="other table"
UnsupportedOperationException: Delete operation is not allowed.
```

## 允许临时表 DDL

`truncate`、`drop` 和 `alter` 规则用于保护表操作。`%` 通配符可以匹配 `databaseName` 或 `tableName` 中的任意字符序列。

```kotlin group="DDL 1" name="tmp tables" icon="kotlin"
import com.kotlinorm.Kronos

DataGuardPlugin.enable {
    truncate {
        allow {
            tableName = "tmp_%"
        }
    }
    drop {
        allow {
            tableName = "tmp_%"
        }
    }
    alter {
        allow {
            tableName = "tmp_%"
        }
    }
}

val wrapper = Kronos.dataSource()

wrapper.table.truncateTable("tmp_session")
wrapper.table.dropTable("tmp_session")
wrapper.table.syncTable(TmpSession())
```

临时表规则之外的表操作会被拒绝。

```text group="DDL 2" name="result"
UnsupportedOperationException: Drop operation is not allowed.
```

## 匹配指定数据库

规则只对当前数据源报告的某个数据库生效时，设置 `databaseName`。

```kotlin group="Database 1" name="database rule" icon="kotlin"
DataGuardPlugin.enable {
    deleteAll {
        allow {
            databaseName = "kronos"
            tableName = "tmp_%"
        }
    }
}
```

同名表来自其他数据库时，规则不会匹配。

```text group="Database 2" name="result"
UnsupportedOperationException: Delete operation is not allowed.
```

## 拒绝敏感表

使用 `allowAll()` 可以默认允许某类操作，再用 `deny { ... }` 拒绝敏感表。

```kotlin group="Deny 1" name="drop" icon="kotlin"
import com.kotlinorm.Kronos

DataGuardPlugin.enable {
    drop {
        allowAll()
        deny {
            tableName = "sensitive_%"
        }
    }
}

val wrapper = Kronos.dataSource()

wrapper.table.dropTable("archive_2026")
wrapper.table.dropTable("sensitive_user")
```

第一次 drop 会通过。第二次 drop 会被 `deny` 规则拒绝。

```text group="Deny 2" name="result"
UnsupportedOperationException: Drop operation is not allowed.
```

显式 delete 和 update 条件写法请参考 {{ $.keyword("mutation/delete", ["删除记录"]) }} 和 {{ $.keyword("mutation/update", ["更新记录"]) }}。
