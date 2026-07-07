{% import "../../../macros/macros-zh-CN.njk" as $ %}

## 定义一对一关系

在 `KPojo` 属性上使用 `@Cascade(properties, targetProperties)` 描述当前行如何关联目标行。第一个数组填写当前类的属性，第二个数组填写目标类的属性。

```kotlin group="CascadeMapping 1 1" name="one to one" icon="kotlin"
import com.kotlinorm.annotations.Cascade
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo

@Table("tb_employee")
data class Employee(
    var id: Int? = null,
    var profileId: Int? = null,
    @Cascade(["profileId"], ["id"])
    var profile: EmployeeProfile? = null
) : KPojo

@Table("tb_employee_profile")
data class EmployeeProfile(
    var id: Int? = null,
    var displayName: String? = null
) : KPojo
```

关系形态：

```text group="CascadeMapping 1 2" name="shape"
Employee.profileId -> EmployeeProfile.id
```

## 定义一对多关系

一个源行需要加载多个目标行时，在集合属性上声明关系。

```kotlin group="CascadeMapping 2 1" name="one to many" icon="kotlin"
@Table("tb_company")
data class Company(
    var id: Int? = null,
    var name: String? = null,
    @Cascade(["id"], ["companyId"])
    var employees: List<Employee>? = null
) : KPojo

@Table("tb_employee")
data class Employee(
    var id: Int? = null,
    var companyId: Int? = null,
    var name: String? = null
) : KPojo
```

关系形态：

```text group="CascadeMapping 2 2" name="collection shape"
Company.id -> Employee.companyId
```

## 匹配多个字段

关系使用组合键时，在两个数组中传入数量一致的属性。

```kotlin group="CascadeMapping 3 1" name="multi field" icon="kotlin"
@Table("tb_order_line")
data class OrderLine(
    var tenantId: Int? = null,
    var orderNo: String? = null,
    var lineNo: Int? = null,
    @Cascade(["tenantId", "orderNo"], ["tenantId", "orderNo"])
    var order: OrderHeader? = null
) : KPojo

@Table("tb_order_header")
data class OrderHeader(
    var tenantId: Int? = null,
    var orderNo: String? = null,
    var customerName: String? = null
) : KPojo
```

关系形态：

```text group="CascadeMapping 3 2" name="multi shape"
OrderLine.tenantId -> OrderHeader.tenantId
OrderLine.orderNo  -> OrderHeader.orderNo
```

## 限定使用该映射的级联操作

可选参数 `usage` 可以限制哪些操作使用这段关系映射。保持默认值时，insert、update、delete、upsert、select 都可以使用该关系。

```kotlin group="CascadeMapping 4" name="usage" icon="kotlin"
import com.kotlinorm.enums.KOperationType.SELECT

@Cascade(["id"], ["companyId"], usage = [SELECT])
var employees: List<Employee>? = null
```

`@Cascade` 定义关系元数据。如何执行级联操作见 {{ $.keyword("advanced/cascade-select", ["级联查询"]) }}、{{ $.keyword("advanced/cascade-insert", ["级联插入"]) }}、{{ $.keyword("advanced/cascade-update", ["级联更新"]) }}、{{ $.keyword("advanced/cascade-delete", ["级联删除"]) }}。

关系属性需要在自动级联查询中默认跳过时，见 {{ $.keyword("mapping/ignore", ["忽略策略"]) }}。
