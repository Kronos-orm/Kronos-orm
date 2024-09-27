{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 级联插入或更新

**级联插入或更新**是**级联插入**和**级联更新**的合并，它的用法与{{ $.keyword("database/upsert-records", ["更新插入"]) }}保持一致。