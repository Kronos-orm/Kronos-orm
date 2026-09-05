/**
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

# Runtime table overrides must rebind qualifiers

## Symptom

Changing a KPojo instance's `__tableName` changed the DML/SELECT target table,
but generated predicates or JOIN projections still qualified columns with the
annotation table name. Pagination snapshots and UNION branches exposed the same
stale qualifier.

## Cause

Compiled `Field` metadata retains the declaration table name. Runtime metadata
already used `__tableName` for the physical table, but select bindings only
treated hand-built dynamic KPojo objects as remappable. JOIN planning also
resolved a qualifier by physical table name only.

## Prevention

Include declaration table names from field metadata in the source binding for
ordinary KPojo instances. JOIN contexts must resolve a table name through source
metadata and rewrite captured qualifiers only when the mapping is unambiguous;
self-join identity aliases remain authoritative. Keep regression coverage for
select, page snapshots, JOIN, UNION, DDL, and update/delete predicates.
