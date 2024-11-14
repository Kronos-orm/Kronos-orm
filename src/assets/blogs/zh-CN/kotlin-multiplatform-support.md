# Kronos对于Kotlin Multiplatform的支持与计划

<center>
<img src="http://localhost:3307/assets/images/features/img-1.png" width="300"/>
</center>

--------

非常感谢大家能看到这篇文章，但是很遗憾的是，Kronos目前还没有提供支持除JVM以外的其他平台支持。

如果您现在非常急迫地需要可以在移动端、js等多平台运行的ORM框架，那么目前我们可能不是最好的选择，也许以下的库可以满足您的需求:

- [SQLLin](https://github.com/ctripcorp/SQLlin)
- [sqlDelight](https://github.com/sqldelight/sqldelight)

Kronos设计被用于后端和移动端开发，且支持多种数据库，截止目前，我们已经初步完成了jvm平台的ORM功能，开发过程中由于非JVM平台标准库的限制，我们暂时将多平台的支持列入到计划中。

我们计划在`0.2`或者`0.3`版本开始添加多平台支持，我们可能会先为android平台及sqlite数据库提供支持（相关问题号：[issue#49](https://github.com/Kronos-orm/Kronos-orm/issues/49)），我们可能要重新架构整个项目，并对大量内容进行分离和修改。

对于javascript，我们计划添加对indexedDB的支持。

我们希望我们的ORM能够尽快运行在所有平台上，但同时，我们希望能够等到kotlinx相关库的稳定版本发布后再正式开始我们的部分功能迁移，这其中需要做的主要工作如下：

- [ ] 使用`kotlinx.datetime`替换java的datetime（实际上我们在测试用例中已经通过value
  transformer实现了这一支持([CommonUtilTest](https://github.com/Kronos-orm/Kronos-orm/blob/main/kronos-testing/src/test/kotlin/com/kotlinorm/utils/CommonUtilTest.kt))
  ，youtrack相关问题：([Promote kotlinx-datetime to Beta](https://youtrack.jetbrains.com/issue/KT-64578)）

- [ ] 使用`kotlinx.io`处理默认的日志文件读写，youtrack相关问题：([Stabilize the kotlinx-io library](https://youtrack.jetbrains.com/issue/KT-71300))

- [ ] 寻找仅在jvm平台可以使用的反射的替代方案（实际上我们已经删除了99%的反射，但是部分功能如`Class.forName().kotlin`
  以及`KClass<T>.newInstance()`，我们还没有找到替代的解决方案，如果这一点不能实现，我们可能为部分平台舍弃**级联操作**、**自动类型转换**等功能。

您可以在[这里](https://github.com/Kronos-orm/Kronos-orm/issues/50)查看我们的最新进展，提供您宝贵的建议或PR贡献，我们期待与您沟通和交流。