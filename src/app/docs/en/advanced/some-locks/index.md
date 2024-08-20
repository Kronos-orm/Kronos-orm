# {{ NgDocPage.title }}

## Pessimistic lock

Kronos provides a pessimistic lock (`lock`) function for **query**`(select)` and **update insert**`(upsert)` functions, and its lock level is **row lock**

The `lock()` method can **take no parameters** or **take a parameter**`lock`, whose type is the enumeration class `PessimisticLock`

Currently, you can choose **exclusive lock**`PessimisticLock.X` or **shared lock**`PessimisticLock.S`

The following is only an example of using pessimistic lock when **query**`(select)`. When using it when **update insert**`(upsert)`, just replace `.select()` with `.upsert()`
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
# It does not support adding row lock function to Sqlite because Sqlite itself does not have row lock function
```

```sql group="Case 1" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age] FROM [user] OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY ROWLOCK
```

```sql group="Case 1" name="Oracle" icon="oracle"
SELECT "id", "name", "age" FROM "user" FOR UPDATE(NOWAIT)

SELECT "id", "name", "age" FROM "user" LOCK IN SHARE MODE
```

## Optimistic lock

Kronos provides the **optimistic lock** function (for specific activation and usage methods, see: （<a href="/documentation/en/class-definition/table-class-definition#optimistic-lock-strategy">Optimistic Lock Strategy</a>)

The column set to **optimistic lock** (default is `version`, which is used as an example below) will be set to 0 when the record is created, and `version = version + 1` for each subsequent update

When performing the **update insert**` (upsert)` operation, the `version` field will be added to the filter item, which means that the data will be updated only when the field in Kpojo is consistent with the number of modifications in the database, otherwise the insert will be executed