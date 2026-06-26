{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Cascading insert or update

**Cascade insert or update** is a combination of **cascade insert** and **cascade update**. Its usage is consistent with {{ $.keyword("database/upsert-records", ["update insert"]) }}.