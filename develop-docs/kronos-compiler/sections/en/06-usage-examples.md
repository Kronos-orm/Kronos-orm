# 6. Usage Examples

A minimal example showing semantic changes and debug output before/after compilation.

## 6.1 Example Code

```kotlin
// A simple entity (KPojo is an interface; imports omitted)
class User(val id: Long, val username: String): KPojo

// An initializer function (for illustration)
@KronosInit
fun initKronos() = { /* initialize mappings, registrations, etc. */ }

// Example with KTableForSelect (pseudo API just to show semantics)
fun <T: KPojo> sampleSelect(block: (KTableForSelect<T>.(T) -> Unit)) {
    // ...
}

fun demo() {
    sampleSelect<User> { it ->
        it.username // + it.id.as_("uid"), etc.
    }
}
```

## 6.2 What happens at compile time?

- KronosParserTransformer collects that the generic arg `User` is a KPojo subclass;
- Before the lambda body returns in `sampleSelect`, IR is injected:
  - `addFieldList(...)` translates `it.username` (and alias calls) to internal field descriptors;
- If `@KronosInit` is present, the plugin replays `initKronos` at the end of compilation to generate KClass mappings.

## 6.3 Enable IR dump and inspect

- In Gradle/Maven add: `-P plugin:kronos-compiler-plugin:debug=true`;
- After compilation, check directory `build/tmp/kronosIrDebug`:
  - Each source file has a corresponding IR-like text file;
  - In the `sampleSelect` function body, you can see the injected IR fragment;
  - Useful to verify rewrite behavior matches expectations.
