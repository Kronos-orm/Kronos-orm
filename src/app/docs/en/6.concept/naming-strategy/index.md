`KronosNamingStrategy`是一个接口，用于定义表名和列名的转换策略。

## 成员函数：

### `fun db2k(name: String): String`

将数据库表/列名转为kotlin类名/属性名。

### `fun k2db(name: String): String`

将kotlin类名/属性名转为数据库表/列名。
