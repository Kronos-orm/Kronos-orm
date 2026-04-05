{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}



## Pessimistic Lock

Kronos provides two types of pessimistic locks: **shared locks** and **exclusive locks**.

### {{$.title("PessimisticLock.S")}} Shared Lock

Kronos offers a **shared lock**,which is classified as a **row lock**.

A shared lock, also known as a read lock or S lock, allows the locked resource to be read by other users but not modified.
When performing a `SELECT`, the object is locked with a shared lock, and once the data has been read, the shared lock is released.
This ensures that the data is not modified while being read.

In Kronos, the **shared lock** can be used for {{$.keyword("database/select-records", ["query records", "lock settings for row locks during queries"])}} and
{{ $.keyword("database/upsert-records", ["update insert", "lock settings for row locks during queries"])}}functions.

### {{$.title("PessimisticLock.X")}} Exclusive Lock

Kronos provides an **exclusive lock**, which is classified as a **table lock**.

An exclusive lock, also known as a write lock or X lock, allows only the current user to modify the locked resource, preventing other users from reading it.
When performing a `SELECT`, the object is locked with an **exclusive lock**, and once the data has been read, the **exclusive lock** is released.
This ensures that the data is not modified while being read.

In Kronos, the **exclusive lock** can be used for {{$.keyword("database/select-records",
["query records", "lock settings for table locks during queries"])}} and {{$.keyword("database/upsert-records",
["update insert", "lock settings for table locks during queries"])}} functions.

## Optimistic Lock

Kronos provides an **optimistic lock** feature, which can be configured globally in {{$.keyword("getting-started/global-config",
["global configuration", "optimistic lock (version) strategy"])}} or used through the {{$.keyword("class-definition/annotation-config",
["annotation configuration", "@Version optimistic lock (version) column"])}} annotation.

Columns set as **optimistic locks** (default is `version`, and this column will be used as an example here) are initialized to 0 when a record is created,
and subsequent updates will increment the `version = version + 1`.

When performing an **upsert** operation, the `version` field is included in the filter criteria, meaning that the record will only be UPDATED if the `version` field in KPojo matches the modification count in the database; otherwise, an insert will be executed.