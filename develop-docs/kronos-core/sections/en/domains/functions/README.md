# Function System & Transformer

The function system maps function-call-like DSL expressions into dialect SQL fragments for different databases.

- Core components:
  - FunctionManager: register/select FunctionBuilder;
  - FunctionBuilder (com.kotlinorm.interfaces):
    - support(field: FunctionField, db: DBType): Boolean
    - transform(field, dataSource): String
  - FunctionField (com.kotlinorm.beans.dsl): describes function name, args, alias, etc.
- Built-in builders:
  - PolymerizationFunctionBuilder (aggregation)
  - MathFunctionBuilder (math)
  - StringFunctionBuilder (string)
- Extension points:
  - FunctionManager.registerFunctionBuilder() to add custom builders for cross-db function adaptation;
- Relation to compiler Transformer:
  - At compile-time (kronos-compiler-plugin), DSL function expressions are turned into FunctionField;
  - At runtime, FunctionManager selects the proper builder by dbType to generate dialect SQL.
