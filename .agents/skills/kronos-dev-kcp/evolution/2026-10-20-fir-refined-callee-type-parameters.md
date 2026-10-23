# Refined calls need a declaration-site symbol mapping

## Observed failure

Native projection compilation failed while mangling an ordinary generic select function with
`No container found for type parameter T`. The final plugin IR had already rewritten the projection
calls, so inspecting only those rewritten calls did not reveal the invalid parent.

The FIR call-refinement candidate creates a function copy with a refined return type while sharing the
original type/value parameter declarations. Converting that copied callable to IR can replace a shared
type parameter's parent with the refinement copy. Native then cannot find its declaration container.

## Verified correction

The IDE needs the refined callable signature for inference, completion and generated property types.
Restoring only the original callee fixes the backend owner but loses that signature during IDE analysis.

Keep a copy of the refined callable with `FirDeclarationOrigin.SubstitutionOverride.CallSite` and set
`originalForSubstitutionOverrideAttr` to the original declaration. The call resolves to this signature in
the frontend. Kotlin's FIR-to-IR `extractDeclarationSiteSymbol()` unwraps the call-site substitution to
the original declaration before assigning IR parameter owners.

Do not leave the compiler's initial shallow refinement copy as the final resolved callee, and do not
mutate the original generic function's return type. Both can associate shared parameter declarations
with a callable that does not own them.

The complete Kotlin Toolchain 2.4.21 plugin passes the real IDEA 2026.2.3 / KEFS 0.3.12 editor suite:
19 tests, including projection types, aliases, JOIN, subqueries, windows, collection literals,
completion and diagnostics. The fixture compares the loaded FIR, registrar and option-processor
class resources against the exact CLI JAR, so a cached older SNAPSHOT cannot satisfy this check.

When debugging mangling errors, inspect the type parameter's actual parent as well as the final call
target. Keep Native SQLite consumer execution alongside the editor suite when changing this mapping.
