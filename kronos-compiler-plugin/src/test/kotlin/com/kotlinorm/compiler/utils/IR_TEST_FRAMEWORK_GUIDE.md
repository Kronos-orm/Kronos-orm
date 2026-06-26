# IR 测试框架使用指南

## 概述

`IrTestFramework` 是一个通用的 IR 测试框架，用于测试 Kotlin 编译器插件的 IR 相关功能。它完全替代了之前的 JSON debug 机制，提供更直接、更灵活的测试方式。

## 核心概念

### 1. IrTestContext
包含编译结果和收集的 IR 元素的上下文对象：
- `pluginContext`: IrPluginContext，用于访问符号和类型系统
- `collector`: IrCollector，收集的 IR 元素（调用、表达式等）
- `moduleFragment`: IrModuleFragment，编译后的模块
- `exitCode`: 编译结果状态

### 2. IrCollector
自动收集编译过程中的各种 IR 元素：
- `propertyAccesses`: 属性访问表达式（`obj.property`）
- `propertyReferences`: 属性引用表达式（`Class::property`）
- `plusCalls`: 加法表达式
- `minusCalls`: 减法表达式
- `constants`: 常量表达式
- `getValues`: getValue 表达式
- `allCalls`: 所有函数调用
- `allExpressions`: 所有表达式

## 使用场景

### 场景 1: 测试 IR 收集和表达式分析

**适用于**: 测试 IR 转换、表达式分析、代码生成等不需要访问符号的场景

**API**: `IrTestFramework.compile()`

**示例**:
```kotlin
@Test
fun `test property access analysis`() {
    val context = IrTestFramework.compile(
        IrTestFramework.source("Test.kt", """
            package test
            
            fun testPropertyAccess() {
                val user = User(1, "John")
                val name = user.name  // 属性访问
            }
        """)
    )

    context.assertSuccess()
    
    // 验证收集到的 IR 元素
    assertTrue(context.collector.propertyAccesses.size >= 1)
    
    val propertyNames = context.collector.propertyAccesses.mapNotNull { call ->
        call.symbol.owner.correspondingPropertySymbol?.owner?.name?.asString()
    }
    assertTrue(propertyNames.contains("name"))
}
```

**特点**:
- 编译完成后访问 `context.collector` 获取收集的 IR 元素
- 适合测试 IR 结构、表达式类型等
- 不需要在 IR 生成阶段执行代码

### 场景 2: 测试符号解析和 Utils 函数

**适用于**: 测试符号解析、注解读取、类型判断等需要在 IR 生成阶段访问 IrPluginContext 的场景

**API**: `IrTestFramework.testInIrGeneration()`

**示例**:
```kotlin
@Test
fun `test symbol resolution`() {
    IrTestFramework.testInIrGeneration(
        IrTestFramework.source("Test.kt", """
            package test
            import com.kotlinorm.interfaces.KPojo
            data class User(val id: Int) : KPojo
        """)
    ) { ctx, pluginContext ->
        // 在 IR 生成阶段执行测试
        with(pluginContext) {
            // 测试符号解析
            val symbol = kPojoClassSymbol
            assertNotNull(symbol)
            
            // 测试类型判断
            val userClass = ctx.findClass("User")
            assertNotNull(userClass)
            
            val userType = ctx.getDefaultType(userClass)
            assertTrue(userType.isKPojoType())
        }
    }
}
```

**特点**:
- 在 IR 生成阶段执行测试代码
- 可以访问 `pluginContext` 进行符号解析
- 可以使用 `ctx` 查找类、函数、属性等
- 适合测试 Symbols.kt、AnnotationUtils.kt 等工具函数

## API 参考

### IrTestFramework.compile()

```kotlin
fun compile(
    vararg sources: SourceFile,
    customAction: ((IrPluginContext, IrModuleFragment) -> Unit)? = null
): IrTestContext
```

编译代码并返回测试上下文。

**参数**:
- `sources`: 源代码文件（使用 `IrTestFramework.source()` 创建）
- `customAction`: 可选的自定义操作，在 IR 生成时执行

**返回**: `IrTestContext` 包含编译结果和收集的 IR 元素

### IrTestFramework.testInIrGeneration()

```kotlin
fun testInIrGeneration(
    vararg sources: SourceFile,
    testAction: (IrTestContext, IrPluginContext) -> Unit
): IrTestContext
```

在 IR 生成阶段执行测试。

**参数**:
- `sources`: 源代码文件
- `testAction`: 测试操作，接收 `IrTestContext` 和 `IrPluginContext`

**返回**: `IrTestContext` 包含编译结果

### IrTestFramework.source()

```kotlin
fun source(name: String, content: String): SourceFile
```

创建源文件的便捷方法。

**参数**:
- `name`: 文件名（如 "Test.kt"）
- `content`: 文件内容（会自动 trimIndent）

### IrTestContext 辅助方法

```kotlin
// 断言编译成功
fun assertSuccess()

// 断言编译失败
fun assertFailure()

// 查找类
fun findClass(name: String): IrClass?

// 查找函数
fun findFunction(name: String): IrFunction?

// 获取所有 KPojo 类
fun getKPojoClasses(): List<IrClass>

// 获取类的 defaultType
fun getDefaultType(irClass: IrClass): IrType

// 获取类的 properties
fun getProperties(irClass: IrClass): List<IrProperty>
```

## 最佳实践

### 1. 选择正确的 API

- **测试 IR 收集**: 使用 `compile()`
- **测试符号解析**: 使用 `testInIrGeneration()`

### 2. 使用标准测试类

框架提供了 `standardUserClass`，可以直接使用：

```kotlin
val context = IrTestFramework.compile(
    IrTestFramework.standardUserClass,
    IrTestFramework.source("Test.kt", """
        package test
        fun test() {
            val user = User(1, "John", "john@example.com", 30, "secret")
        }
    """)
)
```

### 3. 处理 UnsafeDuringIrConstructionAPI

在测试类上添加 `@OptIn` 注解：

```kotlin
@OptIn(ExperimentalCompilerApi::class, UnsafeDuringIrConstructionAPI::class)
class MyTest {
    // ...
}
```

### 4. 验证编译结果

始终先验证编译是否成功：

```kotlin
val context = IrTestFramework.compile(...)
context.assertSuccess()  // 或 context.assertFailure()
```

## 迁移指南

### 从 JSON Debug 机制迁移

**旧方式** (使用 JSON debug):
```kotlin
val result = IRVerificationUtils.compileWithPlugin(
    source("Test.kt", "..."),
    debug = true
)
val debugLog = result.debugLog
// 解析 JSON 查找信息
```

**新方式** (使用 IrTestFramework):
```kotlin
IrTestFramework.testInIrGeneration(
    IrTestFramework.source("Test.kt", "...")
) { ctx, pluginContext ->
    // 直接测试，无需 JSON
    with(pluginContext) {
        val symbol = kPojoClassSymbol
        assertNotNull(symbol)
    }
}
```

### 从自定义 Extension 迁移

**旧方式** (自定义 Extension):
```kotlin
class MyTestExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        // 测试代码
    }
}

val result = IRVerificationUtils.compileWithCustomExtension(
    source("Test.kt", "..."),
    extension = MyTestExtension()
)
```

**新方式** (使用 IrTestFramework):
```kotlin
IrTestFramework.testInIrGeneration(
    IrTestFramework.source("Test.kt", "...")
) { ctx, pluginContext ->
    // 测试代码直接写在这里
}
```

## 示例

### 完整示例 1: 测试字段分析

```kotlin
@OptIn(ExperimentalCompilerApi::class, UnsafeDuringIrConstructionAPI::class)
class FieldAnalysisTest {
    @Test
    fun `test property access analysis`() {
        val context = IrTestFramework.compile(
            IrTestFramework.standardUserClass,
            IrTestFramework.source("Test.kt", """
                package test
                
                fun testPropertyAccess() {
                    val user = User(1, "John", "john@example.com", 30, "secret")
                    val name = user.name
                    val email = user.email
                }
            """)
        )

        context.assertSuccess()
        
        // 验证属性访问被收集
        assertTrue(context.collector.propertyAccesses.size >= 2)
        
        // 验证属性名称
        val propertyNames = context.collector.propertyAccesses.mapNotNull { call ->
            call.symbol.owner.correspondingPropertySymbol?.owner?.name?.asString()
        }
        assertTrue(propertyNames.contains("name"))
        assertTrue(propertyNames.contains("email"))
    }
}
```

### 完整示例 2: 测试符号解析

```kotlin
@OptIn(ExperimentalCompilerApi::class, UnsafeDuringIrConstructionAPI::class)
class SymbolsTest {
    @Test
    fun `test all class symbols can be resolved`() {
        IrTestFramework.testInIrGeneration(
            IrTestFramework.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                data class User(val id: Int) : KPojo
            """)
        ) { ctx, pluginContext ->
            with(pluginContext) {
                // 测试所有类符号
                val classSymbols = mapOf(
                    "KPojo" to kPojoClassSymbol,
                    "Field" to fieldClassSymbol,
                    "Criteria" to criteriaClassSymbol
                )
                
                classSymbols.forEach { (name, symbol) ->
                    assertNotNull(symbol, "$name symbol should be resolved")
                }
            }
        }
    }
}
```

### 完整示例 3: 测试类型判断

```kotlin
@OptIn(ExperimentalCompilerApi::class, UnsafeDuringIrConstructionAPI::class)
class TypeJudgmentTest {
    @Test
    fun `test isKPojoType function`() {
        IrTestFramework.testInIrGeneration(
            IrTestFramework.source("Test.kt", """
                package test
                import com.kotlinorm.interfaces.KPojo
                
                data class User(val id: Int) : KPojo
                data class NotKPojo(val id: Int)
            """)
        ) { ctx, pluginContext ->
            val userClass = ctx.findClass("User")!!
            val notKPojoClass = ctx.findClass("NotKPojo")!!
            
            with(pluginContext) {
                val userType = ctx.getDefaultType(userClass)
                assertTrue(userType.isKPojoType())
                
                val notKPojoType = ctx.getDefaultType(notKPojoClass)
                assertFalse(notKPojoType.isKPojoType())
            }
        }
    }
}
```

## 常见问题

### Q: 什么时候使用 `compile()` vs `testInIrGeneration()`?

**A**: 
- 使用 `compile()`: 当你只需要收集 IR 元素（表达式、调用等）时
- 使用 `testInIrGeneration()`: 当你需要访问符号、类型系统或测试 utils 函数时

### Q: 为什么我的测试中 `pluginContext` 不可用?

**A**: 如果你使用 `compile()`，`pluginContext` 只在 `customAction` 中可用。如果需要在测试代码中访问，请使用 `testInIrGeneration()`。

### Q: 如何处理 `UnsafeDuringIrConstructionAPI` 警告?

**A**: 在测试类上添加 `@OptIn(UnsafeDuringIrConstructionAPI::class)` 注解。

### Q: 可以同时测试多个源文件吗?

**A**: 可以，传递多个 `SourceFile` 参数：
```kotlin
IrTestFramework.compile(
    IrTestFramework.source("User.kt", "..."),
    IrTestFramework.source("Post.kt", "..."),
    IrTestFramework.source("Test.kt", "...")
)
```

## 总结

`IrTestFramework` 提供了两个核心 API：
1. **`compile()`**: 用于测试 IR 收集和表达式分析
2. **`testInIrGeneration()`**: 用于测试符号解析和 utils 函数

选择正确的 API 可以让测试更简洁、更易维护。完全替代了之前的 JSON debug 机制，提供更直接的测试方式。
