{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

**Kronos ORM (Kotlin Reactive Object-Relational-Mapping) is a modern Kotlin ORM framework based on KCP and designed for K2.**

*Kronos* is a lightweight framework that provides developers with a simple solution for interacting with multiple databases.

*Kronos* analyzes IR expression trees to simplify code logic, making ORM coding concise and semantic. Through a compiler plugin, we also provide a simple solution for converting between Pojo and Map.

The design philosophy behind *Kronos* is to address the shortcomings of existing ORM frameworks and provide a more convenient and efficient data operation experience based on coroutines and task mechanisms.

```kotlin name="demo" icon="kotlin"
if(!db.table.exsits<User>()){
  db.table.create<User>()
}

val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )
    
user.insert(user)

user.update().set { it.name = "Kronos ORM" }.where { it.id == 1 }.execute()

val nameOfUser: String = user.select{ it.name }.where { it.id == 1 }.queryOne<String>()

user.delete().where { it.id == 1 }.execute()
```

{{ NgDocActions.demo("FeatureCardsComponent", {container: false}) }}
