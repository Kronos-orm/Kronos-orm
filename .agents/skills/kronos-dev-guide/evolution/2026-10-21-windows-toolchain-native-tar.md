# Windows Toolchain wrapper must resolve the native system tar

Confirmed on the `windows-2025` JVM and MinGW jobs at commit `93f1f6d49d8135bce2985a6bc3a4743e08996143`: both reached the official Kotlin Toolchain 0.13.0 distribution bootstrap and failed with `tar (child): Cannot connect to C: resolve failed`, followed by `/usr/bin/tar: Child returned status 128`.

The workflows use Bash, whose Git tools can precede Windows system utilities in the inherited PATH. The official `kotlin.bat` invokes PowerShell and passes an absolute Windows temporary archive path to `tar -xzf`. Git's GNU tar treats the `C:` prefix as a remote archive host. Converting the wrapper to CRLF and invoking it through `COMSPEC /d /c` addresses batch execution, but does not change which tar PowerShell resolves.

`scripts/common.main.kts::toolchainEnvironment()` prepends `%SystemRoot%\System32` to the child PATH on Windows and requires its `tar.exe` to exist. Both the bootstrap and subsequent Toolchain invocations, including `scripts/quality.main.kts`, share this environment. Non-Windows PATH remains unchanged. The watchOS SDK provisioning helper is macOS-only and does not share this Windows failure.

Evidence: GitHub check-run annotations [JVM Windows](https://github.com/Kronos-orm/Kronos-orm/runs/118787381595) and [MinGW](https://github.com/Kronos-orm/Kronos-orm/runs/118787381736). The following CI head confirmed that distribution extraction passed, then exposed the separate checksum command conflict below.

## Git/MSYS checksum script conflicts with the bundled BusyBox launcher

The subsequent Windows checks [JVM](https://github.com/Kronos-orm/Kronos-orm/runs/118795721755) and [MinGW](https://github.com/Kronos-orm/Kronos-orm/runs/118795721594) downloaded the Toolchain successfully, then failed while provisioning its Azul Java runtime: `Can't find shasum on PATH.` and an empty actual checksum. This message does not establish a corrupt download.

The official `kotlin.bat` intentionally launches the distribution's `busybox64u.exe sh bin/launcher.sh`; there is no alternate Windows-only launcher script to select. `launcher.sh::check_sha()` prefers any available `shasum` over `sha256sum`, so a Git/MSYS script in the inherited PATH can prevent the fallback. The actual official 0.13.0 distribution contains the `sha256sum` applet; its bundled BusyBox source configurations enable `CONFIG_SHA256SUM`, `CONFIG_FEATURE_PREFER_APPLETS`, and `CONFIG_FEATURE_SH_STANDALONE`.

The Windows child PATH therefore also removes directories containing `msys-2.0.dll` or a `shasum` provider (including `.bat`, `.cmd`, and `.exe`). The diagnostic is Perl's script search failure; filtering only the MSYS DLL would leave other Perl installations' `shasum.bat` eligible. This identifies conflicting tool directories without hardcoding a Git or Perl installation location. Other entries, including Git's native `cmd` directory and the fixture's MinGW GCC directory, remain available. The Kotlin script runner uses native Windows SDK and repository paths. The unmodified official launcher can then use its bundled checksum applet and still verifies the exact declared SHA-256. No shim executable, wrapper modification or checksum bypass is needed.

The Kotlin scripts read subprocess output with UTF-8 and emit Toolchain failures as GitHub annotations. When started from Git Bash, `scripts/run` delegates to the native PowerShell launcher so MSYS paths are converted before entering the Windows JVM.

Hosted confirmation: commit `7b0b169e74ec5fef947736e7440b5fde2563810b` passed [KMP Native (mingwX64)](https://github.com/Kronos-orm/Kronos-orm/actions/runs/39472990633/job/118801889128), including Toolchain/runtime download, checksum verification, compilation and execution of syntax, core and logging tests on Windows. The same commit passed the Linux x64 and ARM64 runtime gates. CLI help, diff checks and macOS environment preservation also passed.
