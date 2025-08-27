# 1. Module Overview

kronos-compiler-plugin is a Kotlin compiler plugin targeting the K2 frontend. Its goal is to understand and enhance Kronos DSL at compile time:
- Identify and track all types implementing the KPojo interface (entities/projections, etc.);
- Translate KTableForSelect/Set/Condition/Sort/Reference DSL into more explicit IR calls (such as addFieldList, conditional composition), resulting in a more stable and performant runtime structure;
- Fix/complete type parameters for TypedQuery and SelectFrom* APIs, reducing issues caused by erasure or insufficient inference;
- Provide a deferred initialization mechanism via @KronosInit: users annotate initializer lambdas anywhere, and the plugin replays them at the end of compilation to generate KClass mappings.

Difference from code generation (kronos-codegen):
- codegen: "generates" source files from DB metadata;
- compiler-plugin: performs IR-level semantic enhancement and rewriting on the user’s Kotlin code during compilation.

Use cases:
- Enforce stronger static guarantees and clearer intermediate representation;
- Reduce runtime reflection/parsing overhead;
- Provide compile-time rules for unified DSL usage within a team.
