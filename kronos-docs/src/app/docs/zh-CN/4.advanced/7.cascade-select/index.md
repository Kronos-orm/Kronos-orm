{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 部分开启及关闭级联查询

### 关闭级联查询

Kronos默认开启级联查询功能，需要在`select`函数中显式关闭：

```kotlin
KPojo.select().cascade(enable = false).queryList()
```

### 部分开启级联查询

当KPojo中有多个级联声明，但只有部分需要级联查询时，可以将需要级联查询的属性传入`cascade`函数，其余的属性及子属性将不触发级联查询。

```kotlin
// 若KPojo中只有property1和property2需要级联删除，那么如下：
KPojo.select().cascade(KPojo::property1, KPojo::property2).queryList()
```

可以限制其子属性级联查询，如下：

```kotlin
KPojo.select().cascade(
    KPojo::property1,
    KPojo::property2,
    Property1::subProperty1,
    Property1::subProperty2
).queryList()
```

### {{ $.annotation("CascadeSelectIgnore") }} 声明关闭级联查询

在定义`KPojo`类时,通过添加`@CascadeSelectIgnore`注解声明某属性查询时不级联查询, 详见：
{{ $.keyword("class-definition/annotation-config", ["注解配置", "CascadeSelectIgnore关闭属性级联查询"]) }}

## 级联查询

在级联关系被定义后，使用：
1. {{$.keyword("database/select-records", ["queryList查询指定类型列表"])}}
2. {{$.keyword("database/select-records", ["queryOne查询单条记录"])}}
3. {{$.keyword("database/select-records", ["queryOneOrNull查询单条记录（可空）"])}} 

以上三种方法查询数据时，我们将自动为您根据级联关系进行逻辑查询，详见：{{ $.keyword("advanced/cascade-definition", ["级联关系定义"]) }}。

级联查询默认不限制层级和级联关系方向，如果您的级联关系层数很深，在查询时请注意{{ $.keyword("advanced/cascade-select", ["关闭级联查询"]) }}，或仅{{ $
.keyword("advanced/cascade-select", ["部分开启级联查询"]) }}以保证不会**查询到您不需要的数据**。

> **Note**
> **Q:** Kronos中如何处理级联关系中的循环引用？会无限循环查询吗？
>
> **A:** Kronos中会对循环引用进行处理，每个KPojo类的属性在不同层级仅会被查询一次，遇到重复引用时自动停止，避免无限循环查询。
>
>```
A-->B;
B-->C;
C-->A;
A-->B; //将不会触发此查询
> ```
> 如下是一个示例：
>```mermaid
erDiagram
A {
    int bId
    B b
    List[C] listOfC
}
B {
    int cId
    C c
    List[A] listOfA
}
C {
    int aId
    A a
    List[B] listOfB
}
A ||--o{ B : "关联"
B ||--o{ C : "关联"
C ||--o{ A : "关联"
> ```
> 若此时查询某个A实体，查询出的完整结构将是：(树状图表示)
>```
A1
├── bId: <A1的bId>
├── b
│ ├── cId: <B1的cId>
│ ├── c
│ │ ├── aId: <C1的aId>
│ │ └── listOfB
│ │ ├── B2
│ │ ├── B3
│ │ └── ...
│ └── listOfA
│ ├── A2
│ ├── A3
│ └── ...
└── listOfC
├── C1
├── C2
└── ...
>```