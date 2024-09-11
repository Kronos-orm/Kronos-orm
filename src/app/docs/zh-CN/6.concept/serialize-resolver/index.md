`KronosSerializeResolver`是Kronos定义的序列化解析器接口，用于字符串和Kotlin实体类之间的序列化和反序列化转换。

## 成员函数：

### deserialize
`fun <T> deserialize(serializedStr: String, kClass: KClass<*>): T`

用于将字符串反序列化为指定的类型T

### deserializeObj
`fun deserializeObj(serializedStr: String, kClass: KClass<*>): Any`

用于将字符串反序列化为指定的KClass

### serialize
`fun serialize(obj: Any): String`

用于将Kotlin对象序列化为String
