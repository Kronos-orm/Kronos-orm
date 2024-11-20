{% import "../../../macros/macros-zh-CN.njk" as $ %}

在未使用{{ $.keyword("class-definition/annotation-config", ["注解设置","@ColumnType列类型及长度"]) }}的情况下，Kronos会根据Kotlin类型自动推测在数据库中持久化使用的列类型

您可以参考以下表格查看Kotlin数据类型与{{ $.keyword("concept/kcolumn-type", ["Kronos列类型"]) }}的映射关系：

**Kronos列类型**在不同数据库中的表现有所不同，具体请参考{{ $.keyword("concept/kcolumn-type", ["Kronos列类型"]) }}。

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
