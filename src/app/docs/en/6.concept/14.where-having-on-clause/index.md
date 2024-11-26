{% import "../../../macros/macros-zh-CN.njk" as $ %}

## Criteria Conditional Expression

Kronos uses Criteria objects to build conditional expressions and supports complex combinations of conditions such as `&&`, `||`, `! `, `||`, `!', etc., which are used in conditions such as `where`, `having`, `on`, and so on.

You can use where condition objects to compose complex query conditions in methods that require conditions in `select`, `delete`, `update`, and `join` functions.

```kotlin
val list = user
    .select { it.name }
    .where { (it.id == 1 || it.age > 18) && it.name like "Kronos%" }
    .queryList<String>()
```

Based on KCP, Kronos allows you to build `Criteria` queries using real kotlin operators such as `==`, `! =`, `>`, `<`, `>=`, `<=`, `in`, `||`, `&&`, and so on, providing a super-expressive, concise and semantic way to write them.

kronos expressions support **dynamic construction of conditions** and automatic generation of conditions** based on properties** of objects, and support passing **sql strings** as conditions.

Using kronos is like writing native kotlin code, which will greatly **improve development efficiency** and **reduce learning costs and mental load**.

## Conditional Operators

kronos supports the following kotlin operators for constructing conditional expressions:

- {{ $.title("==") }}: equal to
- {{ $.title("! =") }}: not equal
- {{ $.title(">") }}: greater than
- {{ $.title("<") }}: less than
- {{ $.title("=") }}: greater than or equal to
- {{ $.title("=") }}: less than or equal to
- {{ $.title("in") }}: in range, can also use `contains` instead, e.g. `a in b` can be written as `b.contains(a)`
- {{ $.title("||") }}: or
- {{ $.title("&&") }}: with
- {{ $.title("!") }}: not, can be used with other functions and operators such as `! (a == 1 || a == 2) && a !in listOf(3, 4)`
- {{ $.title("()") }}: parentheses, used to change precedence

> **Note**
> kronos does not restrict the order of the left and right operands of expressions, e.g. `it.age > 18` and `18 < it.age` are equivalent.
> If you need to fetch the value of an object's property, use {{ $.keyword("database/where-having-on-clause", ["KPojo.xxx.value Get Object Value"]) }}.

> **Warning**
> Operators like `?.` and `?:` used outside of `xxx.value` or `xxx.asSql` will lead to incorrect condition expression generation, such as `it.school?.name.eq` and `it.name ?: "Kronos".eq`. You can use `!!.` for secondary calls, like `it.school!!.name.eq`.
> 
## Conditional Functions

### Equal

#### {{ $.title("eq") }} Equal

Equal to, equivalent to `==`, can be passed without parameters, e.g. `it.age.eq()` or `it.age.eq`

```kotlin {2,5,8}
// Equivalent to where { it.age == 18 }
where { it.age.eq(18) }

// Supports infix calls
where { it.age eq 18 }

// Can be passed without parameters, equivalent to where { it.age.eq(18) }
User(age = 18).select().where { it.age.eq }
```

#### {{ $.title("notEq") }} Not Equal

Not equal to, equivalent to `!=`, can be passed without parameters, e.g. `it.age.notEq()` or `it.age.notEq`

```kotlin {2,5,8}
// Equivalent to where { it.age != 18 }
where { it.age.notEq(18) }

// Supports infix calls
where { it.age notEq 18 }

// Can be passed without parameters, equivalent to where { it.age.notEq(18) }
User(age = 18).select().where { it.age.notEq }
```

### Range

#### {{ $.title("between") }} In Range

Receive parameters of type {{$.code("ClosedRange<*>")}}

```kotlin
where { it.age.between(1..10) }
// Supports infix calls
where { it.age between 1..10 }
```

#### {{ $.title("notBetween") }} Not In Range

Receive parameters of type {{$.code("ClosedRange<*>")}}

```kotlin
where { it.age.notBetween(1..10) }
// Supports infix calls
where { it.age notBetween 1..10 }
```

#### {{ $.title("gt") }} Greater Than

Greater than, equivalent to `>`, can be passed without parameters, e.g. `it.age.gt()` or `it.age.gt`

```kotlin {2,5,8}
 // Equivalent to where { it.age > 18 }
where { it.age.gt(18) }

// Supports infix calls
where { it.age gt 18 }

// Can be passed without parameters, equivalent to where { it.age.gt(18) }
User(age = 18).select().where { it.age.gt }
```

#### {{ $.title("lt") }} Less Than

Less than, equivalent to `<`, can be passed without parameters, e.g. `it.age.lt()` or `it.age.lt`

```kotlin {2,5,8}
// Equivalent to where { it.age < 18 }
where { it.age.lt(18) }

// Supports infix calls
where { it.age lt 18 }

// Can be passed without parameters, equivalent to where { it.age.lt(18) }
User(age = 18).select().where { it.age.lt }
```

#### {{ $.title("ge") }} Greater Than or Equal To

Greater than or equal to, equivalent to `>=`, can be passed without parameters, e.g. `it.age.ge()` or `it.age.ge`

```kotlin {2,5,8}
// Equivalent to where { it.age >= 18 }
where { it.age.ge(18) }

// Supports infix calls
where { it.age ge 18 }

// Can be passed without parameters, equivalent to where { it.age.ge(18) }
User(age = 18).select().where { it.age.ge }
```

#### {{ $.title("le") }} Less Than or Equal To

Less than or equal to, equivalent to `<=`, can be passed without parameters, e.g. `it.age.le()` or `it.age.le`

```kotlin {2,5,8}
// Equivalent to where { it.age <= 18 }
where { it.age.le(18) }

// Supports infix calls
where { it.age le 18 }

// Can be passed without parameters, equivalent to where { it.age.le(18) }
User(age = 18).select().where { it.age.le }
```

### In

#### {{ $.title("like") }} Fuzzy Query

Fuzzy query, accepts a parameter of type `String`

```kotlin {1,3}
where { it.name.like("Kronos%") }
// Supports infix calls
where { it.name like "Kronos%" }
```

#### {{ $.title("notLike") }} Not Fuzzy Query

Not fuzzy query, accepts a parameter of type `String`

```kotlin {1,3}
where { it.name.notLike("Kronos%") }
// Supports infix calls
where { it.name notLike "Kronos%" }
```

#### {{ $.title("startsWith") }} Left Fuzzy Query

Left fuzzy query, accepts String type parameters, can be passed without parameters, such as `it.name.startsWith()` or `it.name.startsWith`.

```kotlin {2,5,8}
// Equivalent to where { it.name like "Kronos%" }
where { it.name.startsWith("Kronos") }

// Supports infix calls
where { it.name startsWith "Kronos" }

// Can be passed without parameters
User(name = "Kronos").select().where { it.name.startsWith }
```

#### {{ $.title("endsWith") }} Right Fuzzy Query

Right fuzzy query, accepts String type parameters, can be passed without parameters, such as `it.name.endsWith()` or `it.name.endsWith`.

```kotlin {2,5,8}
// Equivalent to where { it.name like "%Kronos" }
where { it.name.endsWith("Kronos") }

// Supports infix calls
where { it.name endsWith "Kronos" }

// Can be passed without parameters
User(name = "Kronos").select().where { it.name.endsWith }
```

#### {{ $.title("contains") }} Full Fuzzy Query

Full fuzzy query, accepts String type parameters, can be passed without parameters, such as `it.name.contains()` or `it.name.contains`.

```kotlin {2,5,8}
// Equivalent to where { it.name like "%Kronos%" }
where { it.name.contains("Kronos") }

// Supports infix calls
where { it.name contains "Kronos" }
where { "Kronos" in it.name }

// Can be passed without parameters
User(name = "Kronos").select().where { it.name.contains }
```

### Other

#### {{ $.title("isNull") }} Is Null

Determine if it is null

```kotlin {1}
where { it.name.isNull }
```

#### {{ $.title("notNull") }} Is Not Null

Determine if it is not null

```kotlin {1}
where { it.name.notNull }
```

#### {{ $.title("regexp") }} Regular Expression Query

Regular expression query, accepts a parameter of type `String`

```kotlin {1,3}
where { it.name.regexp("Kronos.*") }
// Supports infix calls
where { it.name regexp "Kronos.*" }
```

## Special Expressions

### {{ $.title("KPojo.xxx.value") }} Get Object Value

In the where expression, `user.name` represents the field `name`, and `it.name.value` represents the `name` property value of the `User` object in Kotlin.

```kotlin {3,5}
val user = User(username = "Kronos")

User().select().where { it.username == user.username.value }.query()
// Equivalent to where { it.username == "Kronos" }.query()
User().select().where { it.username == "Kronos" }.query()
```

### {{ $.title("KPojo.eq") }} Automatically Generate Conditions

`user.eq` indicates that equality conditions for all non-empty fields within the object are automatically generated through `User`, such as `it.eq` being equivalent to `it.id == id && it.name == name && it.age == age`. It can be combined with operators such as `&&`, `||`, and `!`.

```kotlin {3}
val user = User(id = 1, name = "Kronos", age = 18)

User().select().where { it.eq || it.name like "Kronos%" }.query()
```

Using `-` can ignore the equality condition of a certain field.

```kotlin {3}
val user = User(id = 1, name = "Kronos", age = 18)
// The equality condition `it.id == 1` will not be generated.
User().select().where { (it - it.id).eq }.query()
```

### {{ $.title("(String/Boolean).asSql") }} SQL String Query

You can pass a string as a condition, such as `"name = 'Kronos' and age > 18"`, or a boolean value, such as `false`, as a condition.

```kotlin {3,5,7}
val user = User(id = 1, name = "Kronos", age = 18)

where { "name = 'Kronos' and age > 18".asSql() } // where name = 'Kronos' and age > 18

where { (it.id == 1 || it.age > 18).asSql() } // where false

where { (it.name == null).asSql() } // where false
```

Custom string SQL query conditions support named parameters, such as `"name = :name and age > :age"`, and Kronos will automatically replace `name` and `age` with specific values.

If the outer KPojo does not have the value required by the named parameter, you can use the `patch` method to pass in the parameter, such as:

```kotlin {1}
where { "name = :name".asSql() }.patch("name" to "Kronos", ...)
```

### {{ $.title("ifNoValue") }} No Value Strategy

NoValueStrategy, which receives the parameter `NoValueStrategy`, is used to handle cases with no value and has a higher priority than Kronos's default no-value strategy. For details, see: {{ $.keyword("concept/no-value-strategy", ["concept", "No Value Strategy"]) }}

```kotlin
val age: Int? = null
User()
    .select()
    .where { (it.age == age).ifNoValue(ignore) } // Ignore the condition when there is no value.
    .query()

val username: String? = null
User(username = username)
    .delete()
    .where { it.username.eq.ifNoValue(alwaysFalse) } // Always return false when there is no value.
    .execute()
```