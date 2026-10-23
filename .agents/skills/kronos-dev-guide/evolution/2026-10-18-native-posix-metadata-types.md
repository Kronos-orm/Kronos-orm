# Native metadata compilation checks platform C interop types

## Symptoms

- `compileLinuxMainKotlinMetadata` rejects `nativeHeap.free(pointer)` because the overload expects `NativePtr`.
- `pthread_mutexattr_settype` rejects the Linux `PTHREAD_MUTEX_RECURSIVE` constant as `UInt` when the argument expects `Int`.

## Confirmed fixes

- Import `kotlinx.cinterop.rawValue` and free the allocated pointer's `.rawValue` in the native cleaner.
- Pass `PTHREAD_MUTEX_RECURSIVE.toInt()` to `pthread_mutexattr_settype`.
- Verify standalone common/native/Linux metadata compilation. A leaf macOS compile does not check the Linux commonized API signatures.

## Evidence

The isolated archived-head metadata/JVM publication completed after the free-pointer correction (`/tmp/kronos-head-publication-freelockfix.log`). Current-source core common/native/Linux metadata compilation passed after the mutex constant correction (`/tmp/kronos-sqlite-current-orm3.log`). The latter build then stopped on uncached offline test dependencies, after the platform compilation tasks had succeeded.
