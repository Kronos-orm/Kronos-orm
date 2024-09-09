{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

`KronosDataSourceWrapper`是一个接口，是对数据库操作的封装，它不关心具体的数据库连接细节，与平台无关，只关心数据库操作的逻辑：

**成员变量**

{{$.params([
    ['dbType', '数据库类型', 'DbType'],
    ['url', '数据库连接地址', 'String'],
    ['username', '数据库用户名', 'String']
])}}

**成员函数**

{{$.params([
    ['forList', '执行查询', '(KAtomicQueryTask) -> List&lt;Map&lt;String, Any&gt;&gt;'],
    ['forList', '执行查询', '(KAtomicQueryTask, KClass&lt;*&gt;): List&lt;Any&gt;'],
    ['forMap', '执行查询', '(KAtomicQueryTask) -> Map&lt;String, Any&gt;?'],
    ['forObject', '执行查询', '(KAtomicQueryTask, KClass&lt;*&gt;): Any?'],
    ['update', '执行更新', '(KAtomicActionTask) -> Int'],
    ['batchUpdate', '批量执行更新', '(KAtomicBatchTask) -> Int'],
    ['transact', '事务', '((DataSource) -> Any?) -> Any?']
])}}
