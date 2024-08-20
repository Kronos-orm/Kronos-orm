# {{ NgDocPage.title }}

`KronosSerializeResolver` is a serialization resolver interface defined by Kronos, used for serialization and deserialization conversion between strings and Kotlin entity classes.

## Member functions:

### deserialize
`fun <T> deserialize(serializedStr: String, kClass: KClass<*>): T`

Used to deserialize a string to the specified type T

### deserializeObj
`fun deserializeObj(serializedStr: String, kClass: KClass<*>): Any`

Used to deserialize a string to the specified KClass

### serialize
`fun serialize(obj: Any): String`

Used to serialize a Kotlin object to a String