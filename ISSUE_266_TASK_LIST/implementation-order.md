# Recommended Implementation Order

1. Keep Task 1 fixed as the public opt-in marker contract; change it only if implementation proves the marker cannot cover both duplicate names and Context shadowing.
2. Finish Tasks 2-4 together around one projection-name allocation contract: FIR diagnostics/model first, then IR/runtime/SQL consumers.
3. Task 7 is complete: the JOIN return type, recursive `FromSource` boundary, generated Selected propagation, nested JOINs, and JOIN-specific verification are green.
4. Finish Task 8 next on the common selected-query boundary produced by ordinary select, JOIN select, and derived select. Do not duplicate pagination implementations per query kind.
5. Implement Task 9 after Tasks 2-4 and 7 stabilize public projection shapes; IDEA must consume the same names rather than maintain its own allocator.
6. Execute Task 5 continuously: targeted allocator/checker tests, official diagnostics/box tests, core SQL/mapping tests, then all non-Codacy PR gates and coverage.
7. Complete Task 10 before the next Marketplace release. Inspect the signed ZIP and verify Marketplace installability on the installed IDEA 2026.2.
8. Finish Task 6 last so English/Chinese docs, migration examples, release notes, and IDEA docs describe the verified final API.
