# Projection caches must be owned by their FIR session

## Symptom and cause

Running the complete compiler test suite in one JVM retained successive FIR graphs and exhausted a 4 GiB heap. Two caches outlived their compilation: a global strong generated-member map, and a `WeakHashMap<FirSession, Map<ClassId, Model>>` whose values referred back to the key. Bound projection symbols refer to their FIR declaration, then its module data, then the session. Weak keys cannot break that strong value-to-key path.

## Fix

Register a private `FirSessionComponent` that owns projection models, generated members and the declaration generator. Keep the backend's cross-session lookup index as weak session keys **and weak component values**. A live session strongly owns its component, so models survive collection during compilation; retiring the session releases the entire graph. Invalidate alias-refinement member entries inside that same component.

Do not replace these caches with global weak model/member values: those can disappear while a compilation still needs them. Do not clear production caches from test teardown or split tests into separate JVMs to conceal retained compiler state.

## Verification

`ProjectionSessionLifetimeTest` generates real source-backed projection declarations and members. The model remains available after collection while the session is live; after the method and source environment retire, only a weak session reference remains and collection releases it. The test passes against the fixed compiler artifact and fails with “retained a retired FIR session” against the previous compiler artifact. The complete official Toolchain compiler suite passed 353 tests under the official Kover agent in one JVM with a 4 GiB heap, without skipped tests. The published coverage baseline gate passed at 96.44% lines and 74.18% branches. The real IDEA 2026.2.3/KEFS editor suite also passed all 26 contracts against the same complete compiler artifact, including loaded-class byte checks. Repeat both suites after changes because IDE sessions exercise the same ownership in a longer-lived process.
