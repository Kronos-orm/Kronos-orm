# 11. 常见问题（FAQ）

- 如何确认插件已生效？
  - 开启 `debug=true`，编译后检查 `debug-info-path` 是否生成了 IR 文本；
  - 或在构建日志中看到 `Debug info saved to ...`；
- 与 KAPT/KSP 是否冲突？
  - 本插件基于 Kotlin 编译管线的 IR 阶段，通常与 KAPT/KSP 并行存在；若其他插件也修改 IR，需注意顺序与兼容性；
- 是否必须使用 K2？
  - 是。插件使用了 K2 对外暴露的 IrGenerationExtension API；
- 我能否禁用某些变换？
  - 当前暴露的是全量启用的能力。若你有定制化需求，可 fork 并在 Transformer 分发处添加开关；
- IR dump 文件如何阅读？
  - `dumpKotlinLike` 输出接近 Kotlin 的伪代码，关注插件注入的 `addFieldList` 等片段即可；
- 为什么我的 TypedQuery 类型不正确？
  - 检查是否匹配到 `fqNameOfTypedQuery` 或 `fqNameOfSelectFromsRegexes`；如存在新的变体，欢迎 PR 扩展规则。