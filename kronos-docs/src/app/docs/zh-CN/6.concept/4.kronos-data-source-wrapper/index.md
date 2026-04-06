{% import "../../../macros/macros-zh-CN.njk" as $ %}

`KronosDataSourceWrapper`是一个接口，是对数据库操作的封装，它不关心具体的数据库连接细节，与平台无关，只关心数据库操作的逻辑：

**成员变量**

{{$.members([
    ['dbType', '数据库类型', 'DbType'],
    ['url', '数据库连接地址', 'String'],
    ['username', '数据库用户名', 'String']
])}}

**成员函数**

{{$.members([
    ['forList', '执行查询', '(KAtomicQueryTask) -> List<Map<String, Any>>'],
    ['forList', '执行查询', '(KAtomicQueryTask, KClass<*>) -> List<Any>'],
    ['forMap', '执行查询', '(KAtomicQueryTask) -> Map<String, Any>?'],
    ['forObject', '执行查询', '(KAtomicQueryTask, KClass<*>) -> Any?'],
    ['update', '执行更新', '(KAtomicActionTask) -> Int'],
    ['batchUpdate', '批量执行更新', '(KAtomicBatchTask) -> Int'],
    ['transact', '事务', '((DataSource) -> Any?) -> Any?']
])}}
