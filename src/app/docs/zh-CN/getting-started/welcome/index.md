# {{ NgDocPage.title }}

{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

Kronos是一款基于**Code First**模式、KCP**编译器插件**，为kotlin设计的现代化的**ORM**框架。

*Kronos*为Kotlin而开发，通过KCP实现的表达式树分析支持以及kotlin的泛型和高阶函数，提供了**超级富有表现力、简洁而又语义化**的写法，通过使操作数据库变得更加简单。

基于Code First的理念，我们提供了**数据库表结构的自动创建、自动同步，以及对表结构、索引**等操作的简单支持。

同时通过编译器插件，我们实现了提供了无反射的Pojo和Map互转方案。

*Kronos*的级联操作、跨表跨库查询大大提升了开发效率，并基于kotlin协程机制大大提高了高并发性能。

以下是一个简单的示例。

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
