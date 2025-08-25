# 6. Transformers in Detail

This section explains each transformer’s responsibility, trigger conditions, and the core IR rewrites so you can navigate dumps and extend the plugin effectively.

## 6.1 KronosParserTransformer (Dispatcher)

- Role: global IR visitor and dispatcher.
- Triggers: visitCall / visitFunctionNew / visitClassNew / visitClassReference / visitConstructorCall.
- Key duties:
  - KPojo collection: wherever a type argument/class/ref/constructor has a KPojo supertype, collect it into `KClassCreatorUtil.kPojoClasses`.
  - Init hooks: for calls annotated with `@KronosInit`, capture the function expression into `KClassCreatorUtil.initFunctions` for end-of-compilation replay.
  - Type fixes: match `TypedQuery` and `SelectFrom*` calls (see `utils.KQueryTaskUtil`’s `fqNameOfTypedQuery` and `fqNameOfSelectFromsRegexes`) and call `updateTypedQueryParameters` to inject `isKPojo` and `superTypes`.
  - KTable routing: dispatch bodies to concrete KTable* transformers based on extension receiver FQ name.

## 6.2 KTableParserForSelectTransformer (Field selection)

- Trigger: function body whose extension receiver is `KTableForSelect<*>`.
- Core logic:
  - Inject `addFieldList(irFunction, irReturn)` before the `return` (see `kTableForSelect.KTableForSelectUtil`).
  - `addFieldList/collectFields` parses DSL patterns:
    - Property access: `it.username`, `it.password` → `FieldSymbol` entries;
    - Alias: `it.createTime.as_("time")` → aliased `FieldSymbol`;
    - Arithmetic/aggregations: `a + b`, `+x`, `-User::class` (exclude columns) via IR origin and function names;
    - Constants/SQL snippets: constants become `Field` with type `CUSTOM_CRITERIA_SQL`.
  - Effect: collect SELECT field list at compile time without changing semantics.

## 6.3 KTableParserForSetTransformer (Assignments)

- Trigger: extension receiver is `KTableForSet<*>`.
- Responsibilities:
  - Parse entries like `set { it.username to "Tom"; it.age to 18 }`;
  - Build internal update expressions and inject them into IR;
  - Construction is encapsulated in `kTableForSet.KTableForSetUtil`.

## 6.4 KTableParserForConditionTransformer (Predicates)

- Trigger: extension receiver is `KTableForCondition<*>`.
- Core logic:
  - Use `updateCriteriaIr` and `buildCriteria` (see `kTableForCondition.KTableForConditionUtil`) to convert boolean expression trees into `CriteriaIR`:
    - Supports `== != > >= < <= in notIn between like isNull isNotNull and or not`;
    - Handles nesting, no-value strategies, and annotation effects (cascade/ignore/serialize);
    - Extract column/table/value/children from IR nodes (IrCall/IrWhen/etc.).
  - The built `CriteriaIR` is assigned to the receiver for runtime use.

## 6.5 KTableParserForSortReturnTransformer (Sorting)

- Trigger: extension receiver is `KTableForSort<*>`.
- Core logic:
  - Recognize `it.username.asc()` / `it.createTime.desc()`;
  - Build key + direction and inject into the sort collection;
  - IR construction is in `kTableForSort.KTableForSortUtil`.

## 6.6 KTableParserForReferenceTransformer (References)

- Trigger: extension receiver is `KTableForReference<*>`.
- Core logic:
  - Recognize cross-table field references, aliases, and join-related semantics;
  - Record reference info in IR to bind table aliases and fields correctly;
  - Utilities live in `kTableForReference.KTableForReferenceUtil`.

## 6.7 End-of-compilation: KClass mapping generation

- Replay functions recorded in `KClassCreatorUtil.initFunctions`;
- Use `buildKClassMapper` to generate the unified `kClassCreator` mapping logic:
  - Match collected `kPojoClasses` and return instances accordingly or null;
  - Enables `KClass<KPojo>` → `KPojo` lookup at runtime.

## 6.8 Cooperation with TypedQuery/SelectFrom*

- When a query execution call is seen, `KronosParserTransformer` calls `updateTypedQueryParameters` first:
  - Inject `isKPojo` flag and `superTypes` (as strings) for downstream logic;
  - This step is orthogonal to KTable* transforms, typically within the same function body.
