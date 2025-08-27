# 10. Debugging and Troubleshooting

## 10.1 Enable IR dump

- In Gradle/Maven add:
  - `-P plugin:kronos-compiler-plugin:debug=true`
  - `-P plugin:kronos-compiler-plugin:debug-info-path=<dir>` (optional, default `build/tmp/kronosIrDebug`)
- After compilation, inspect the directory; files are organized by source file names.

## 10.2 Common issues

- K2 not enabled
  - The plugin relies on K2 IR APIs; ensure you use a Kotlin version with K2 support;
- Plugin not found or options invalid
  - Check `-Xplugin` and `plugin:kronos-compiler-plugin:*` arguments;
  - Ensure the plugin jar is on the compiler classpath (compileOnly or kotlinCompilerPluginClasspath);
- Runtime error but IR dump looks fine
  - Minimize the input in tests; verify the IR rewrite aligns with runtime APIs;
- NPE or ClassCast during IR transform
  - Use dumps to pinpoint the exact injected IR fragment causing trouble;
  - Harden for nullability/generic bounds/extension receivers.

## 10.3 Best practices

- Add unit tests whenever a new transformer or rule is introduced;
- Move brittle IR construction into util/helper layer;
- Use logging and dumps wisely; avoid long-term debug in production builds.
