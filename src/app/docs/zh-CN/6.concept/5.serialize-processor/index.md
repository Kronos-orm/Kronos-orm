{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

`KronosSerializeResolver`是Kronos定义的序列化解析器接口，用于字符串和Kotlin实体类之间的序列化和反序列化转换。

**成员函数：**

## {{ $.title("deserialize") }} 将字符串反序列化为指定的KClass

参数：
{{ $.params([
['serializedStr', '要反序列化的字符串', 'String'],
['kClass', '要反序列化的KClass', 'KClass<*>']
])}}

返回：

{{ $.title("Any") }} 反序列化后的对象

## {{ $.title("serialize") }} 将对象序列化为字符串

参数：
{{ $.params([
['obj', '要序列化的对象', 'Any']
])}}

返回：

{{ $.title("String") }} 序列化后的字符串


