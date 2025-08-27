# 6. Usage Examples (Expanded)

Below are patterns you can follow to integrate codegen in tests, scripts, or build tools.

## Minimal script

```kotlin
fun main() {
  init("config.toml")
  TemplateConfig.template {
    +"package $packageName"
    +""
    +imports.joinToString("\n") { "import $it" }
    +""
    +formatedComment
    +"@Table(name = \"$tableName\")"
    +indexes.toAnnotations()
    +"data class $className("
    fields.forEach { f ->
      f.annotations().forEach { +"    $it" }
      +"    var ${f.name}: ${f.kotlinType}? = null,"
    }
    +"): KPojo"
  }.write()
}
```

## Testing pattern

- See `kronos-testing/src/test/kotlin/com/kotlinorm/codegen/CodeGenerateTest.kt` for a full, assertive example that checks every output line.

## Tips

- To keep diffs stable, avoid including timestamps unless needed.
- Use `tableCommentLineWords` to keep header comments readable.
