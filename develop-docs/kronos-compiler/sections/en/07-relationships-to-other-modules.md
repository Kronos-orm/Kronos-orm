# 7. Relationship with Other Modules

- With kronos-core
  - Uses interfaces (KPojo), annotations (KronosInit), and DSL types (KTableFor* series) defined in core;
  - IR rewrites are based on these public contracts to keep runtime behavior aligned;
- With kronos-gradle-plugin / kronos-maven-plugin
  - Build plugins can help wire compiler plugin dependencies and options more conveniently;
  - It’s recommended to centralize debug and version alignment in the build layer.
