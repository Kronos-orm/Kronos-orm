{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Cascading insert or update

Use `KPojo.upsert().on(...).cascade(...).execute()` to upsert a root record and let the declared `@Cascade` relationships participate in the operation.

Cascade upsert uses the match-field upsert flow described in {{ $.keyword("mutation/upsert", ["Upsert Records"]) }}:

```text
1. Check whether the root record exists by the fields passed to `on { ... }`.
2. If the record exists, run the update branch and apply cascade update behavior.
3. If the record is absent, run the insert branch and apply cascade insert behavior.
```

Cascade upsert runs through the insert and update branches, so a relation participates when its `@Cascade.usage` allows `KOperationType.INSERT` or `KOperationType.UPDATE`. The default `@Cascade` usage already includes both operation types. For relation mapping rules, see {{ $.keyword("advanced/cascade", ["Cascade Definition"]) }}.

## Enable cascade upsert

Cascade is enabled by default. You can also call `.cascade(true)` to make the setting explicit.

```kotlin group="Enable cascade 1" name="kotlin" icon="kotlin" {27}
@Table("account")
data class Account(
    @PrimaryKey
    var id: Long? = null,
    var email: String? = null,
    var profile: AccountProfile? = null,
    var auditLogs: List<AccountAudit> = listOf()
) : KPojo

@Table("account_profile")
data class AccountProfile(
    @PrimaryKey
    var id: Long? = null,
    var accountEmail: String? = null,
    var displayName: String? = null,
    @Cascade(["accountEmail"], ["email"])
    var account: Account? = null
) : KPojo

@Table("account_audit")
data class AccountAudit(
    @PrimaryKey
    var id: Long? = null,
    var accountEmail: String? = null,
    var action: String? = null,
    @Cascade(["accountEmail"], ["email"])
    var account: Account? = null
) : KPojo

Account(
    id = 1,
    email = "ada@kronos.dev",
    profile = AccountProfile(displayName = "Ada")
)
    .upsert { it.email }
    .on { it.id }
    .cascade(true)
    .execute()
```

```sql group="Enable cascade 1" name="mysql" icon="mysql"
SELECT COUNT(1) FROM `account` WHERE `id` = :id LIMIT 1 FOR UPDATE;

# If the root record is absent
INSERT INTO `account` (`id`, `email`) VALUES (:id, :email);
INSERT INTO `account_profile` (`account_email`, `display_name`) VALUES (:accountEmail, :displayName);

# If the root record exists
SELECT `id`, `email` FROM `account` WHERE `id` = :id;
SELECT `id`, `account_email` AS `accountEmail`, `display_name` AS `displayName`
FROM `account_profile`
WHERE `account_email` = :accountEmail;
UPDATE `account_profile`
SET `account_email` = :accountEmailNew
WHERE `id` = :id AND `account_email` = :accountEmail;
UPDATE `account` SET `email` = :emailNew WHERE `id` = :id;
```

Result shape:

```text group="Enable cascade 2" name="result"
Absent root: account is inserted first, then account_profile is inserted with account_email = account.email.
Existing root: account_profile.account_email is updated before account.email when email changes.
```

## One-to-one cascade upsert

For a one-to-one relationship, put the reference object on the root object and declare the reference fields with `@Cascade`.

```kotlin group="One-to-one 1" name="kotlin" icon="kotlin" {17,25}
@Table("user_profile")
data class UserProfile(
    @PrimaryKey
    var id: Long? = null,
    var userCode: String? = null,
    var nickname: String? = null,
    @Cascade(["userCode"], ["code"])
    var user: User? = null
) : KPojo

@Table("user")
data class User(
    @PrimaryKey
    var id: Long? = null,
    var code: String? = null,
    var profile: UserProfile? = null
) : KPojo

User(
    id = 10,
    code = "U-001",
    profile = UserProfile(nickname = "Ada")
)
    .upsert { it.code }
    .on { it.id }
    .execute()
```

```text group="One-to-one 1" name="execution order"
1. SELECT COUNT(1) on `user` by id.
2. Insert branch: insert `user`, then insert `user_profile`.
3. Update branch: query the existing `user`, query the matching `user_profile`, update the profile reference field, then update `user`.
```

```sql group="One-to-one 1" name="mysql" icon="mysql"
INSERT INTO `user` (`id`, `code`) VALUES (:id, :code);
INSERT INTO `user_profile` (`user_code`, `nickname`) VALUES (:userCode, :nickname);

UPDATE `user_profile`
SET `user_code` = :userCodeNew
WHERE `id` = :id AND `user_code` = :userCode;
UPDATE `user` SET `code` = :codeNew WHERE `id` = :id;
```

Result shape:

```text group="One-to-one 2" name="result"
The child reference column is filled from the parent field declared in `@Cascade`.
On the update branch, Kronos updates the child reference column that depends on the changed parent field.
```

## One-to-many cascade upsert

For a one-to-many relationship, put the child list on the parent and declare `@Cascade` on the child reference back to the parent.

```kotlin group="One-to-many 1" name="kotlin" icon="kotlin" {16,28}
@Table("catalog")
data class Catalog(
    @PrimaryKey
    var id: Long? = null,
    var code: String? = null,
    var items: List<CatalogItem> = listOf()
) : KPojo

@Table("catalog_item")
data class CatalogItem(
    @PrimaryKey
    var id: Long? = null,
    var catalogCode: String? = null,
    var sku: String? = null,
    @Cascade(["catalogCode"], ["code"])
    var catalog: Catalog? = null
) : KPojo

Catalog(
    id = 20,
    code = "SPRING",
    items = listOf(
        CatalogItem(sku = "SKU-1"),
        CatalogItem(sku = "SKU-2")
    )
)
    .upsert { it.code }
    .on { it.id }
    .execute()
```

```text group="One-to-many 1" name="execution order"
1. SELECT COUNT(1) on `catalog` by id.
2. Insert branch: insert `catalog`, then insert each `catalog_item`.
3. Update branch: query the existing `catalog`, query child rows by the old catalog code, update child reference fields, then update `catalog`.
```

```sql group="One-to-many 1" name="mysql" icon="mysql"
INSERT INTO `catalog` (`id`, `code`) VALUES (:id, :code);
INSERT INTO `catalog_item` (`catalog_code`, `sku`) VALUES (:catalogCode, :sku);

SELECT `id`, `catalog_code` AS `catalogCode`, `sku`
FROM `catalog_item`
WHERE `catalog_code` = :catalogCode;
UPDATE `catalog_item`
SET `catalog_code` = :catalogCodeNew
WHERE `id` = :id AND `catalog_code` = :catalogCode;
UPDATE `catalog` SET `code` = :codeNew WHERE `id` = :id;
```

Result shape:

```text group="One-to-many 2" name="result"
Inserted children receive catalog_code from catalog.code.
When catalog.code changes on the update branch, matching child rows receive the new catalog_code before the parent update runs.
```

## Specify cascade fields

Use `.cascade { [...] }` to run cascade upsert for selected reference fields. The callback must select at least one reference property.

```kotlin group="Specify fields 1" name="kotlin" icon="kotlin" {17}
Account(
    id = 1,
    email = "ada@kronos.dev",
    profile = AccountProfile(displayName = "Ada"),
    auditLogs = listOf(AccountAudit(action = "created"))
)
    .upsert { it.email }
    .on { it.id }
    .cascade { [Account::profile] }
    .execute()
```

```text group="Specify fields 1" name="execution order"
1. Root existence check still uses `on { it.id }`.
2. The root insert or update task runs as usual.
3. Only `Account::profile` participates in the cascade tree.
```

```sql group="Specify fields 1" name="mysql" icon="mysql"
SELECT COUNT(1) FROM `account` WHERE `id` = :id LIMIT 1 FOR UPDATE;
INSERT INTO `account` (`id`, `email`) VALUES (:id, :email);
INSERT INTO `account_profile` (`account_email`, `display_name`) VALUES (:accountEmail, :displayName);
```

Result shape:

```text group="Specify fields 2" name="result"
The profile row is cascaded.
Other cascade-capable properties on Account keep their current database state for this call.
```

## Disable cascade upsert

Use `.cascade(false)` when the call should affect only the root table.

```kotlin group="Disable cascade 1" name="kotlin" icon="kotlin" {10}
Account(
    id = 1,
    email = "ada@kronos.dev",
    profile = AccountProfile(displayName = "Ada")
)
    .upsert { it.email }
    .on { it.id }
    .cascade(false)
    .execute()
```

```sql group="Disable cascade 1" name="mysql" icon="mysql"
SELECT COUNT(1) FROM `account` WHERE `id` = :id LIMIT 1 FOR UPDATE;

# If the root record is absent
INSERT INTO `account` (`id`, `email`) VALUES (:id, :email);

# If the root record exists
UPDATE `account` SET `email` = :emailNew WHERE `id` = :id;
```

Result shape:

```text group="Disable cascade 2" name="result"
Only the account row is inserted or updated.
The profile object in memory is ignored by this database call.
```

## Logic delete and optimistic lock fields

Cascade upsert follows the same strategy fields as insert and update. On the insert branch, `@LogicDelete` and `@Version` fields receive their configured initial values. On the update branch, logically deleted rows can be matched by the `on` fields, the logic-delete column is restored to the normal value, and the version column is incremented by the update strategy.

```kotlin group="Strategy fields 1" name="kotlin" icon="kotlin" {6-10,19-22}
@Table("account_flag")
data class AccountFlag(
    @PrimaryKey
    var id: Long? = null,
    var name: String? = null,
    @LogicDelete
    @Default("0") // @Default("false") for Postgres
    var deleted: Boolean? = null,
    @Version
    var version: Int? = null
) : KPojo

AccountFlag(id = 1, name = "active")
    .upsert { it.name }
    .on { it.id }
    .execute()
```

```sql group="Strategy fields 1" name="mysql" icon="mysql"
# Insert branch
INSERT INTO `account_flag` (`id`, `name`, `deleted`, `version`)
VALUES (:id, :name, :deleted, :version);

# Update branch
UPDATE `account_flag`
SET `name` = :nameNew,
    `deleted` = :deletedNew,
    `version` = `version` + :version2PlusNew
WHERE `id` = :id;
```

Result shape:

```text group="Strategy fields 2" name="result"
Inserted row: deleted = false, version = 0.
Existing logically deleted row matched by id: name is updated, deleted is restored to false, version is incremented.
```

> **Note**
> Leave `@Version` managed by Kronos. If the update branch tries to update the version field manually, the update planner raises an error.

## Primary key, unique key, and onConflict boundaries

The `on { ... }` fields define the root-row existence check. Use the primary key or the same field set as a unique business key.

```kotlin group="Key fields 1" name="kotlin" icon="kotlin" {15-16}
@Table("product")
@TableIndex(name = "uk_product_tenant_code", columns = ["tenant_id", "code"], type = "UNIQUE")
data class Product(
    @PrimaryKey
    var id: Long? = null,
    var tenantId: Long? = null,
    var code: String? = null,
    var name: String? = null
) : KPojo

Product(id = 100, tenantId = 7, code = "A-100", name = "Desk")
    .upsert { it.name }
    .on { [it.tenantId, it.code] }
    .execute()
```

```sql group="Key fields 1" name="mysql" icon="mysql"
SELECT COUNT(1)
FROM `product`
WHERE `tenant_id` = :tenantId AND `code` = :code
LIMIT 1 FOR UPDATE;

UPDATE `product`
SET `name` = :nameNew
WHERE `tenant_id` = :tenantId AND `code` = :code;
```

Result shape:

```text group="Key fields 2" name="result"
The row matched by tenantId + code is updated.
If no row matches tenantId + code, Kronos inserts the product row.
```

For identity primary keys, insert-branch cascade can use the generated root id when child rows reference that id.

```kotlin group="Identity key" name="kotlin" icon="kotlin" {3,15,25}
@Table("identity_parent")
data class IdentityParent(
    @PrimaryKey(identity = true)
    var id: Long? = null,
    var code: String? = null,
    var children: List<IdentityChild> = listOf()
) : KPojo

@Table("identity_child")
data class IdentityChild(
    @PrimaryKey(identity = true)
    var id: Long? = null,
    var parentId: Long? = null,
    var name: String? = null,
    @Cascade(["parentId"], ["id"])
    var parent: IdentityParent? = null
) : KPojo

IdentityParent(
    code = "P-1",
    children = listOf(IdentityChild(name = "child-1"))
)
    .upsert { it.code }
    .on { it.code }
    .execute()
```

```text group="Identity key" name="result"
The parent insert omits the identity id.
After the parent insert returns the generated id, child.parentId is filled before the child insert runs.
```

`onConflict()` handles only the root upsert. Use the match-field upsert flow shown above when this call needs the cascade tree.

```kotlin group="onConflict 1" name="kotlin" icon="kotlin" {5}
Product(id = 100, tenantId = 7, code = "A-100", name = "Desk")
    .upsert { it.name }
    .on { [it.tenantId, it.code] }
    .onConflict()
    .execute()
```

```sql group="onConflict 1" name="mysql" icon="mysql"
INSERT INTO `product` (`id`, `tenant_id`, `code`, `name`)
VALUES (:id, :tenantId, :code, :name)
ON DUPLICATE KEY UPDATE `name` = :name;
```

Result shape:

```text group="onConflict 2" name="result"
The generated task contains the root product upsert statement.
Database uniqueness on the conflict fields is required for `onConflict()`.
If `on { ... }` is omitted, Kronos infers the conflict fields from primary-key values or declared unique indexes.
```
