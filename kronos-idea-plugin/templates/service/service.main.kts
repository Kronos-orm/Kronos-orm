#!/usr/bin/env kotlin

@file:Repository("https://central.sonatype.com/repository/maven-snapshots/")
@file:Repository("https://repo1.maven.org/maven2")
@file:DependsOn("com.kotlinorm:kronos-codegen:0.1.0")
@file:DependsOn("com.kotlinorm:kronos-jdbc-wrapper:0.1.0")
@file:DependsOn("com.mysql:mysql-connector-j:9.2.0")
@file:DependsOn("com.alibaba:druid:1.2.24")

import com.kotlinorm.codegen.KronosConfig.Companion.write
import com.kotlinorm.codegen.TemplateConfig.Companion.template
import com.kotlinorm.codegen.init
import java.time.LocalDateTime

private fun lineToHump(line: String): String {
    val str = line.trim()
    if (str.isEmpty()) return ""
    return str.split("_")
        .mapIndexed { index, word ->
            when {
                index == 0 || word.isEmpty() -> word
                else -> word.replaceFirstChar { it.uppercaseChar() }
            }
        }
        .joinToString("")
}

init("config.toml")

template {
    val pojoName = lineToHump(tableName)
    val pojoClassName = pojoName.replaceFirstChar { it.uppercaseChar() }
    +"package $packageName"
    +""
    +"import org.springframework.stereotype.Service"
    +"import com.kotlinorm.orm.upsert.upsert"
    +""
    +"// @author: Kronos-Codegen"
    +"// @date: ${LocalDateTime.now()}"
    +""
    +"@Service"
    +"class $className {"
    +"    fun create${pojoClassName}($pojoName: $pojoClassName){"
    +"        return $pojoName.upsert().on{ it.id }.execute()"
    +"    }"
    +""
    +"    fun create${pojoClassName}($pojoName: $pojoClassName){"
    +"        return $pojoName.upsert().on{ it.id }.execute()"
    +"    }"
    +"}"
}.write()