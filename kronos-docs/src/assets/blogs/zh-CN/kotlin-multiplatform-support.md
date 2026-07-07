# Kronos对于Kotlin Multiplatform的支持与计划

<center>
<img src="/assets/images/features/img-1.png" width="300"/>
</center>

--------

感谢阅读这篇文章。Kronos 当前可复制运行的 ORM 教程链路聚焦 JVM 和 JDBC，同时产品方向继续包含 Kotlin Multiplatform、mobile、Android 和 JavaScript 场景。

如果当前项目需要已经在移动端、JS 等多平台运行的 ORM 框架，也可以参考以下库：

- [SQLLin](https://github.com/ctripcorp/SQLlin)
- [sqlDelight](https://github.com/sqldelight/sqldelight)

Kronos设计被用于后端和移动端开发，且支持多种数据库，截止目前，我们已经初步完成了jvm平台的ORM功能，开发过程中由于非JVM平台标准库的限制，我们暂时将多平台的支持列入到计划中。

我们计划在`0.2`或者`0.3`版本开始添加多平台支持，我们可能会先为android平台及sqlite数据库提供支持（相关问题号：[issue#49](https://github.com/Kronos-orm/Kronos-orm/issues/49)），我们可能要重新架构整个项目，并对大量内容进行分离和修改。

对于javascript，我们计划添加对indexedDB的支持。

我们希望我们的ORM能够尽快运行在所有平台上，但同时，我们希望能够等到kotlinx相关库的稳定版本发布后再正式开始我们的部分功能迁移，这其中需要做的主要工作如下：

1. [ ] 使用`kotlinx.datetime`替换java的datetime（实际上我们在测试用例中已经通过valuetransformer实现了这一支持([KotlinXDateTimeTransformer](https://github.com/Kronos-orm/Kronos-orm/blob/main/kronos-testing/src/test/kotlin/com/kotlinorm/utils/KotlinXDateTimeTransformer.kt))，youtrack相关问题：([Promote kotlinx-datetime to Beta](https://youtrack.jetbrains.com/issue/KT-64578)）
2. [ ] 使用`kotlinx.io`处理默认的日志文件读写，youtrack相关问题：([Stabilize the kotlinx-io library](https://youtrack.jetbrains.com/issue/KT-71300))
3. [x] 提供 Kronos 模型操作需要的 `KClass<KPojo>` 动态实例化路径。使用方式见 [KPojo 的动态实例化](/documentation/zh-CN/advanced/kpojo-dynamic-instantiate)

您可以在[这里](https://github.com/Kronos-orm/Kronos-orm/issues/50)跟进路线图讨论，提供您宝贵的建议或PR贡献，我们期待与您沟通和交流。
