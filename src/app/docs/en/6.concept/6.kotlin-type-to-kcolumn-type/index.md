{% import "../../../macros/macros-zh-CN.njk" as $ %}

Without {{ $.keyword("class-definition/annotation-config", ["Annotation Settings","@ColumnType column type and length"]) }}, Kronos automatically infers the type of columns used for persistence in the database based on the Kotlin type

You can refer to the following table to see the mapping of Kotlin datatypes to {{ $.keyword("concept/kcolumn-type", ["Kronos Column Type"]) }}:

**Kronos column type** behave differently in different databases, see {{ $.keyword("concept/kcolumn-type", ["Kronos Column Type"]) }}.

| Kotlin类型                                                                               | Kronos列类型  |
|----------------------------------------------------------------------------------------|------------|
| `kotlin.Boolean`                                                                       | `TINYINT`  |
| `kotlin.Byte`                                                                          | `TINYINT`  |
| `kotlin.Short`                                                                         | `SMALLINT` |
| `kotlin.Int`                                                                           | `INT`      |
| `kotlin.Long`                                                                          | `BIGINT`   |
| `kotlin.Float`                                                                         | `FLOAT`    |
| `kotlin.Double`                                                                        | `DOUBLE`   |
| `java.math.BigDecimal`                                                                 | `NUMERIC`  |
| `kotlin.Char`                                                                          | `CHAR`     |
| `kotlin.String`                                                                        | `VARCHAR`  |
| `kotlinx.datetime.LocalDate`, `java.util.Date`, `java.sql.Date`, `java.time.LocalDate` | `DATE`     |
| `kotlinx.datetime.LocalTime`, `java.time.LocalTime`                                    | `TIME`     |
| `kotlinx.datetime.LocalDateTime`, `java.time.LocalDateTime`                            | `DATETIME` |
| `kotlin.ByteArray`                                                                     | `BINARY`   |
| else                                                                                   | `VARCHAR`  |
