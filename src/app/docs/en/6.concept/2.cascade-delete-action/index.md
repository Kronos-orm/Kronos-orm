{% import "../../../macros/macros-en.njk" as $ %}

规定级联删除时的操作，级联删除的使用方法详见 {{ $.keyword("advanced/cascade-delete", ["级联删除"]) }}

级联删除操作共支持以下几种策略：

{{ $.members([
    ["NO_ACTION", "不做任何操作，若不设置，默认为<code>NO_ACTION</code>", "CascadeDeleteAction"],
    ["CASCADE", "级联删除该实体的所有关联实体", "CascadeDeleteAction"],
    ["RESTRICT", "若存在关联数据，不允许删除该实体", "CascadeDeleteAction"],
    ["SET_NULL", "删除当前实体，将关联实体的引用列设置为<code>null</code>", "CascadeDeleteAction"],
    ["SET_DEFAULT", "删除当前实体，将关联实体的引用列设置为默认值，默认值需要在注解中指定<code>defaultValue</code>属性", "CascadeDeleteAction"]
])
}}
