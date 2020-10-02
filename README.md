# StORM

StORM (Shujutech Object Relational Mapper) maps Java POJO into relational databases (currently supported databases are mariadb, mysql, oracle and postgres). StORM is the only true ORM that is able to map all OOP concept into a relational database.

# Features

StORM supports mapping the following Java OOP concepts:

1. Class (maps into a table)
2. Properties (maps class properties into database fields for all primitive datatypes)
3. Class Properties (maps complex datatypes of type class, in OOP also known as a member of)
4. Inheritance (maps the inheritance relationship of a class with the inherited class)
5. Abstract Class (places the properties of an abstract class into a concrete class)
6. Polymorphism (enable a member of class type to behave polymorphically)

StORM supports the following database operations:

1. Creation (generate DDL to create tables, columns for the defined class in Java)
2. 
