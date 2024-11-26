{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Partially enable and disable cascade queries

### Disable cascading queries

Kronos enables the cascading query function by default and needs to be explicitly disabled in the `select` function:

```kotlin
KPojo.select().cascade(enable = false).queryList()
```

### Partially enable cascading queries

When there are multiple cascade declarations in KPojo, but only some of them need cascade query, you can pass the attributes that need cascade query into the `cascade` function, and the remaining attributes and sub-attributes will not trigger cascade query.
```kotlin
// If only property1 and property2 in KPojo need to be cascaded deleted, then it is as follows:
KPojo.select().cascade(KPojo::property1, KPojo::property2).queryList()
```

You can limit the cascade query of its sub-attributes as follows:

```kotlin
KPojo.select().cascade(
    KPojo::property1,
    KPojo::property2,
    Property1::subProperty1,
    Property1::subProperty2
).queryList()
```

### {{ $.annotation("CascadeSelectIgnore") }} Declare to turn off cascading queries

When defining the `KPojo` class, add the `@CascadeSelectIgnore` annotation to declare that a certain attribute query does not cascade query, see:
{{ $.keyword("class-definition/annotation-config", ["annotation configuration", "CascadeSelectIgnore close attribute cascade query"]) }}

## Cascading queries

After the cascade relationship is defined, use:
1. {{$.keyword("database/select-records", ["queryList: query the specified type list"])}}
2. {{$.keyword("database/select-records", ["queryOne: query a single record"])}}
3. {{$.keyword("database/select-records", ["queryOneOrNull: query a single record (optional)"])}} 

When you use the above three methods to query data, we will automatically perform logical queries for you based on the cascade relationship. For details, see: {{ $.keyword("advanced/cascade-definition", ["cascade relationship definition"]) }}.

Cascade query does not limit the level and direction of cascade relationship by default. If your cascade relationship is very deep, please pay attention to {{ $.keyword("advanced/cascade-select", ["turn off cascade query"]) }} when querying, or only {{ $
.keyword("advanced/cascade-select", ["partially turn on cascade query"]) }} to ensure that you will not **query data you don't need**.

> **Note**
> **Q:** How to handle circular references in cascading relationships in Kronos? Will there be an infinite query loop?
>
> **A:** Kronos handles circular references. The properties of each KPojo class will only be queried once at different levels. When a duplicate reference is encountered, the query will stop automatically to avoid infinite loop queries.
>
>```
A-->B;
B-->C;
C-->A;
A-->B; // This query will not be triggered again
> ```
> Here is an example:
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
A ||--o{ B : "cascade"
B ||--o{ C : "cascade"
C ||--o{ A : "cascade"
> ```
> If we query a certain A entity at this time, the complete structure of the query will be: (tree diagram representation)
>```
A1
├── bId: <bId of A1>
├── b
│ ├── cId: <cId of B1>
│ ├── c
│ │ ├── aId: <aId of C1>
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