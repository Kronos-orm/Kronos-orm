# 11. FAQ

- How to confirm the plugin is active?
  - Enable `debug=true` and check if files are generated under `debug-info-path` after compilation;
  - Or look for `Debug info saved to ...` in the build logs;
- Does it conflict with KAPT/KSP?
  - This plugin works at the IR phase and typically coexists with KAPT/KSP; if other plugins also modify IR, mind order and compatibility;
- Is K2 mandatory?
  - Yes. The plugin uses K2 IrGenerationExtension APIs;
- Can I disable certain transforms?
  - Currently the capability is enabled as a whole. For customization, consider forking and adding switches at the dispatcher level;
- How to read the IR dump files?
  - `dumpKotlinLike` produces pseudo-Kotlin text; focus on injected fragments like `addFieldList`;
- Why is my TypedQuery type incorrect?
  - Check if your call matches `fqNameOfTypedQuery` or patterns in `fqNameOfSelectFromsRegexes`; if you have a new variant, contributions are welcome to extend the rules.
