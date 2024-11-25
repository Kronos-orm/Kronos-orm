{% import "../../../macros/macros-en.njk" as $ %}

The operation for defining cascade delete is specified, and the usage of cascade delete is detailed in {{ $.keyword("advanced/cascade-delete", ["Cascade Deletion"]) }}

The cascade delete operation supports the following strategies:

{{ $.members([

["NO_ACTION", "No action is taken; if not set, defaults to <code>NO_ACTION</code>", "CascadeDeleteAction"],

["CASCADE", "Cascade delete all associated entities of this entity", "CascadeDeleteAction"],

["RESTRICT", "If associated data exists, deleting this entity is not allowed", "CascadeDeleteAction"],

["SET_NULL", "Delete the current entity and set the reference column of associated entities to <code>null</code>", "CascadeDeleteAction"],

["SET_DEFAULT", "Delete the current entity and set the reference column of associated entities to the default value, which needs to be specified in the annotation with the <code>defaultValue</code> property", "CascadeDeleteAction"]

])

}}

For details on how to use cascade deletion, please refer to {{ $.keyword("advanced/cascade-delete", ["Cascade Deletion"]) }}。