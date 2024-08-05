# {{ NgDocPage.title }}

规定级联删除时的操作，详见[级联删除](/documentation/database/reference-delete)。

1. `CASCADE` 级联删除
2. `RESTRICT` 限制删除，如果有关联数据，不允许删除
3. `SET_NULL` 设置为`null`
4. `NO_ACTION` **默认**，不做任何操作
5. `SET_DEFAULT` 设置为默认值，默认值需要在字段定义时指定`defaultValue`属性
