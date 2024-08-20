# {{ NgDocPage.title }}

`KronosNamingStrategy` is an interface used to define the conversion strategy for table and column names.

## Member functions:

### `fun db2k(name: String): String`

Convert database table/column names to kotlin class names/property names.

### `fun k2db(name: String): String`

Convert kotlin class names/property names to database table/column names.