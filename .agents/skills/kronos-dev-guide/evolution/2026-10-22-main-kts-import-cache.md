# main.kts imported source cache invalidation

Kotlin 2.4.21 main.kts cached an unchanged entry after an imported script changed. A two-file
probe printed `before` twice after the imported value was changed to `after`. This also caused
`build.main.kts` to retain an earlier `common.main.kts` bootstrap implementation during CI work.

The repository launchers set the official `KOTLIN_MAIN_KTS_COMPILED_SCRIPTS_CACHE_DIR` environment variable
to an SDK-version/shared-script-content/JAVA_HOME namespace. The entry's own content remains
part of Kotlin's cache key. Both Unix and PowerShell launchers compute the shared content digest.
Keep generated dependency annotations in that digest. Do not cache main.kts under a version-only
key or assume Kotlin invalidates imported script contents automatically.

Use the environment variable when invoking the SDK batch runner. A `-J-Dname=value` argument
adds another CMD argument-parsing boundary; the environment variable avoids that boundary.
The SDK exposes both settings; an isolated cache selected through the environment printed
the updated imported value in the reproduction.
