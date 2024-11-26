{% import "../../../macros/macros-en.njk" as $ %}

查询忽略共支持以下几种策略：

{{ $.members([
["SELECT", "在所有查询时均忽略对该属性的查询和赋值", "IgnoreAction"],
["CASCADE_SELECT", "在级联查询时忽略对该属性的查询和赋值", "IgnoreAction"]
])}}

级联删除的使用方法详见 {{ $.keyword("class-definition/annotation-config", ["@Ignore 查询时忽略属性"]) }}。