## StORMi

<img src="http://shujutech.mywire.org/img/dblogo5.png" alt="alt text" width="50%" height="50%">

StORMi maps Java POJO into relational databases. StORMi is the only true ORM that is able to map all OO concept into a relational database.


## Supported Database

- MySQL
- MariaDB
- Postgres
- Oracle


## Features

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
- No SQL hence can be easily ported to any SQL database, DDL/DML can be done through java methods

## Benefits

- Allow full enterprise team to use standard OOP as the universal design model
- Never redesign your data model again with StORMi standard, consistent and reusable model
- Allows team to have common understanding of a single design principal and concept
- No duplication of enterprise information
- Unlimited standardise scaling capabilities for all your enterprise information system

## Examples

#### Defining database objects from Java classes 

````java
public class Addr extends Clasz {
	@ReflectField(type=FieldType.STRING, size=32, displayPosition=5) 
	public static String Addr1;
  
	@ReflectField(type=FieldType.STRING, size=32, displayPosition=10) 
	public static String Addr2;
  
	@ReflectField(type=FieldType.STRING, size=32, displayPosition=15) 
	public static String Addr3;
  
	@ReflectField(type=FieldType.STRING, size=8, displayPosition=20) 
	public static String PostalCode;
  
	@ReflectField(type=FieldType.OBJECT, deleteAsMember=false, 
	clasz=biz.shujutech.bznes.Country, displayPosition=35, prefetch=true, lookup=true) 
	public static String Country; 
  
	@ReflectField(type=FieldType.OBJECT, deleteAsMember=false, 
	clasz=biz.shujutech.bznes.State, displayPosition=40, prefetch=true, lookup=true) 
	public static String State; 
  
	@ReflectField(type=FieldType.OBJECT, deleteAsMember=false, 
	clasz=biz.shujutech.bznes.City, displayPosition=45, prefetch=true, lookup=true) 
	public static String City; 
}
````

#### Persisting objects (insert or update)

````java
	Person employee = (Person) ObjectBase.CreateObject(conn, Person.class);
	employee.setName("Ken Miria");
	employee.setBirthDate(new DateTime());
	employee.setGender(Gender.Male);
	employee.setNationality(Country.UnitedStates);
	employee.setMaritalStatus(Marital.Married);
	company.addEmployee(conn, employee);

	ObjectBase.PersistCommit(conn, company);
````

#### Deleting objects

````java
	Person person = (Person) objectDb.createObject(Person.class); // remove previous Person testing data if exist
	person.setName("Edward Yourdon");
	if (person.populate(conn) == true) {
		if (person.deleteCommit(conn)) {
			App.logInfo("Deleted person Edward Yourdon");
		} else {
			throw new Hinderance("Fail to delete person Edward Yourdon");
		}
	}
````


				
## Quick Start



## Contact Us

For any further support, please contact me at shujutech@gmail.com



