The mapping relationship between common Kotlin types and `Kronos column types` is as follows:
When the ColumnType annotation is not used, Kronos will automatically infer the database column type based on the Kotlin type. You can refer to the following table to view the mapping relationship between Kotlin data types and Kronos column types (`KColumnType`).
The performance of Kronos column types varies in different databases. For details, please refer to [Kronos column types](/documentation/en/class-definition/kcolumn-type).

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
