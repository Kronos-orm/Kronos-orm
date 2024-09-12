{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 悲观锁

Kronos为**查询**`(select)`与**更新插入**`(upsert)`功能提供了**悲观锁**(`lock`)的功能，其锁等级为**行锁**

`lock()`方法可**不接受参数**或**接收一个参数**`lock`，其类型为枚举类`PessimisticLock`

目前可选择**独占锁**`PessimisticLock.X`或**共享锁**`PessimisticLock.S`两种类型的锁

以下仅为**查询**`(select)`时使用悲观锁的用法示例，在**更新插入**`(upsert)`时使用时仅需将`.select()`替换为`.upsert()`
```kotlin group="Case 1" name="kotlin" icon="kotlin" 
val listOfUser: List<User> = User().select()
                          .lock()
//                          .lock(PessimisticLock.X)
                          .queryList()
                          
val listOfUser: List<User> = User().select()
                          .lock(PessimisticLock.S)
                          .queryList()
```

```sql group="Case 1" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age` FROM `user` FOR UPDATE

SELECT `id`, `name`, `age` FROM `user` LOCK IN SHARE MODE
```

```sql group="Case 1" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age" FROM "user" FOR UPDATE

SELECT "id", "name", "age" FROM "user" FOR SHARE
```

```sql group="Case 1" name="SQLite" icon="sqlite"
# 不支持对Sqlite添加行锁功能因为Sqlite本身没有行锁功能
```

```sql group="Case 1" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age] FROM [user] OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY ROWLOCK
```

```sql group="Case 1" name="Oracle" icon="oracle"
SELECT "id", "name", "age" FROM "user" FOR UPDATE(NOWAIT)

SELECT "id", "name", "age" FROM "user" LOCK IN SHARE MODE
```

## 乐观锁

Kronos提供**乐观锁**功能（具体的开启与使用方法见：<a href="/documentation/zh-CN/class-definition/table-class-definition#乐观锁策略">[乐观锁策略]</a>）

被设置为**乐观锁**的列（默认为`version`，接下来均以该列为例）在记录新建时会被设置成0，后续每次更新`version = version + 1`

在执行**更新插入**`(upsert)`操作时，会将`version`字段添加进筛选项，意为仅当Kpojo的该字段与数据库中修改次数一致时才会更新该条数据，否则则执行插入


