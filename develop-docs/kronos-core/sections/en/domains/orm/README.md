# ORM Clause Modeling

This domain summarizes common conventions across ClauseInfo types. See details under features/*:
- SelectClauseInfo (features/dsl-select)
- InsertClauseInfo (features/dsl-insert)
- UpdateClauseInfo (features/dsl-update)
- DeleteClauseInfo (features/dsl-delete)
- JoinClauseInfo (features/join)
- CascadeInsertClause/NodeOfKPojo (features/cascade)

Conventions:
- ClauseInfo carries data only; it does not build the final SQL by itself;
- Final dialect SQL is assembled by the execution layer (wrappers + function builders, etc.);
- Naming, no-value, and common strategies apply at different stages (see domains/mechanisms/*).
