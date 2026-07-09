#!/usr/bin/env kotlin

@file:Repository("https://central.sonatype.com/repository/maven-snapshots/")
@file:Repository("https://repo1.maven.org/maven2")
@file:DependsOn("com.kotlinorm:kronos-codegen:0.1.2")
@file:DependsOn("com.kotlinorm:kronos-jdbc-wrapper:0.1.2")
@file:DependsOn("com.mysql:mysql-connector-j:9.2.0")
@file:DependsOn("com.alibaba:druid:1.2.24")

import com.kotlinorm.codegen.KronosConfig.Companion.write
import com.kotlinorm.codegen.TemplateConfig.Companion.template
import com.kotlinorm.codegen.init
import com.kotlinorm.codegen.kotlinType
import java.time.LocalDateTime


init("config.toml")

template {
    +"package $packageName"
    +""
    +imports.joinToString("\n") { "import $it" }
    +""
    +formatedComment
    +"// @author: Kronos-Codegen"
    +"// @date: ${LocalDateTime.now()}"
    +""
    +"@Table(name = \"$tableName\")"
    +indexes.toAnnotations()
    +"data class $className("
    fields.forEach { field ->
        field.annotations().forEach { annotation ->
            +"${indent(4)}$annotation"
        }
        +"${indent(4)}var ${field.name}: ${field.kotlinType}? = null,"
    }
    +"): KPojo"
}.write()
