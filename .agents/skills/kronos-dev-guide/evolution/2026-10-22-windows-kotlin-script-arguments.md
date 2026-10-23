# Clear inherited Kotlin SDK arguments before nested Windows scripts

At commit `e97b6d9b91799500e5355ad9a936be4a967b6f0c`, the Windows JVM and MinGW checks failed during `bootstrap()`, before their runtime tests. The child command selected `ide bootstrap`, but its stack entered `Ci_main` and rejected `--library-version`, `--kotlin-version` and `--ide-import-repository`.

The official Kotlin 2.4.21 SDK `bin/kotlinc.bat` appends its command arguments to `KOTLIN_OPTS` without resetting an inherited value. Its Java child inherits the completed variable, including the parent script path. A nested SDK launch therefore selects that parent again and appends the child entry point as another script argument.

Clear `KOTLIN_OPTS` in `scripts/run.ps1` immediately before invoking the SDK's `kotlin.bat`. Keep the explicit new entry point and argument array. Both direct PowerShell launches from Git Bash and nested `run.cmd` calls pass through this boundary. Preserve `JAVA_OPTS`, Java selection, exit-code propagation and Toolchain checksum handling.

Evidence: [Windows JVM annotation](https://github.com/Kronos-orm/Kronos-orm/actions/runs/39629371264/job/119323091220), [MinGW annotation](https://github.com/Kronos-orm/Kronos-orm/actions/runs/39629371264/job/119323090816), and the downloaded official SDK `bin/kotlinc.bat` argument loop. Actual Windows verification belongs to the hosted JVM and MinGW checks.
