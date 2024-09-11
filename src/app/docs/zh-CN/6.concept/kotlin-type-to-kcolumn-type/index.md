常见Kotlin类型与`Kronos列类型`的映射关系如下：
在未使用ColumnType注解的情况下，Kronos会根据Kotlin类型自动推测数据库列类型，您可以参考以下表格查看Kotlin数据类型与Kronos列类型(`KColumnType`)的映射关系。
Kronos列类型在不同数据库中的表现有所不同，具体请参考[Kronos列类型](/documentation/zh-CN/class-definition/kcolumn-type)。

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
