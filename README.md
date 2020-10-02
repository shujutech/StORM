# StORMi

StORMi maps Java POJO into relational databases. StORMi is the only true ORM that is able to map all OO concept into a relational database.


# Supported Databae

- MySQL- 
- MariaDB
- Postgres
- Oracle


# Features

StORMi supports mapping the following Java OOP concepts:

- Class (maps into a table)
- Properties (maps class properties into database fields for all primitive datatypes)
- Class Properties (maps complex datatypes of type class, in OOP also known as a member of)
- Inheritance (maps the inheritance relationship of a class with the inherited class)
- Abstract Class (places the properties of an abstract class into a concrete class)
- Polymorphism (enable a member of class type to behave polymorphically)
- Array of Objects (supports handling of array of objects)

StORMi supports the following database operations:

- DDL Creation (generate DDL to create tables, columns for the defined class in Java)
- Persistence (when a Java POJO class is persisted, StORMi will handle all the underlying intricancies)
- Deletion (when deleting a class, StORMi can also delete its related class when configured to do so)
- Updating (like Persistence and Deletion, many complex class relationship is being manage by StORMi)



