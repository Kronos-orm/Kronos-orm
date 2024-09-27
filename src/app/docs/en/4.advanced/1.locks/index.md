{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 悲观锁

Kronos共提供**共享锁**和**独占锁**两种悲观锁。

### {{$.title("PessimisticLock.S")}} 共享锁

Kronos提供**共享锁**，其锁等级为**行锁**。

共享锁也叫读锁或 S 锁，共享锁锁定的资源可以被其他用户读取，但不能修改。在进行`SELECT`
的时候，会将对象进行共享锁锁定，当数据读取完毕之后，就会释放共享锁，这样就可以保证数据在读取时不被修改。

在Kronos中，**共享锁**可用于{{$.keyword("database/select-records", ["查询记录", "lock设置查询时行锁"])}}与{{
$.keyword("database/upsert-records", ["更新插入", "lock设置查询时行锁"])}}功能。

### {{$.title("PessimisticLock.X")}} 独占锁

Kronos提供**独占锁**，其锁等级为**表锁**。

独占锁也叫写锁或 X 锁，独占锁锁定的资源只能被当前用户修改，不能被其他用户读取。在进行`SELECT`
的时候，会将对象进行独占锁锁定，当数据读取完毕之后，就会释放独占锁，这样就可以保证数据在读取时不被修改。

在Kronos中，**独占锁**可用于{{$.keyword("database/select-records", ["查询记录", "lock设置查询时表锁"])}}与{{
$.keyword("database/upsert-records", ["更新插入", "lock设置查询时表锁"])}}功能。

## 乐观锁

Kronos提供**乐观锁**功能，可以在{{$.keyword("getting-started/global-config", ["全局配置", "乐观锁（版本）策略"])}}中设置全局乐观锁策略或通过{{
$.keyword("class-definition/annotation-config", ["注解配置", "@Version乐观锁（版本）列"])}}注解使用乐观锁功能。

被设置为**乐观锁**的列（默认为`version`，接下来均以该列为例）在记录新建时会被设置成0，后续每次更新`version = version + 1`

在执行**更新插入**(**upsert**)操作时，会将`version`字段添加进筛选项，意为仅当KPojo的该字段与数据库中修改次数一致时才会更新该条数据，否则则执行插入。


