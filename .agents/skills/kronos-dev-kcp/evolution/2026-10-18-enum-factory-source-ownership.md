# Enum factories belong to source declarations

## Observed failure

A Native test compilation referenced a public enum from its main KLIB. The external IR declaration was a
header with no enum-entry bodies, and the compiler rejected it as inaccessible. A shared commonMain nullable
dependency-enum query then exposed the same producer/consumer boundary on JVM. Rebuilding an enum factory
from that external header could produce an empty decoder even if accessibility were relaxed.

## Verified correction

Every accessible enum declaration contributes its generated factory in the source compilation that owns the
declaration. JVM source fragments retain that declaration ownership; Native emits only current-module source
factories. Consumer query and property metadata still include dependency type descriptors, nullable types and
enum keys. The producer graph supplies the actual enum factory through JVM ServiceLoader or explicit Native
registration. A required private local enum still receives the original diagnostic.

Declaration-time production matters for incremental compilation: adding a new query that refers to an
unchanged enum must not depend on rebuilding the enum's source fragment. Producing only when a current query
uses the enum leaves that unchanged producer incomplete.

The four-module consumer verified actual JVM and Native enum decoding from shared commonMain code,
main-to-test factory reuse, incremental query addition/removal, private-source diagnostics on both platforms,
and published metadata-manifest consumption. It also checked complete static descriptors so working enum
factories cannot conceal descriptor drift. Do not treat an empty external stub as a source-owned enum or
relax private-source diagnostics to make cross-module compilation pass.
