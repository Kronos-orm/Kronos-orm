# {{ NgDocPage.title }}

Specify the operation when cascading deletion, see [Cascading Delete](/documentation/en/database/reference-delete) for details.

1. `CASCADE` Cascading Delete
2. `RESTRICT` Restrict deletion, if there is related data, deletion is not allowed
3. `SET_NULL` Set to `null`
4. `NO_ACTION` **Default**, do nothing
5. `SET_DEFAULT` Set to the default value, the default value needs to specify the `defaultValue` attribute when defining the field