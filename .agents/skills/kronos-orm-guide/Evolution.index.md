# Evolution Index

Use this index before opening `Evolution.md`. Match by API, symptom, SQL behavior, database type, or error message, then read only the matching entry with a targeted search such as:

```powershell
Select-String -Path .agents/skills/kronos-orm-guide/Evolution.md -Pattern "ENTRY TITLE OR UNIQUE ERROR" -Context 0,18
```

If no entry matches, do not open the full evolution log; continue with the relevant guide section or reference file. This user-facing ORM guide currently has no recorded pitfalls.

When an ORM usage issue is verified and should be remembered:

1. Add a full entry to `Evolution.md`.
2. Add one concise index row here with keywords and the entry title.

| Area | Keywords / Symptoms | Evolution Entry |
|------|---------------------|-----------------|
