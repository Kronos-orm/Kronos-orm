{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 级联插入或更新

使用`KPojo.upsert().on(...).cascade(...).execute()`可以对根记录执行更新插入，并让已经声明的`@Cascade`关系参与本次操作。

级联 upsert 使用 {{ $.keyword("mutation/upsert", ["更新插入"]) }} 中的 fallback 流程：

```text
1. 通过 `on { ... }` 指定的字段检查根记录是否存在。
2. 记录存在时，执行 update 分支，并应用级联更新行为。
3. 记录不存在时，执行 insert 分支，并应用级联插入行为。
```

级联 upsert 会进入 insert 或 update 分支，因此`@Cascade.usage`允许`KOperationType.INSERT`或`KOperationType.UPDATE`时，该关系会参与级联。`@Cascade`默认`usage`已经包含这两类操作。关系映射规则见 {{ $.keyword("advanced/cascade", ["级联关系定义"]) }}。

## 开启级联 upsert

级联默认开启，也可以通过`.cascade(true)`显式开启。

```kotlin group="开启级联 1" name="kotlin" icon="kotlin" {27}
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

```sql group="开启级联 1" name="mysql" icon="mysql"
SELECT COUNT(1) FROM `account` WHERE `id` = :id LIMIT 1 FOR UPDATE;

# 根记录不存在时
INSERT INTO `account` (`id`, `email`) VALUES (:id, :email);
INSERT INTO `account_profile` (`account_email`, `display_name`) VALUES (:accountEmail, :displayName);

# 根记录存在时
SELECT `id`, `email` FROM `account` WHERE `id` = :id;
SELECT `id`, `account_email` AS `accountEmail`, `display_name` AS `displayName`
FROM `account_profile`
WHERE `account_email` = :accountEmail;
UPDATE `account_profile`
SET `account_email` = :accountEmailNew
WHERE `id` = :id AND `account_email` = :accountEmail;
UPDATE `account` SET `email` = :emailNew WHERE `id` = :id;
```

结果形态：

```text group="开启级联 2" name="result"
根记录不存在：先插入 account，再插入 account_profile，account_profile.account_email = account.email。
根记录存在：当 email 变化时，先更新 account_profile.account_email，再更新 account.email。
```

## 一对一级联 upsert

一对一关系中，在根对象上放置引用对象，并通过`@Cascade`声明引用字段。

```kotlin group="一对一 1" name="kotlin" icon="kotlin" {17,25}
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

```text group="一对一 1" name="execution order"
1. 通过 id 对 `user` 执行 SELECT COUNT(1)。
2. insert 分支：插入 `user`，再插入 `user_profile`。
3. update 分支：查询已有 `user`，查询匹配的 `user_profile`，更新 profile 引用字段，再更新 `user`。
```

```sql group="一对一 1" name="mysql" icon="mysql"
INSERT INTO `user` (`id`, `code`) VALUES (:id, :code);
INSERT INTO `user_profile` (`user_code`, `nickname`) VALUES (:userCode, :nickname);

UPDATE `user_profile`
SET `user_code` = :userCodeNew
WHERE `id` = :id AND `user_code` = :userCode;
UPDATE `user` SET `code` = :codeNew WHERE `id` = :id;
```

结果形态：

```text group="一对一 2" name="result"
子表引用列会由 `@Cascade` 声明的父表字段填充。
update 分支中，当父表字段变化时，Kronos 会更新依赖该字段的子表引用列。
```

## 一对多级联 upsert

一对多关系中，在父对象上放置子对象列表，并在子对象回到父对象的引用属性上声明`@Cascade`。

```kotlin group="一对多 1" name="kotlin" icon="kotlin" {16,28}
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

```text group="一对多 1" name="execution order"
1. 通过 id 对 `catalog` 执行 SELECT COUNT(1)。
2. insert 分支：插入 `catalog`，再插入每条 `catalog_item`。
3. update 分支：查询已有 `catalog`，通过旧 catalog code 查询子记录，更新子表引用字段，再更新 `catalog`。
```

```sql group="一对多 1" name="mysql" icon="mysql"
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

结果形态：

```text group="一对多 2" name="result"
插入的子记录会从 catalog.code 得到 catalog_code。
update 分支中，当 catalog.code 变化时，匹配的子记录会先得到新的 catalog_code，然后更新父记录。
```

## 指定级联字段

使用`.cascade { [...] }`可以只让选中的引用字段参与本次级联 upsert。回调中必须选中至少一个引用属性。

```kotlin group="指定字段 1" name="kotlin" icon="kotlin" {9}
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

```text group="指定字段 1" name="execution order"
1. 根记录存在性检查仍然使用 `on { it.id }`。
2. 根记录 insert 或 update 任务照常执行。
3. 只有 `Account::profile` 进入级联树。
```

```sql group="指定字段 1" name="mysql" icon="mysql"
SELECT COUNT(1) FROM `account` WHERE `id` = :id LIMIT 1 FOR UPDATE;
INSERT INTO `account` (`id`, `email`) VALUES (:id, :email);
INSERT INTO `account_profile` (`account_email`, `display_name`) VALUES (:accountEmail, :displayName);
```

结果形态：

```text group="指定字段 2" name="result"
profile 记录会参与级联。
Account 上其他可级联属性本次调用保持数据库现状。
```

## 关闭级联 upsert

当本次调用只需要影响根表时，使用`.cascade(false)`。

```kotlin group="关闭级联 1" name="kotlin" icon="kotlin" {10}
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

```sql group="关闭级联 1" name="mysql" icon="mysql"
SELECT COUNT(1) FROM `account` WHERE `id` = :id LIMIT 1 FOR UPDATE;

# 根记录不存在时
INSERT INTO `account` (`id`, `email`) VALUES (:id, :email);

# 根记录存在时
UPDATE `account` SET `email` = :emailNew WHERE `id` = :id;
```

结果形态：

```text group="关闭级联 2" name="result"
只有 account 记录被插入或更新。
内存中的 profile 对象不会参与本次数据库调用。
```

## 逻辑删除和乐观锁字段

fallback 级联 upsert 沿用 insert 与 update 的策略字段。insert 分支会为`@LogicDelete`和`@Version`字段写入初始值。update 分支可以通过`on`字段匹配逻辑删除记录，将逻辑删除列恢复为正常值，并通过更新策略递增版本列。

```kotlin group="策略字段 1" name="kotlin" icon="kotlin" {6-9,18-21}
@Table("account_flag")
data class AccountFlag(
    @PrimaryKey
    var id: Long? = null,
    var name: String? = null,
    @LogicDelete
    var deleted: Boolean? = null,
    @Version
    var version: Int? = null
) : KPojo

AccountFlag(id = 1, name = "active")
    .upsert { it.name }
    .on { it.id }
    .execute()
```

```sql group="策略字段 1" name="mysql" icon="mysql"
# insert 分支
INSERT INTO `account_flag` (`id`, `name`, `deleted`, `version`)
VALUES (:id, :name, :deleted, :version);

# update 分支
UPDATE `account_flag`
SET `name` = :nameNew,
    `deleted` = :deletedNew,
    `version` = `version` + :version2PlusNew
WHERE `id` = :id;
```

结果形态：

```text group="策略字段 2" name="result"
新插入记录：deleted = false，version = 0。
通过 id 匹配到的逻辑删除记录：name 被更新，deleted 恢复为 false，version 递增。
```

> **Note**
> `@Version`字段交给 Kronos 管理。update 分支中手动更新 version 字段时，update planner 会抛出错误。

## 主键、唯一键和 onConflict 边界

`on { ... }`中的字段决定根记录存在性检查。可以使用主键，也可以使用与业务唯一键一致的一组字段。

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

结果形态：

```text group="Key fields 2" name="result"
tenantId + code 匹配到的记录会被更新。
tenantId + code 没有匹配记录时，Kronos 会插入 product 记录。
```

对于自增主键，insert 分支的级联可以使用根记录生成的 id 来写入子记录引用。

```kotlin group="自增主键" name="kotlin" icon="kotlin" {3,15,25}
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

```text group="自增主键" name="result"
父记录插入时会省略自增 id。
父记录插入返回生成 id 后，child.parentId 会在子记录插入前被填充。
```

`onConflict()`会生成单条原生根表 upsert 语句。需要执行级联树时，使用上面的 fallback 流程。

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

结果形态：

```text group="onConflict 2" name="result"
生成的任务包含 product 根表 upsert 语句。
原生冲突处理需要数据库在冲突字段上存在唯一性约束。
```
