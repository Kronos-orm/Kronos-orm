**Database First** is an ORM (Object-Relational Mapping) development methodology for projects with an existing database
structure. This approach emphasizes designing the database first and then generating code based on the database
structure. The following are the key features of Database First:

### Key features of Database First

1. **Database First**: The Database First approach first builds the structure of the database tables, relationships, and
   constraints through a database design tool, and then generates the data model classes and contexts from the existing
   database using the ORM framework, which is suitable for projects that have an existing database.
2. **Automatic Model Generation**: ORM tools (such as Entity Framework) automatically generate entity classes, data
   contexts, and relationship mappings, eliminating the need for developers to manually write model code. This greatly
   reduces duplication of effort.
3. **For Complex Databases**: For databases that already exist and have complex designs, Database First allows
   developers to implement mappings between the database and the business logic without having to design the database
   from scratch.
4. **Facilitate database maintenance**: In the process of database design and change, developers can update the
   generated code model through ORM tools to ensure the consistency of the data model and database structure.
5. **Lower decoupling from code**: Since the data model is generated directly from the database, any changes to the
   database structure need to be updated to the model code through the ORM tool. This lower decoupling is suitable for
   scenarios with strict database design and management.

### Example

Suppose we already have a database that contains a `user` table. The table has fields `id`, `name` and `email`. Using
the Database First method, we can generate the corresponding `User` entity class directly from the database, enabling
developers to use the class in their code to manipulate the database.

```kotlin
data class User(
    val id: Int? = null,
    val name: String? = null,
    val email: String? = null
): KPojo
```
[Code Generator (Home/Resource/Code Generator)](/)

### Scenarios

- For existing databases, especially for refactoring or data migration projects.
- Database First is ideal when the database structure is complex and needs to be tightly controlled.

### Summary

**Database First** provides tight integration with the database structure, allowing developers to quickly generate code
models based on existing databases and maintain tight consistency with the database. It is suitable for projects that
require strict management of the database structure and supports a database-first development process.