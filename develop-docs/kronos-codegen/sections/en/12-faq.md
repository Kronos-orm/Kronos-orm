# 12. FAQ (Expanded)

## DB connection issues
- Ensure driver on classpath and credentials are correct.
- If using default BasicDataSource, verify pool settings (URL, driverClassName, username/password).

## Wrapper constructor not found
- `createWrapper` tries `(dataSource::class.java)` first, then `(javax.sql.DataSource)`.
- Implement one of these constructors in custom wrappers.

## Package name inference oddities
- Provide `output.packageName` explicitly for full control.
- If relying on inference, ensure `targetDir` contains `main/kotlin/`.

## Comment wrapping
- Adjust `output.tableCommentLineWords`.
- For non-Latin scripts without spaces, consider custom wrappers or pre-processed comments.
