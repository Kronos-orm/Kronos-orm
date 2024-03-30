## Koto 是一个数据持久层框架，设计用于 Kotlin，轻量级，现代化。

Koto 2.0旨在通过编写kotlin k2编译器插件，实现更加强大且语义化的orm功能。

-------
>查询示例：

```kotlin
// 1. 条件查询 / 查询单个字段 / 使用query()查询List<Map>结果
val users: List<Map<String, Any>> = User(1).select{ where() }.query()

// 2.多条件查询 / 查询多个字段 / 带分页 / 带去重 / 带排序
val (users, total): Pair<List<User>, Int> = 
		User().select { t.id + t.userName + t.authCode }
			.where { t.id == 1 }
			.by{ t.id }
			.page(1, 10)
			.orderBy{ t.updateTime.desc }
			.distinct()
			.withTotal()
			.queryForList()


// 3.连表查询
val result: List<Map<String, Any>> = 
		User().join(ShoppingCart(), Good()){ user, cart, good ->
			leftJoin(cart)
				.on(user.id == cart.id && user.age > 35)
			rightJoin(good)
				.on(good.id == cart.id)
			select(user, good.id, cart.id)
			where (
				user.id == 1 &&
				user.age >= 20 &&
				user.email like "%@qq.com" &&
				user.telephone notLike "159%" &&
				(
					user.userName in listOf("a", "b") || 
					user.id !in listOf(1, 2, 3)
				) &&
				user.nickname.notNull &&
				user.age between 1..2 &&
				user.age notBetween 1..2
			)
			groupBy(user.age)
			page(1, 100)
		}
		.withTotal()
		.query()

```

------
>插入示例：

```kotlin
// 1.插入一行数据
val affectRowNumber: Pair<Int, Int> = 
		User(1).insert().execute()

// 2.根据主键更新或插入一行数据
val affectRowNumber: Pair<Int, Int> = 
		User(1).upsert()
			.set{ t.createTime = "YYYY-MM-DD" }
			.execute()
			
// 3.根据部分列更新或插入一行数据
val affectRowNumber: Pair<Int, Int> = 
		User(1).upsert{ t.id }
			.onDuplicate() // 默认
			.execute()

// 4.根据部分列更新或插入一行数据
val affectRowNumber: Pair<Int, Int> = 
		User(1).upsert{ t.id }
			.by{ t.name + t.email }
			.execute()
```


------
>更新示例：

```kotlin
// 更新行
val affectRowNumber: Int = 
		User(1).update()
			.set { t.id = 2 }
			.by { t.id }
			.execute()
				
// 更新行
val affectRowNumber: Int = 
		User(2).update{ t.id }
			.where{ t.id == 1 }
			.execute()
```

------
>删除示例：

```kotlin
// 删除行
val affectRowNumber: Int = 
		User.remove()
			.by { t.id + t.status }
			.execute()
				
val affectRowNumber: Int = 
		User.remove()
			.where { t.id > 1 && t.id < 10  }
			.execute()
```

------
[https://github.com/ousc/kotoframework-v2](Read%20more%20on%20GitHub)