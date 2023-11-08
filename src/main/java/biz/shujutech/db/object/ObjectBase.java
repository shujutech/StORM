package biz.shujutech.db.object;

import biz.shujutech.base.App;
import biz.shujutech.base.Connection;
import biz.shujutech.base.DateAndTime;
import biz.shujutech.base.Hinderance;
import biz.shujutech.db.object.FieldClasz.FetchStatus;
import biz.shujutech.db.relational.Database;
import biz.shujutech.db.relational.Field;
import biz.shujutech.db.relational.FieldDate;
import biz.shujutech.db.relational.FieldDateTime;
import biz.shujutech.db.relational.FieldLong;
import biz.shujutech.db.relational.FieldStr;
import biz.shujutech.db.relational.FieldType;
import biz.shujutech.db.relational.Record;
import biz.shujutech.db.relational.SortOrder;
import biz.shujutech.db.relational.Table;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.lang.reflect.Modifier;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import org.joda.time.DateTime;

public class ObjectBase extends Database {

	public static String LEAF_CLASS = "leaf_class";
	public static int CLASS_NAME_LEN = 128;
	public static String MEMBER_NAME = "member_name";
	public static int MEMBER_NAME_LEN = 64;
	public static String ABSTRACT_OBJECT_ID = "leaf_object_id";
	public static String ABSTRACT_CLASS = "leaf_clasz";
	
	/*
	 * Traverses the class of Clasz type inheritance hierarchy and create each
	 * table for each of the traverse class. Uses reflection to determine the
	 * hierarchy tree class. For every created object, this method will 
	 * automatically create the primary key (using database sequence to genrate
	 * the primary key value). The inherited classes relationship uses this unique
	 * primary key to describe their inheritance relationship using an 
	 * intermediary table of the child class. For each created object, the
	 * system will also create a field to track the number of child object that
	 * inherited from it. This count is use when deleting an object, an object
	 * with no child inherited from it will be deleted during the inheritance
	 * traversal.
	 * 
	 * For normal class, a table is created for each of the inherited parent 
	 * class. For abstract class, the fields in the abstract class is added to
	 * the first normal child class in the inheritance tree.
	 * 
	 */

	public <Ty extends Clasz<?>> Ty createObject(Connection aConn, Class<Ty> aClass) throws Exception {
		return ObjectBase.CreateObject(aConn, aClass);
	}
	
	public Clasz<?> createClasz(Connection aConn, Class<?> aClass) throws Exception {
		return ObjectBase.CreateObject(aConn, aClass.asSubclass(Clasz.class));
	}

	public static Clasz<?> CreateClaszFreeType(Connection aConn, Class<?> aClass) throws Exception {
		return CreateObject(aConn, aClass.asSubclass(Clasz.class));
	}

	public static <Ty extends Clasz<?>> Ty CreateObject(Connection conn, Class<Ty> aClass) throws Exception {
		Ty result = Clasz.CreateObject(conn, aClass);
		result.initBeforePopulate();
		result.initBeforePopulate(conn);
		return aClass.cast(result);
	}

	public static Clasz<?> CreateObjectFromAnyClass(Connection conn, Class<?> aClass) throws Exception {
		Clasz<?> result = Clasz.CreateObjectFromAnyClass(conn, aClass);
		result.initBeforePopulate();
		result.initBeforePopulate(conn);
		return Clasz.class.cast(result);
	}

	public static <Ty extends Clasz<?>> Ty CreateObjectTransient(Connection conn, Class<Ty> aClass) throws Exception {
		Ty result = Clasz.CreateObjectTransient(conn, aClass);
		result.initBeforePopulate();
		result.initBeforePopulate(conn);
		return aClass.cast(result);
	}

	public static Clasz<?> CreateObjectTransientFromAnyClass(Connection conn, Class<?> aClass) throws Exception {
		Clasz<?> result = Clasz.CreateObjectTransientFromAnyClass(conn, aClass);
		result.initBeforePopulate();
		result.initBeforePopulate(conn);
		return result;
	}

	public static void SetDbRecursive(Clasz<?> aClasz, ObjectBase aDb) throws Exception {
		aClasz.setDb(aDb);
		Clasz<?> parent = aClasz.getParentObjectByContext();
		if (ObjectBase.IsNotAtClaszYet(parent)) {
			SetDbRecursive(parent, aDb);
		}
	}

	/**
	 * For classes that inherit from Clasz class, their class relationship is
	 * kept in a table with the primary key of the child object and its parent's
	 * object. This method will create that table (prefix with IH_) and their
	 * primary key columns to link its inheritance relationship.
	 * 
	 * @param aConn
	 * @param aParent
	 * @param aChild
	 * @throws Exception 
	 */
	public static void createInheritance(Connection aConn, Class<?> aParent, Class<?> aChild) throws Exception {
		String ihName = Clasz.GetIhTableName(aChild);
		Table ihTable = new Table(ihName);
		String childPkName = Clasz.CreatePkColName(aChild);
		if (Database.TableExist(aConn, ihName)) { // if the parent ih table already exist, add the child pk field if is not there
			ihTable.initMeta(aConn);
			if (ihTable.fieldExist(childPkName) == false) {
				ihTable.createField(childPkName, FieldType.LONG); // create the child field in the ih_ table
				Database.AlterTableAddColumn(aConn, ihTable); // add the new column, do 'alter table...'
			}
		} else {
			String parentPkName = Clasz.CreatePkColName(aParent);
			ihTable.createField(parentPkName, FieldType.LONG); // create the parent field
			ihTable.createField(childPkName, FieldType.LONG); // create the child field in the ih_ table
			ihTable.getField(childPkName).setPrimaryKey();
			Database.CreateTable(aConn, ihTable);
			Database.CreatePrimaryKey(aConn, ihTable);
		}
	}

	public static void InsertInheritance(Connection aConn, Clasz<?> aObject) throws Exception {
		try {
			String ihName = Clasz.GetIhTableName(aObject.getClass()); // get the inheritance table name for this aObject
			Table ihTable = new Table(ihName); // Insert into the inheritance table for this object and its parent
			Record ihRecord = ihTable.createRecord(aConn);
			ihRecord.setValueLong(aObject.getPkName(), aObject.getObjectId());
			ihRecord.setValueLong(aObject.getParentObjectByContext().getPkName(), aObject.getParentObjectByContext().getObjectId());
			ihTable.insert(aConn, ihRecord); 
		} catch(Exception ex) {
			throw new Hinderance(ex, "Fail to insert inheritance relationship into ih_ table for child: '" + aObject.getClaszName() + "', parent: '" + aObject.getParentObjectByContext().getClaszName() + "'");
		}
	}

	/**
	 * Create the 'member of' relationship table, this method is 99% similar
	 * to createInheritance method, only one line difference, re-factor?
	 * 
	 * @param aConn
	 * @param aParent
	 * @param aChild
	 * @param isChildPolymorphic
	 * @param aLinkField
	 * @throws Exception 
	 */
	public static void createMemberOfTable(Connection aConn, Class<?> aParent, Class<?> aChild, boolean isChildPolymorphic, String aLinkField) throws Exception {
		String relationType = Clasz.GetIvTableName(aParent);
		Table linkTable = new Table(relationType);
		if (Database.TableExist(aConn, relationType)) { 
			linkTable.initMeta(aConn);
			if (linkTable.fieldExist(aLinkField) == false) {
				Table alterTable = new Table(relationType);
				alterTable.createField(aLinkField, FieldType.LONG); // create the child field in the iv_ table
				if (isChildPolymorphic) {
					alterTable.createField(Clasz.CreateLeafClassColName(aLinkField), FieldType.STRING, CLASS_NAME_LEN); // create the polymorphic column for this field
				}
				Database.AlterTableAddColumn(aConn, alterTable); // add the new column, do 'alter table...'
			}
		} else {
			String parentPkName = Clasz.CreatePkColName(aParent);
			linkTable.createField(parentPkName, FieldType.LONG); // create the parent field
			linkTable.createField(aLinkField, FieldType.LONG); // create the child field in the iv_ table
			if (isChildPolymorphic) {
				linkTable.createField(Clasz.CreateLeafClassColName(aLinkField), FieldType.STRING, CLASS_NAME_LEN); 
			}
			linkTable.getField(parentPkName).setPrimaryKey(); // here its different from createInheritance
			Database.CreateTable(aConn, linkTable);
			Database.CreatePrimaryKey(aConn, linkTable);
		}
	}

	/**
	 * Insert the member of relationship between the pass in object.
	 * 
	 * @param aConn
	 * @param aMasterObject
	 * @param aMemberOid
	 * @param aLeafClass
	 * @throws Exception 
	 */
	public static void InsertMemberOf(Connection aConn, Clasz<?> aMasterObject, Field aMemberOid, Field aLeafClass) throws Exception {
		String relationType = Clasz.GetIvTableName(aMasterObject.getClass());
		Table linkTable = new Table(relationType); 
		Record linkRecord = new Record();
		linkRecord.createField(aMasterObject.getPkName(), aMasterObject.getObjectId());
		if (aMemberOid.getDbFieldType() == FieldType.LONG) {
			linkRecord.createField(aMemberOid.getDbFieldName(), ((FieldLong) aMemberOid).getValueLong());
		} else if (aMemberOid.getValueObj(aConn) == null) {
			linkRecord.createField(aMemberOid.getDbFieldName(), FieldType.LONG);
			linkRecord.getField(aMemberOid.getDbFieldName()).setModified(true);
		} else {
			throw new Hinderance("Invalid field type while attempting to insert a class field relationship in: '" + relationType + "'");
		}
		if (aLeafClass != null && aLeafClass.getValueStr(aConn).isEmpty() == false) {
			linkRecord.createField(aLeafClass.getDbFieldName(), aLeafClass.getValueStr(aConn));
		}
		linkTable.insert(aConn, linkRecord); 
	}

	public static void InsertBoxMember(Connection aConn, Clasz<?> aMasterObject, String aMemberName,  Long aMemberValue, String aMemberClass) throws Exception {
		String relationType = Clasz.GetIwTableName(aMasterObject.getClass(), aMemberName);
		Table linkTable = new Table(relationType); 
		Record linkRecord = new Record();
		linkRecord.createField(aMasterObject.getPkName(), aMasterObject.getObjectId());
		linkRecord.createField(aMemberName, aMemberValue);
		if (aMemberClass != null && aMemberClass.isEmpty() == false) {
			linkRecord.createField(LEAF_CLASS, FieldType.STRING, CLASS_NAME_LEN, aMemberClass);
		}
		linkTable.insert(aConn, linkRecord); 
	}

	public void deleteBoxMember(Connection aConn, Clasz<?> aMasterObject, FieldObjectBox<?> aFieldBox, Clasz<?> aMemberObj) throws Exception {
		String relationType = Clasz.GetIwTableName(aMasterObject.getClass(), aFieldBox.getDbFieldName());
		Table linkTable = new Table(relationType); 
		Record whereRecord = new Record();
		whereRecord.createField(aMasterObject.getPkName(), aMasterObject.getObjectId());
		whereRecord.createField(aFieldBox.getDbFieldName(), aMemberObj.getObjectId());
		linkTable.delete(aConn, whereRecord); 
		if (aFieldBox.deleteAsMember()) {
			//ObjectBase.DeleteNoCommit(aConn, aMemberObj, aFieldBox.isInline()); // isInline is false, box member inline is not supported yet
			ObjectBase.DeleteNoCommit(aConn, aFieldBox, aMasterObject, aMemberObj);
		}
	}

	public void deleteBoxMember(Connection aConn, Clasz<?> aMasterObject, FieldObjectBox<?> aFieldBox, Long aMemberValue, String aMemberClass) throws Exception {
		String relationType = Clasz.GetIwTableName(aMasterObject.getClass(), aFieldBox.getDbFieldName());
		Table linkTable = new Table(relationType); 
		Record whereRecord = new Record();
		whereRecord.createField(aMasterObject.getPkName(), aMasterObject.getObjectId());
		whereRecord.createField(aFieldBox.getDbFieldName(), aMemberValue);
		linkTable.delete(aConn, whereRecord); 

		if (aFieldBox.deleteAsMember()) {
			Class<?> leafClass = Class.forName(aFieldBox.getDeclareType()); // default to the field object class
			if (aMemberClass != null && aMemberClass.isEmpty() == false) {
				leafClass = Class.forName(aMemberClass); // if polymorpic replace the default class to polymorphic class
			}
			Clasz<?> whereObj = Clasz.CreateObjectFromAnyClass(aConn, leafClass);
			whereObj.setObjectId(aMemberValue);
			Clasz<?> deleteObj = Clasz.Fetch(aConn, whereObj); // will this still Fetch according to the getClassName method????, if it works then yes
			if (deleteObj != null && deleteObj.isPopulated()) {
				ObjectBase.DeleteNoCommit(aConn, aFieldBox, aMasterObject, deleteObj);
			}
		}
	}


	/*
	 * create polymorphic table with prefix ip_ip_<master class name> e.g. 
	 * ip_person primary key of master class field name inside the master class
	 * fully qualified leaf class name
	 * 
	 */

	/**
	 * 
	 * @param aConn
	 * @param aMemberField
	 * @param aLeafField
	 * @return
	 * @throws Exception 
	 */
	private static Class<?> GetEffectiveClass(Connection aConn, FieldObject<?> aMemberField, FieldStr aLeafField) throws Exception {
		Class<?> result = Class.forName(aMemberField.getDeclareType());
		if (aMemberField.isPolymorphic()) {
			try {
				String strLeafClass = aLeafField.getValueStr();
				if (strLeafClass != null && strLeafClass.isBlank() == false) {
					result = Class.forName(strLeafClass);
				}
			} catch (Exception ex) {
				throw new Hinderance(ex, "For field: " + aMemberField.getCamelCaseName() + ", fail to get effective leaf class: " + aLeafField.getValueStr());
			}
		}
		return(result);
	}

	public static Class<?> GetEffectiveClass(Connection aConn, Clasz<?> aMasterObject, FieldObject<?> aMemberField) throws Exception {
		Class<?> result = Class.forName(aMemberField.getDeclareType());
		String memberName = aMemberField.getDbFieldName();
		String leafClassColName = Clasz.CreateLeafClassColName(memberName);	
		String relationType = Clasz.GetIvTableName(aMasterObject.getClass()); // get the iv_ table name
		Table linkTable = new Table(relationType); // create the iv_ table instant
		linkTable.getMetaRec().createField(memberName, FieldType.LONG);
		linkTable.getMetaRec().createField(leafClassColName, FieldType.STRING, CLASS_NAME_LEN);

		Record whereRecord = new Record();
		whereRecord.createField(aMasterObject.getPkName(), aMasterObject.getObjectId());
		linkTable.fetch(aConn, whereRecord); 
		if (linkTable.totalRecord() == 1) {
			result = GetEffectiveClass(aConn, aMemberField, (FieldStr) linkTable.getRecord(0).getField(leafClassColName));
		}
		return(result);
	}

	/**
	 * Update the relationship of "instant variable" for a Clasz. This update
	 * is for one "instant variable".
	 * 
	 * @param aConn
	 * @param aMasterObject
	 * @param aMemberField
	 * @throws Exception 
	 */
	public void updateMemberOf(Connection aConn, Clasz<?> aMasterObject, FieldObject<?> aMemberField) throws Exception {
		Clasz<?> memberObj = aMemberField.getObj();
		String parentName = aMasterObject.getClass().getSimpleName();
		String memberDbFieldName = aMemberField.getDbFieldName();
		String memberFieldName = aMemberField.getCamelCaseName();
		String relationType = Clasz.GetIvTableName(aMasterObject.getClass()); // get the iv_ table name
		Table linkTable = new Table(relationType); // create the iv_ table
		linkTable.getMetaRec().createField(memberDbFieldName, FieldType.LONG);

		String leafClassColName = null;
		if (aMemberField.isPolymorphic()) {
			leafClassColName = Clasz.CreateLeafClassColName(memberDbFieldName);	
			linkTable.getMetaRec().createField(leafClassColName, FieldType.STRING, CLASS_NAME_LEN);
		}

		Record whereRecord = new Record();
		whereRecord.createField(aMasterObject.getPkName(), aMasterObject.getObjectId());

		if (aMemberField.isForDelete() || (aMemberField.isModified() && memberObj == null)) { // field mark for deletion or user set field obj to null indicating intention to delete
			linkTable.fetch(aConn, whereRecord); 
			if (linkTable.totalRecord() == 1) {
				if (aMemberField.deleteAsMember()) {
					Class<?> deleteClass = GetEffectiveClass(aConn, aMemberField, (FieldStr) linkTable.getRecord(0).getField(leafClassColName));
					Clasz<?> oldMember = Clasz.CreateObjectFromAnyClass(aConn, deleteClass); 
					Long oldMemberId = linkTable.getRecord(0).getFieldLong(memberDbFieldName).getValueLong();
					oldMember.setObjectId(oldMemberId);
					Clasz<?> forDelete = Clasz.Fetch(aConn, oldMember);
					if (forDelete != null && forDelete.isPopulated()) {
						DeleteNoCommit(aConn, forDelete);
					}
				}

				// for deleting member field, we empty their member's primary key from the iv_ tables
				linkTable.getRecord(0).removeField(memberDbFieldName);
				linkTable.getRecord(0).createField(memberDbFieldName, FieldType.LONG);
				linkTable.getRecord(0).getField(memberDbFieldName).setModified(true);

				// for deleting member field, we empty the leaf class too
				Field fieldLeafClass = null;
				if (aMemberField.isPolymorphic()) {
					linkTable.getRecord(0).removeField(leafClassColName);
					linkTable.getRecord(0).createField(leafClassColName, FieldType.STRING, CLASS_NAME_LEN);
					fieldLeafClass = linkTable.getRecord(0).getField(leafClassColName);
				}

				if (linkTable.update(aConn, linkTable.getRecord(0), whereRecord) == 0) {
					Field memberOid = linkTable.getRecord(0).getField(memberDbFieldName);
					InsertMemberOf(aConn, aMasterObject, memberOid, fieldLeafClass);
				}
			}
		} else { // is insert orupdate new member 
			Long memberId = memberObj.getObjectId();
			FieldLong memberField = new FieldLong(memberDbFieldName);
			memberField.setValueLong(memberId);

			FieldStr memberLeaf = null;
			if (aMemberField.isPolymorphic()) {
				String memberClass = memberObj.getClass().getName();
				memberLeaf = new FieldStr(leafClassColName, ObjectBase.CLASS_NAME_LEN);
				memberLeaf.setValueStr(memberClass);
			}
			if (memberId != Clasz.NOT_INITIALIZE_OBJECT_ID) {
				linkTable.fetch(aConn, whereRecord); 
				if (linkTable.totalRecord() == 0) { // there is no such member exist for this master object
					InsertMemberOf(aConn, aMasterObject, memberField, memberLeaf);
				} else if (linkTable.totalRecord() == 1) { // there is one Fetch record for this instant variable
					Long oldMemberId = linkTable.getRecord(0).getFieldLong(memberDbFieldName).getValueLong();
					if (oldMemberId == null || oldMemberId.equals(memberId) == false) {
						if (oldMemberId != null && aMemberField.deleteAsMember()) {
							Clasz<?> delCriteria = Clasz.CreateObject(aConn, memberObj.getClass()); // create the previous object to be deleted
							delCriteria.setObjectId(oldMemberId);
							Clasz<?> forDelete = Clasz.Fetch(aConn, memberObj.getClass().cast(delCriteria));
							if (forDelete != null && forDelete.isPopulated()) {
								DeleteNoCommit(aConn, forDelete);
							}
						}

						linkTable.getRecord(0).getFieldLong(memberDbFieldName).setValueLong(memberId);
						if (aMemberField.isPolymorphic() && memberLeaf != null) {
							String leafName = memberLeaf.getDbFieldName();
							String leafClass = memberLeaf.getValueStr(aConn);
							linkTable.getRecord(0).getFieldStr(leafName).setValueStr(leafClass);
						} else if (aMemberField.isPolymorphic() == false) { // not polymorphic, do nohting
							// do nothing
						} else { // is polymorphic, but no leaf class, possible??
							throw new Hinderance("Member leaf is null, member field name: " + parentName + "." + memberFieldName);
						}
						if (linkTable.update(aConn, linkTable.getRecord(0), whereRecord) == 0) { // now update to the latest member
							InsertMemberOf(aConn, aMasterObject, memberField, memberLeaf); 
						}
					}
				} else {
					throw new Hinderance("Error, object: " + aMasterObject.getClass().getName() + ", got mulitiple instant variable of: " + memberDbFieldName + ", id: " + aMasterObject.getObjectId());
				}
			}
		}
	}


	/**
	 * In a clasz object, it can have instant variable of object type and array
	 * with multiple instant variable of object type.When changes is made to the 
	 * objects in this array, each of the object in the array is updated by 
	 * this method.
	 * 
	 * The changes can be either insertion of new object into the array, updating
	 * to the existing array member or removal of existing object from the array.
	 * To determine which member in the array is updated, inserted or deleted. The 
	 * following 2 process is perform:
	 * 1.	By comparing the objects in the array against the objects in the 
	 * database. If the objects in the array do not exist in the database.
	 * The relationship of this new object is inserted into the database. This
	 * is in crudEachMember method below.
	 * 
	 * 2. Comparing objects in the database with the objects in the array/memory.
	 * Those objects in the database that do not exist anymore in the array
	 * is deleted. This is in compareDisk2Memory method below.
	 * 
	 * @param <Ty>
	 * @param aConn
	 * @param aMasterObject
	 * @param aFobField
	 * @param aAvoidRecursive
	 * @throws Exception 
	 */
	public <Ty extends Clasz<?>> void updateBoxMember(Connection aConn, Clasz<?> aMasterObject, FieldObjectBox<Ty> aFobField, CopyOnWriteArrayList<Clasz<?>> aAvoidRecursive) throws Exception {
		String relationType = Clasz.GetIwTableName(aMasterObject.getClass(), aFobField.getDbFieldName()); // get the iv_ table name
		Table linkTable = new Table(relationType); // create the iv_ table
		linkTable.getMetaRec().createField(aMasterObject.getPkName(), FieldType.LONG);
		linkTable.getMetaRec().createField(aFobField.getDbFieldName(), FieldType.LONG);
		linkTable.getMetaRec().createField(ObjectBase.LEAF_CLASS, FieldType.STRING, CLASS_NAME_LEN);
		Record whereRecord = new Record();
		whereRecord.createField(aMasterObject.getPkName(), aMasterObject.getObjectId());
		linkTable.fetch(aConn, whereRecord); // fetches ALL entries in the disk for this master object

		crudEachMember(aConn, aMasterObject, aFobField, linkTable, aAvoidRecursive);
	}

	public <Ty extends Clasz<?>> void crudEachMember(Connection aConn, Clasz<?> aMasterObject, FieldObjectBox<Ty> aFobField, Table linkTable, CopyOnWriteArrayList<Clasz<?>> aAvoidRecursive) throws Exception {
		for (Ty memberObject : aFobField.getObjectMap().values()) {
			if (memberObject.getForDelete() == true) {
				this.deleteBoxMember(aConn, aMasterObject, aFobField, memberObject);
			} else {
				CopyOnWriteArrayList<Clasz<?>> avoidRecursive = new CopyOnWriteArrayList<>(aAvoidRecursive);
				this.persist(aConn, memberObject, false, avoidRecursive); // Insert/update the member object into the database
				Long memberId = memberObject.getObjectId();
				if (memberId != Clasz.NOT_INITIALIZE_OBJECT_ID) {
					if (IsMember(linkTable, aFobField.isPolymorphic(), aFobField.getDbFieldName(), memberId, ObjectBase.LEAF_CLASS, memberObject.getClass().getName()) == false) { // there is no such member exist in the database for this memeber, so Insert this new entry
						String polymorphicName = null;
						polymorphicName = memberObject.getClass().getName(); // polymorphic or not, we still fill this field with the clasz type
						InsertBoxMember(aConn, aMasterObject, aFobField.getDbFieldName(), memberId, polymorphicName); // Insert the relationship of master object to the member object
					}
				} else {
					// throw new Hinderance("Cannot set member object of: " + aField.getDeclareType() + ", for parent: " + aMasterObject.getClaszName());
					// the member object is not inserted/updated because there's no changes or it wasn't retrieve, so its a valid ignore
				}
			}
		}
	}

	public static boolean IsMember(Table aLinkTable, boolean aIsPolymorphic, String aFieldMemberName, Long aMemberValue, String aFieldLeafName, String aFieldLeafValue) throws Exception {
		boolean result = false;
		for(Record rec : aLinkTable.getRecordBox().values()) {
			if (aIsPolymorphic) {
				if (aMemberValue.equals(rec.getFieldLong(aFieldMemberName).getValueLong()) && aFieldLeafValue.equals(rec.getFieldStr(aFieldLeafName).getValueStr())) {
					result = true;
					break;
				}
			} else {
				if (aMemberValue.equals(rec.getFieldLong(aFieldMemberName).getValueLong())) {
					result = true;
					break;
				}
			}
		}
		return(result);
	}

	public void compareDisk2Memory(Connection aConn, Clasz<?> aMasterObject, FieldObjectBox<?> aFieldBox, Table linkTable) throws Exception {
		for(Record rec : linkTable.getRecordBox().values()) {
			Long memberId = rec.getFieldLong(aFieldBox.getDbFieldName()).getValueLong(); // object id of the member from the disk
			if (aFieldBox.containObjectId(memberId) == false) { // this entry from the disk is not in the map, user have removed it, so remove it from database
				String memberClass = "";
				if (rec.fieldExist(LEAF_CLASS)) {
					memberClass = rec.getFieldStr(LEAF_CLASS).getValueStr();
				}
				this.deleteBoxMember(aConn, aMasterObject, aFieldBox, memberId, memberClass);
			}
		}
	}

	/**
	 * Delete the pass in object from the database, the pass in object can be
	 * a fully populated object or just an object populated with criteria that
	 * is use to determine which object to be deleted, i.e. its use to build up 
	 * the where clause (such where record need not contain the primary key of
	 * the object). Only one object can be deleted.
	 * 
	 * @param aObject
	 * @return
	 * @throws Exception 
	 */

	/**
		Delete an object from the database, the object to be deleted must not
		have any child object to it. The deleting will also DeleteCommit all the 
		parent object of this object if all of those parent do not have other 
		child object attach to it. If the parent object contain other child 
		object attach to it, the DeleteCommit will not DeleteCommit it and all its parent
		object above the inheritance tree since all the  parent object will have
		child object attach to it.

		To make sense, you should instantiate an object of the lowest class
		in the inheritance tree to DeleteCommit it. E.g. if a clerk inherit from 
		employee and employee inherit from person, then you can only DeleteCommit the
		clerk. You cannot DeleteCommit the employee object since the employee is still
		a clerk. If that person is still a clerk, she must still be an employee,
		so it only make senses if you DeleteCommit the clerk and if that specific clerk
		do not have other relationship in the database with other object, then it 
		will be deleted from the database up to the person class object of that
		clerk.

		No deletion of an object can happen if the object to DeleteCommit is a parent
		object to other objects.

		Delete only the object at the lowest hierarchy tree in the inheritance
		tree, other related object to this class will not be deleted.

		Delete all the object associated to this class (either via inheritance or
		composition object), if those related class have no association to other
		object that is not related to that class.
	 * 
	 * @param aConn
	 * @param aObject
	 * @return
	 * @throws Exception 
	 */
	public static Integer DeleteCommit(Connection aConn, Clasz<?> aObject) throws Exception {
		int result;
		Clasz<?> objectToDelete; // if this object is a where object, then Fetch the object first to properly populate the object
		if (aObject.isPopulated() == false) {
			objectToDelete = ObjectBase.FetchObject(aConn, aObject);
		} else {
			objectToDelete = aObject;
		}

		boolean prevAutoCommit = aConn.getAutoCommit();
		aConn.setAutoCommit(false);
		try {
			result = DeleteObject(aConn, objectToDelete, 0);
			aConn.commit(); 
		} catch(Exception ex) {
			aConn.rollback();
			throw new Hinderance(ex, "Fail to delete and commit object, all transction rollback, clasz: " + aObject.getClass().getSimpleName());
		} finally {
			aConn.setAutoCommit(prevAutoCommit);
		}
		return(result);
	}

	public static Integer DeleteNoCommit(Connection aConn, Clasz<?> aObject) throws Exception {
		Integer result = null;
		Clasz<?> objectToDelete; // if this object is a where object, then Fetch the object first to properly populate the object
		if (aObject.isPopulated() == false) {
			objectToDelete = ObjectBase.FetchObject(aConn, aObject);
		} else {
			objectToDelete = aObject;
		}
		if (objectToDelete != null) {
			try {
				result = DeleteObject(aConn, objectToDelete, 0);
			} catch(Exception ex) {
				throw new Hinderance(ex, "Fail to delete object, in DeleteNoCommit(), object: " + objectToDelete);
			}
		} else {
			App.logEror("Data transaction integrity error, trying to delete a non existing object: " + objectToDelete);
		}
		return(result);
	}

	public static int DeleteNoCommit(Connection aConn , FieldObjectBox<?> aFob , Clasz<?> aMasterClasz , Clasz<?> aMemberClasz) throws Exception {
		int result = 0;
		if (aFob.isInline() == false) {
			DeleteNoCommit(aConn, aMemberClasz);
		} else {
			throw new Hinderance("Current version do not support inline FOB");
		}
		return(result);
	}

	public static int DeleteInlineField(Connection aConn, FieldObject<?> aField2Delete) throws Exception {
		/*
		if (aField2Delete.getFieldType() == FieldType.OBJECT) { // inline field will have flattend fields and it's object
		} else {
			throw new Hinderance("Inline field to delete must be OBJECT type with flattend fields, programming error!");
		}
		*/
		int result = 0;
		Record recUpdField = new Record(); // create record for atomic inline field to update
		Clasz<?> masterClasz = aField2Delete.getMasterObject();
		Record recUpdWhere = new Record();
		recUpdWhere.createField(aConn, masterClasz.getField(masterClasz.getPkName())); // update from the pk value
		recUpdWhere.copyValue(aConn, masterClasz.getField(masterClasz.getPkName())); 

		// inline field have no support for ObjectIndex yet, so not doing any index delete

		Clasz<?> clasz2Inline = ((FieldObject<?>) aField2Delete).getObj(); // inline field will have flattend fields and it's object
		for (Field eachField : clasz2Inline.getInstantRecord().getFieldBox().values()) { 
			if (eachField.getDbFieldType() != FieldType.OBJECT && eachField.isSystemField() == false) { // ignore structured object, only process flatten inline field
				Field field2Null = recUpdField.createField(aConn, eachField);
				field2Null.setDbFieldName(aField2Delete.getDbFieldName() + "_" + field2Null.getDbFieldName());
				field2Null.setValue(null);
			}
		}

		result = masterClasz.update(aConn, recUpdField, recUpdWhere); 
		if (result != 1) {
			throw new Hinderance("Fail deleting inline field, non unique master record found for clasz: '" + masterClasz.getClass().getSimpleName() + "'");
		}

		return(result);
	}

	private static int DeleteObject(Connection aConn, Clasz<?> aObject, int deletedChild) throws Exception {
		int result;

		DeleteMemberOf(aConn, aObject); // delete all this object member first

		Record recWhere = new Record();
		recWhere.createField(aConn, aObject.getField(aObject.getPkName())); // Delete from the pk value
		recWhere.copyValue(aConn, aObject.getField(aObject.getPkName())); 

		recWhere.createField(aConn, aObject.getField(aObject.createChildCountColName())); // only Delete when object doesn't have any child object
		recWhere.getFieldInt(aObject.createChildCountColName()).setValueInt(deletedChild); // reduce by the total deleted child, if its 0, then it has no children, so can Delete it
		recWhere.getField(aObject.createChildCountColName()).setFormulaStr(aObject.getChildCountNameFull(aConn) + " - ? <= 0");

		result = aObject.delete(aConn, recWhere);
		if (result > 1) {
			throw new Hinderance("Cannot delete object from non unique criteria for: " + aObject.getTableName() + ", criteria: " + recWhere.toStr());
		}

		aObject.deleteIndex(aConn);

		if (result == 1) {
			if (ObjectBase.ParentIsNotAtClaszYet(aObject)) {
				Clasz<?> parentObject = aObject.getParentObjectByContext(); // Delete the ih_* table if this object has inheritance relationship
				if (parentObject != null ) { // null means is not at Clasz yet bcos it has inheritance parent class but no parent object is set, proabably because it has inherited an abstract class
					if (parentObject.isPopulated()) {
						Record recWhereIh = new Record(); // to Delete the inheritance table entry, it needs the pk of the both parent and child table 
						recWhereIh.createField(aConn, aObject.getField(aObject.getPkName()));
						recWhereIh.copyValue(aConn, aObject.getField(aObject.getPkName()));
			
						recWhereIh.createField(aConn, parentObject.getField(parentObject.getPkName()));
						recWhereIh.copyValue(aConn, parentObject.getField(parentObject.getPkName()));
						String ihName = Clasz.GetIhTableName(aObject.getClass());
						Table ihTable = new Table(ihName);
						ihTable.delete(aConn, recWhereIh);
					}
					DeleteObject(aConn, parentObject, 1); // recursively call Insert, now try to Delete the parent object
				}
			}
		} else { // if nothing is deleted, we stop traversing the inheritance tree upwards, probably because the object still have child object to it, if so update the parent record
			if (deletedChild == 0) {
				// its deleted by others or when one class having 2 same member, so silently ignore it
			}
			else { // cannot Delete the parent record, so update its child count value
				Record recUpdWhere = new Record();
				recUpdWhere.createField(aConn, aObject.getField(aObject.getPkName())); // update from the pk value
				recUpdWhere.copyValue(aConn, aObject.getField(aObject.getPkName())); 

				Record recUpdField = new Record();
				recUpdField.createField(aConn, aObject.getField(aObject.createChildCountColName())); // only Delete when object doesn't have any child object
				recUpdField.getFieldInt(aObject.createChildCountColName()).setValueInt(deletedChild); // reduce by 1, if its 0, then it has no children, so can Delete it
				recUpdField.getField(aObject.createChildCountColName()).setFormulaStr(aObject.getChildCountNameFull(aConn) + " - ?");

				result = aObject.update(aConn, recUpdField, recUpdWhere); 
				if (result != 1) {
					throw new Hinderance("Fail to reduce the child count of object of class: '" + aObject.getClass().getSimpleName() + "'");
				}
			}
		}
		return(result);
	}

	/**
	 * 
	 * By default member object is deleted with its master, need to add later 
	 * to support annotation to enable user to specify that a member object not
	 * be deleted with its parent. Look into this, when other objects may have member
	 * of relationship with the same object and hence this cannot be known from
	 * the iv_* tables.
	 * 
	 * @param aConn
	 * @param aMaster
	 * @throws Exception 
	 */
	private static int DeleteMemberOf(Connection aConn, Clasz<?> aMaster) throws Exception {
		int result = 0;
		boolean gotInstantVariable = false;
		for (Field eachField : aMaster.getInstantRecord().getFieldBox().values()) { // for each of this member, 
			if (eachField.getDbFieldType() == FieldType.OBJECT || eachField.getDbFieldType() == FieldType.OBJECTBOX) {
				if (eachField.deleteAsMember()) {
					if (eachField.isInline() == false) {
						if (eachField.getDbFieldType() == FieldType.OBJECT) {
							gotInstantVariable = true; // if got member of relationship, remove them from the iv_* table
							Clasz<?> memberObj = ((FieldObject<?>) eachField).getValueObj(aConn);
							if (memberObj != null) {
								ObjectBase.DeleteNoCommit(aConn, memberObj);
								result++;
							} else {
								// not populated because there is no such member for this field, so just ignore
							}
						} else if (eachField.getDbFieldType() == FieldType.OBJECTBOX) {
							if (((FieldObjectBox<?>) eachField).getMetaObj().getClaszName().equals("Clasz") || ((FieldClasz) eachField).getFetchStatus() == FetchStatus.SOF) {
								((FieldObjectBox<?>) eachField).fetchAll(aConn);
							} else if (((FieldClasz) eachField).isPrefetch() == false) { // KIV, instead of Fetch all when prefetch is false, we can use Fetch by batch?
								((FieldObjectBox<?>) eachField).removeAll(); // remove all the objects from the map
								((FieldObjectBox<?>) eachField).fetchAll(aConn);
							}

							for (Clasz<?> eachMember : ((FieldObjectBox<?>) eachField).getObjectMap().values()) {
								if (eachMember.isPopulated()) {
									ObjectBase.DeleteNoCommit(aConn, eachMember);
									result++;
								} else {
									// not populated because there is no such member for this field, so just ignore 
								}
							}
							DeleteBoxMember(aConn, aMaster, (FieldObjectBox<?>) eachField);
						} else {
							// atomic field, don't do anything
						}
					} else { // handle inline delete
						DeleteInlineField(aConn, (FieldObject<?>) eachField);
					}
				}
			}
		}

		if (gotInstantVariable) {
			DeleteAllMember(aConn, aMaster); // since the master object is deleted, remove all its instant variable relationship, one master object will have only one member of record entry
			//deleteAllPolymorphic(aConn, aMaster); // not needed anymore as leaf class is kept in iv_ table and it's deleted with this one record for instant variable
		}
		return(result);
	}

	/**
	 * Remove ALL the entries in the iv_* table for this master
	 * object, there's another DeleteBoxMember table that DeleteCommit individual
	 * iv_* table entries.
	 * 
	 * @param aConn
	 * @param aMasterObject
	 * @param aFieldBox
	 * @throws Exception 
	 */
	public static void DeleteBoxMember(Connection aConn, Clasz<?> aMasterObject, FieldObjectBox<?> aFieldBox) throws Exception {
		String relationType = Clasz.GetIwTableName(aMasterObject.getClass(), aFieldBox.getDbFieldName());
		Table linkTable = new Table(relationType); 
		Record whereRecord = new Record();
		whereRecord.createField(aMasterObject.getPkName(), aMasterObject.getObjectId());
		linkTable.delete(aConn, whereRecord); 
	}

	public static void DeleteAllMember(Connection aConn, Clasz<?> aMaster) throws Exception {
		Record recWhereIv = new Record(); 
		recWhereIv.createField(aConn, aMaster.getField(aMaster.getPkName()));
		recWhereIv.copyValue(aConn, aMaster.getField(aMaster.getPkName()));

		String ivName = Clasz.GetIvTableName(aMaster.getClass());
		Table ivTable = new Table(ivName);
		ivTable.delete(aConn, recWhereIv); // the iv_* table stores one record and all the possible member variable to it, see the iv_tr
	}

	/*
	@Deprecated
	public static void deleteAllPolymorphic(Connection aConn, Clasz<?> aMaster) throws Exception {
		String ipName = Clasz.getPolymorphicTableName(aMaster.getClass());
		if (Database.TableExist(aConn, ipName)) {
			Record recWhereIp = new Record(); 
			recWhereIp.createFieldObject(aMaster.getField(aMaster.getPkName()));
			recWhereIp.copyValue(aMaster.getField(aMaster.getPkName()));
			Table ipTable = new Table(ipName);
			ipTable.delete(aConn, recWhereIp); 
		}
	}
	*/

	/**
	 * When persisting the object, we use only one connection so we can commit
	 * all the Insert/update of the object that consist of records from multiple
	 * table as a single transaction.
	 * 
	 * @param aConn
	 * @param aObject
	 * @return 
	 * @throws Exception 
	 */
	public static Long PersistCommit(Connection aConn, Clasz<?> aObject) throws Exception {
		Long result = null;
		boolean prevAutoCommit = aConn.getAutoCommit();
		aConn.setAutoCommit(false);
		try {
			result = ((ObjectBase) aConn.getBaseDb()).persist(aConn, aObject, false);
			aConn.commit(); 
		} catch(Exception ex) {
			aConn.rollback();
			throw new Hinderance(ex, "Persist fail, transaction rollback, clasz: " + aObject.getClass().getSimpleName());
		} finally {
			aConn.setAutoCommit(prevAutoCommit);
		}
		return(result);
	}

	public static Long PersistNoCommit(Connection aConn, Clasz<?> aObject) throws Exception {
		Long result = null;
		//boolean prevAutoCommit = aConn.getAutoCommit();
		try {
			result = ((ObjectBase) aConn.getBaseDb()).persist(aConn, aObject, false);
		} catch(Exception ex) {
			throw new Hinderance(ex, "Persist fail, transaction rollback, clasz: " + aObject.getClass().getSimpleName());
		} finally {
			//aConn.setAutoCommit(prevAutoCommit); // can't do this as it'll commit the transaction, caller to this method must handle it's autoCommit instead
		}
		return(result);
	}

	/**
	 * Places all the member object fields values into the flatten inline 
	 * fields in the root object
	 * 
	 * @param aRoot
	 * @param aConn
	 * @throws Exception 
	 */
	public static void Tree2Inline(Clasz<?> aRoot, Connection aConn) throws Exception {
		for (Field eachField : aRoot.getInstantRecord().getFieldBox().values()) { 
			if (eachField.isInline() && eachField.getDbFieldType() == FieldType.OBJECT) { 
				Clasz.PopulateInlineField(aConn, aRoot, (FieldObject<?>) eachField, eachField.getDbFieldName(), "flat");
			}
		}
	}

	public static List<Field> FlattenInlineField(Connection aConn, Field aField2Inline) throws Exception {
		List<Field> flattenFieldList = new CopyOnWriteArrayList<>();
		if (aField2Inline.getDbFieldType() == FieldType.OBJECT) { 
			Clasz<?> clasz2Inline = ((FieldObject<?>) aField2Inline).getObj();
			flattenFieldList = FlattenInlineField(aConn, clasz2Inline, aField2Inline.getDbFieldName(), flattenFieldList);
		} else if (aField2Inline.getDbFieldType() == FieldType.OBJECTBOX) { 
			int cntr = 0;
			for (Clasz<?> eachMember : ((FieldObjectBox<?>) aField2Inline).getObjectMap().values()) {
				flattenFieldList = FlattenInlineField(aConn, eachMember, cntr + "_" + aField2Inline.getDbFieldName(), flattenFieldList);
			}
		} else {
			throw new Hinderance("Only can flatten FO or FOB field type, atomic fields cannot be flatten!");
		}

		return(flattenFieldList);
	}

	private static List<Field> FlattenInlineField(Connection aConn, Clasz<?> clasz2Inline, String aAccumFieldName, List<Field> aResult) throws Exception {
		for (Field eachField : clasz2Inline.getInstantRecord().getFieldBox().values()) { 
			if (eachField.getDbFieldType() == FieldType.OBJECT) {
				String flatFieldName = Clasz.CreateDbFieldName(eachField.getDbFieldName(), aAccumFieldName); 
				Clasz<?> member2Inline = ((FieldObject<?>) eachField).getObj();
				FlattenInlineField(aConn, member2Inline, flatFieldName, aResult); // recursive to process the field, once inline all subsequent field in thie obj is inline
			} else if (eachField.getDbFieldType() == FieldType.OBJECTBOX) { 
				int cntr = 0;
				for (Clasz<?> eachMember : ((FieldObjectBox<?>) eachField).getObjectMap().values()) {
					String flatFieldName = Clasz.CreateDbFieldName(eachField.getDbFieldName(), cntr++ + "_" + aAccumFieldName); 
					FlattenInlineField(aConn, eachMember, flatFieldName, aResult); // recursive to process the field, once inline all subsequent field in thie obj is inline
				}
			} else {
				if (eachField.isSystemField() == false) {
					String dbFieldName = Clasz.CreateDbFieldName(eachField.getDbFieldName(), aAccumFieldName); // create field name prefix with the instant variable name
					Field atomicField = Field.CreateField(eachField);
					atomicField.setDbFieldName(dbFieldName);
					aResult.add(atomicField);
				}
			}
		}
		return aResult;
	}

	public Long persist(Connection aConn, Clasz<?> aObject, boolean isChildInsert) throws Exception {
		CopyOnWriteArrayList<Clasz<?>> avoidRecursive = new CopyOnWriteArrayList<>();
		Long result = this.persist(aConn, aObject, isChildInsert, avoidRecursive);
		return(result);
	}

	public static boolean AlreadyPersist(List<Clasz<?>> aAvoidRecursive, Clasz<?> aObject) throws Exception {
		boolean result = false;	
		for(Clasz<?> eachClasz : aAvoidRecursive) {
			if (eachClasz.getObjectId().equals(aObject.getObjectId()) && eachClasz.getClaszName().equals(aObject.getClaszName())) {
				result = true;
				break;
			}
		}
	
		return(result);
	}

	/**
	 * When an object persistCommit is call, the system will determine if the object to
	 * persistCommit is an object from the database or a newly created object. If its a
	 * newly created object, it is inserted, if its an existing object from the
	 * database, it is updated. 
	 * 
	 * During the insertion or updating, the inheritance tree is parse and the
	 * necessary inserting and updating is perform on the parent classes too. The
	 * child count for each of the process parent object is also incremented if
	 * new child is inserted for that parent object.
	 * 
	 * If a child of an object have already been inserted, then its parent object
	 * will also be save irregardless if the parent object is modified or not.
	 * 
	 * @param aConn
	 * @param aObject
	 * @param isChildInsert - use to track the number of child this parent object has
	 * @throws Exception 
	 */
	private Long persist(Connection aConn, Clasz<?> aObject, boolean isChildInsert, CopyOnWriteArrayList<Clasz<?>> aAvoidRecursive) throws Exception {
		try {
			if (aObject.getClaszName().equals("Clasz")) {
				return(aObject.getObjectId());
			}
	
			aObject.clearErrorField();
			if (AlreadyPersist(aAvoidRecursive, aObject) == true) {
				return(aObject.getObjectId());
			} else {
				aAvoidRecursive.add(aObject);
			}
	
			boolean childInserted = false;
			if (aObject.isPopulated() == false) { // Insert the object, if its a new object i.e. never been populated from the db before
				try {
					if (aObject.allOfMyselfIsModified() || isChildInsert) {  // will only Insert if any of its field is change, including the changed to its member objects and its parent object
						aObject.validateBeforePersist(aConn);
						if (isChildInsert) { // aObject SURE is a parent object, its child have been inserted previously, so now its parent will have child count of 1
							aObject.getFieldInt(aObject.createChildCountColName()).setModified(true);
							aObject.getFieldInt(aObject.createChildCountColName()).setValueInt(1); // increment by 1
							//aObject.getField(aObject.getChildCountName()).setFormulaStr(aObject.getChildCountNameFull(aConn) + " + ?");
							aObject.getField(aObject.createChildCountColName()).setFormulaStr("0 + ?");
						} else {
							aObject.getFieldInt(aObject.createChildCountColName()).setModified(false);
						}
			
						ObjectBase.Tree2Inline(aObject, aConn); // flatten and set the value inline object fields before the Insert
						aObject.insert(aConn);
						childInserted = true;
			
						for (Field eachField : aObject.getInstantRecord().getFieldBox().values()) { // now update/insert all member variable
							if (eachField.getDbFieldType() == FieldType.OBJECT) { // if field is of object type, recusive the persistCommit method
								if (eachField.isInline() == false) {
									Clasz<?> memberObj = (Clasz<?>)((FieldObject<?>) eachField).getObj();
									if (memberObj != null) {
										if (memberObj.getClaszName().equals("Clasz") == false) {
											//CopyOnWriteArrayList<Clasz> avoidRecursive = (CopyOnWriteArrayList<Clasz>) aAvoidRecursive.clone();
											CopyOnWriteArrayList<Clasz<?>> avoidRecursive = new CopyOnWriteArrayList<>(aAvoidRecursive);
											this.persist(aConn, memberObj, false, avoidRecursive);
											this.updateMemberOf(aConn, aObject, (FieldObject<?>) eachField); 
										}
									}
								} else {
									// inline object field, do nothing as would have been inserted by aObject.insert above
								}
							//} else if (eachField.getFieldType() == FieldType.OBJECTBOX) { 
							} else if (eachField instanceof FieldObjectBox) { 
								if (eachField.isInline() == true) { 
									App.logEror(this, "FieldObjectBox cannot be inline, ignoring it, class: " + aObject.getClass().getSimpleName() + ", field: " + eachField.getCamelCaseName());
								}

								if (((FieldClasz) eachField).getFetchStatus() == FetchStatus.SOF && ((FieldObjectBox<?> )eachField).getTotalMemberInList() == 0) {
									continue; // ignore field object box that was never populated
								}
	
								FieldObjectBox<?> memberFob = (FieldObjectBox<?>) eachField;
								this.updateBoxMember(aConn, aObject, memberFob, aAvoidRecursive); 
							}
						}
					}
			
					if (ObjectBase.ParentIsNotAtClaszYet(aObject)) {
						Clasz<?> parentObject = aObject.getParentObjectByContext(); // get the parent object of the class to be inserted 
						if (parentObject != null ) { // null means is not at Clasz yet bcos it has inheritance parent class but no parent object is set, proabably because it has inherited an abstract class
							this.persist(aConn, parentObject, childInserted, aAvoidRecursive); // recursively call Insert, now is to Insert the parent object
							if (childInserted) {
								InsertInheritance(aConn, aObject); // with the child and parent object inserted, now Insert the inheritance relationship
							}
						}
					}
				} catch(Exception ex) {
					throw new Hinderance(ex, "Fail to insert object: '" + aObject.getClaszName() + "'");
				}
			} else { // update the object
				try {
					aObject.validateBeforePersist(aConn);
					aObject.getFieldInt(aObject.createChildCountColName()).setModified(false); // don't touch the child count field
					ObjectBase.Tree2Inline(aObject, aConn); // flatten and set the values in the inline object fields before the update 
					if (aObject.onlyMyselfIsModified()) {  // will only update if modified
						aObject.update(aConn);
					}
		
					for (Field eachField : aObject.getInstantRecord().getFieldBox().values()) { // now update/insert all member variable
						if (eachField.getDbFieldType() == FieldType.OBJECT) { // if field is of object type, recusive the persistCommit method
							if (eachField.isInline() == false) {
								Clasz<?> memberObj = (Clasz<?>)((FieldObject<?>) eachField).getObj();
								if (memberObj == null && eachField.isModified()) {
									this.updateMemberOf(aConn, aObject, (FieldObject<?>) eachField); 
								} else if (memberObj != null && memberObj.getClaszName().equals("Clasz") == false) {
									//CopyOnWriteArrayList<Clasz> avoidRecursive = (CopyOnWriteArrayList<Clasz>) aAvoidRecursive.clone();
									CopyOnWriteArrayList<Clasz<?>> avoidRecursive = new CopyOnWriteArrayList<>(aAvoidRecursive);
									this.persist(aConn, memberObj, false, avoidRecursive);
									this.updateMemberOf(aConn, aObject, (FieldObject<?>) eachField); 
								}
							} else { 
								// inline object field, do nothing, how is inline persist? but its working
							}
						//} else if (eachField.getFieldType() == FieldType.OBJECTBOX) { 
						} else if (eachField instanceof FieldObjectBox) { 
							if (eachField.isInline() == true) { 
								App.logEror(this, "FieldObjectBox cannot be inline, ignoring it, class: " + aObject.getClass().getSimpleName() + ", field: " + eachField.getCamelCaseName());
							}

							if (((FieldClasz) eachField).getFetchStatus() == FetchStatus.SOF && ((FieldObjectBox<?>) eachField).getTotalMemberInList() == 0) {
								continue; // ignore field object box that was never populated
							}
	
							FieldObjectBox<?> memberFob = (FieldObjectBox<?>) eachField;
							this.updateBoxMember(aConn, aObject, memberFob, aAvoidRecursive); 
						}
					}
		
					if (ObjectBase.ParentIsNotAtClaszYet(aObject)) {
						Clasz<?> parentObject = aObject.getParentObjectByContext(); // get the parent object of the class to be updated
						if (parentObject != null ) { // null means is not at Clasz yet bcos it has inheritance parent class but no parent object is set, proabably because it has inherited an abstract class
							this.persist(aConn, parentObject, false, aAvoidRecursive); // recursively call Insert, now is to Insert the parent object
						}
					}
				} catch(Exception ex) {
					throw new Hinderance(ex, "Fail to update object: '" + aObject.getClaszName() + "'");
				}
			}
			aObject.updateIndex(aConn);
		} catch(Exception ex) {
			aObject.handleError(ex);
			throw new Hinderance(ex);
		}
		return(aObject.getObjectId());
	}

	/**
	 * Fetches an object as specify in the pass in criteria. The 
	 * criteria object contain fields with the object to be retrieve
	 * must match.
	 
	 * Before the real Fetch is done, the pass in criteria is checkto 
	 * determine if the object can be Fetch with using one sql Fetch or
	 * need to be slowly Fetch with multiple sql Fetch.
	 * 
	 * @param aConn
	 * @param aCriteria
	 * @return
	 * @throws Exception 
	 */
	public static Clasz<?> FetchObject(Connection aConn, Clasz<?> aCriteria) throws Exception {
		Clasz<?> result = Clasz.Fetch(aConn, aCriteria);
		return result;
	}

	public Clasz<?> fetchObject(Clasz<?> aCriteria) throws Exception {
		Connection conn = null;
		try {
			conn = this.connPool.getConnection();
			Clasz<?> result = Clasz.Fetch(conn, aCriteria);
			return(result);
		} finally {
			if (conn != null) this.connPool.freeConnection(conn);
		}
	}

	public static Clasz<?> FetchObjectFreeType(Connection aConn, Class<?> aClass, Long aObjId) throws Exception {
		// return FetchObject(aConn, aClass, aObjId);
		Clasz<?> result = Clasz.FetchFreeType(aConn, aClass, aObjId);
		return result;
	}

	public static <Ty extends Clasz<?>> Ty FetchObject(Connection aConn, Class<Ty> aClass, Long aObjId) throws Exception {
		Ty result = Clasz.Fetch(aConn, aClass, aObjId);
		return result;
	}

	public <Ty extends Clasz<?>> Ty fetchObject(Class<Ty> aClass, Long aObjId) throws Exception {
		Connection conn = null;
		try {
			conn = this.connPool.getConnection();
			Ty result = Clasz.Fetch(conn, aClass, aObjId);
			return result;
		} finally {
			if (conn != null) this.connPool.freeConnection(conn);
		}
		
	}

	public static ResultSet FetchIndexKey(Connection aConn, Class<?> aClass, PreparedStatement aStmt, String aIndexName, String aWhereClause, String aOrderClause) throws Exception {
		ResultSet result = null;
		if (Clasz.class.isAssignableFrom(aClass)) {
			String pkName = Clasz.CreatePkColName(aClass);
			String strSql = "select " + pkName + " from " + aIndexName;
			if (aWhereClause.isEmpty() == false) {
				strSql += " where " + aWhereClause;
			}
			if (aOrderClause.isEmpty() == false) {
				strSql += " order by " + aOrderClause;
			}
			aStmt = aConn.prepareStatement(strSql);
			result = aStmt.executeQuery();
		} else {
			throw new Hinderance("Cannot fetch objects from database for '" + aClass.getSimpleName() + "', because it is not CLASZ type");
		}
		return(result);
	}

	public ResultSet fetchAllByChrono(Class<?> aClass, PreparedStatement aStmt) throws Exception {
		ResultSet result = null;
		if (Clasz.class.isAssignableFrom(aClass)) {
			Connection conn = null;
			try {
				conn = this.getConnPool().getConnection();
				String pkName = Clasz.CreatePkColName(aClass);
				String strSql = "select " + pkName + " from " + Clasz.CreateTableName(aClass) + " order by " + pkName + " desc";
				aStmt = conn.prepareStatement(strSql);
				result = aStmt.executeQuery();
			} finally {
				if (conn != null) this.getConnPool().freeConnection(conn);
			}
		} else {
			throw new Hinderance("Cannot fetch objects from database for '" + aClass.getSimpleName() + "', because it is not CLASZ type");
		}
		return(result);
	}

	public static ResultSet FetchAllByChrono(Connection aConn, Class<?> aClass, PreparedStatement aStmt) throws Exception {
		ResultSet result = null;
		if (Clasz.class.isAssignableFrom(aClass)) {
			try {
				String pkName = Clasz.CreatePkColName(aClass);
				String strSql = "select " + pkName + " from " + Clasz.CreateTableName(aClass) + " order by " + pkName + " desc";
				aStmt = aConn.prepareStatement(strSql);
				result = aStmt.executeQuery();
			} finally {
			}
		} else {
			throw new Hinderance("Cannot fetch objects from database for '" + aClass.getSimpleName() + "', because it is not 'Clasz' type");
		}
		return(result);
	}

	public Clasz<?> fetchNext(Connection aConn, Class<?> aClass, ResultSet aRset) throws Exception {
		Clasz<?> result = null;
		if (Clasz.class.isAssignableFrom(aClass)) {
			if (aRset.next()) {
				//Clasz objCriteria = this.createObject(aClass);
				Clasz<?> objCriteria = this.createClasz(aConn, aClass);
				objCriteria.setObjectId(aRset.getLong(Clasz.CreatePkColName(aClass)));
				result = this.fetchObject(objCriteria);
				if (result == null) {
					throw new Hinderance("Fail to fetch the next object from the database, for: '" + aClass.getSimpleName() + "'");
				}
			}
		} else {
			throw new Hinderance("Cannot fetch the next object from class not inherited from Clasz for: '" + aClass.getSimpleName() + "'");
		}
		return result;
	}

	public static Clasz<?> FetchNext(Connection aConn, Class<?> aClass, ResultSet aRset) throws Exception {
		Clasz<?> result = null;
		if (Clasz.class.isAssignableFrom(aClass)) {
			if (aRset.next()) {
				//Clasz objCriteria = ObjectBase.CreateObject(aConn, aClass);
				Clasz<?> objCriteria = ObjectBase.CreateClaszFreeType(aConn, aClass);
				objCriteria.setObjectId(aRset.getLong(Clasz.CreatePkColName(aClass)));
				result = ObjectBase.FetchObject(aConn, objCriteria);
				if (result == null) {
					throw new Hinderance("Fail to fetch the next object from the database, for: '" + aClass.getSimpleName() + "'");
				}
			}
		} else {
			throw new Hinderance("Cannot fetch the next object from class not inherited from Clasz for: '" + aClass.getSimpleName() + "'");
		}
		return(result);
	}

	public static String GetIvPolymorphicTableName(Connection aConn, FieldObject<?> aMemberField) throws Exception {
		String memberTableName;
		Long memberOid = aMemberField.getObj().getObjectId();
		Class<?> memberClass = aMemberField.getObjectClass(aConn); 
		if (memberOid != null && memberOid != Clasz.NOT_INITIALIZE_OBJECT_ID) {  
			// with oid, we can get the right polymorhic clasz for this object, its member table is from the leaf_class column, need to get this and get the member table name, do it later/needed 
			Clasz<?> parentObject = aMemberField.getMasterObject();
			Class<?> parentClass = parentObject.getClass();

			String parentPkName = Clasz.CreatePkColName(parentClass);
			Long parentPkValue = parentObject.getObjectId();
			String memberName = aMemberField.getDbFieldName();

			Field fieldParentPk = new FieldLong(parentPkName, parentPkValue);
			Field fieldMemberPk = new FieldLong(memberName, memberOid);
			Record whereRec = new Record();
			whereRec.createField(aConn, fieldParentPk);
			whereRec.createField(aConn, fieldMemberPk);

			String memberColName;
			Record selectRec = new Record();

			memberColName = Clasz.CreateLeafClassColName(memberName);
			selectRec.createField(memberColName, FieldType.STRING, CLASS_NAME_LEN); // select the primary key of the child clszObject
			String ivTableName = Clasz.GetIvTableName(parentClass);

			Table ivTable = new Table(ivTableName);
			if (ivTable.fetch(aConn, selectRec, whereRec) == 1) {
				String memberClassName = selectRec.getField(memberColName).getValueStr();
				memberTableName = selectRec.getValueStr(Class.forName(memberClassName).getSimpleName());
			} else {
				throw new Hinderance("Missing or more then one member record, iv table: " + ivTableName + ", parent pk: " + parentPkName + ", value: " + parentPkValue + ", member field: " + memberName + ", value: " + memberOid);
			}
		} else {
			memberTableName = Clasz.CreateTableName(memberClass); // user have to state if the search uses normal or polymorhic clasz 
		}
		return(memberTableName);
	}

	public static String GetIwPolymorphicTableName(Connection aConn, FieldObjectBox<?> aMemberBoxField, Clasz<?> aMemberClasz) throws Exception {
		String memberTableName;
		Long memberOid = aMemberClasz.getObjectId();
		Class<?> memberClass = aMemberClasz.getClass();

		if (memberOid != null && memberOid != Clasz.NOT_INITIALIZE_OBJECT_ID) {  
			// with oid, we can get the right polymorhic clasz for this object, its member table is from the leaf_class column, need to get this and get the member table name, do it later/needed 
			Clasz<?> parentObject = aMemberBoxField.getMasterObject();
			Class<?> parentClass = parentObject.getClass();

			String parentPkName = Clasz.CreatePkColName(parentClass);
			Long parentPkValue = parentObject.getObjectId();
			String memberName = aMemberBoxField.getDbFieldName();

			Field fieldParentPk = new FieldLong(parentPkName, parentPkValue);
			Field fieldMemberPk = new FieldLong(memberName, memberOid);
			Record whereRec = new Record();
			whereRec.createField(aConn, fieldParentPk);
			whereRec.createField(aConn, fieldMemberPk);

			String memberColName;
			Record selectRec = new Record();
			memberColName = LEAF_CLASS;
			selectRec.createField(LEAF_CLASS, FieldType.STRING, CLASS_NAME_LEN); // select the primary key of the child clszObject

			String iwTableName = Clasz.GetIwTableName(aMemberBoxField);
			Table iwTable = new Table(iwTableName);
			if (iwTable.fetch(aConn, selectRec, whereRec) == 1) {
				String memberClassName = selectRec.getField(memberColName).getValueStr();
				memberTableName = selectRec.getValueStr(Class.forName(memberClassName).getSimpleName());
			} else {
				throw new Hinderance("Missing or more then one member record, iw table: " + iwTableName + ", parent pk: " + parentPkName + ", value: " + parentPkValue + ", member field: " + memberName + ", value: " + memberOid);
			}
		} else {
			memberTableName = Clasz.CreateTableName(memberClass); // user have to state if the search uses normal or polymorhic clasz 
		}
		return(memberTableName);
	}

	private static void GetJoinMemberClause(Connection aConn, Multimap<String, Record> aWhereBox, Class<?> aParentClass, FieldObject<?> aMemberField) throws Exception {
		String ivTableName = Clasz.GetIvTableName(aParentClass);
		Long memberOid = aMemberField.getObj().getObjectId();
		if (memberOid != null && memberOid != Clasz.NOT_INITIALIZE_OBJECT_ID) { // if member object has object id, then will need to get the member join clause
			Record linkIv2Member = new Record();
			linkIv2Member.createField(aMemberField.getDbFieldName(), memberOid);
			aWhereBox.put(ivTableName, linkIv2Member);
		} else {
			Record linkIv2Member = new Record();
			aWhereBox.put(ivTableName, linkIv2Member); // need iv table in from clause, but link to empty record
		}

		// link iv table to master table
		Record whereRecParent = new Record();
		String parentPkName = Clasz.CreatePkColName(aParentClass);
		whereRecParent.createField(parentPkName, "");
		String parentTableName = Clasz.CreateTableName(aParentClass);
		whereRecParent.getField(parentPkName).setFormulaStr(ivTableName + "." + parentPkName + " = " + parentTableName + "." + parentPkName);
		aWhereBox.put(parentTableName, whereRecParent);

		// link iv table to member table
		Record whereRecLink = new Record();
		whereRecLink.createField(aMemberField.getDbFieldName(), "");
		String memberTableName;
		if (aMemberField.isPolymorphic() == false) {
			memberTableName = Clasz.CreateTableName(aMemberField.getObjectClass());
		} else {
			App.logWarn(ObjectBase.class, "Using polymorphic object field as search criteria, remember to use the right polymorphic type in your criteria");
			memberTableName = GetIvPolymorphicTableName(aConn, aMemberField);
		}
		String memberPkName = Clasz.CreatePkColName(aMemberField.getObjectClass());
		whereRecLink.getField(aMemberField.getDbFieldName()).setFormulaStr(ivTableName + "." + aMemberField.getDbFieldName() + " = " + memberTableName + "." + memberPkName);
		aWhereBox.put(memberTableName, whereRecLink);
	}

	public static void GetLeafSelectCriteria(Connection aConn, Clasz<?> aCriteria, Multimap<String, Record> aWhereBox) throws Exception {
		String accumFieldName = "";
		Clasz<?> highestParent2Link = GetHighestParent2Link(aConn, aCriteria);
		GetLeafSelectCriteria(aConn, aCriteria, aWhereBox, accumFieldName, highestParent2Link);
	}

	private static void SetWhereRecField(Connection aConn, Record aWhereRec, Field aCriteriaField) throws Exception {
		if (aCriteriaField.getFormulaStr().isEmpty()) {
			aWhereRec.createField(aConn, aCriteriaField);
			aWhereRec.copyValue(aConn, aCriteriaField);
		} else {
			Field newField = aWhereRec.createField(aConn, aCriteriaField);
			aWhereRec.copyValue(aConn, aCriteriaField);
			newField.setFormulaStr(aCriteriaField.getFormulaStr());
		}
	}

	/*
	private static List<Object> LinkFob2Parent(Connection aConn, FieldObjectBox<?> aFobMember) throws Exception {
		Clasz aMemberParent = aFobMember.getMasterObject();
		String iwBoxTableName = Clasz.GetIwTableName(aFobMember);
		Record linkIwBox2Member = new Record();
		linkIwBox2Member.createField(aFobMember.getFieldName(), "");
		String parentTableName;
		if (aFobMember.isPolymorphic() == false) {
			parentTableName = Clasz.CreateTableName(aMemberParent.getClass());
		} else {
			App.logWarn(ObjectBase.class, "Using polymorphic FOB as search criteria, remember to use the right polymorphic type in your criteria");
			parentTableName = GetIwPolymorphicTableName(aConn, aFobMember, aMemberParent);
		}
		String parentPkName = Clasz.CreatePkColName(aMemberParent.getClass());
		linkIwBox2Member.getField(aFobMember.getFieldName()).setFormulaStr(iwBoxTableName + "." + parentPkName + " = " + parentTableName + "." + parentPkName);

		List<Object> result = new CopyOnWriteArrayList<>();
		result.add(parentTableName);
		result.add(linkIwBox2Member);
		return result;
	}
	*/

	// link fob to member only i.e. iw_ table links to member, doesn't link to its parent
	private static List<Object> LinkFob2Member(Connection aConn, FieldObjectBox<?> aFobMember) throws Exception {
		Clasz<?> aMemberCriteria = aFobMember.getMetaObj();
		String iwBoxTableName = Clasz.GetIwTableName(aFobMember);
		Record linkIwBox2Member = new Record();
		linkIwBox2Member.createField(aFobMember.getDbFieldName(), "");
		String memberTableName;
		if (aFobMember.isPolymorphic() == false) {
			memberTableName = Clasz.CreateTableName(aMemberCriteria.getClass());
		} else {
			App.logWarn(ObjectBase.class, "Using polymorphic FOB as search criteria, remember to use the right polymorphic type in your criteria");
			memberTableName = GetIwPolymorphicTableName(aConn, aFobMember, aMemberCriteria);
		}
		String memberPkName = Clasz.CreatePkColName(aMemberCriteria.getClass());
		linkIwBox2Member.getField(aFobMember.getDbFieldName()).setFormulaStr(iwBoxTableName + "." + aFobMember.getDbFieldName() + " = " + memberTableName + "." + memberPkName);

		List<Object> result = new CopyOnWriteArrayList<>();
		result.add(memberTableName);
		result.add(linkIwBox2Member);
		return result;
	}

	private static void GetLeafSelectCriteria(Connection aConn, Clasz<?> aCriteria, Multimap<String, Record> aWhereBox, String aAccumFieldName, Clasz<?> aHighestParent2Link) throws Exception {
		Record whereRec = new Record();
		String whereTableName = aCriteria.getTableName();
		for (Field eachField : aCriteria.getInstantRecord().getFieldBox().values()) { // for each populated field, build up the sql where clause
			if (eachField.isModified()) {
				if (eachField.isInline() == false) {
					if (eachField.getDbFieldType() == FieldType.OBJECT) { // if field is of object type, traverse it
						GetJoinMemberClause(aConn, aWhereBox, aCriteria.getClass(), (FieldObject<?>) eachField);
						Clasz<?> fieldObject = ((FieldObject<?>) eachField).getObj(); // convert table type to Clasz type
						if (fieldObject.getClaszName().equals("Clasz") == false) {
							GetLeafSelectCriteria(aConn, fieldObject, aWhereBox);
						}
					} else if (eachField.getDbFieldType() == FieldType.OBJECTBOX) {
						Multimap<String, Record> fobWhereBox = Multimaps.synchronizedListMultimap(ArrayListMultimap.create());
						FieldObjectBox<?> fobField = (FieldObjectBox<?>) eachField;
						for(Clasz<?> eachFobCriteria: fobField.getObjectMap().values()) {
							GetLeafSelectCriteria(aConn, eachFobCriteria, fobWhereBox, aAccumFieldName, aHighestParent2Link); 
						}

						int cntrRec = 0;
						Record whereRecFob = new Record();
						String tableNameFob = "";
						for (Entry<String, Record> entry : fobWhereBox.entries()) {
							tableNameFob = entry.getKey();
							String tableName = tableNameFob + "_" + cntrRec++;
							Record eachRec = entry.getValue();
							whereRecFob.createField(tableName, eachRec);
						}
						if (whereRecFob.totalField() > 0) {
							aWhereBox.put(tableNameFob, whereRecFob);
						}
					} else {
						SetWhereRecField(aConn, whereRec, eachField);
					}
				} else { // handle inline field
					if (eachField.getDbFieldType() == FieldType.OBJECT) { // if field is of inline object type, traverse it
						FieldObject<?> inlineField = (FieldObject<?>) eachField;
						Clasz<?> memberObj = inlineField.getObj();
						if (memberObj != null) {
							for (Field eachInlineField : memberObj.getInstantRecord().getFieldBox().values()) {
								if (eachInlineField.getDbFieldType() == FieldType.OBJECT || eachInlineField.getDbFieldType() == FieldType.OBJECTBOX) { // TODO handle objectbox type
									String dbFieldName = Clasz.CreateDbFieldName(eachInlineField.getDbFieldName(), aAccumFieldName); // create the field name of the inline field from its member name and its field name
									GetLeafSelectCriteria(aConn, aCriteria, aWhereBox, dbFieldName, aHighestParent2Link); // recursive to accumulate inline field name
								} else {
									if (eachInlineField.isSystemField() == false) {
										Clasz<?> masterClasz = eachField.getMasterObject();
										String inlineFieldName = Clasz.CreateDbFieldName(eachInlineField.getDbFieldName(), aAccumFieldName);
										Field fieldRec = masterClasz.getField(inlineFieldName);
										SetWhereRecField(aConn, whereRec, fieldRec);
									} else {
										// ignore system field
									}
								}
							}
						}
					} else if (eachField.getDbFieldType() == FieldType.OBJECTBOX) {
						// kiv, currently not supported yet
					} else {
						throw new Hinderance("Internal application error, inline field can only be of FieldObject or FieldObjectBox type!");
					}
				}
			}
		}
		if (whereRec.totalField() > 0) {
			aWhereBox.put(whereTableName, whereRec);
		}

		if (ObjectBase.ParentIsNotAtClaszYet(aCriteria)) { 
			Clasz<?> parentObject = aCriteria.getParentObjectByContext();
			if (parentObject != null && parentObject.getClass().equals(Clasz.class) == false) { // no parent clszObject because its an abstract class and no inherited normal class
				if (aHighestParent2Link.getClass().equals(parentObject.getClass()) == false) { // if haven't reach the highest parent that have modified field, recurse
					GetInheritanceLink(aConn, aWhereBox, parentObject, aCriteria);
					GetLeafSelectCriteria(aConn, parentObject, aWhereBox, aAccumFieldName, aHighestParent2Link); // recurse only up to parent with edited fields
				}
			}
		}
	}

	public static Clasz<?> GetHighestParent2Link(Connection aConn, Clasz<?> aLeafClasz) throws Exception {
		Clasz<?> highestParent2Link = aLeafClasz;
		GetHighestParent2Link(aConn, aLeafClasz, highestParent2Link);
		return highestParent2Link;
	}

	private static void GetHighestParent2Link(Connection aConn, Clasz<?> aLeafClasz, Clasz<?> aHighestParent2Link) throws Exception {
		if (ObjectBase.ParentIsNotAtClaszYet(aLeafClasz)) { 
			Clasz<?> parentObject = aLeafClasz.getParentObjectByContext();
			if (parentObject != null && parentObject.getClass().equals(Clasz.class) == false) { // no parent clszObject because its an abstract class and no inherited normal class
				for (Field eachField : aLeafClasz.getInstantRecord().getFieldBox().values()) { 
					if (eachField.isModified()) {
						aHighestParent2Link = parentObject;
						break;
					}
				}
				GetHighestParent2Link(aConn, parentObject, aHighestParent2Link);
			}
		}
	}

	public static void GetInheritanceLink(Connection aConn, Multimap<String, Record> aWhereBox, Clasz<?> aParentObject, Clasz<?> aChildObject) throws Exception {
		String ihTableName = Clasz.GetIhTableName(aChildObject.getClass());

		Record linkParentRec = new Record();
		String parentPkName = Clasz.CreatePkColName(aParentObject.getClass());
		linkParentRec.createField(parentPkName, "");
		String parentTableName = Clasz.CreateTableName(aParentObject.getClass());
		linkParentRec.getField(parentPkName).setFormulaStr(ihTableName + "." + parentPkName + " = " + parentTableName + "." + parentPkName);
		aWhereBox.put(parentTableName, linkParentRec);
		

		Record linkChildRec = new Record();
		String childPkName = Clasz.CreatePkColName(aChildObject.getClass());
		linkChildRec.createField(childPkName, "");
		String childTableName = Clasz.CreateTableName(aChildObject.getClass());
		linkChildRec.getField(childPkName).setFormulaStr(ihTableName + "." + childPkName + " = " + childTableName + "." + childPkName);
		aWhereBox.put(ihTableName, linkChildRec);
	}

	public static void GetMemberBoxByMemberCriteria(Connection aConn, FieldObjectBox<?> aFobMember, Clasz<?> aMemberCriteria, Multimap<String, Record> aWhereBox) throws Exception {
		if (aFobMember.isInline() == false) {
			/*
			String iwBoxTableName = Clasz.GetIwTableName(aFobMember);
			Record linkIwBox2Member = new Record();
			linkIwBox2Member.createField(aFobMember.getFieldName(), "");
			String memberTableName;
			if (aFobMember.isPolymorphic() == false) {
				memberTableName = Clasz.CreateTableName(aMemberCriteria.getClass());
			} else {
				App.logWarn(ObjectBase.class, "Using polymorphic FOB as search criteria, remember to use the right polymorphic type in your criteria");
				memberTableName = GetIwPolymorphicTableName(aConn, aFobMember, aMemberCriteria);
			}
			String memberPkName = Clasz.CreatePkColName(aMemberCriteria.getClass());
			linkIwBox2Member.getField(aFobMember.getFieldName()).setFormulaStr(iwBoxTableName + "." + aFobMember.getFieldName() + " = " + memberTableName + "." + memberPkName);
			*/
			List<Object> linkFob2Member = LinkFob2Member(aConn, aFobMember);
			String memberTableName = (String) linkFob2Member.get(0);
			Record linkIwBox2Member = (Record) linkFob2Member.get(1);

			aWhereBox.put(memberTableName, linkIwBox2Member);
			GetLeafSelectCriteria(aConn, aMemberCriteria, aWhereBox); // link iw table to it's member
		} else {
			App.logWarn(ObjectBase.class, "Using inline FOB field as search criteria, nothing wrong but please check");
			Record linkIvBox2Parent = new Record();
			Class<?> parentClass = aFobMember.getMasterClass();
			String parentPkName = Clasz.CreatePkColName(parentClass);
			String parentTableName = Clasz.CreateTableName(parentClass);
			String ivBoxTableName = Clasz.GetIwTableName(aFobMember);
			linkIvBox2Parent.getField(aFobMember.getDbFieldName()).setFormulaStr(ivBoxTableName + "." + parentPkName + " = " + parentTableName + "." + parentPkName);
			aWhereBox.put(parentTableName, linkIvBox2Parent);
			GetLeafSelectCriteria(aConn, aMemberCriteria, aWhereBox); // with inline field, we link it to it's own parent table
		}
	}

	/**
	 * Below is a inheritance relationship between user, employee
	 * and person.  The user is a type of employee and employee is a 
	 * type of person. They're inherited related by the ih_user 
	 * and ih_employee table.
	 * 
	 *   user     employee   person
	 *     \       /  \       /   
	 *      \     /    \     /   
	 *     ih_user    ih_employee
	 * 
	 * The SQL to retrieve all the related records is shown below:
	 * 
	 * select * from cz_user, ih_user, cz_employee, ih_employee, cz_person
	 * where cz_user.cz_user_pk = ih_user.cz_user_pk
	 * and ih_user.cz_employee_pk = cz_employee.cz_employee_pk
	 * and cz_employee.cz_employee_pk = ih_employee.cz_employee_pk
	 * and ih_employee.cz_person_pk = cz_person.cz_person_pk; 
	 * 
	 * 
	 * 
	 * @param aObject
	 * @param strInherit
	 * @param strTable
	 * @throws Exception 
	 * 
	 */
	public static void getInheritanceWhere(Clasz<?> aObject, StringBuffer strInherit, StringBuffer strTable) throws Exception {
		if (strTable.toString().isEmpty() == false) {
			strTable.append(", ");
		}

		if (aObject.getParentObjectByContext() != null && ObjectBase.ParentIsNotAtClaszYet(aObject)) { // it is null if parent class is abstract up the tree till Clasz class, else surely got parent object
			strTable.append(aObject.getTableName() + ", " + aObject.getIhTableName());

			if (strInherit.toString().isEmpty() == false) {
				strInherit.append(" and ");
			}
			strInherit.append(aObject.getTableName() + "." + aObject.getPkName() + " = " + aObject.getIhTableName() + "." + aObject.getPkName());
			strInherit.append(" and " + aObject.getIhTableName() + "." + aObject.getParentObjectByContext().getPkName() + " = " + aObject.getParentObjectByContext().getTableName() + "." + aObject.getParentObjectByContext().getPkName());

			getInheritanceWhere(aObject.getParentObjectByContext(), strInherit, strTable);
		} else {
			strTable.append(aObject.getTableName()); // at the highest class level
		}
	}


	/**
	* TODO not sure if this will work when there is member object in a member object
	* TODO also not sure if this will work if member object contain the same member object type recursively
	* 
	* select * from cz_user, ih_user, cz_employee, ih_employee, cz_person, cz_money
	* where (
	* cz_user.cz_user_pk = ih_user.cz_user_pk
	* and ih_user.cz_employee_pk = cz_employee.cz_employee_pk
	* and cz_employee.cz_employee_pk = ih_employee.cz_employee_pk
	* and ih_employee.cz_person_pk = cz_person.cz_person_pk
	* )
	* and (
	* cz_money.cz_money_pk = (
	* select iv_employee.employee_salary from iv_employee where cz_employee.cz_employee_pk = iv_employee.cz_employee_pk
	* )
	* );
	* 
	* 
	* @param aMaster
	* @param strInherit
	* @param strTable
	* @throws Exception 
	*/
	public static void getMemberOfWhere(Connection aConn, Clasz<?> aMaster, StringBuffer strInherit, StringBuffer strTable) throws Exception {
		for (Field eachField : aMaster.getInstantRecord().getFieldBox().values()) {
			if (eachField.isInline() == false && eachField.getDbFieldType() == FieldType.OBJECT) { 
				Clasz<?> memberObj = ((FieldObject<?>) eachField).getValueObj(aConn);

				if (strTable.toString().isEmpty() == false) {
					strTable.append(", ");
				}
				strTable.append(memberObj.getTableName());

				if (strInherit.toString().isEmpty() == false) {
					strInherit.append(" and ");
				}
				strInherit.append(memberObj.getTableName() + "." + memberObj.getPkName());
				strInherit.append(" = (select " + eachField.getDbFieldName() + " from " + aMaster.getIvTableName() + " where " + aMaster.getTableName() + "." + aMaster.getPkName() + " = " + aMaster.getIvTableName() + "." + aMaster.getPkName() + ")");
				getMemberOfWhere(aConn, memberObj, strInherit, strTable); // recursive call to flatten the object hierarchy into a single sql statement
			}
		}
	}

	/**
	 * Determine if the pass in object is Clasz type, aObject MUST be of
	 * Clasz type and not the child of Clasz. This method is use to when
	 * traversing the inheritance tree of aObject. As the traversal travel 
	 * from child to parent, it will call this method to determine if the 
	 * traversing process have reach the instance of Clasz type. It it does,
	 * then it indicate its at the end of the inheritance traversing path.
	 * 
	 * @param aObject
	 * @return 
	 */
	public static boolean IsNotAtClaszYet(Clasz<?> aObject) {
		boolean result;
		if (aObject == null || aObject.getClass() == Clasz.class) {
			result = false;
		} else {
			result = true;
		}
		return(result);
	}

	public static boolean ParentIsNotAtClaszYet(Clasz<?> aObject) {
		return(ParentIsNotAtClaszYet(aObject.getClass()));
	}

	public static boolean ParentIsNotAtClaszYet(Class<?> aClass) {
		boolean result;
		Class<?> ParentClass = aClass.getSuperclass();
		if (ParentClass == null || ParentClass.equals(Clasz.class)) {
			result = false;
		} else {
			result = true;
		}
		return(result);
	}

	public static boolean ParentIsNotAbstract(Clasz<?> aObject) {
		return(ParentIsNotAbstract(aObject.getClass()));
	}

	public static boolean ParentIsNotAbstract(Class<?> aClass) {
		boolean result;
		Class<?> ParentClass = aClass.getSuperclass();
		if (Modifier.isAbstract(ParentClass.getModifiers())) {
			result = false;
		} else {
			result = true;
		}
		return(result);
	}


	/*
	public static String CreateObjectIndex(Connection aConn, Clasz<?> aClasz) throws Exception {
		String result = ObjectIndex.CreateObjectIndex(aConn, aClasz);
		return(result);
	}
	*/

	public String createFieldIndex(Clasz<?> aClasz, String aFieldName) throws Exception {
		Connection conn = this.connPool.getConnection();
		try {
			String result = ObjectIndex.createFieldIndex(conn, aClasz, aFieldName);
			return(result);
		} finally {
			if (conn != null) {
				this.connPool.freeConnection(conn);
			}
		}		
	}

	public static boolean ObjectExist(Connection aConn, Clasz<?> aObject) throws Exception {
		boolean result = false;
		long count = ObjectCount(aConn, aObject, true);
		if (count > 0) result = true;
		return(result);
	}

	public static long ObjectCount(Connection aConn, Clasz<?> aObject, boolean aCheckExistOnly) throws Exception {
		//Map<String, Record> whereBox = new ConcurrentHashMap<>(); // each table name (string) and a record for the where fields (record)
		Multimap<String, Record> whereBox = Multimaps.synchronizedListMultimap(ArrayListMultimap.create());
		ObjectBase.GetLeafSelectCriteria(aConn, aObject, whereBox); 
		StringBuffer strBuffer = new StringBuffer();
		List<Field> aryWhere = Database.GetWhereClause(aConn, whereBox, strBuffer); // convert the where record into array list
		String sqlStr = "select * from " + Database.GetFromClause(aObject.getTableName(), whereBox);
		sqlStr += " where " + strBuffer.toString();

		int cntrRec = 0;
		PreparedStatement stmt = null; // now do the sql Fetch
		ResultSet rs = null;
		try {
			stmt = aConn.prepareStatement(sqlStr);
			Database.SetStmtValue(aConn, stmt, aryWhere);
			rs = stmt.executeQuery();
			while (rs.next()) {
				aObject.setObjectId(rs.getLong(aObject.getPkName()));
				cntrRec++;
				if (aCheckExistOnly) break;
			}
		} catch (Exception ex) {
			if (stmt != null) {
				throw new Hinderance(ex, "Fail retrieving pk: " + stmt.toString());
			} else {
				throw new Hinderance(ex, "Fail retrieving pk");
			}
		} finally {
			if (rs != null) {
				rs.close();
			}
			if (stmt != null) {
				stmt.close();
			}
		}

		return(cntrRec);
	}

	public static void LoadAllClasz(Connection aConn, Class<?> aClass, CopyOnWriteArrayList<Clasz<?>> aResultList) throws Exception {
		PreparedStatement stmt = null;
		ResultSet rset = null;
		Clasz<?> objFetched;
		aResultList.clear();
		try {
			rset = ObjectBase.FetchAllByChrono(aConn, aClass, stmt);
			while((objFetched = (Clasz<?>) ObjectBase.FetchNext(aConn, aClass, rset)) != null) {
				aResultList.add(objFetched);
			}
		} finally {
			if (rset != null) {
				rset.close();
			}
			if (stmt != null) {
				stmt.close();
			}
		}
	}

	public static boolean Exist(Connection aConn, Clasz<?> aCriteria) throws Exception {
		boolean result = false;

		// set the where record
		//Map<String, Record> whereBox = new ConcurrentHashMap<>(); // each table name (string) and a record for the where fields (record)
		Multimap<String, Record> whereBox = Multimaps.synchronizedListMultimap(ArrayListMultimap.create());
		ObjectBase.GetLeafSelectCriteria(aConn, aCriteria, whereBox); // the select criteria for this leaf clszObject, doesn't do the parent clszObject

		// place in the where criteria into sql string
		StringBuffer strBuffer = new StringBuffer();
		List<Field> aryWhere = Database.GetWhereClause(aConn, whereBox, strBuffer); // convert the where record into array list
		String sqlStr = "select * from " + Database.GetFromClause(aCriteria.getTableName(), whereBox);
		sqlStr += " where " + strBuffer.toString();

		PreparedStatement stmt = null; 
		ResultSet rs = null;
		try {
			stmt = aConn.prepareStatement(sqlStr);
			Database.SetStmtValue(aConn, stmt, aryWhere);
			rs = stmt.executeQuery();
			while (rs.next()) {
				result = true;
				break;
			}
		} catch (Exception ex) {
			if (stmt != null) {
				throw new Hinderance(ex, "Fail retrieving pk: " + stmt.toString());
			} else {
				throw new Hinderance(ex, "Fail retrieving pk");
			}
		} finally {
			if (rs != null) {
				rs.close();
			}
			if (stmt != null) {
				stmt.close();
			}
		}

		return(result);
	}

	/*
	private static FetchStatus FetchIfGotMember(Connection aConn, FieldObjectBox<?> aFetchedBox, Clasz<?> aMasterClasz, FieldObject<?> aWantedMember, String aSortField, String aSortValue) throws Exception {
		FetchStatus fetchStatus = FetchIfGotMember(aConn, aFetchedBox, aMasterClasz, aWantedMember, 5, "next", SortOrder.DSC, aSortField, aSortValue);
		return(fetchStatus);
	}
	*/

	// Fetches objects in aMasterClasz that have member object aWantedMember
	// This is refactor copy for FieldObjectBox.fetchMemberOfBoxObjectBySection
	/*
	private static FetchStatus FetchIfGotMember(Connection aConn, FieldObjectBox<?> fieldBox, Clasz<?> aMasterClasz, FieldObject<?> aWantedMember, int aPageSize, String aPageDirection, SortOrder aDisplayOrder, String aSortField, String aSortValue) throws Exception {
		FetchStatus fetchStatus;
		PreparedStatement stmt;
		ResultSet rset;

		String masterTableName = aMasterClasz.getTableName();
		String ivTableName = Clasz.GetIvTableName(aMasterClasz.getClass());
		String memberName = aWantedMember.getFieldName();
		String leafClassColName = null;
		String leafClassColValue = null;
		if (aWantedMember.isPolymorphic()) {
			leafClassColName = Clasz.CreateLeafClassColName(memberName);	
			leafClassColValue = aWantedMember.getObj().getClass().getName();
		}
		String wantedMemberOid = aWantedMember.getObj().getObjectId().toString();

		List<Object> whereResult = new CopyOnWriteArrayList<>();
		GetClauseForPageDirectionAndSort(aConn, whereResult, masterTableName, aDisplayOrder, aPageDirection, aSortField, aSortValue);
		String whereClause = (String) whereResult.get(0);
		String sortOrder = (String) whereResult.get(1);
		Field keyField = (Field) whereResult.get(2);

		String sqlSelect = "select " + ivTableName + "." + aMasterClasz.getPkName();
		sqlSelect += " from " + masterTableName + ", " + ivTableName;
		sqlSelect += " where " + masterTableName + "." + aMasterClasz.getPkName() + " == " +  ivTableName + "." + aMasterClasz.getPkName();
		sqlSelect += " and " + memberName + " = " + wantedMemberOid;
		sqlSelect += " and " + whereClause;
		sqlSelect += " order by " + aSortField + " " + sortOrder;
		if (leafClassColName != null) sqlSelect += " and " + leafClassColName + " = '" + leafClassColValue + "'"; // if polymorphic, the iv_ table got this leaf column

		stmt = aConn.prepareStatement(sqlSelect);
		SetSqlIfDateParameter(stmt, keyField, whereClause, aSortValue);
		rset = stmt.executeQuery();
		Class fieldClass = Class.forName(fieldBox.getDeclareType());
		fetchStatus = FetchClaszByOid(aConn, aMasterClasz, fieldBox, rset, aPageSize, fieldClass, aSortField, aDisplayOrder);

		return(fetchStatus);	
	}
	*/

	public static void GetClauseForPageDirectionAndSort(Connection aConn, List<Object> aResult, String masterTableName, SortOrder aDisplayOrder, String aPageDirection, String aSortField, String aSortValue) throws Exception {
		if (aDisplayOrder == SortOrder.DSC) {
			if (aPageDirection.equals("next")) { // reverse the direction if display by descending order
				aPageDirection = "prev";
			} else {
				aPageDirection = "next";
			}
		}

		String whereClause = "";
		String sortOrder = "asc";
		Table masterTable = new Table(masterTableName);
		masterTable.initMeta(aConn);
		Field keyField = masterTable.getField(aSortField);
		if (aSortValue == null || aSortValue.trim().isEmpty()) aSortValue = null;  // for empty or null value, there'll be no where range clause
		if (keyField.getDbFieldType() == FieldType.STRING) {
			if (aPageDirection.equals("next")) {
				if (aSortValue != null) whereClause += "lower(" + aSortField + ") > lower('" + aSortValue + "')";
				sortOrder = "asc";
			} else if (aPageDirection.equals("prev")) {
				if (aSortValue != null) whereClause += "lower(" + aSortField + ") < lower('" + aSortValue + "')";
				sortOrder = "desc";
			} else if (aPageDirection.equals("first")) {
				sortOrder = "asc";
			} else if (aPageDirection.equals("last")) {
				sortOrder = "desc";
			}
		} else if (keyField.getDbFieldType() == FieldType.DATETIME || keyField.getDbFieldType() == FieldType.DATE) {
			if (aPageDirection.equals("next")) {
				if (aSortValue != null) whereClause += aSortField + " > ?";
				sortOrder = "asc";
			} else if (aPageDirection.equals("prev")) {
				if (aSortValue != null) whereClause += aSortField + " < ?";
				sortOrder = "desc";
			} else if (aPageDirection.equals("first")) {
				sortOrder = "asc";
			} else if (aPageDirection.equals("last")) {
				sortOrder = "desc";
			}
		} else {
			throw new Hinderance("The field: " + aSortField + ", type: " + keyField.getDbFieldType() + ", is not supported!");
		}

		aResult.add(whereClause);
		aResult.add(sortOrder);
		aResult.add(keyField);
	}

	public static void SetSqlIfDateParameter(PreparedStatement stmt, Field keyField, String whereClause, String aSortValue) throws Exception {
		if (aSortValue != null) {
			if ((keyField.getDbFieldType() == FieldType.DATETIME || keyField.getDbFieldType() == FieldType.DATE) && whereClause.isEmpty() == false) {
				java.sql.Timestamp dateValue;
				Field fieldDt = (Field) keyField;
				fieldDt.setValueStr(aSortValue);
				String dbDate;
				if (fieldDt instanceof FieldDateTime) {
					dbDate = DateAndTime.FormatForJdbcTimestamp(((FieldDateTime) fieldDt).getValueDateTime());
				} else { // this is instance of FieldDate with no time
					FieldDate castedDate = (FieldDate) fieldDt;
					DateTime dateNoTime = castedDate.getValueDate();
					//DateTime endOfDay = DateAndTime.GetDayEnd(dateNoTime);
					DateTime endOfDay = DateAndTime.GetDayStart(dateNoTime);
					dbDate = DateAndTime.FormatForJdbcTimestamp(endOfDay);
				}
				dateValue = java.sql.Timestamp.valueOf(dbDate); // format must be in "2005-04-06 09:01:10"
				stmt.setTimestamp(1, dateValue);
			}
		}
	}

	
	/*
	@SuppressWarnings("unchecked")
	private static FetchStatus FetchClaszByOid(Connection aConn, Clasz<?> aMasterObj, FieldObjectBox<?> aFieldBox, ResultSet rset, int aPageSize, Class<?> fieldClass, String aSortField, SortOrder aDisplayOrder) throws Exception {
		FetchStatus result = FetchStatus.EOF;
		aFieldBox.getObjectMap().clear();
		int cntrRow = 0;
		Integer[] cntrThreadPassAsRef = {0};
		List<Thread> threadPool = new CopyOnWriteArrayList<>();
		while (rset.next()) {
			if (cntrRow < aPageSize) {
				cntrRow++;
				long pk = rset.getLong(1);
				Class leafClass = fieldClass;
				String polymorphicName = rset.getString(2);
				if (polymorphicName != null && polymorphicName.isEmpty() == false) {
					leafClass = Class.forName(polymorphicName); // if its a polymorphic member field, get its polymorphic class name
				}

				// can refactor as a spawner pattern in future
				if (App.getMaxThread() == 1) { // no threading
					cntrThreadPassAsRef[0]++;
					(new FieldObjectBox.PopulateMemberObjectThreadPk(cntrThreadPassAsRef, aConn, aFieldBox, leafClass, pk)).join();
				} else {
					int cntrAttempt = 0;
					int maxAttempt = App.MAX_GET_CONNECTION_ATTEMPT;
					Connection conn = null;
					while(true) {
						try {
							cntrAttempt++;
							conn = aConn.getBaseDb().getConnPool().getConnection(); // here will throw exception from connection pool, if too many attempts
							if (cntrThreadPassAsRef[0] >= App.getMaxThread()) { 
								aConn.getBaseDb().getConnPool().freeConnection(conn); 
								App.ShowThreadingStatus(ObjectBase.class, "FetchClaszByOid", cntrThreadPassAsRef[0], App.getMaxThread(), cntrAttempt, maxAttempt);
								Thread.sleep(App.MAX_THREAD_OR_CONN_WAIT); // if max thread reach, wait continue the loop
							} else { 
								cntrThreadPassAsRef[0]++;
								Thread theThread = new FieldObjectBox.PopulateMemberObjectThreadPk(cntrThreadPassAsRef, conn, aFieldBox, leafClass, pk);
								threadPool.add(theThread);
								break;
							}
						} catch(Exception ex) {
							App.ShowThreadingStatus(ObjectBase.class, "FetchClaszByOid", cntrThreadPassAsRef[0], App.getMaxThread(), cntrAttempt, maxAttempt);
							if (cntrAttempt >= maxAttempt) {
								throw new Hinderance(ex, "[FetchClaszByOid] Give up threading due to insufficent db connection");
							} else  {
								Thread.sleep(App.MAX_THREAD_OR_CONN_WAIT); // wait for other db conn to free up
							}
						} 
					}
				}
				// end of spawner

				result = FetchStatus.EOF;
			} else {
				result = FetchStatus.MOF;
				break;
			}
		}

		for(Thread eachThread : threadPool) {
			eachThread.join(); // for each spawn thread, call join to wait for them to complete
		}

		String sortField = aSortField.substring(aSortField.lastIndexOf("$") + 1);
		aFieldBox.clearAllSortKey();
		aFieldBox.getMetaObj().getField(sortField).setSortKey(true);
		aFieldBox.getMetaObj().getField(sortField).setSortOrder(aDisplayOrder);
		aFieldBox.sort();
		return(result);
	}
	*/

	// poopuate aMasterClasz if it has FieldObject of aWantedMember
	public static boolean PopulateIfGotMember(Connection aConn, Clasz<?> aMasterClasz, FieldObject<?> aWantedMember) throws Exception {
		boolean result = false;
		PreparedStatement stmt;
		ResultSet rset;

		if (aMasterClasz.gotField(aWantedMember.getDbFieldName()) == false) {
			throw new Hinderance("No such field: " + aWantedMember.getDbFieldName() + ", to populate clasz: " + aMasterClasz.getClaszName());
		}

		String masterTableName = aMasterClasz.getTableName();
		String ivTableName = Clasz.GetIvTableName(aMasterClasz.getClass());
		String memberName = aWantedMember.getDbFieldName();
		String leafClassColName = null;
		String leafClassColValue = null;
		if (aWantedMember.isPolymorphic()) {
			leafClassColName = Clasz.CreateLeafClassColName(memberName);	
			leafClassColValue = aWantedMember.getObj().getClass().getName();
		}
		String wantedMemberOid = aWantedMember.getObj().getObjectId().toString();

		String sqlSelect = "select " + ivTableName + "." + aMasterClasz.getPkName();
		sqlSelect += " from " + masterTableName + ", " + ivTableName;
		sqlSelect += " where " + masterTableName + "." + aMasterClasz.getPkName() + " = " +  ivTableName + "." + aMasterClasz.getPkName();
		sqlSelect += " and " + memberName + " = " + wantedMemberOid;
		if (leafClassColName != null) sqlSelect += " and " + leafClassColName + " = '" + leafClassColValue + "'"; // if polymorphic, the iv_ table got this leaf column

		stmt = aConn.prepareStatement(sqlSelect);
		rset = stmt.executeQuery();
		while (rset.next()) {
			long pk = rset.getLong(1);
			aMasterClasz.setObjectId(pk);
			if (aMasterClasz.populate(aConn)) {
				result = true;
				break;
			}
			throw new Hinderance("Fail to populate clasz: " + aMasterClasz.getClaszName() + " with member: " + aWantedMember.getObj().getClaszName());
		}
		return(result);
	}

	public static String SetWhereClause(Connection aConn, Clasz<?> aCriteria, List<Field> aryWhere) throws Exception {
		String sqlStr = null;
		//Map<String, Record> whereBox = new ConcurrentHashMap<>(); // each table name (string) and a record for the where fields (record)
		Multimap<String, Record> whereBox = Multimaps.synchronizedListMultimap(ArrayListMultimap.create());
		ObjectBase.GetLeafSelectCriteria(aConn, aCriteria, whereBox); // the select criteria for this leaf clszObject, doesn't do the parent clszObject
		//Record recWhere = whereBox.get(aCriteria.getTableName());
		//if (recWhere != null) {
		if (whereBox.keySet().isEmpty() == false) {
			StringBuffer strBuffer = new StringBuffer();
			aryWhere = Database.GetWhereClause(aConn, whereBox, strBuffer); // convert the where record into array list
			sqlStr = "select * from " + Database.GetFromClause(aCriteria.getTableName(), whereBox);
			sqlStr += " where " + strBuffer.toString();
		}
		return(sqlStr);
	}

	public static Clasz<?> GetMasterInstantUnique(Connection aConn, Class<?> aMasterClass, String aMemberName, Clasz<?> aMemberInstant) throws Exception {
		List<Clasz<?>> theMasterInstants = GetMasterInstant(aConn, aMasterClass, aMemberName, aMemberInstant);
		switch (theMasterInstants.size()) {
			case 0:
				return(null);
			case 1:
				return(theMasterInstants.get(0));
			default:
				throw new Hinderance("Fail to get UNIQUE master instant of type: " + aMasterClass.getName() + " of member: " + aMemberName);
		}
	}

	public static List<Clasz<?>> GetMasterInstant(Connection aConn, Class<?> aMasterClass, String aMemberName, Clasz<?> aMemberInstant) throws Exception {
		CopyOnWriteArrayList<Clasz<?>> result = new CopyOnWriteArrayList<>();
		Clasz<?> masterClasz = ObjectBase.CreateClaszFreeType(aConn, aMasterClass);
		if (masterClasz.gotField(aMemberName) == false) {
			throw new Hinderance("Fail to get master instant: " + masterClasz.getClaszName() + ", no such field: " + aMemberName);
		}

		Field field = masterClasz.getField(aMemberName);
		if (field instanceof FieldObjectBox) {
			//FieldObjectBox fob = (FieldObjectBox) field;
			String ivTableName = masterClasz.getIwTableName(aMemberName);
			String fobDbFieldName = Database.Java2DbTableName(aMemberName); 
			String sqlSelect = "select " + masterClasz.getPkName();
			sqlSelect += " from " + ivTableName;
			sqlSelect += " where " + fobDbFieldName + " = " + aMemberInstant.getObjectId();
			sqlSelect += " and " + LEAF_CLASS + " = " + "'" + aMemberInstant.getClass().getName() + "'";

			PreparedStatement stmt;
			ResultSet rset;
			stmt = aConn.prepareStatement(sqlSelect);
			rset = stmt.executeQuery();
			while (rset.next()) {
				long pk = rset.getLong(1);
				Clasz<?> masterInstant = ObjectBase.CreateClaszFreeType(aConn, aMasterClass);
				masterInstant.setObjectId(pk);
				if (masterInstant.populate(aConn)) {
					result.add(masterInstant);
				}
			}
		} else {
			throw new Hinderance("Fail to get master instant of type: " + aMasterClass.getName() + ", field type unsupported yet: " + aMemberName);
		}

		return(result);
	}
}

