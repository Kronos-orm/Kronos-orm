{% import "../../../macros/macros-en.njk" as $ %}

Query Ignore supports the following strategies in total:

{{ $.members([
    ["SELECT", "Ignore queries and assignments to this attribute on all queries", "IgnoreAction" ], 
    ["CASCADE_SELECT", "Ignore queries and assignments to this property on cascade queries", "IgnoreAction"]
])}}

See {{ $.keyword("class-definition/annotation-config", ["@Ignore Ignore Property During Select"]) }} for details on how to use cascade deletion.