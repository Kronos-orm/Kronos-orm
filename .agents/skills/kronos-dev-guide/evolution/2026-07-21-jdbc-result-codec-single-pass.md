# JDBC Result Mapping Must Separate Physical Reads From Logical Decode

## Symptom

JDBC result mapping carried only `Map<String, KType>`, so serialized fields and date metadata were unavailable at the result boundary. The column mapper then performed target-type coercion and KPojo mapping could deserialize the same value a second time.

## Confirmed solution

- Carry one `ResultColumnMetadata` per result label: complete logical `KType`, optional `Field`, derived storage, and label.
- Keep `KronosColumnMapperRegistry` limited to raw/vendor reads (LOB, SQLXML, PGobject, Oracle NUMBER/LONG workarounds). It must not select behavior by logical target type.
- Decode typed scalar, typed Map, and KPojo values exactly once through the core `ValueCodecRegistry` database entry point. An untyped Map or `Any` scalar with no metadata keeps the raw physical value and bypasses semantic codecs.
- Oracle LONG values may be read ahead, but the override must still pass through the same metadata-aware decode before assignment.
- Track mapped KPojo instances with an operation-local identity set. A reused factory result is an `InvalidKPojoFactoryResult`; do not use a global identity cache.
- JDBC argument binding receives prepared database values. Enum name conversion belongs to the codec layer; JDBC-native temporal and vendor binding remains a physical concern.

## Prevention

When adding a result shape, assert both the complete metadata contract and the final value. Add a test for raw Map/Any bypass, one codec invocation, vendor normalization, and fresh KPojo instances before changing the mapper.
