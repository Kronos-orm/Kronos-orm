{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

规定级联删除时的操作，级联删除的使用方法详见 {{ $.keyword("advanced/cascade-delete", ["级联删除"]) }}

级联删除操作共支持以下几种策略：

{{ $.params([
    ["NO_ACTION", "CascadeDeleteAction", "不做任何操作，若不设置，默认为`NO_ACTION`","/"],
    ["CASCADE", "CascadeDeleteAction", "级联删除该实体的所有关联实体","/"],
    ["RESTRICT", "CascadeDeleteAction", "若存在关联数据，不允许删除该实体","/"],
    ["SET_NULL", "CascadeDeleteAction", "删除当前实体，将关联实体的引用列设置为`null`","/"],
    ["SET_DEFAULT", "CascadeDeleteAction", "删除当前实体，将关联实体的引用列设置为默认值，默认值需要在注解中指定`defaultValue`属性","/"]
])
}}
