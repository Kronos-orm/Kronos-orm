# Kronos Support and Plans for Kotlin Multiplatform

<center>

<img src="/assets/images/features/img-1.png" width="300"/>

</center>

--------

Thank you very much for reading this article, but unfortunately, Kronos currently does not support any platform other than JVM.

If you urgently need an ORM framework that can run on multiple platforms such as mobile and JS, we may not be the best choice at the moment. Perhaps the following libraries can meet your needs:

- [SQLLin](https://github.com/ctripcorp/SQLlin)

- [sqlDelight](https://github.com/sqldelight/sqldelight)

Kronos is designed for backend and mobile development and supports various databases. Up to now, we have initially completed the ORM functionality for the JVM platform. Due to limitations of the non-JVM platform standard library during development, we have temporarily put the multiplatform plans on our roadmap.

We plan to start adding multiplatform support in version `0.2` or `0.3`, and we may first provide support for the Android platform and SQLite database (related issue number: [issue#49](https://github.com/Kronos-orm/Kronos-orm/issues/49)). We may need to restructure the entire project and separate and modify a large amount of content.

For JS, we plan to add support for indexedDB.

We hope that our ORM can run on all platforms as soon as possible, and we hope to officially start migrating some of our functionalities after the stable version of the kotlinx-related libraries is released. The main tasks to be done include:

1. [ ] Replace Java's datetime with kotlinx.datetime (in fact, we have already implemented this support through value transformer in our test cases ([KotlinXDateTimeTransformer](https://github.com/Kronos-orm/Kronos-orm/blob/main/kronos-testing/src/test/kotlin/com/kotlinorm/utils/KotlinXDateTimeTransformer.kt))

, related YouTrack issue: ([Promote kotlinx-datetime to Beta](https://youtrack.jetbrains.com/issue/KT-64578))

2. [ ] Use kotlinx.io to handle the default log file read and write, related YouTrack issue: ([Stabilize the kotlinx-io library](https://youtrack.jetbrains.com/issue/KT-71300))

3. [x] Looking for alternatives to reflection that can be used only on the JVM platform (we implemented the dynamic instantiation of `KClass<KPojo>` without relying on reflection using a compiler plugin, related commit: [Commit 2499037](https://github.com/Kronos-orm/Kronos-orm/commit/2499037008d6affe4495142f2a907be4a85f182b)), for more information, please refer to [Dynamic Instantiation of KPojo](/#/documentation/en/concept/kpojo-dynamic-instantiate).

and `KClass<T>.newInstance()`, we have not yet found alternative solutions. If this cannot be achieved, we may have to give up features such as **chaining operations** and **automatic type conversion** for some platforms.

You can check our latest progress [here](https://github.com/Kronos-orm/Kronos-orm/issues/50), and we welcome your valuable suggestions or PR contributions. We look forward to communicating and interacting with you.
