# Exceptions & i18n

Diagram:
```mermaid
flowchart LR
  A[Runtime check] --> B[exceptions/*]
  B --> C[Throw]
  C --> D[i18n Noun message]
```

What it does:
- Define and throw common exceptions (NoDataSourceException, InvalidParameterException, etc.)
- Provide message literals via i18n Noun.

Why this design:
- Decouple error type and message; internationalization-friendly.

Example:
```
if (dataSource == null) throw NoDataSourceException(Noun.noDataSourceMessage)
```
