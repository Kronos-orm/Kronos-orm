{% import "../../../macros/macros-en.njk" as $ %}

## Define a one-to-one relationship

Use `@Cascade(properties, targetProperties)` on a `KPojo` property to describe how the current row points to the target row. The first array uses properties on the current class; the second array uses properties on the target class.

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

Relationship shape:

```text group="CascadeMapping 1 2" name="shape"
Employee.profileId -> EmployeeProfile.id
```

## Define a one-to-many relationship

Use a collection property when one source row can load multiple target rows.

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

Relationship shape:

```text group="CascadeMapping 2 2" name="collection shape"
Company.id -> Employee.companyId
```

## Match multiple fields

Pass the same number of properties in both arrays when a relationship uses a composite key.

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

Relationship shape:

```text group="CascadeMapping 3 2" name="multi shape"
OrderLine.tenantId -> OrderHeader.tenantId
OrderLine.orderNo  -> OrderHeader.orderNo
```

## Choose which cascade operations use the mapping

The optional `usage` parameter limits the operations that use this relationship mapping. Leave it unset when insert, update, delete, upsert, and select can all use the relationship.

```kotlin group="CascadeMapping 4" name="usage" icon="kotlin"
import com.kotlinorm.enums.KOperationType.SELECT

@Cascade(["id"], ["companyId"], usage = [SELECT])
var employees: List<Employee>? = null
```

`@Cascade` defines the relationship metadata. Operation pages show how to execute with that metadata: {{ $.keyword("advanced/cascade-select", ["Cascade Select"]) }}, {{ $.keyword("advanced/cascade-insert", ["Cascade Insert"]) }}, {{ $.keyword("advanced/cascade-update", ["Cascade Update"]) }}, and {{ $.keyword("advanced/cascade-delete", ["Cascade Delete"]) }}.

See {{ $.keyword("mapping/ignore", ["Ignore"]) }} when a relationship property should be skipped by default in automatic cascade select.
