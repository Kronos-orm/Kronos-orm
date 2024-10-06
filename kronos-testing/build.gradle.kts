plugins {
    id("kronos.jvm")
    id("com.kotlinorm.kronos-gradle-plugin")
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(kotlin("reflect"))
    testImplementation(project(":kronos-core"))
    testImplementation(project(":kronos-logging"))
    testImplementation(project(":kronos-jdbc-wrapper"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testImplementation("org.apache.commons:commons-dbcp2:2.12.0")

    // mysql
    testImplementation("com.mysql:mysql-connector-j:8.4.0")
    // sqlite
    testImplementation("org.xerial:sqlite-jdbc:3.46.0.0")
    // SQLServer
    testImplementation("com.microsoft.sqlserver:mssql-jdbc:12.7.0.jre8-preview")
    // postgresql
    testImplementation("org.postgresql:postgresql:42.7.3")
    // oracle
    testImplementation("com.oracle.database.jdbc:ojdbc8:23.2.0.0")
    testImplementation("com.google.code.gson:gson:2.11.0")

    //after additional testing, wrapper in spring-x 6.x also works, we will provide additional examples
    testImplementation("org.springframework:spring-jdbc:5.3.37")
    testImplementation("org.springframework:spring-tx:5.3.37")
    testImplementation("org.springframework:spring-beans:5.3.37")
    testImplementation("org.springframework:spring-core:5.3.37")
}
