# Toolchain JVM arguments use their own quote grammar

Kotlin Toolchain 0.13's `org.jetbrains.amper.cli.options.UserJvmArgsOptionKt.splitArgsHonoringQuotes` recognizes double quotes and backslash escaping. Passing a POSIX single-quoted token to `--jvm-args` leaves those single quotes in the JVM argument. An always-quoting shell helper therefore corrupts `-javaagent` even when the path has no spaces.

Use `scripts/common.main.kts::quoteJvmArgument` for every token joined into `--jvm-args`. It wraps the argument in double quotes and escapes backslashes and embedded double quotes. The IDE fixture and Kover invocation share this function. `KOTLIN_CLI_JAVA_OPTIONS` is interpreted by the wrapper's shell and retains the separate shell quoting helper.

An independent JVM loaded the actual 0.13 parser and verified exact round trips for an ordinary agent path, a path containing spaces, Windows drive/backslash paths and embedded double quotes, plus a combined argument vector. A compiled Java agent at a path containing spaces was then loaded by a real JVM using that parser's output and received its complete spaced file argument. Evidence: `/tmp/kronos-kts-official-jvm-quote.json` and `/tmp/kronos-kts-official-jvm-quote.log`. The direct parser probe uses JDK 25 because the Toolchain implementation classes have class-file version 69; repository test JVMs remain independently selected.
