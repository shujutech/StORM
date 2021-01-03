package biz.shujutech.db.object;

import biz.shujutech.reflect.ReflectField;
import biz.shujutech.base.App;
import biz.shujutech.base.Connection;
import biz.shujutech.base.DateAndTime;
import biz.shujutech.base.Hinderance;
import biz.shujutech.base.Unknown;
import biz.shujutech.db.object.FieldClasz.FetchStatus;
import biz.shujutech.db.relational.Database;
import biz.shujutech.db.relational.Database.DbType;
import biz.shujutech.db.relational.Field;
import biz.shujutech.db.relational.FieldBoolean;
import biz.shujutech.db.relational.FieldDate;
import biz.shujutech.db.relational.FieldDateTime;
import biz.shujutech.db.relational.FieldInt;
import biz.shujutech.db.relational.FieldLong;
import biz.shujutech.db.relational.FieldStr;
import biz.shujutech.db.relational.FieldType;
import biz.shujutech.db.relational.Record;
import biz.shujutech.db.relational.SortOrder;
import biz.shujutech.db.relational.Table;
import biz.shujutech.reflect.AttribField;
import biz.shujutech.reflect.AttribIndex;
import biz.shujutech.reflect.ReflectIndex;
import biz.shujutech.technical.LambdaObject;
import biz.shujutech.technical.ResultSetFetch;
import java.lang.reflect.Modifier;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.Comparator;
import org.joda.time.DateTime;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import biz.shujutech.technical.Callback2ProcessClasz;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

class ClassAndField {
	Class nameClass;
	String nameField; 

	public ClassAndField(Class aClass, String aField) {
		this.nameClass = aClass;
		this.nameField = aField;
	}
}

public class Clasz extends Table implements Comparable {

	public static final long NOT_INITIALIZE_OBJECT_ID = -1;
	public static final Long RECORD_AT = new Long(0); // instantiated objeck is always at this location in the table clszObject
	public static final String TABLE_NAME_PREFIX = "cz_";
	public static final String SEQUENCE_NAME_PREFIX = "sq_";
	public static final boolean PRE_CREATE_OBJECT= false; // if set to true, will create empty objects and place into FieldObject value
	public static final int RECURSIVE_DEPTH = 8;

	public static String CreateLeafClassColName(String aFieldName) {
		return aFieldName + "_" + ObjectBase.LEAF_CLASS;
	}

	private Clasz parentObject = null;
	private Clasz masterObject = null;
	public ObjectBase db = null;
	public CopyOnWriteArrayList<String> errorField = new CopyOnWriteArrayList<>();
	ConcurrentHashMap<String, Field> claszField = new ConcurrentHashMap<>();
	private boolean gotDeletedField = false;
	private boolean gotCreatedField = false;
	private Boolean forDelete = false;

	public void initBeforePopulate() throws Exception {};
	public void initBeforePopulate(Connection aConn) throws Exception {};
	public void setFieldByDisplayPosition() throws Exception {};
	public void setFieldByDisplayPosition(Clasz aObj) throws Exception {};

	@Override
	public ObjectBase getDb() {
		return db;
	}

	public void setDb(ObjectBase db) {
		this.db = db;
	}

	public void populateLookupField(Connection conn) throws Exception {
		this.populateLookupField(this.getDb(), conn);
	}

	public void populateLookupField(ObjectBase aDb, Connection aConn) throws Exception {
		for(Field eachField : this.getTreeField().values()) {
			if (eachField.getFieldType() == FieldType.OBJECT) {
				FieldObject objField = (FieldObject) eachField;
				Class clas = Class.forName(objField.getDeclareType());
				//if (Lookup.class.isAssignableFrom(clas)) {
				if (Lookup.class.isAssignableFrom(clas)) {
					if (objField.getValueObj(aConn) == null) {
						objField.createNewObject(aDb, aConn);
					}
				}
			}
		}
	}

	public boolean populate(Connection aConn) throws Exception {
		boolean result = false;
		if (fetchObjectSlow(aConn, this) != null) {
			result = true;
		}
		return(result);
	}

	public boolean deleteCommit(Connection aConn) throws Exception {
		boolean result = false;
		int totalDeleted = ObjectBase.DeleteCommit(aConn, this);
		if (totalDeleted > 0) result = true;
		return(result);
	}

	public boolean deleteNoCommit(Connection aConn) throws Exception {
		boolean result = false;
		int totalDeleted = ObjectBase.DeleteNoCommit(aConn, this);
		if (totalDeleted > 0) result = true;
		return(result);
	}

	/*
		This method will do commit for the object and restore the original 
		autocommit status.
	*/
	public Long persistCommit(Connection aConn) throws Exception {
		Long result = ObjectBase.PersistCommit(aConn, this);
		return(result);
	}

	public Long persistNoCommit(Connection aConn) throws Exception {
		Long result = ObjectBase.PersistNoCommit(aConn, this);
		return(result);
	}

	public static void ClassAndFieldExistCheck(List<ClassAndField> aList, Class aClass, String aField) {
		for(ClassAndField each : aList) {
			App.logInfo("Class: " + each.nameClass.getSimpleName() + ", field: " + each.nameField);
			if (each.nameClass == aClass && each.nameField.equals(aField)) {
			}
		}
	}

	public static boolean ClassAndFieldExist(List<ClassAndField> aList, Class aClass, String aField) {
		boolean result = false;
		for(ClassAndField each : aList) {
			if (each.nameClass == aClass && each.nameField.equals(aField)) {
				result = true;
			}
		}
		return(result);
	}

	public Clasz() {
		super();
		this.setTableName(CreateTableName(this.getClass()));
		this.setUniqueIndexName(CreateUniqueIndexName(this.getClass()));
	}

	public Record getRecord() {
		return(this.getRecordBox().get(Clasz.RECORD_AT));
	}

	/**
	 * By context meaning, this clasz is retrieved from db via it's parent
	 * 
	 * @return Clasz
	 */
	public Clasz getParentObjectByContext() {
		return parentObject;
	}

	public void setParentObject(Clasz parentObject) {
		this.parentObject = parentObject;
	}

	public Clasz getMasterObject() {
		return masterObject;
	}

	public void setMasterObject(Clasz masterObject) {
		this.masterObject = masterObject;
	}

	public static String GetInheritancePrefix() {
		return PREFIX_INHERITANCE;
	}

	public static String GetIvPrefix() {
		return PREFIX_MEMBER_OF;
	}

	public static String GetIwPrefix() {
		return PREFIX_MEMBER_BOX_OF;
	}

	public static String GetObjectIndexPrefix() {
		return PREFIX_OBJECT_INDEX;
	}

	public static String GetFieldIndexPrefix() {
		return PREFIX_FIELD_INDEX;
	}

	public static String GetPolymorphicPrefix() {
		return PREFIX_POLYMORPHIC;
	}

	public static String GetTableNamePrefix() {
		return TABLE_NAME_PREFIX;
	}

	public static String GetSequenceNamePrefix() {
		return SEQUENCE_NAME_PREFIX;
	}

	public static String CreateTableName(Class aClass) {
		String result = GetTableNamePrefix() + Database.Java2DbTableName(aClass.getSimpleName());
		return(result);
	}

	public String createSequenceTableName() {
		String result = GetSequenceNamePrefix() + Database.Java2DbTableName(this.getClass().getSimpleName());
		return(result);
	}

	public static String CreatePkColName(Class aClass) throws Exception {
		String result = CreateTableName(aClass) + "_pk";
		return(result);
	}

	public static String CreateUniqueIndexName(Class aClass) {  // change to defineRealWorldId
		String result = CreateTableName(aClass) + "_idx";
		return(result);
	}

	public void createFieldPk() throws Exception { // change to defineObjectId
		this.createFieldPk(this.getMetaRec());
	}

	public void createFieldPk(Record aRec) throws Exception {
		String pkName = this.tableName + "_pk";
		Field pkField = aRec.getField(pkName);
		if (pkField == null) {
			pkField = aRec.createField(this.tableName + "_pk", FieldType.LONG);
		}
		pkField.setPrimaryKey();
	}

	public String getChildCountNameFull(Connection aConn) throws Exception {
		String result;
		if (Database.GetDbType(aConn) == DbType.MYSQL || Database.GetDbType(aConn) == DbType.ORACLE) {
			result = this.getTableName() + "." + this.createChildCountColName();
		} else {
			result = this.createChildCountColName();
		}
		return(result);
	}

	public String createChildCountColName() throws Exception {
		String result;
		result = this.tableName + POSTFIX_FIELD_CHILD_COUNT;
		return(result);
	}

	public void createFieldChildCount() throws Exception {
		this.createFieldChildCount(this.getMetaRec());
	}

	public void createFieldChildCount(Record aRec) throws Exception {
		FieldInt countField = (FieldInt) aRec.getField(this.createChildCountColName());
		if (countField == null) {
			countField = (FieldInt) aRec.createField(this.createChildCountColName(), FieldType.INTEGER);
			countField.setDefaultValue("0"); // in persistCommit we do childCount + 1, and if no default is NULL + 1
		}
		countField.setValueInt(0);
		countField.setModified(false);
		countField.setChildCount(true);
	}

	public String getIhTableName() {
		String result = GetIhTableName(this.getClass());
		return(result);
	}

	public static String GetIhTableName(Class aClasz) {
		String ihName = Clasz.GetInheritancePrefix() + Database.Java2DbTableName(aClasz.getSimpleName()); // create the parent ih_ table
		return(ihName);
	}

	/*
	@Deprecated
	public static String getPolymorphicTableName(Class aClass) {
		String ipName = Clasz.getPolymorphicPrefix() + Database.Java2DbTableName(aClass.getSimpleName()); // create the parent iv_ table
		return(ipName);
	}
	*/

	public String getIvTableName() {
		String result = GetIvTableName(this.getClass());
		return(result);
	}

	public static String GetIvTableName(Class aChild) {
		String ivName = Clasz.GetIvPrefix() + Database.Java2DbTableName(aChild.getSimpleName()); // create the parent iv_ table
		return(ivName);
	}

	public static String GetIwTableName(FieldObjectBox aFobMember) {
		Clasz parentClasz = aFobMember.getMasterObject();
		return(Clasz.GetIwTableName(parentClasz.getClass(), aFobMember.getFieldName()));
	}

	public String getIwTableName(String aFieldName) {
		String result = Clasz.GetIwTableName(this.getClass(), aFieldName);
		return(result);
	}

	public static String GetIwTableName(Class aParent, String aFieldName) {
		String iwName = Clasz.GetIwPrefix() + Database.Java2DbTableName(aParent.getSimpleName()) + "_" + Database.Java2DbTableName(aFieldName); // create the parent iv_ table
		return(iwName);
	}

	public Long getObjectId() throws Exception {
		return(this.getRecord().getFieldLong(this.getPkName()).getValueLong());
	}

	public Long getObjectId(Class aClass) throws Exception {
		if (this.getClass() == aClass) {
			return(this.getRecord().getFieldLong(this.getPkName()).getValueLong());
		} else {
			return(this.getParentObjectByContext().getObjectId(aClass));
		}
	}

	public void setObjectId(String aObjectId) throws Exception {
		Long objectId = Long.parseLong(aObjectId);
		this.setObjectId(objectId, true); // set the clszObject id and do not state that he it as modify 
	}
	
	public void setObjectId(long objectId) throws Exception {
		this.setObjectId(objectId, true); // set the clszObject id and do not state that he it as modify 
	}

	public void setObjectId(Long objectId) throws Exception {
		this.setObjectId(objectId, true); // set the clszObject id and do not state that he it as modify 
	}

	public void setObjectId(long objectId, boolean aSetAsNotModify) throws Exception {
		this.getRecord().getFieldLong(this.getPkName()).setValueLong(objectId);
		if (aSetAsNotModify) {
			this.getRecord().getField(this.getPkName()).setModified(false); // changing primary key is not assume as modified
		}
	}

	public boolean isPopulated() throws Exception {
		if (this.getObjectId().equals(NOT_INITIALIZE_OBJECT_ID) == false) {
			return(true);
		} else {
			return(false);
		}
	}

	public void validateField(String aFieldName) throws Exception {
		if (this.getParentObjectByContext() == null) {
			throw new Hinderance("The class inheritance tree do not have the field: " + aFieldName);
		}
	}

	// TODO for each field type, need to do the Clasz get from parent if get fail
	public Boolean getValueBoolean(String aFieldName) throws Exception {
		if (this.fieldExist(aFieldName)) {
			return(this.getRecord().getValueBoolean(aFieldName));
		} else {
			this.validateField(aFieldName);
			return(this.getParentObjectByContext().getValueBoolean(aFieldName));
		}
	}

	public String getValueStr(Connection aConn) throws Exception {
		return this.getValueStr();
	}

	public String getValueStr() throws Exception {
		throw new Hinderance("Missing method getValueStr in Clasz object: '" + this.getClass().getSimpleName() + "'");
	}

	public String getValueStr(String aFieldName) throws Exception {
		String result;
		if (this.fieldExist(aFieldName)) {
			result = (this.getRecord().getValueStr(aFieldName));
			if (result == null) {
				result = "";
			}
		} else {
			this.validateField(aFieldName);
			result = (this.getParentObjectByContext().getValueStr(aFieldName));
		}
		return(result);
	}

	public Integer getValueInt(String aFieldName) throws Exception {
		Integer result;
		if (this.fieldExist(aFieldName)) {
			result = this.getRecord().getValueInt(aFieldName);
		} else {
			this.validateField(aFieldName);
			result = (this.getParentObjectByContext().getValueInt(aFieldName));
		}
		return(result);
	}

	public Long getValueLong(String aFieldName) throws Exception {
		Long result;
		if (this.fieldExist(aFieldName)) {
			result = this.getRecord().getValueLong(aFieldName);
		} else {
			this.validateField(aFieldName);
			result = (this.getParentObjectByContext().getValueLong(aFieldName));
		}
		return(result);
	}

	@Deprecated
	public Clasz getValueObject(String aFieldName, long aIndex) throws Exception {
		Clasz result;
		if (this.fieldExist(aFieldName)) {
			result = (this.getRecord().getValueObject(aFieldName, aIndex));
		} else {
			this.validateField(aFieldName);
			result = (this.getParentObjectByContext().getValueObject(aFieldName, aIndex));
		}

		if (result == null) {
			throw new Hinderance("Fail to get field: '" + aFieldName + "', no such field in object: " + this.getClaszName() + "'");
		}
		return(result);
	}

	@Deprecated
	public Clasz getValueObject(String aFieldName) throws Exception {
		Clasz result = this.gotValueObject(aFieldName);
		if (result == null) {
			throw new Hinderance("Fail to get value from field: '" + aFieldName + "', in object: " + this.getClaszName() + "'");
		}
		return(result);
	}

	public Clasz getValueObject(Connection aConn, String aFieldName) throws Exception {
		Clasz result = this.gotValueObject(aConn, aFieldName);
		if (result == null) {
			throw new Hinderance("Fail to get value from field: '" + aFieldName + "', in object: " + this.getClaszName() + "'");
		}
		return(result);
	}

	@Deprecated
	public Clasz gotValueObject(String aFieldName) throws Exception {
		Clasz result;
		if (this.fieldExist(aFieldName)) {
			result = (this.getRecord().getValueObject(aFieldName));
		} else {
			this.validateField(aFieldName);
			result = (this.getParentObjectByContext().getValueObject(aFieldName));
		}
		return(result);
	}

	public Clasz gotValueObject(Connection aConn, String aFieldName) throws Exception {
		Clasz result;
		if (this.fieldExist(aFieldName)) {
			result = (this.getRecord().getValueObject(aConn, aFieldName));
		} else {
			this.validateField(aFieldName);
			result = (this.getParentObjectByContext().gotValueObject(aConn, aFieldName));
		}
		return(result);
	}

	public DateTime getValueDateTime(String aFieldName) throws Exception {
		DateTime result;
		if (this.fieldExist(aFieldName)) {
			result = (this.getRecord().getValueDateTime(aFieldName));
		} else {
			this.validateField(aFieldName);
			result = (this.getParentObjectByContext().getValueDateTime(aFieldName));
		}
		return(result);
	}

	public DateTime getValueDate(String aFieldName) throws Exception {
		DateTime result;
		if (this.fieldExist(aFieldName)) {
			result = (this.getRecord().getValueDate(aFieldName));
		} else {
			this.validateField(aFieldName);
			result = (this.getParentObjectByContext().getValueDate(aFieldName));
		}
		return(result);
	}

	public void setValueStr(String aValue) throws Exception {
		throw new Hinderance("The clasz: " + this.getClaszName() + ", have not implemented setValueStr method");
	}

	public void setValueStr(Connection aConn, String aValue) throws Exception {
		this.setValueStr(aValue);
	}

	public void setValueStr(String aFieldName, String aFieldValue) throws Exception {
		if (this.fieldExist(aFieldName)) {
			this.getRecord().setValueStr(aFieldName, aFieldValue);
		} else {
			this.validateField(aFieldName);
			this.getParentObjectByContext().setValueStr(aFieldName, aFieldValue);
		}
	}

	public void setValueLong(String aFieldName, Long aFieldValue) throws Exception {
		if (this.fieldExist(aFieldName)) {
			this.getRecord().setValueLong(aFieldName, aFieldValue);
		} else {
			this.validateField(aFieldName);
			this.getParentObjectByContext().setValueLong(aFieldName, aFieldValue);
		}
	}

	public void setValueInt(String aFieldName, Integer aFieldValue) throws Exception {
		if (this.fieldExist(aFieldName)) {
			this.getRecord().setValueInt(aFieldName, aFieldValue);
		} else {
			this.validateField(aFieldName);
			this.getParentObjectByContext().setValueInt(aFieldName, aFieldValue);
		}
	}

	public void setValueBoolean(String aFieldName, boolean aFieldValue) throws Exception {
		if (this.fieldExist(aFieldName)) {
			this.getRecord().setValueBoolean(aFieldName, aFieldValue);
		} else {
			this.validateField(aFieldName);
			this.getParentObjectByContext().setValueBoolean(aFieldName, aFieldValue);
		}
	}

	public void setValueDateTime(String aFieldName, DateTime aFieldValue) throws Exception {
		if (this.fieldExist(aFieldName)) {
			this.getRecord().setValueDateTime(aFieldName, aFieldValue);
		} else {
			this.validateField(aFieldName);
			this.getParentObjectByContext().setValueDateTime(aFieldName, aFieldValue);
		}
	}

	public void setValueDate(String aFieldName, DateTime aFieldValue) throws Exception {
		if (this.fieldExist(aFieldName)) {
			this.getRecord().setValueDate(aFieldName, aFieldValue);
		} else {
			this.validateField(aFieldName);
			this.getParentObjectByContext().setValueDate(aFieldName, aFieldValue);
		}
	}

	public void setValueObject(String aFieldName, Clasz aFieldValue) throws Exception {
		if (this.fieldExist(aFieldName)) {
			this.getRecord().setValueObject(aFieldName, aFieldValue);
		} else {
			this.validateField(aFieldName);
			this.getParentObjectByContext().setValueObject(aFieldName, aFieldValue);
			if (aFieldValue != null) aFieldValue.setMasterObject(this);
		}
	}

	public void addValueObject(Connection aConn, String aFieldName, Clasz aFieldValue) throws Exception {
		if (this.fieldExist(aFieldName)) {
			this.getRecord().addValueObject(aConn, aFieldName, aFieldValue);
		} else {
			this.validateField(aFieldName);
			this.getParentObjectByContext().addValueObject(aConn, aFieldName, aFieldValue);
			if (aFieldValue != null) aFieldValue.setMasterObject(this);
		}
	}

	public void setObjectId(Connection aConn) throws Exception {
		long oid = this.getNextObjectId(aConn);
		this.setObjectId(oid, true);
	}

	public long getNextObjectId(Connection aConn) throws Exception {
		long result = Database.GetNextSequence(aConn, this.createSequenceTableName());
		return(result);
	}

	/**
	 * When inserting clszObject, the primary key needs to be handle differently
	 * according to the database type, for mysql database, the primary key MUST be
	 * zero value so that its automatically generated by the database. For oracle,
	 * the primary key value must be extracted from a sequence
	 *
	 * 
	 * @param aConn
	 * @throws Exception 
	 */
	public void insert(Connection aConn) throws Exception {
		if (this.getObjectId().equals(NOT_INITIALIZE_OBJECT_ID) == false) {
			throw new Hinderance("Cannot insert an object that already have primary key assigned to it");
		}
		this.setObjectId(aConn); // create and place in the objectId/primarykey into this clszObject before the insertion
		this.insert(aConn, Clasz.RECORD_AT);
	}

	public void update(Connection aConn) throws Exception {
		Record rec2Update = this.getRecord();

		Record recWhere = new Record();
		recWhere.createField(rec2Update.getField(rec2Update.getPkFieldName()));
		recWhere.copyValue(rec2Update.getField(rec2Update.getPkFieldName()));

		this.update(aConn, rec2Update, recWhere);
	}

	public void updateIndex(Connection aConn) throws Exception {
		ObjectIndex.UpdateIndexAll(aConn, this);
	}

	public void deleteIndex(Connection aConn) throws Exception {
		ObjectIndex.DeleteIndex(aConn, this);
	}

	public boolean removeField(String aFieldName) throws Exception {
		boolean result;
		if (aFieldName == null) {
			result = true;
		} else if (this.fieldExist(aFieldName)) {
			result = this.getRecord().removeField(aFieldName);
			this.gotDeletedField = true;
		} else {
			this.validateField(aFieldName);
			result = this.getParentObjectByContext().removeField(aFieldName);
		}
		return(result);
	}

	public void removeMarkField() throws Exception {
		for(Field eachField : this.getTreeField().values()) {
			if (eachField.isSystemField() == false) {
				if (eachField.forRemove()) {
					this.removeField(eachField.getFieldName());
				}
			}
		}
	}

	public void markAllFieldForRemoval() throws Exception {
		for(Field eachField : this.getTreeField().values()) {
			if (eachField.isSystemField() == false) {
				eachField.forRemove(true);
			}
		}
	}

	public void markAllFieldForKeep() throws Exception {
		for(Field eachField : this.getTreeField().values()) {
			if (eachField.isSystemField() == false) {
				eachField.forRemove(false);
			}
		}
	}

	public boolean gotFieldDeleted() throws Exception {
		boolean result = false;
		if (this.gotDeletedField == false) {
			if (this.getParentObjectByContext() != null) result = this.getParentObjectByContext().gotFieldDeleted();
		} else {
			result = true;
		}
		return(result);
	}

	public boolean gotFieldCreated() throws Exception {
		boolean result = false;
		if (this.gotCreatedField == false) {
			if (this.getParentObjectByContext() != null) result = this.getParentObjectByContext().gotFieldCreated();
		} else {
			result = true;
		}
		return(result);
	}

	// TODO when copying table field type, do a deep copy since by default java uses shallow copy (i.e. copying by reference)
	public void copyAllFieldWithModifiedState(Clasz aSource) throws Exception {
		this.copyClasz(aSource, false); // clonse fields will keep the modify status of the original fields
	}

	public void copyAllFieldWithoutModifiedState(Clasz aSource) throws Exception {
		this.copyClasz(aSource, true);
	}

	private void copyClasz(Clasz aSource, boolean aIsCopyWithoutModifiedState) throws Exception {
		for(Field eachField : aSource.getRecord().getFieldBox().values()) {
			try {
				if (eachField.getFieldType() == FieldType.OBJECT) {
					Clasz memberObj = (Clasz) ((FieldObject) eachField).getObj();
					if (memberObj != null && memberObj.getClaszName().equals("Clasz")) {
						continue;
					}
				}
				if (eachField.getFieldType() == FieldType.OBJECTBOX) {
					Clasz memberObj = (Clasz) ((FieldObjectBox) eachField).getMetaObj();
					if (memberObj.getClaszName().equals("Clasz")) {
						continue;
					}
				}
				if (aIsCopyWithoutModifiedState) {
					this.getRecord().copyValue(eachField); // for clszObject types, there's a overidden method to enable each clszObject type knows how to copy among themselves
				} else {
					this.getRecord().cloneField(eachField);
				}
			} catch (Exception ex) {
				throw new Hinderance(ex, "Error when copying object : '" + this.getClass().getSimpleName() + "', field: '" + eachField.getFieldName() + "'");
			}
		}

		if (ObjectBase.ParentIsNotAtClaszYet(this)) {
			Clasz parntObject = this.getParentObjectByContext();
			Clasz parentSource = aSource.getParentObjectByContext();
			if (parntObject != null) {// its possible the parent class for this clszObject is abstract and still not at Clasz
				parntObject.copyClasz(parentSource, aIsCopyWithoutModifiedState);
			}
		}
	}

	/**
	 * TODO separate out each field type to reduce memory consumption
	 *
	 * @return
	 * @throws Exception
	 */
	public String asString() throws Exception {
		return(this.asString(false));
	}

	public String asString(boolean aDisplayMember) throws Exception {
		String result = "";
		for(Field eachField : this.getRecord().getFieldBox().values()) {
			if (aDisplayMember && eachField.getFieldType() == FieldType.OBJECT) {
				result += ((FieldObject) eachField).getObj().asString(aDisplayMember);
			} else if (aDisplayMember && eachField.getFieldType() == FieldType.OBJECTBOX) {
				throw new Hinderance("Field of OBJECTBOX is not supported yet!!");
			} else {
				result += eachField.getFieldName() + ": " + eachField.getValueStr() + App.LineFeed;
			}
		}
		if (ObjectBase.ParentIsNotAtClaszYet(this)) {
			Clasz parntObject = this.getParentObjectByContext();
			if (parntObject != null) { // its possible the parent class for this clszObject is abstract and still not at Clasz
				result += parntObject.asString();
			}
		}
		return(result);
	}

	public Clasz getChildObject(Connection aConn, Clasz aLeafObject) throws Exception {
		Clasz result;
		Clasz childObject = this.getChildClasz(aLeafObject);
		if (childObject != null) {
			String ihName = childObject.getIhTableName();

			Record recSelect = new Record();
			recSelect.createField(Clasz.CreatePkColName(childObject.getClass()), FieldType.LONG); // select the primary key of the child clszObject

			Record recWhere = new Record();
			recWhere.createField(this.getPkName(), FieldType.LONG);
			recWhere.getFieldLong(this.getPkName()).setValueLong(this.getObjectId());

			Table ihTable = new Table(ihName);
			if (ihTable.fetch(aConn, recSelect, recWhere) != 1) {// Fetch, result is in recSelect
				//ignore, it doesn't have a child clszObject in the database
			} else {
				// Fetch the clszObject from the database
				if (FetchUnique(aConn, childObject, recSelect) != 1) { // Fetch, result is in recSelect
					throw new Hinderance("Fail to retrieve object for: '" + childObject.getClass().getSimpleName() + "', from its field: " + recSelect.toStr());
				}
			}

			result = childObject;
		} else {
			result = null;
		}
		return(result);
	}

	public static int FetchUnique(Connection aConn, Clasz aClasz, Record aWhere) throws Exception {
		int result = 0;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			rs = Table.Fetch(aConn, stmt, aClasz.getTableName(), null, aWhere);
			while (rs.next()) {
				if (result > 0) {
					throw new Hinderance("Object to fetch criteria returns more then one record: '" + aClasz.getClass().getSimpleName() + "'");
				}
				aClasz.populateObject(aConn, rs, false);
				result++;
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

	public static void BindSqlDateTimeOrDate(PreparedStatement aStmt, Field aKeyField, String aKeyValue, int aPosition) throws Exception {
		java.sql.Timestamp dateValue;
		Field fieldDt = (Field) aKeyField;
		fieldDt.setValueStr(aKeyValue);
		String dbDate;
		if (fieldDt instanceof FieldDateTime) {
			dbDate = DateAndTime.FormatForJdbcTimestamp(((FieldDateTime) fieldDt).getValueDateTime());
		} else { // this is instance of FieldDate with no time
			FieldDate castedDate = (FieldDate) fieldDt;
			DateTime dateNoTime = castedDate.getValueDate();
			DateTime endOfDay = DateAndTime.GetDayStart(dateNoTime);
			dbDate = DateAndTime.FormatForJdbcTimestamp(endOfDay);
		}
		dateValue = java.sql.Timestamp.valueOf(dbDate); // format must be in "2005-04-06 09:01:10"
		aStmt.setTimestamp(aPosition, dateValue);
	}

	public static String SetOrderBy(String aAccumStr, String aFieldName, SortOrder aOrderBy) {
		aAccumStr = aAccumStr.trim();
		if (aAccumStr.isEmpty() == false) aAccumStr += ",";
		String strOrder = "asc";
		if (aOrderBy.equals(SortOrder.DSC)) strOrder = "desc";
		aAccumStr += " " + aFieldName + " " + strOrder;

		return(aAccumStr.trim());
	}

	@Deprecated
	public static List<Object> GetBySectionSqlObject(Table claszTable, String strFieldName, String strFieldValue, String aPageDirection, String whereClause, String sortOrder) throws Exception {
		List<Object> result = new CopyOnWriteArrayList<>();
		Field keyField = claszTable.getField(strFieldName);
		if (strFieldValue == null || strFieldValue.trim().isEmpty()) strFieldValue = null;  // for empty or null value, there'll be no where range clause
		if (keyField.getFieldType() == FieldType.STRING || keyField.getFieldType() == FieldType.INTEGER) {
			if (aPageDirection.equals("next")) {
				if (strFieldValue != null) {
					if (whereClause.isEmpty() == false) whereClause += " and ";
					whereClause += "lower(" + strFieldName + ") > lower(?)";
					keyField.setValueStr(strFieldValue);
				}
				sortOrder = SetOrderBy(sortOrder, strFieldName, SortOrder.ASC);
			} else if (aPageDirection.equals("prev")) {
				if (strFieldValue != null) {
					if (whereClause.isEmpty() == false) whereClause += " and ";
					whereClause += "lower(" + strFieldName + ") < lower(?)";
					keyField.setValueStr(strFieldValue);
				}
				sortOrder = SetOrderBy(sortOrder, strFieldName, SortOrder.DSC);
			} else if (aPageDirection.equals("first")) {
				sortOrder = SetOrderBy(sortOrder, strFieldName, SortOrder.ASC);
			} else if (aPageDirection.equals("last")) {
				sortOrder = SetOrderBy(sortOrder, strFieldName, SortOrder.DSC);
			}
		} else if (keyField.getFieldType() == FieldType.DATETIME || keyField.getFieldType() == FieldType.DATE) {
			if (aPageDirection.equals("next")) {
				if (strFieldValue != null) {
					if (whereClause.isEmpty() == false) whereClause += " and ";
					whereClause += strFieldName + " > ?";
					keyField.setValueStr(strFieldValue);
				}
				sortOrder = SetOrderBy(sortOrder, strFieldName, SortOrder.ASC);
			} else if (aPageDirection.equals("prev")) {
				if (strFieldValue != null) {
					if (whereClause.isEmpty() == false) whereClause += " and ";
					whereClause += strFieldName + " < ?";
					keyField.setValueStr(strFieldValue);
				}
				sortOrder = SetOrderBy(sortOrder, strFieldName, SortOrder.DSC);
			} else if (aPageDirection.equals("first")) {
				sortOrder = SetOrderBy(sortOrder, strFieldName, SortOrder.ASC);
			} else if (aPageDirection.equals("last")) {
				sortOrder = SetOrderBy(sortOrder, strFieldName, SortOrder.DSC);
			}
		} else {
			throw new Hinderance("The field type in table: " + claszTable + ", type: " + keyField.getFieldType() + ", is not supported!");
		}

		result.add(whereClause.trim());
		result.add(sortOrder.trim());
		if (strFieldValue != null) result.add(keyField); // only got sort clause but no conditional clause
		return(result);
	}

	public static Clasz FetchUnique(Connection aConn, Clasz aClasz, Record aWhere, String aSortFieldName, SortOrder aDisplayOrder) throws Exception {
		List<String> keyField = new CopyOnWriteArrayList<>();
		List<String> keyValue = new CopyOnWriteArrayList<>();
		String colName =  Database.Java2DbFieldName(aSortFieldName);
		keyField.add(colName);
		keyValue.add("");
		return FetchUnique(aConn, aClasz, keyField, keyValue, aWhere, aDisplayOrder);
	}

	@Deprecated
	private static Clasz FetchUnique(Connection aConn, Clasz aClasz, List<String> aKeyField, List<String> aKeyValue, Record aWhere, SortOrder aDisplayOrder) throws Exception {
		FieldObjectBox fob = new FieldObjectBox(aClasz);
		FetchStatus status = Clasz.FetchBySection(aConn, aClasz, aKeyField, aKeyValue, aWhere, fob, SortOrder.DSC, "next", 1);
		fob.setFetchStatus(status);
		fob.resetIterator();
		if (fob.hasNext(aConn)) {
			Clasz result = fob.getNext();
			return(result);
		} 
		return(null);
	}

	// replaced with FetchByPageFromTable
	@Deprecated
	public static FetchStatus FetchBySection(Connection aConn, Clasz aClasz, List<String> aKeyField, List<String> aKeyValue, Record aWhere, FieldObjectBox aBox, SortOrder aDisplayOrder, String aPageDirection, int aPageSize) throws Exception {
		int result = 0;
		FetchStatus fetchStatus;
		if (aDisplayOrder == SortOrder.DSC) {
			if (aPageDirection.equals("next")) { // reverse the direction if display by descending order
				aPageDirection = "prev";
			} else {
				aPageDirection = "next";
			}
		}

		// get sql objects for full sql select statement
		Clasz typeClasz = aBox.getMetaObj();
		List<Object> fetchObject = Table.GetFetchObject(typeClasz.getTableName(), null, aWhere, null);
		String whereSqlStr = (String) fetchObject.get(0);
		List<Field> whereBindList = (List<Field>) fetchObject.get(1);

		// get sql objects for pagination clause
		String byPageWhereStr = "";
		String byPageSortOrder = "";
		List<Field> byPageBindList = new CopyOnWriteArrayList<>();
		String tableName = aClasz.getTableName();
		Table claszTable = new Table(tableName);
		claszTable.initMeta(aConn);
		for(int cntrField = 0; cntrField < aKeyField.size(); cntrField++) {
			String strFieldName = aKeyField.get(cntrField);
			String strFieldValue = aKeyValue.get(cntrField);
			List<Object> sqlPageObject = GetBySectionSqlObject(claszTable, strFieldName, strFieldValue, aPageDirection, byPageWhereStr, byPageSortOrder); // get the where clause that restrict the search to a section
			byPageWhereStr = ((String) sqlPageObject.get(0));
			byPageSortOrder = ((String) sqlPageObject.get(1));
			if (sqlPageObject.size() >= 3) byPageBindList.add((Field) sqlPageObject.get(2));
		}

		byPageWhereStr = byPageWhereStr.trim();
		byPageSortOrder = byPageSortOrder.trim();

		if (byPageWhereStr.isEmpty() == false) {
			if (whereSqlStr.indexOf("where") < 0) {
				whereSqlStr += " where";
			} else {
				whereSqlStr += " and";
			}
			whereSqlStr += " " + byPageWhereStr;
		}

		if (byPageSortOrder.isEmpty() == false) {
			//if (whereSqlStr.indexOf("where") < 0) whereSqlStr += " where";
			whereSqlStr += " order by " + byPageSortOrder;
		}

		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = aConn.prepareStatement(whereSqlStr);
			Database.SetStmtValue(stmt, whereBindList);
			Database.SetStmtValue(stmt, byPageBindList, whereBindList.size());
			rs = stmt.executeQuery();
			while (rs.next()) {
				Clasz clasz = ObjectBase.CreateObject(aConn, typeClasz.getClass());
				clasz.populateObject(aConn, rs, false);
				aBox.addValueObject(clasz);
				result++;
				if (result >= aPageSize) break;
			}
		} finally {
			fetchStatus = FetchStatus.EOF;
			if (rs != null) {
				rs.close();
			}
			if (stmt != null) {
				stmt.close();
			}
		}

		for(int cntrField = 0; cntrField < aKeyField.size(); cntrField++) {
			String sortField = aKeyField.get(cntrField);
			aBox.clearAllSortKey();
			aBox.getMetaObj().getField(sortField).setSortKey(true);
			aBox.getMetaObj().getField(sortField).setSortOrder(aDisplayOrder);
		}
		if (aKeyField.size() > 0) aBox.sort();

		aBox.setFetchStatus(fetchStatus);
		return(fetchStatus);
	}

	public void ForEachClasz(Connection aConn, Record aWhere, Callback2ProcessClasz  aCallback) throws Exception {
		ForEachClasz(aConn, this.getClass(), aWhere, aCallback);
	}

	public static void ForEachClasz(Connection aConn, Class aClaszClass, Record aWhere, Callback2ProcessClasz  aCallback) throws Exception {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			String tableName = CreateTableName(aClaszClass);
			rs = Table.Fetch(aConn, stmt, tableName, null, aWhere);
			while (rs.next()) {
				Clasz clasz = ObjectBase.CreateObject(aConn, aClaszClass);
				clasz.populateObject(aConn, rs, false);
				if (aCallback != null) {
					if (aCallback.processClasz(aConn, clasz) == false) {
						break;
					}
				}
			}
		} finally {
			if (rs != null) {
				rs.close();
			}
			if (stmt != null) {
				stmt.close();
			}
		}
	}

	public static int FetchAll2Box(Connection aConn, Record aWhere, FieldObjectBox aBox) throws Exception {
		int result = 0;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			Clasz typeClasz = aBox.getMetaObj();
			rs = Table.Fetch(aConn, stmt, typeClasz.getTableName(), null, aWhere);
			while (rs.next()) {
				Clasz clasz = ObjectBase.CreateObject(aConn, typeClasz.getClass());
				clasz.populateObject(aConn, rs, false);
				aBox.addValueObject(clasz);
				result++;
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

	/**
	 * Traverse upwards from the leaf clszObject to get the immediate child class of
	 * 'this' clszObject. Since there can be multiple child class for a parent class,
	 * the leaf class is use to guide the traversing to the leaf class path.
	 * Return null if there is no child class or if the class is already at the
	 * leaf class.
	 *
	 * @param aLeafClass
	 * @return
	 * @throws Exception if the leaf class is not in the inheritance tree of 'this'
	 */
	public Clasz getChildClasz(Clasz aLeafClass) throws Exception {
		if (aLeafClass.getClass().equals(this.getClass())) {
			return(null);
		} else {
			return(this.getChildClaszRecursively(aLeafClass));
		}
	}

	public Class getChildClass(Class aLeafClass) throws Exception {
		if (aLeafClass.equals(this.getClass())) {
			return(null);
		} else {
			return(this.getChildClassRecursively(aLeafClass));
		}
	}

	private Clasz getChildClaszRecursively(Clasz aLeafClass) throws Exception {
		Clasz result = aLeafClass;
		Clasz parentClass = aLeafClass.getParentObjectByContext();
		try {
			if (parentClass == null) {
				throw new Hinderance("This class: '" + aLeafClass.getClass().getSimpleName() + "', do not have inheritance from the class: " + this.getClass().getSimpleName() + "'");
			} else if (parentClass.getClass().equals(this.getClass()) == false) { // if the parent of this class is the same as the class of 'this' clszObject, then this class is the child class
				result = this.getChildClaszRecursively(parentClass); // recursive traversing up the tree to get the immediate child class of this clszObject
			}
		} catch (Exception ex) {
			if (parentClass != null)
				throw new Hinderance(ex, "Fail to get child class for: '" + parentClass.getClass().getSimpleName() + "', for class: '" + this.getClass().getSimpleName() + "'");
			else
				throw new Hinderance(ex, "Fail to get child class for class: '" + this.getClass().getSimpleName() + "'");
		}

		return(result);
	}

	private Class getChildClassRecursively(Class aLeafClass) throws Exception {
		Class result = aLeafClass;
		Class parentClass = aLeafClass.getSuperclass();
		try {
			if (parentClass == null) {
				throw new Hinderance("This class: '" + aLeafClass.getSimpleName() + "', do not have inheritance from the class: '" + this.getClass().getSimpleName() + "'");
			} else if (parentClass.equals(this.getClass()) == false) { // if the parent of this class is the same as the class of 'this' clszObject, then this class is the child class
				result = this.getChildClassRecursively(parentClass); // recursive traversing up the tree to get the immediate child class of this clszObject
			}
		} catch (Exception ex) {
			if (parentClass != null) {
				throw new Hinderance(ex, "Fail to get child class for: '" + parentClass.getSimpleName() + "', this class: '" + this.getClass().getSimpleName() + "'");
			} else {
				throw new Hinderance(ex, "Fail to get child class for null" + ", this class: '" + this.getClass().getSimpleName() + "'");
			}
		}

		return(result);
	}

	public void copyShallow(Clasz aSource) throws Exception {
		for(Field eachField : aSource.getRecord().getFieldBox().values()) {
			this.copyField(eachField);
		}
	}

	public void copyField(Field eachField) throws Exception {
		copyField(eachField, this);
	}

	public static void copyField(Field eachField, Clasz aObject) throws Exception {
		//App.logDebg("Shallow copying field: " + reflectField.getFieldName() + ", value: " + reflectField.getValueStr());
		if (eachField.getFieldType() == FieldType.STRING) {
			aObject.setValueStr(eachField.getFieldName(), eachField.getValueStr());
		} else if (eachField.getFieldType() == FieldType.DATETIME) {
			aObject.setValueDateTime(eachField.getFieldName(), ((FieldDateTime) eachField).getValueDateTime());
		} else if (eachField.getFieldType() == FieldType.DATE) {
			aObject.setValueDate(eachField.getFieldName(), ((FieldDate) eachField).getValueDate());
		} else if (eachField.getFieldType() == FieldType.LONG) {
			aObject.setValueLong(eachField.getFieldName(), ((FieldLong) eachField).getValueLong());
		} else if (eachField.getFieldType() == FieldType.INTEGER) {
			aObject.setValueInt(eachField.getFieldName(), ((FieldInt) eachField).getValueInt());
		} else if (eachField.getFieldType() == FieldType.BOOLEAN) {
			aObject.setValueBoolean(eachField.getFieldName(), ((FieldBoolean) eachField).getValueBoolean());
		} else if (eachField.getFieldType() == FieldType.ENCRYPT) {
			aObject.setValueStr(eachField.getFieldName(), eachField.getValueStr());
		} else {
			throw new Hinderance("Unknown type for field: '" + eachField.getFieldName() + "', when attempting to copy it from: '" + aObject.getClass().getSimpleName() + "'");
		}
	}

	public void populateObject(Connection aConn, ResultSet aRset) throws Exception {
		this.populateObject(aConn, aRset, this, true, true);
	}

	public void populateObject(Connection aConn, ResultSet aRset, boolean aPopulateInheritance) throws Exception {
		this.populateObject(aConn, aRset, this, aPopulateInheritance, true);
	}

	/**
	* Populate member variable fields (either one instant variable or multiple
	* instant variable of aMasterObject).
	*
	* @param aConn
	* @param aRset
	* @param aObject
	* @param aPopulateInheritance
	* @param aPopulateMember
	* @throws Exception 
	*/
	public void populateObject(Connection aConn, ResultSet aRset, Clasz aObject, boolean aPopulateInheritance, boolean aPopulateMember) throws Exception {
		try {
			// must first populate the master clszObject before populating member fields because the objectid is needed when populating member fields
			for(Field eachField : aObject.getRecord().getFieldBox().values()) {
				if (eachField.getFieldType() == FieldType.OBJECT || eachField.getFieldType() == FieldType.OBJECTBOX) {
				} else {
					try {
						eachField.setMasterObject(aObject);
						eachField.populateField(aRset);	 // retrieve the field from result set according to the field's name and place the value into the field
					} catch (Exception ex) {
						throw new Hinderance(ex, "Fail to populate from database into: '" + aObject.getClass().getSimpleName() + "." + eachField.getFieldName() + "'");
					}
				}
			}

			// now for the member fields
			for(Field eachField : aObject.getRecord().getFieldBox().values()) {
				try {
					if (eachField.getFieldType() == FieldType.OBJECT || eachField.getFieldType() == FieldType.OBJECTBOX) {
						if (((FieldClasz) eachField).isPrefetch() == true) {
							if (aPopulateMember) {
								if (eachField.isInline() == false) {
									if (eachField.getFieldType() == FieldType.OBJECT) {
											((FieldObject) eachField).fetch(aConn); // Fetch the clszObject into this field
											eachField.setMasterObject(aObject); // after fetch, it's a different master during create
											// TODO for OBJECT type, fetchObjectFast result set would have had the value, so the above sql is not needed if performance is needed 
									} else if (eachField.getFieldType() == FieldType.OBJECTBOX) {
										if (((FieldObjectBox) eachField).getMetaObj().getClaszName().equals("Clasz") == false) { // if only this is not a recursive field, then we populate it
											((FieldObjectBox) eachField).fetchAll(aConn);
										}
										eachField.setMasterObject(aObject); // after fetch, it's a different master during create
									} else {
										throw new Hinderance("Field is not either OBJECT or OBJECTBOX type, internal error when processing field: '" + eachField.getFieldName() + "'");
									}
								} else { // inline field, copy the value from the result set into the member clszObject
									eachField.setMasterObject(aObject); // after fetch, it's a different master during create
									resultset2Tree(this.getDb(), aConn, aRset, eachField, eachField.getFieldName());
								}
							} else {
								eachField.setMasterObject(aObject); // after fetch, it's a different master during create
							}
						} else {
							eachField.setMasterObject(aObject); // after fetch, it's a different master during create
						}
					}
				} catch (Exception ex) {
					throw new Hinderance(ex, "Fail to populate field: '" + aObject.getClass().getSimpleName() + "." + eachField.getFieldName() + "'");
				}
			}
		} catch (Exception ex) {
			throw new Hinderance(ex, "Fail to populate result set values to object: '" + aObject.getClass().getSimpleName() + "'");
		}

	}

	/**
	 * Populate values from the result set and place them into the member object
	 * by retrieving the field from result set according to the field's name and
	 * place the value into the member object field.
	 *
	 * @param aDb
	 * @param aConn
	 * @param aRset
	 * @param aMasterField
	 * @param aFieldName
	 * @throws Exception 
	 */

	/**
	 * Populate values from the result set and place them into the member clszObject
	 * by retrieving the field from result set according to the field's name and
	 * place the value into the member clszObject field.
	 * @param aDb
	 * @param aConn
	 * @param aRset
	 * @param aMasterField
	 * @param aFieldName
	 * @throws Exception
	 */
	public static void resultset2Tree(ObjectBase aDb, Connection aConn, ResultSet aRset, Field aMasterField, String aFieldName) throws Exception {
		try {
			FieldObject fieldObj = (FieldObject) aMasterField;
			Clasz memberObj = fieldObj.getObj();
			if (memberObj == null) {
				fieldObj.createNewObject(aDb, aConn);
				memberObj = fieldObj.getObj();
			}

			for(Field eachField : memberObj.getRecord().getFieldBox().values()) {
				eachField.setMasterObject(memberObj); // after fetch, it's a different master during create
				if (eachField.getFieldType() == FieldType.OBJECT || eachField.getFieldType() == FieldType.OBJECTBOX) { // TODO handle objectbox type
					String dbFieldName = Clasz.CreateDbFieldName(eachField.getFieldName(), aFieldName); // create the field name of the inline field from its member name and its field name
					resultset2Tree(aDb, aConn, aRset, eachField, dbFieldName);
				} else {
					if (eachField.isSystemField() == false) {
						Field tmpField = Field.CreateField(eachField);
						//String dbFieldName = Clasz.CreateDbFieldName(reflectField.getFieldName(), aMasterField.getFieldName()); // create the field name of the inline field from its member name and its field name
						String dbFieldName = Clasz.CreateDbFieldName(eachField.getFieldName(), aFieldName); // create the field name of the inline field from its member name and its field name
						tmpField.setFieldName(dbFieldName);
						tmpField.populateField(aRset);
						eachField.copyValue(tmpField);
					}
				}
			}
		} catch (Exception ex) {
			throw new Hinderance(ex, "Fail at resultset2Tree for field: '" + aMasterField.getFieldName() + "'");
		}
	}

	public static void PopulateInlineField(Clasz aMasterClasz, FieldObject aInlineField, String aAccumFieldName, String aDirection) throws Exception {
		Clasz memberObj = aInlineField.getObj();
		if (memberObj != null) {
			for(Field eachField : memberObj.getRecord().getFieldBox().values()) {
				if (eachField.getFieldType() == FieldType.OBJECT || eachField.getFieldType() == FieldType.OBJECTBOX) { // TODO handle objectbox type
					String dbFieldName = Clasz.CreateDbFieldName(eachField.getFieldName(), aAccumFieldName); // create the field name of the inline field from its member name and its field name
					PopulateInlineField(aMasterClasz, (FieldObject) eachField, dbFieldName, aDirection);
				} else {
					if (eachField.isSystemField() == false) {
						String inlineFieldName = Clasz.CreateDbFieldName(eachField.getFieldName(), aAccumFieldName);
						Field fieldRec = aMasterClasz.getField(inlineFieldName);
						if (aDirection.equals("tree")) {
							eachField.copyValue(fieldRec);
						} else if (aDirection.equals("flat")) {
							fieldRec.copyValue(eachField);
						} else {
							throw new Hinderance("Attempting to copy inline fields to object or vice versa, invalid direction: " + aDirection);
						}
					}
				}
			}
		}
	}

	public static Clasz CreateObject(ObjectBase aDb, Connection aConn, Class aClass) throws Exception {
		Clasz result = Clasz.CreateObject(aDb, aConn, aClass, true);
		return(result);
	}

	public static Clasz CreateObjectTransient(ObjectBase aDb, Connection aConn, Class aClass) throws Exception {
		Clasz result = Clasz.CreateObject(aDb, aConn, aClass, false);
		return(result);
	}

	public static Clasz CreateObject(ObjectBase aDb, Connection aConn, Class aClass, boolean aDoDDL) throws Exception {
		CopyOnWriteArrayList<ClassAndField> avoidRecursive = new CopyOnWriteArrayList<>();
		Clasz result = Clasz.CreateObject(aDb, aConn, aClass, null, aDoDDL, avoidRecursive, 0, false);
		return(result);
	}

	/**
	 * Creates an clszObject from the given Class, the created clszObject will contain the
	 * appropriate parent clszObject if it has a inherited class. The method traverses
	 * up the inheritance tree until the Clasz class to create and assign its
	 * parent clszObject.
	 * 
	 * Handle abstract classes by combining the abstract class into the lowest
	 * normal child class and creates those abstract class fields inside a normal
	 * class.
	 *
	 * @param aDb
	 * @param aConn
	 * @param aClass
	 * @param aInstant - the previously instantiated clszObject for this class (use when contain abstract class)
	 * @param aDoDdl
	 * @param aDepth
	 * @param aAvoidRecursive
	 * @param aInline
	 * @return
	 * @throws Exception
	 */
	private static Clasz CreateObject(ObjectBase aDb, Connection aConn, Class aClass, Clasz aInstant, boolean aDoDdl, List aAvoidRecursive, int aDepth, boolean aInline) throws Exception {
		Clasz object = null;
		if (Modifier.isAbstract(aClass.getModifiers())) { // abstract class
			if (aDepth == 0) {
				throw new Hinderance("Cannot create object of type abstract!");
			} else {
				AlterObjectForAbstract(aConn, aClass, aInstant, aAvoidRecursive, aDepth, aDoDdl); // add the abstract class fields into the child table by using SQL alter table..
			}
		} else {
			object = CreateObjectDdl(aConn, aClass, aDoDdl, aAvoidRecursive, aDepth, aInline); // create the table for this class in this db and also return the created instant of aClass 
			object.setObjectId(Clasz.NOT_INITIALIZE_OBJECT_ID, true);
			object.setDb(aDb);
			aInstant = object;
		}

		Class ParentClass = aClass.getSuperclass(); // get the parent clszObject of the class to be created

		if (ParentClass != null && ParentClass != Clasz.class) {
			if (ParentClass.isInterface()) {
				// not interested in interface class
			} else if (Modifier.isAbstract(ParentClass.getModifiers())) { // if the parent is an abstract class
				Clasz.CreateObject(aDb, aConn, ParentClass, aInstant, aDoDdl, aAvoidRecursive, aDepth + 1, aInline); // recursive call to check for the parent class if is still abstract class, do the alter else, do the normal create 
			} else {
				if (Modifier.isAbstract(aClass.getModifiers())) {  // if the parent of aClass is NOT abstract and aClass is abstract, means we need to create table for the non abstract parent class 
					Clasz parntObject = Clasz.CreateObject(aDb, aConn, ParentClass, aInstant, aDoDdl, aAvoidRecursive, aDepth + 1, aInline); // recursive call to create the non abstract parent class table 
					ObjectBase.createInheritance(aConn, ParentClass, aInstant.getClass()); // after creating the parent, create the inheritance tables, note the aInstant.getClass() parameter
					aInstant.setParentObject(parntObject); // link the inheritance to a non abstract class
				} else {
					Clasz parntObject = Clasz.CreateObject(aDb, aConn, ParentClass, aInstant, aDoDdl, aAvoidRecursive, aDepth + 1, aInline); // recursive call to create the parent table of the inherited parent class
					ObjectBase.createInheritance(aConn, ParentClass, aClass); // after creating the parent, create the inheritance tables
					if (object != null) {
						object.setParentObject(parntObject);
					} else {
						throw new Hinderance("Fail to create the object for DDL creation: " + aClass.getSimpleName());
					}
				}
			}
		}
		return(object);
	}

	/**
	 * Translate the class into a table and create it inside the database, it will
	 * also populate the db field name into the static variable declare in the
	 * class with the annotation ReflectField. The aClass argument is use to
	 * create an instant of an Clasz/Table that is then use to create the table in
	 * the database.
	 * 
	 * NOTE: The field name in the clszObject MUST be isUnique throughout the
	 * inheritance tree. The code get overly complex if those fields name are not
	 * isUnique throughout the inheritance tree.
	 *
	 *
	 * @param aConn
	 * @param aDoDdl
	 * @param aAvoidRecursive
	 * @param aDepth
	 * @param aClass
	 * @param aInline
	 * @return
	 * @throws Exception
	 */
	private static Clasz CreateObjectDdl(Connection aConn, Class aClass, boolean aDoDdl, List aAvoidRecursive, int aDepth, boolean aInline) throws Exception {
		Clasz clszObject = (Clasz) aClass.newInstance();
		clszObject.createFieldPk(); // create field in meta record
		clszObject.createFieldChildCount(); // this field tracks the number of other child clszObject inherted from this clszObject
		clszObject.createRecord(); // with meta record fields created, create a data record
		CreateFieldFromClass(aConn, aClass, clszObject, "", aInline, aAvoidRecursive, aDepth, aDoDdl);

		if (aDoDdl && Database.TableExist(aConn, clszObject.getTableName()) == false) {
			App.logInfo(Clasz.class, "Creating table for class: '" + clszObject.getClass().getSimpleName() + "'");
			Database.CreateTable(aConn, clszObject);
			Database.CreatePrimaryKey(aConn, clszObject); // do alter table to create the primary key
			Database.CreateSequence(aConn, clszObject.createSequenceTableName()); // alter the table to make the primary key auto increment
			Database.CreateIndexes(aConn, clszObject.getTableName(), clszObject.getRecord());
		}

		return(clszObject);
	}

	/**
	 * Creates the data record for this clasz.
	 *
	 * @return
	 * @throws Exception
	 */
	private Record createRecord() throws Exception {
		Record dataRec = new Record();
		dataRec.createField(this.getMetaRec()); // create the structure of the meta rec to dataRec
		this.getRecordBox().put(Clasz.RECORD_AT, dataRec);
		this.createFieldPk(dataRec);
		this.createFieldChildCount(dataRec);

		return(dataRec);
	}

	/**
	 *
	 * This method is analogous to CreateObject method, unlike it this method 
	 * alter the table that should already been created by CreateObject method.
	 *
	 * @param aConn
	 * @param aClass - the abstract class
	 * @param aAvoidRecursive
	 * @param aDepth
	 * @param aInstant - the first instant-able child of the aClass abstract class
	 * @throws Exception
	 */
	private static void AlterObjectForAbstract(Connection aConn, Class aClass, Clasz aInstant, List aAvoidRecursive, int aDepth, boolean aDoDdl) throws Exception {
		Clasz object = aInstant.getClass().newInstance(); // a temporary clszObject to get the fields of aInstan clasz
		Record dataRec = new Record();
		object.getRecordBox().put(Clasz.RECORD_AT, dataRec);
		CreateFieldFromClass(aConn, aClass, object, "", false, aAvoidRecursive, aDepth, aDoDdl); // we create the fields in new clszObject so it can be use for sql alter table...

		if (Database.TableExist(aConn, object.getTableName())) {
			for(Field eachField : object.getRecord().getFieldBox().values()) { // place the abstract fields into the leaf clszObject
				aInstant.createField(eachField);
				aInstant.cloneField(eachField);
			}

			if (object.getRecord().totalField() > 0) {
				if (Database.AnyFieldExist(aConn, object.getTableName(), object.getRecord()) == false) {
					App.logInfo("Altering table for class: '" + object.getClass().getSimpleName() + "'");
					Database.AlterTableAddColumn(aConn, object);
					Database.CreateIndexes(aConn, object.getTableName(), object.getRecord());
				}
			}
		} else {
			throw new Hinderance("Cannot alter a non existing table: '" + aInstant.getClaszName() + "', for abstract class: '" + aClass.getSimpleName() + "', check inline flag");
		}
	}


	/**
	 * Function to retrieve the annotation fields declared in a class and convert
	 * them into Fields and place them into the pass in aRoot as the aRoot
	 * properties (in fact its a record at location 0 with those fields)
	 *
	 * @param aMember
	 * @param aParent
	 * @return
	 * @throws Exception
	 */
	private static void CreateFieldFromClass(Connection aConn, Class aMember, Clasz aParent, String fieldPrefix, Boolean aInline, List aAvoidRecursive, int aDepth, boolean aDoDdl) throws Exception {
		ObjectBase odb = aParent.getDb();
		for(java.lang.reflect.Field reflectField : aMember.getDeclaredFields()) {
			try {
				ReflectField eachAnnotation = (ReflectField) reflectField.getAnnotation(ReflectField.class);
				if (eachAnnotation != null) {
					AttribField attribField = new AttribField();
					FieldType fieldType = eachAnnotation.type();
					attribField.isInline = aInline;
					attribField.fieldName = reflectField.getName();
					attribField.fieldSize = eachAnnotation.size();
					attribField.fieldMask = eachAnnotation.mask();
					attribField.displayPosition = eachAnnotation.displayPosition();
					attribField.polymorphic = eachAnnotation.polymorphic();
					attribField.prefetch = eachAnnotation.prefetch();
					attribField.updateable = eachAnnotation.updateable();
					attribField.changeable = eachAnnotation.changeable();
					attribField.uiMaster = eachAnnotation.uiMaster();
					attribField.lookup = eachAnnotation.lookup();

					ReflectIndex[] reflectIndex = eachAnnotation.indexes();
					if (reflectIndex != null && reflectIndex.length != 0) {
						List<AttribIndex> uniqueIndexes = new CopyOnWriteArrayList<>();
						for (ReflectIndex eachReflect : reflectIndex) {
							AttribIndex attribIndex = new AttribIndex();
							attribIndex.indexName = eachReflect.indexName();
							attribIndex.isUnique = eachReflect.isUnique();
							attribIndex.indexNo = eachReflect.indexNo();
							attribIndex.indexOrder = eachReflect.indexOrder();
							uniqueIndexes.add(attribIndex);
						}
						attribField.indexes = uniqueIndexes;
					}

					if (Modifier.isStatic(reflectField.getModifiers()) == false) {
						throw new Hinderance("Database field must be of static type: '" + attribField.fieldName + "'");
					}

					// create the field name in database
					String dbFieldName = Clasz.CreateDbFieldName(attribField.fieldName, fieldPrefix);
					if (fieldPrefix == null || fieldPrefix.isEmpty()) {
						reflectField.set(attribField.fieldName, dbFieldName); // set the field name into the static variable
					}

					// programmer definition error, for diagnosing purpose only
					if (fieldType == FieldType.OBJECT || fieldType == FieldType.OBJECTBOX) {
						//if (eachAnnotation.clasz().isEmpty()) {
						if (eachAnnotation.clasz() == null) {
							throw new Hinderance("Error, missing clasz defintion in field: '" + attribField.fieldName + "'");
						}
					}

					if (fieldType == FieldType.OBJECT) {
						boolean doFieldDdl = true;
						boolean isAbstract = FieldObject.IsAbstract(eachAnnotation.clasz());

						if (isAbstract && eachAnnotation.inline()) {
							throw new Hinderance("Abstract field cannot be inline!");
						}

						if (isAbstract) attribField.polymorphic = true; // if abstract field, force it to be polymorphic
						if (eachAnnotation.inline() == true || isAbstract) {
							doFieldDdl = false; // for inline objects or abstract field, no need to create table for it
						}

						boolean fieldCreated = ClassAndFieldExist(aAvoidRecursive, aMember, attribField.fieldName);
						Clasz objField;
						Class objClass;
						if (PRE_CREATE_OBJECT == false && attribField.prefetch == false) {
							objField = null;
							//objClass = Class.forName(eachAnnotation.clasz());
							objClass = eachAnnotation.clasz();
						} else if (fieldCreated && aDepth > RECURSIVE_DEPTH) {
							objField = new Clasz();
							objClass = objField.getClass();
						} else {
							aAvoidRecursive.add(new ClassAndField(aMember, attribField.fieldName));
							if (isAbstract) {
								objField = null;
							} else {
								//objField = Clasz.CreateObject(odb, aConn, Class.forName(eachAnnotation.clasz()), null, doFieldDdl && aDoDdl, aAvoidRecursive, aDepth + 1, attribField.isInline);
								objField = Clasz.CreateObject(odb, aConn, eachAnnotation.clasz(), null, doFieldDdl && aDoDdl, aAvoidRecursive, aDepth + 1, attribField.isInline);
								objField.setMasterObject(aParent);
							}
							if (objField != null) {
								objClass = objField.getClass();
							} else {
								throw new Hinderance("Fail to create object for field: " + attribField.fieldName + ", of class: " + aParent.getClass().getSimpleName());
							}
						}

						if (attribField.isInline == false) {
							aParent.createFieldObject(dbFieldName, objField); // create the field of aRoot type in this aRoot // TODO change this to new FieldObject() for consistency
							aParent.getField(dbFieldName).setModified(false); // placing the clszObject into this new created field do not constitued the field is modified
							aParent.getField(dbFieldName).setInline(eachAnnotation.inline());
							aParent.getField(dbFieldName).deleteAsMember(eachAnnotation.deleteAsMember());
							SetFieldAttrib(aParent.getField(dbFieldName), attribField);
							//((FieldClasz) aParent.getField(dbFieldName)).setDeclareType(eachAnnotation.clasz());
							((FieldClasz) aParent.getField(dbFieldName)).setDeclareType(eachAnnotation.clasz().getName());
							((FieldClasz) aParent.getField(dbFieldName)).setPrefetch(attribField.prefetch);
						}

						if (attribField.isInline == true) {
							if (objClass != Clasz.class) {
								CreateFieldFromClass(aConn, objClass, aParent, dbFieldName, true, aAvoidRecursive, aDepth + 1, aDoDdl); // flatten all inline fields including inline inside an inline onto the master aRoot
							}
						} else {
							if (eachAnnotation.inline() == true) {
								if (objClass != Clasz.class) {
									CreateFieldFromClass(aConn, objClass, aParent, dbFieldName, true, aAvoidRecursive, aDepth + 1, aDoDdl); // flatten all inline fields including inline inside an inline onto the master aRoot
								}
							} else {
								ObjectBase.createMemberOfTable(aConn, aParent.getClass(), objClass, attribField.polymorphic, dbFieldName); // now create the table to associate the "member of" relationship
								/*
								if (attribField.polymorphic) {
									ObjectBase.createPolymorphicTable(aConn, aParent.getClass());
								}
								*/
							}
						}
					} else if (fieldType == FieldType.OBJECTBOX) {
						//Class memberClass = Class.forName(eachAnnotation.clasz());						
						Class memberClass = eachAnnotation.clasz();
						boolean fieldCreated = ClassAndFieldExist(aAvoidRecursive, aMember, attribField.fieldName);
						Clasz metaObj;
						if (fieldCreated && aDepth > RECURSIVE_DEPTH) {
							metaObj = new Clasz();
						} else {
							aAvoidRecursive.add(new ClassAndField(aMember, attribField.fieldName));
							if (FieldObject.IsAbstract(eachAnnotation.clasz())) {
								metaObj = new Clasz();
							} else {
								metaObj = Clasz.CreateObject(odb, aConn, memberClass, null, aDoDdl, aAvoidRecursive, aDepth + 1, false);
							}
						}
						metaObj.setMasterObject(aParent);
						aParent.createFieldObjectBox(dbFieldName, new FieldObjectBox(metaObj)); // create the field box inside aRoot
						aParent.getField(dbFieldName).setModified(false);
						aParent.getField(dbFieldName).deleteAsMember(eachAnnotation.deleteAsMember());
						((FieldClasz) aParent.getField(dbFieldName)).setPrefetch(attribField.prefetch);
						SetFieldAttrib(aParent.getField(dbFieldName), attribField);
						//((FieldObjectBox) aParent.getField(dbFieldName)).setDeclareType(eachAnnotation.clasz());
						((FieldObjectBox) aParent.getField(dbFieldName)).setDeclareType(eachAnnotation.clasz().getName());
						if (aDoDdl) FieldObjectBox.CreateBoxMemberTable(aConn, aParent.getClass(), dbFieldName); // now create the table to associate the array of "member of" relationship
					} else {
						if (fieldType == FieldType.UNKNOWN) {
							throw new Hinderance("Database field type is not define for field: '" + dbFieldName + "'");
						} else if (fieldType == FieldType.STRING && attribField.fieldSize == 0) {
							throw new Hinderance("Database field: '" + dbFieldName + "', of string type cannot have 0 size, in class: '" + aMember.getSimpleName() + "'");
						}

						Field createdField;
						if (attribField.fieldSize <= 0) {
							createdField = aParent.createField(dbFieldName, fieldType);
						} else {
							createdField = aParent.createField(dbFieldName, fieldType, attribField.fieldSize);
						}

						if (attribField.fieldMask.isEmpty() == false) {
							createdField.setFieldMask(attribField.fieldMask);
						}

						SetFieldAttrib(createdField, attribField);
					}
				}
			} catch (Exception ex) {
				throw new Hinderance(ex, "Error at class: '" + aMember.getSimpleName() + "', fail to create field: '" + reflectField.getName() + "'");
			}
		}
	}

	private static void SetFieldAttrib(Field createdField, AttribField aAttribField) throws Exception {
		createdField.setUiMaster(aAttribField.uiMaster);
		createdField.setLookup(aAttribField.lookup);
		createdField.displayPosition(aAttribField.displayPosition);
		createdField.setPolymorphic(aAttribField.polymorphic);
		createdField.setUpdateable(aAttribField.updateable);
		createdField.setChangeable(aAttribField.changeable);

		if (aAttribField.isInline != null && aAttribField.isInline == true) {
			createdField.forDisplay(false); // if is inline field or child of inline field, then is not for display
			createdField.setFlatten(true); // flatten field is not for display
		} else {
			if (createdField.isSystemField()) {
				createdField.forDisplay(false); // system field is never for display 
			} else {
				createdField.forDisplay(true); // default for display attribute
			}
		}

		if (aAttribField.indexes != null && aAttribField.indexes.isEmpty() == false) {
			for (AttribIndex eachIndex : aAttribField.indexes) {
				createdField.indexes.add(eachIndex);
			}
		}
	}

	public static String CreateDbFieldName(String fieldName) {
		return(Clasz.CreateDbFieldName(fieldName, null));
	}

	public static String CreateDbFieldName(String fieldName, String fieldPrefix) {
		String dbFieldName = Database.Java2DbFieldName(fieldName);
		if (fieldPrefix != null && fieldPrefix.isEmpty() == false) {
			dbFieldName = Database.Java2DbFieldName(fieldPrefix) + "_" + dbFieldName; // place prefix for inline fields (not supported yet though)
		}
		return(dbFieldName);
	}

	public static Clasz Fetch(Connection aConn, Clasz aCriteria) throws Exception {
		Clasz result;
		ObjectBase odb = aCriteria.getDb();
		result = CreateObject(odb, aConn, aCriteria.getClass()); // use a new clszObject to do slow Fetch, just as fast Fetch, don't touch the aCriteria
		result.copyAllFieldWithModifiedState(aCriteria);
		result = fetchObjectSlow(aConn, result);
		return(result);
	}

	public static Clasz Fetch(Connection aConn, Class aClass, Long aObjId) throws Exception {
		Clasz result = ObjectBase.CreateObject(aConn, aClass);
		result.setObjectId(aObjId);
		result = fetchObjectSlow(aConn, result);
		return(result);
	}

	public static Clasz Fetch(ObjectBase aDb, Connection aConn, Class aClass, Long aObjId) throws Exception {
		Clasz result = CreateObject(aDb, aConn, aClass);
		result.setObjectId(aObjId);
		result = fetchObjectSlow(aConn, result);
		return(result);
	}

	/**
	 * To be eligible for fast Fetch, there Fetch criteria must be at the leaf
	 * clszObject.
	 *
	 * @param aCriteria
	 * @return
	 */
	private static boolean canFastFetch(Clasz aCriteria) {
		boolean isEligible = false;
		for(Field eachField : aCriteria.getRecord().getFieldBox().values()) {
			if (eachField.isModified()) {
				isEligible = true;
				break;
			}
		}

		return(isEligible);
	}

	/**
	 * This method fetches the record for the leaf clszObject, then thereafter
	 * recursively fetches each of the parent clszObject in the inheritance tree. This
	 * mean if the inheritance tree has X depth, then there will be X sql fetches.
	 * This is slow compare to the fast version that uses ONE sql Fetch
	 * irregardless of the inheritance depth.
	 *
	 * @param aConn
	 * @param aCriteria
	 * @return
	 * @throws Exception
	 */
	private static Clasz fetchObjectSlow(Connection aConn, Clasz aCriteria) throws Exception {
		Clasz result = aCriteria;
		Clasz objectWithPk;
		if (aCriteria.getObjectId() == null || aCriteria.isPopulated() == false) { // if no objectId, we do fetch for it using given criteria in the object fields
			objectWithPk = FetchUniqueUsingCriteria(aConn, aCriteria);
		} else {
			objectWithPk = aCriteria;
		}

		if (objectWithPk != null) {
			boolean upFetchFound = fetchObjectUpTheTree(aConn, objectWithPk, objectWithPk.getObjectId());
			boolean downFetchFound = fetchObjectDownTheTree(aConn, objectWithPk, aCriteria);
			if (upFetchFound == false && downFetchFound == false) {
				result = null;
			}
		} else {
			result = null;
		}

		return(result);
	}

	/**
	 * This is a tune for Fetch clszObject, the pass in class must be a leaf clszObject as
	 * this method do not do fetching down the tree, only fetching up the tree.
	 *
	 * @param aDb
	 * @param aConn
	 * @param aClass
	 * @param aObjectId
	 * @return
	 * @throws Exception
	 */
	public static Clasz fetchObjectByPk(ObjectBase aDb, Connection aConn, Class aClass, Long aObjectId) throws Exception {
		Clasz result = CreateObject(aDb, aConn, aClass);
		fetchObjectUpTheTree(aConn, result, aObjectId);
		return(result);
	}

	/**
	 * Get the primary key of the aCriteria clszObject by fetching the record
	 * according to the aCriteria fill in fields. The filled values inside the
	 * Clasz clszObject must be one that can uniquely identify a specific clszObject. TODO
	 * validation to ensure field is fill with fields that make up a isUnique key
	 * for that Clasz.
	 *
	 * @param aConn
	 * @param aCriteria
	 * @return
	 * @throws Exception
	 */
	public static Clasz FetchUniqueUsingCriteria(Connection aConn, Clasz aCriteria) throws Exception {
		Clasz result = null;

		// set the where record
		//Map<String, Record> whereBox = new ConcurrentHashMap<>(); // each table name (string) and a record for the where fields (record)
		Multimap<String, Record> whereBox = Multimaps.synchronizedListMultimap(ArrayListMultimap.create());
		ObjectBase.GetLeafSelectCriteria(aConn, aCriteria, whereBox); // the select criteria for this leaf clszObject, doesn't do the parent clszObject
		int cntrRec = 0;

		// place in the where criteria into sql string
		StringBuffer strBuffer = new StringBuffer();
		List<Field> aryWhere = Database.GetWhereClause(whereBox, strBuffer); // convert the where record into array list
		String sqlStr = "select * from " + Database.GetFromClause(aCriteria.getTableName(), whereBox);
		sqlStr += " where " + strBuffer.toString();

		PreparedStatement stmt = null; // now do the sql Fetch
		ResultSet rs = null;
		try {
			stmt = aConn.prepareStatement(sqlStr);
			Database.SetStmtValue(stmt, aryWhere);
			rs = stmt.executeQuery();
			while (rs.next()) {
				if (cntrRec > 0) {
					throw new Hinderance("Fetch unique returns more then one record: '" + aCriteria.getClass().getSimpleName() + "', " + stmt.toString());
				}
				aCriteria.setObjectId(rs.getLong(aCriteria.getPkName()));
				result = aCriteria;
				cntrRec++;
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

	public static boolean StartDebug = false;
	public static GregorianCalendar StartTime = new GregorianCalendar();

	/**
	 * @param aConn
	 * @param aResult
	 * @param aParentKey
	 * @return 
	 * @throws Exception
	 */
	public static boolean fetchObjectUpTheTree(Connection aConn, Clasz aResult, Long aParentKey) throws Exception {
		boolean result = false;
		String sqlStr;
		Long parentPkValue = null;

		Clasz parentObject = aResult.getParentObjectByContext(); // parent clszObject can be null if parent class is abstract class up the tree until Clasz class
		if (parentObject != null && parentObject.getClass().equals(Clasz.class) == false && ObjectBase.ParentIsNotAbstract(aResult)) {
			sqlStr = "select * from " + aResult.getTableName() + ", " + aResult.getIhTableName(); // got parent class, do the sql matching
			sqlStr += " where " + aResult.getTableName() + "." + aResult.getPkName() + " = " + aResult.getIhTableName() + "." + aResult.getPkName();
			sqlStr += " and " + aResult.getTableName() + "." + aResult.getPkName() + " = ?";
		} else { // if aResult parent is objeck, then it will not have the ih_* table
			sqlStr = "select * from " + aResult.getTableName();
			sqlStr += " where " + aResult.getTableName() + "." + aResult.getPkName() + " = ?";
		}

		FieldLong whereField = new FieldLong(aResult.getPkName());
		whereField.setValueLong(aParentKey);
		List<Field> fieldArr = new CopyOnWriteArrayList<>();
		fieldArr.add(whereField);

		PreparedStatement stmt = null; // now do the sql Fetch
		ResultSet rs = null;
		int cntrRec = 0;
		try {
			stmt = aConn.prepareStatement(sqlStr);
			Database.SetStmtValue(stmt, fieldArr);
			rs = stmt.executeQuery();
			while (rs.next()) {
				result = true;
				if (cntrRec > 0) {
					throw new Hinderance("Object to fetch criteria returns more then one record: '" + aResult.getClass().getSimpleName() + "'");
				}
				aResult.populateObject(aConn, rs, false);
				if (parentObject != null && parentObject.getClass().equals(Clasz.class) == false) {
					String parentPkName = parentObject.getPkName();
					parentPkValue = rs.getLong(parentPkName);
				}
				cntrRec++;
			}
		} catch (Exception ex) {
			if (stmt != null) 
				throw new Hinderance(ex, "Fail fetching up the inheritance tree: '" + stmt.toString() + "'!");
			else 
				throw new Hinderance(ex, "Fail fetching up the inheritance tree");
		} finally {
			if (rs != null) {
				rs.close();
			}
			if (stmt != null) {
				stmt.close();
			}
		}

		if (cntrRec == 1 && (parentObject != null && parentObject.getClass().equals(Clasz.class) == false)) {
			fetchObjectUpTheTree(aConn, parentObject, parentPkValue);
		}

		return(result);
	}

	//
	// Tune by fetching parent and child in one sql call
	//
	public static void fetchObjectUpTheTree2(Connection aConn, Clasz aResult, Long aParentKey) throws Exception {
		String sqlStr;
		Clasz parentObject = aResult.getParentObjectByContext(); // parent clszObject can be null if parent class is abstract class up the tree until Clasz class
		if (ObjectBase.ParentIsNotAtClaszYet(aResult) && ObjectBase.ParentIsNotAbstract(aResult)) {
			if (parentObject != null && ObjectBase.ParentIsNotAtClaszYet(aResult)) {
				sqlStr = "select * from " + aResult.getTableName() + ", " + aResult.getIhTableName() + ", " + parentObject.getTableName(); // got parent class, do the sql matching
				sqlStr += " where " + aResult.getTableName() + "." + aResult.getPkName() + " = " + aResult.getIhTableName() + "." + aResult.getPkName();
				sqlStr += " and " + aResult.getTableName() + "." + aResult.getPkName() + " = ?";
				sqlStr += " and " + aResult.getTableName() + "." + aResult.getPkName() + " = " + parentObject.getTableName() + "." + parentObject.getPkName();
			} else {
				sqlStr = "select * from " + aResult.getTableName() + ", " + aResult.getIhTableName(); // got parent class, do the sql matching
				sqlStr += " where " + aResult.getTableName() + "." + aResult.getPkName() + " = " + aResult.getIhTableName() + "." + aResult.getPkName();
				sqlStr += " and " + aResult.getTableName() + "." + aResult.getPkName() + " = ?";
			}
		} else { // if aResult parent is objeck, then it will not have the ih_* table
			sqlStr = "select * from " + aResult.getTableName();
			sqlStr += " where " + aResult.getTableName() + "." + aResult.getPkName() + " = ?";
		}
		FieldLong whereField = new FieldLong(aResult.getPkName());
		whereField.setValueLong(aParentKey);
		List<Field> fieldArr = new CopyOnWriteArrayList<>();
		fieldArr.add(whereField);

		PreparedStatement stmt = null; // now do the sql Fetch
		ResultSet rs = null;
		int cntrRec = 0;
		try {
			stmt = aConn.prepareStatement(sqlStr);
			Database.SetStmtValue(stmt, fieldArr);
			rs = stmt.executeQuery();
			while (rs.next()) {
				if (cntrRec > 0) {
					throw new Hinderance("Object to fetch criteria returns more then one record: '" + aResult.getClass().getSimpleName() + "'");
				}
				aResult.populateObject(aConn, rs, false);
				if (parentObject != null && ObjectBase.ParentIsNotAtClaszYet(aResult)) {
					parentObject.populateObject(aConn, rs, false);
				}
				cntrRec++;
			}
		} catch (Exception ex) {
			if (stmt != null) {
				throw new Hinderance(ex, "Fail fetching up the inheritance tree: " + stmt.toString());
			} else {
				throw new Hinderance(ex, "Fail fetching up the inheritance tree");
			}
		} finally {
			if (rs != null) {
				rs.close();
			}
			if (stmt != null) {
				stmt.close();
			}
		}

		if (parentObject != null) {
			Clasz parentParentObject = parentObject.getParentObjectByContext();
			if (cntrRec == 1 && (parentParentObject != null && ObjectBase.ParentIsNotAtClaszYet(parentObject))) {
				sqlStr = "select * from " + parentObject.getTableName() + ", " + parentObject.getIhTableName(); // got parent class, do the sql matching
				sqlStr += " where " + parentObject.getTableName() + "." + parentObject.getPkName() + " = " + parentObject.getIhTableName() + "." + parentObject.getPkName();
				sqlStr += " and " + parentObject.getTableName() + "." + parentObject.getPkName() + " = ?";
				
				whereField = new FieldLong(parentObject.getPkName());
				whereField.setValueLong(parentObject.getObjectId());
				fieldArr = new CopyOnWriteArrayList<>();
				fieldArr.add(whereField);

				PreparedStatement stmt1 = null;
				ResultSet rs1 = null;
				try {
					stmt1 = aConn.prepareStatement(sqlStr);
					Database.SetStmtValue(stmt1, fieldArr);
					rs1 = stmt1.executeQuery();
					if (rs1.next()) {
						String parentParentPkName = parentParentObject.getPkName();
						Long parentParentPkValue = rs1.getLong(parentParentPkName);
						fetchObjectUpTheTree(aConn, parentParentObject, parentParentPkValue);
					} else {
						throw new Hinderance("Inheritance database error: " + stmt1.toString());
					}
				} catch (Exception ex) {
					if (stmt1 != null) {
						throw new Hinderance(ex, "Fail fetching up the inheritance tree: " + stmt1.toString());
					} else {
						throw new Hinderance(ex, "Fail fetching up the inheritance tree");
					}
				} finally {
					if (rs1 != null) {
						rs1.close();
					}
					if (stmt1 != null) {
						stmt1.close();
					}
				}
			}
		}
	}

	public static boolean fetchObjectDownTheTree(Connection conn, Clasz aObjectBranch, Clasz aObjectLeaf) throws Exception {
		boolean result = false;
		if (aObjectBranch.getClass().equals(aObjectLeaf.getClass()) == false) { // determine if still to Fetch down the tree
			Clasz childObject = aObjectBranch.getChildObject(conn, aObjectLeaf); // get the immediate child of this branch clszObject, aObjectLeaf will be populated with values from db
			if (childObject != null) {
				result = true;
				if (childObject.getClass().equals(aObjectLeaf.getClass()) == false) {
					fetchObjectDownTheTree(conn, childObject, aObjectLeaf); // recursive Fetch down the tree
				}
			}
		}
		return(result);
	}

	/**
	 * Fetches one clszObject from the database according to the filled in properties
	 * in aCriteria. The criteria can be set in the properties of the clszObject
	 * either at any of its parent, child or instant variable. This method will
	 * parse the clszObject and build up the select criteria to Fetch the clszObject. Can
	 * also chose to Fetch all the fields in the clszObject or just the primary key
	 * for each record in the clszObject, this is use in cases where the structure of
	 * the clszObject is use for deletion. This is known as a fast Fetch because it
	 * uses one sql call to Fetch all the record related to each other in the
	 * inheritance tree.
	 *
	 * TODO: need not Fetch all field if aFetchAllField is set to false (will it
	 * improve performance?)
	 *
	 *
	 * @param aConn
	 * @param aCriteria
	 * @param aFetchAllField
	 * @return
	 * @throws Exception
	 */
	/*
	public static Clasz fetchObjectFast(Connection aConn, Clasz aCriteria, boolean aFetchAllField) throws Exception {
		Clasz result = CreateObject(aCriteria.getDb(), aConn, aCriteria.getClass());

		// populate the whereRec with the fields for select criteria
		Map<String, Record> whereBox = new ConcurrentHashMap<>(); // each table name (string) and a record for the where fields (record)
		ObjectBase.GetLeafSelectCriteria(aConn, aCriteria, whereBox);

		// get the sql that describes the inheritance relationship
		StringBuffer strInherit = new StringBuffer();
		StringBuffer strTable = new StringBuffer();
		ObjectBase.getInheritanceWhere(result, strInherit, strTable);
		String sqlStr = "select * from " + strTable.toString();
		if (strInherit.toString().isEmpty() == false) {
			sqlStr += " where " + strInherit.toString();
		}

		// get the field array to be use by SetStmtValue and get the string for this additional where string
		StringBuffer strBuffer = new StringBuffer();
		Iterator whereIter = whereBox.entrySet().iterator();
		List<Field> masterArr = new CopyOnWriteArrayList<>();
		while (whereIter.hasNext()) { // for each of the table in the inheritance tree
			Entry whereEntry = (Entry) whereIter.next();
			String tableName = (String) whereEntry.getKey();
			Record whereRec = (Record) whereEntry.getValue();
			if (strBuffer.length() != 0) {
				strBuffer.append(" and ");
			}
			List<Field> fieldArr = Database.GetWhereClause(tableName, whereRec, strBuffer);
			masterArr.addAll(fieldArr);
		}
		String sqlWhere = strBuffer.toString();

		// now do the sql Fetch
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			if (sqlWhere.trim().isEmpty() == false) {
				if (sqlStr.contains("where")) {
					sqlStr += " and ";
				} else {
					sqlStr += " where ";
				}
				sqlStr += sqlWhere; // append the inheritance sql statement to the select criteria where
			}
			stmt = aConn.prepareStatement(sqlStr);
			Database.SetStmtValue(stmt, masterArr);
			rs = stmt.executeQuery();
			int cntrRec = 0;
			while (rs.next()) {
				if (cntrRec > 0) {
					throw new Hinderance("Cannot fetch object with duplicate record from database for class: '" + aCriteria.getClass().getSimpleName() + "'");
				}
				result.populateObject(aConn, rs);
				cntrRec++;
			}
			if (cntrRec != 1) {
				result = null;
			}
		} catch (Exception ex) {
			if (stmt != null) {
				throw new Hinderance(ex, "Fail when performing fast fetch: " + stmt.toString());
			} else {
				throw new Hinderance(ex, "Fail when performing fast fetch");
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
	*/

	/* 
	* Start of the central methods to create field object and field object box
	*/
	public FieldObjectBox createFieldObjectBoxTransient(Connection aConn, String aName, Class aType) throws Exception {
		Clasz tempMeta = ObjectBase.CreateObjectTransient(aConn, aType);
		FieldObjectBox fob = new FieldObjectBox(tempMeta);
		return(createFieldObjectBox(aName, fob, aType));
	}

	public FieldObjectBox createFieldObjectBox(String aName, FieldObjectBox aFieldObjectBox) throws Exception {
		if (aFieldObjectBox == null) {
			return createFieldObjectBox(aName, aFieldObjectBox, Unknown.class);
		} else {
			return createFieldObjectBox(aName, aFieldObjectBox, aFieldObjectBox.getMetaObj().getClass());
		}
	}

	public FieldObjectBox createFieldObjectBox(String aName, FieldObjectBox aFieldObjectBox, Class aType) throws Exception {
		FieldObjectBox result = (FieldObjectBox) this.getRecord().createField(aName, aFieldObjectBox);
		if (aFieldObjectBox != null) {
			if (aFieldObjectBox.getMetaObj() == null) {
				throw new Hinderance("Fail to create FieldObjectBox, its meta object is not define, field name: " + aName);
			}
			if (aFieldObjectBox.getMetaObj().getClass().equals(aType) == false) {
				throw new Hinderance("The pass in clasz object type and its pass in class type is different when trying to create FieldObjectBox: " + aName);
			}
		}
		result.setDeclareType(aType.getName()); // here we allow creating field with null object
		result.setMasterObject(this);
		//this.claszField.put(aName, result);
		this.gotCreatedField = true;
		return(result);
	}

	public FieldObject createFieldObject(String aName, Clasz aObject) throws Exception {
		if (aObject == null) {
			return createFieldObject(aName, aObject, Unknown.class);
		} else {
			return createFieldObject(aName, aObject, aObject.getClass());
		}
	}

	public FieldObject createFieldObject(String aName, Clasz aObject, Class aType) throws Exception {
		FieldObject result = (FieldObject) this.getRecord().createField(aName, aObject);
		if (aObject != null && aObject.getClass().equals(aType) == false) {
			throw new Hinderance("The pass in clasz object type and its pass in class type is different when trying to create FieldObject: " + aName);
		}
		result.setDeclareType(aType.getName()); // here we allow creating field with null object
		result.setObj(aObject);
		result.setMasterObject(this);
		//this.claszField.put(aName, result);
		this.gotCreatedField = true;
		return(result);
	}
	/* 
	* End of the central methods to create field object and field object box
	*/

	public FieldStr createFieldStr(String aFieldName, String aValue) throws Exception {
		FieldStr result = (FieldStr) this.createField(aFieldName, FieldType.STRING, aValue.length());
		result.setValueStr(aValue);
		return(result);
	}

	public FieldDate createFieldDate(String aFieldName, DateTime aDateValue) throws Exception {
		FieldDate result = (FieldDate) this.createField(aFieldName, FieldType.DATE);
		result.setValueDate(aDateValue);
		return(result);
	}

	public void createField(Field aField) throws Exception {
		if (aField.getFieldType() == FieldType.OBJECT) {
			Clasz objField = ((FieldObject) aField).getObj();
			this.createFieldObject(aField.getFieldName(), objField); // copy by reference for clszObject, no need do deep copy
		} else if (aField.getFieldType() == FieldType.OBJECTBOX) {
			this.createFieldObjectBox(aField.getFieldName(), (FieldObjectBox) aField); // copy by reference for clszObject, no need do deep copy
		} else {
			this.getRecord().createField(aField);
			this.getRecord().getField(aField.getFieldName()).setMasterObject(this);
			//this.claszField.put(aField.getFieldName(), aField);
			this.gotCreatedField = true;
		}
	}

	@Override
	public Field createField(String aName, FieldType aType) throws Exception {
		super.createField(aName, aType);
		Field result = this.getRecord().createField(aName, aType);
		result.setMasterObject(this);
		//this.claszField.put(aName, result);
		this.gotCreatedField = true;
		return(result);
	}

	@Override
	public Field createField(String aName, FieldType aType, int aSize) throws Exception {
		super.createField(aName, aType, aSize);
		Field result = this.getRecord().createField(aName, aType, aSize);
		result.setMasterObject(this);
		//this.claszField.put(aName, result);
		this.gotCreatedField = true;
		return(result);
	}

	public Field createField(String aName, FieldType aType, int aSize, String aValue) throws Exception {
		super.createField(aName, aType, aSize);
		Field result = this.getRecord().createField(aName, aType, aSize, aValue);
		result.setMasterObject(this);
		//this.claszField.put(aName, result);
		this.gotCreatedField = true;
		return(result);
	}

	public void cloneField(Field aField) throws Exception {
		if (aField.getFieldType() == FieldType.OBJECT || aField.getFieldType() == FieldType.OBJECTBOX) {
			this.getRecord().getField(aField.getFieldName()).copyAttribute(aField); // the clszObject field have been shallowed copy, so no need to clone it, just preserve the field attribute
		} else {
			this.getRecord().cloneField(aField);
		}
		//this.claszField.put(aName, result);
	}

	/**
	 * Recursively traverse all the fields, if its modified, return true.
	 *
	 * TODO: tune this, the flag be set in the field clszObject and propagated to the
	 * clszObject when its change
	 *
	 *
	 * @return
	 * @throws Exception
	 *
	 */
	public boolean allOfMyselfIsModified() throws Exception {
		if (this.getClaszName().equals("Clasz")) {
			return(false);
		}

		boolean result = false;
		for(Field eachField : this.getRecord().getFieldBox().values()) {
			try {
				if (eachField.getFieldType() == FieldType.OBJECT) {
					if (eachField.isModified()) {
						result = true;
						break;
					} else {
						Clasz obj = ((FieldObject) eachField).getObj();
						if (obj != null) {
							result = obj.allOfMyselfIsModified();
						}
					}
				} else if (eachField.getFieldType() == FieldType.OBJECTBOX) {
					if (eachField.isModified()) {
						result = true;
						break;
					} else {
						for(Clasz memberObject : ((FieldObjectBox) eachField).getObjectMap().values()) {
							result = memberObject.allOfMyselfIsModified();
							if (result == true) {
								break;
							}
						}
					}
				} else {
					if (eachField.isModified()) {
						result = true;
					}
				}

				if (result == true) {
					break;
				} else {
					if (ObjectBase.ParentIsNotAtClaszYet(this)) {
						Clasz parentObj = this.getParentObjectByContext();
						if (parentObj != null) {
							result = parentObj.allOfMyselfIsModified();
						}
					}
				}
			} catch (Exception ex) {
				throw new Hinderance(ex, "Error at: '" + this.getClass().getSimpleName() + "', when determining if field: " + eachField.getFieldName() + "' is modified");
			}

		}
		return(result);
	}

	public boolean onlyMyselfIsModified() throws Exception {
		boolean result = false;
		for(Field eachField : this.getRecord().getFieldBox().values()) {
			if (eachField.getFieldType() == FieldType.OBJECT) {
				// do nothing, inline field is handled by the usual FieldObject and detected as inLine when persisting 
			} else if (eachField.getFieldType() == FieldType.OBJECTBOX) {
				// do nothing, inline field is handled by the usual FieldObjectBox and detected as inLine when persisting 
			} else {
				if (eachField.isModified()) {
					result = true;
				}
			}

			if (result == true) {
				break;
			}
		}
		return(result);
	}

	public FieldClasz getFieldObj(String aFieldName) throws Exception {
		FieldClasz result = (FieldClasz) this.getField(aFieldName);
		return(result);
	}

	public FieldObject getFieldObject(String aFieldName) throws Exception {
		FieldObject result = (FieldObject) this.getField(aFieldName);
		return(result);
	}

	public FieldObjectBox getFieldObjectBox(String aFieldName) throws Exception {
		FieldObjectBox result = (FieldObjectBox) this.getField(aFieldName);
		return(result);
	}

	public boolean gotField(String aFieldName) {
		boolean result = true;
		try {
			this.getField(aFieldName);
		} catch(Exception ex) {
			result = false;
		}

		return(result);
	}

	@Override
	public Field getField(String aFieldName) throws Exception {
		try {
			Field result;
			if (aFieldName.equals("objectid")) {
				result = this.getRecord().getFieldLong(this.getPkName());
			} else if (this.fieldExist(aFieldName)) {
				result = this.getRecord().getField(aFieldName);
			} else {
				result = this.getParentObjectByContext().getField(aFieldName);
				if (result == null) {
					throw new Hinderance("Fail to get field: '" + aFieldName + ", no such field in object: '" + this.getClaszName() + "'");
				}
			}
			return(result);
		} catch (Exception ex) {
			throw new Hinderance(ex, "Fail to get the field: '" + aFieldName + "'");
		}
	}

	public FieldInt getFieldInt(String aFieldName) throws Exception {
		FieldInt result = (FieldInt) this.getField(aFieldName);
		return(result);
	}

	@Override
	public boolean fieldExist(String aName) {
		boolean result = false;
		if (this.getRecord() != null) {
			if (this.getRecord().fieldExist(aName)) {
				result = true;
			}
		}
		return(result);
	}

	public List<Field> getLeafField() throws Exception {
		List<Field> result = new CopyOnWriteArrayList<>();
		if (this.getRecord() != null) {
			this.getRecord().getFieldBox().values().forEach((eachField) -> {
				result.add(eachField);
			});
		}
		return(result);
	}

	/**
	 * Get the field definition of this clasz, i.e. the field created with the
	 * CreateField method and all its inherited field
	 *
	 * @return
	 * @throws java.lang.Exception
	 */
	public ConcurrentHashMap<String, Field> refreshTreeField() throws Exception {
		this.loadTreeField();
		return(this.claszField);
	}

	public ConcurrentHashMap<String, Field> getTreeField() throws Exception {
		if (this.claszField.isEmpty() || this.gotFieldDeleted() || this.gotFieldCreated()) {
			this.loadTreeField();
		}
		return(this.claszField);
	}

	public ConcurrentHashMap<String, Field> loadTreeField() throws Exception {
		this.claszField.clear();
		GetTreeField(this, claszField);
		return(this.claszField);
	}

	/**
	 * Recursively go up the inheritance three and get all the field that's been
	 * defined in the meta record.
	 *
	 * @param aClasz
	 * @param aResult
	 * @throws Exception
	 */
	private static void GetTreeField(Clasz aClasz, ConcurrentHashMap<String, Field> aResult) throws Exception {
		try {
			if (aClasz.getRecord() != null) { // recursive clasz will not have any record to it
				for(Field eachField : aClasz.getRecord().getFieldBox().values()) {
					aResult.put(eachField.getFieldName(), eachField);
				}

				if (ObjectBase.ParentIsNotAtClaszYet(aClasz)) {
					Clasz parentClasz = aClasz.getParentObjectByContext();
					if (parentClasz != null) { // its possible the parent class for this clszObject is abstract and still not at Clasz
						GetTreeField(parentClasz, aResult);
					}
				}
			}
		} catch (Exception ex) {
			throw new Hinderance(ex, "Fail to flatten the field in class: '" + aClasz.getClass().getSimpleName() + "'");
		}
	}

	public static int compare(Clasz aLeft, Clasz aRight) throws Exception {
		int result = 0;
		Clasz left = (Clasz) aLeft;
		Clasz right = (Clasz) aRight;

		// get the sorting key first
		ConcurrentSkipListMap<Integer, Field> sortKey = new ConcurrentSkipListMap<>();
		for(Field eachField : left.getTreeField().values()) {
			if (eachField.isSortKey()) {
				sortKey.put(eachField.getSortKeyNo(), eachField);
			}
		}

		// compare the sort key, with the lowest numbered key first
		for(Field eachField : sortKey.values()) {
			if (eachField.getSortOrder() == null || eachField.getSortOrder() == SortOrder.ASC) {
				result = eachField.compareTo(right.getField(eachField.getFieldName()));
			} else {
				Field rightField = right.getField(eachField.getFieldName());
				result = rightField.compareTo(left.getField(rightField.getFieldName()));
			}
			if (result != 0) {
				break;
			}
		}

		return(result);
	}

	@Override
	public int compareTo(Object aRight) {
		int result = 0;
		try {
			result = compare(this, (Clasz) aRight);
		} catch (Exception ex) {
			throw new AssertionError("Fail to compare clasz: '" + this.getClass().getSimpleName() + "' and " + aRight.getClass().getSimpleName() + "'" + App.LineFeed + ex.getMessage()); 
			//App.logEror(ex, "Fail to compare clasz: '" + this.getClass().getSimpleName() + "' and " + aRight.getClass().getSimpleName() + "'"); // doesn't like this, the overidden method doesn't allow  exception throwing
		}
		return(result);
	}

	public String getClaszName() {
		return(this.getClass().getSimpleName());
	}

	public void validateBeforePersist(Connection aConn) throws Exception {
	}

	public static Clasz getParentForField(Clasz aClasz, FieldObject aField) throws Exception {
		Clasz result = null;
		Field field = aClasz.getRecord().getField(aField.getFieldName());
		if (field != null) {
			result = aClasz;
		} else {
			Clasz parentObj = aClasz.getParentObjectByContext();
			if (parentObj == null) {
				throw new Hinderance("There is no field: '" + aField.getFieldName() + "', in: '" + aClasz.getClass().getSimpleName() + "'");
			}
			result = getParentForField(parentObj, aField);
		}
		return(result);
	}

	/**
	 * Traverse up the inheritance tree and return the parent clszObject that have the
	 * same type of aClass pass in parameter.
	 *
	 * @param aClass
	 * @return
	 * @throws Exception
	 */
	public Clasz getParentObject(Class aClass) throws Exception {
		if (this.getClass().equals(aClass)) {
			return(this);
		}

		Clasz result = null;
		Clasz parentObj = this.getParentObjectByContext();
		try {
			if (parentObj == null) {
				throw new Hinderance("This class: '" + aClass.getSimpleName() + "', do not have parent of the class: '" + this.getClass().getSimpleName() + "'");
			} else if (parentObj.getClass().equals(aClass)) { // if the parent of this class is the same as the class of 'this' clszObject, then this class is the child class
				result = parentObj;
			} else {
				result = parentObj.getParentObject(aClass); // recursive traversing up the tree to get the immediate child class of this clszObject
			}
		} catch (Exception ex) {
			throw new Hinderance(ex, "Fail to get parent object of class: '" + aClass.getSimpleName() + "', from: '" + this.getClass().getSimpleName() + "'");
		}
		return(result);
	}

	/**
	 * Like getMemberObject, this method fetches all the member clszObject from the
	 * database for this array. This method is call when the member field is recursive
	 * in nature. This method is use by the deleteCommit method to retrieve not retrieve 
	 * objects due to recursion.
	 *
	 * @param aConn
	 * @param aField
	 * @param aMemberClass
	 * @return
	 * @throws Exception
	 */
	public FieldObjectBox getMemberObjectBox(Connection aConn, FieldObjectBox aField, Class aMemberClass) throws Exception {
		Clasz masterObj = this.getParentObject(aMemberClass);
		return(this.getMemberObjectBox(aConn, aField, masterObj, aMemberClass));
	}

	public FieldObjectBox getMemberObjectBox(Connection aConn, FieldObjectBox aField, Clasz masterObj, Class aMemberClass) throws Exception {
		aField.getObjectMap().clear();
		if (aField.getMetaObj().getClaszName().equals("Clasz")) {
			//Clasz metaObj = Clasz.CreateObject(masterObj.getDb(), aConn, aMemberClass);
			Clasz metaObj = ObjectBase.CreateObject(aConn, aMemberClass);
			aField.setMetaObj(metaObj);
			aField.fetchAll(aConn);
		}
		return(aField);
	}

	public void displayAll(boolean aSet) throws Exception {
		for(Field eachField : this.getTreeField().values()) {
			if (eachField.isSystemField()) {
				eachField.forDisplay(false);
			} else if (eachField.isFlatten()) {
				eachField.forDisplay(false);
			} else {
				eachField.forDisplay(aSet);
			}
		}
	}

	public List<Field> getFieldListByDisplayPosition() throws Exception {
		List<Field> result = new CopyOnWriteArrayList<>(this.getTreeField().values());
		Collections.sort(result, new Comparator<Field>() {
			@Override
			public int compare(Field o1, Field o2) {
				return new Integer(o1.displayPosition()).compareTo(o2.displayPosition());
			}
    });
		return(result);
	}

	private static String getFqFieldName(String aNowStr, String aNewStr) {
		if (aNowStr.isEmpty() == false) {
			aNowStr += "$";
		}
		return(aNowStr + aNewStr);
	}

	public List<Field> getAllIndexKey() throws Exception {
		List<Field> result = new CopyOnWriteArrayList<>();
		getAllIndexKey(result, this, "");
		return(result);
	}

	private static void getAllIndexKey(List<Field> result, Clasz aClasz, String aFqFieldName) throws Exception {
		for(Field eachField : aClasz.getTreeField().values()) { // no objectid field
			String fqName = getFqFieldName(aFqFieldName, eachField.getFieldName());
			if (eachField.isAtomic()) {
				if (eachField.isObjectKey()) {
					eachField.setFqName(fqName);
					result.add(eachField);
					//aFqFieldName = ""; // start all over again
				}
			} else {
				if (eachField.getFieldType() == FieldType.OBJECT) {
					Clasz clasz = ((FieldObject) eachField).getObj();
					if (clasz != null) {
						getAllIndexKey(result, clasz, fqName); // recursive call to clear the member clszObject index keys
					}
				} else if (eachField.getFieldType() == FieldType.OBJECTBOX) {
					Clasz clasz = ((FieldObjectBox) eachField).getMetaObj();
					getAllIndexKey(result, clasz, fqName); // recursive call to clear the clszObject index keys
				} else {
					throw new Hinderance("Invalid field type in object while getting all its index key name: '" + aClasz.getClaszName() + "'");
				}
			}
		}
	}

	public void clearAllIndexKey() throws Exception {
		clearAllIndexKey(this);
	}

	private static void clearAllIndexKey(Clasz aClasz) throws Exception {
		for(Field eachField : aClasz.getTreeField().values()) {
			if (eachField.isAtomic()) {
				if (eachField.isObjectKey()) {
					eachField.setObjectKey(false);
				}
			} else {
				if (eachField.getFieldType() == FieldType.OBJECT) {
					Clasz clasz = ((FieldObject) eachField).getObj();
					if (clasz != null) {
						clearAllIndexKey(clasz); 
					}
				} else if (eachField.getFieldType() == FieldType.OBJECTBOX) {
					Clasz clasz = ((FieldObjectBox) eachField).getMetaObj();
					clearAllIndexKey(clasz);
				} else {
					throw new Hinderance("Invalid field type in object while getting all its index key name: '" + aClasz.getClaszName() + "'");
				}
			}
		}
	}

	public List<String> getErrorField() {
		return errorField;
	}

	public void setErrorField(CopyOnWriteArrayList<String> errorField) {
		this.errorField = errorField;
	}

	public void clearErrorField() {
		this.getErrorField().clear();
	}

	public void handleError(Exception ex) throws Exception {
		// customize error handling in each class by examaining the exception
	}

	public List<String> getAllErrorField() throws Hinderance {
		CopyOnWriteArrayList<String> result = new CopyOnWriteArrayList<>();
		this.getAllErrorField(result);
		return(result);
	}

	private List<String> getAllErrorField(CopyOnWriteArrayList<String> aAccumField) throws Hinderance {
		if (this.getClaszName().equals("Clasz")) {
			return(aAccumField);
		}

		if (this.getErrorField().size() != 0) {
			aAccumField.addAll(this.getErrorField());
		}

		for(Field eachField : this.getRecord().getFieldBox().values()) {
			try {
				if (eachField.getFieldType() == FieldType.OBJECT) {
					Clasz obj = ((FieldObject) eachField).getObj();
					if (obj != null) {
						if (obj.getErrorField().size() != 0) {
							aAccumField.addAll(obj.getErrorField());
						}
						obj.getAllErrorField(aAccumField);
					}
				} else if (eachField.getFieldType() == FieldType.OBJECTBOX) {
					for(Clasz memberObject : ((FieldObjectBox) eachField).getObjectMap().values()) {
						memberObject.getAllErrorField(aAccumField);
					}
				} else {
					// atomic fields, do nothing
				}

				if (ObjectBase.ParentIsNotAtClaszYet(this)) {
					Clasz parentObj = this.getParentObjectByContext();
					if (parentObj != null) {
						parentObj.getAllErrorField(aAccumField);
					}
				}
			} catch (Exception ex) {
				throw new Hinderance(ex, "Error at: '" + this.getClass().getSimpleName() + "', when obtaining error field from class: '" + eachField.getFieldName() + ";");
			}
		}
		return(aAccumField);
	}

	public interface GetFetchChildSql<T> {
		Object execute(T aParam) throws Exception;
	}

	public void fetchByFilter(Connection aConn, String aChildFieldName, List<String> arrayParam, Clasz.GetFetchChildSql<Object> aChildSql) throws Exception {
		String childSqlStr = (String) aChildSql.execute(arrayParam);
		if (!childSqlStr.isEmpty()) {
			this.getFieldObjectBox(aChildFieldName).fetchByCustomSql(aConn, childSqlStr);
		}
	}

	public Boolean getForDelete() {
		return forDelete;
	}

	public void setForDelete(Boolean forDelete) {
		this.forDelete = forDelete;
	}

	public static void ForEachClasz(Connection aConn, Class aClass, String aSqlToGetObjId, Callback2ProcessClasz aCallback) throws Exception {
		List<Field> noSqlParam = new CopyOnWriteArrayList<>();
		ForEachClasz(aConn, aClass, aSqlToGetObjId, noSqlParam, aCallback);
	}

	public static void ForEachClasz(Connection aConn, Class aClass, String aSqlToGetObjId, List<Field> aPositionalParamValue, Callback2ProcessClasz aCallback) throws Exception {
		ResultSetFetch resultSetFetch = new ResultSetFetch();
		resultSetFetch.forEachFetch(aConn, aClass, aSqlToGetObjId, aPositionalParamValue, aCallback);
	}

	/*
	@Deprecated
	public void fetchLatest(Connection aConn, String aBoxName, String aFieldLatest) throws Exception {
		this.getFieldObjectBox(aBoxName).fetchAll(aConn);
		this.getFieldObjectBox(aBoxName).resetIterator();
		Clasz latestClasz = null;
		while (this.getFieldObjectBox(aBoxName).hasNext(aConn)) {
			Clasz currentClasz = this.getFieldObjectBox(aBoxName).getNext();
			if (latestClasz == null) {
				latestClasz = currentClasz;
			} else {
				if (latestClasz.getField(aFieldLatest) != null) {
					DateTime latestDate;
					DateTime currentDate;
					if (latestClasz.getField(aFieldLatest) instanceof FieldDateTime) {
						latestDate = latestClasz.getValueDateTime(aFieldLatest);	
						currentDate = currentClasz.getValueDateTime(aFieldLatest);
					} else {
						latestDate = latestClasz.getValueDate(aFieldLatest);	
						currentDate = currentClasz.getValueDate(aFieldLatest);
					}
					if (latestDate.isBefore(currentDate)) {
						latestClasz = currentClasz;
					}
				}
			}

		}
		if (latestClasz != null) {
			this.getFieldObjectBox(aBoxName).removeAll();
			this.getFieldObjectBox(aBoxName).addValueObject(latestClasz);
		}
	}
	*/

	public void fetchLatestMemberFromFob(Connection aConn, String aBoxName, String aLatestField) throws Exception {
		LambdaObject lambdaObject = new LambdaObject();
		final String fieldLatest = aLatestField;
		this.getFieldObjectBox(aBoxName).forEachMember(aConn, (Connection bConn, Clasz aClasz) -> {
			Clasz currentClasz = aClasz;
			Clasz latestClasz = (Clasz) lambdaObject.getTheObject();
			if (latestClasz == null) {
				lambdaObject.setTheObject(currentClasz);
			} else {
				try {
					if (latestClasz.getField(fieldLatest) != null) {
						DateTime latestDate;
						DateTime currentDate;
						if (latestClasz.getField(fieldLatest) instanceof FieldDateTime) {
							latestDate = latestClasz.getValueDateTime(fieldLatest);	
							currentDate = currentClasz.getValueDateTime(fieldLatest);
						} else {
							latestDate = latestClasz.getValueDate(fieldLatest);	
							currentDate = currentClasz.getValueDate(fieldLatest);
						}
						if (latestDate.isBefore(currentDate)) {
							lambdaObject.setTheObject(currentClasz);
						}
					}
				} catch(Exception ex) {
					App.logEror(ex, "Fail, exception thrown in Clasz.fetchLatest method.");
					return(false); // discontinue and get out
				}
			}
			return(true);
		});

		Clasz latestClasz = (Clasz) lambdaObject.getTheObject();
		if (latestClasz != null) {
			this.getFieldObjectBox(aBoxName).removeAll();
			this.getFieldObjectBox(aBoxName).addValueObject(latestClasz);
		}
	}

	public void copyValue(Clasz aSource) throws Exception {
		for(Field eachField : aSource.getRecord().getFieldBox().values()) {
			try {
				if (eachField.getFieldType() == FieldType.OBJECT) {
					Clasz memberObj = (Clasz) ((FieldObject) eachField).getObj();
					if (memberObj != null && memberObj.getClaszName().equals("Clasz")) {
						continue;
					}
				}
				if (eachField.getFieldType() == FieldType.OBJECTBOX) {
					Clasz memberObj = (Clasz) ((FieldObjectBox) eachField).getMetaObj();
					if (memberObj.getClaszName().equals("Clasz")) {
						continue;
					}
				}

				if (eachField.isPrimaryKey() == false) {
					Field destField = this.getRecord().getField(eachField.getFieldName());
					if (eachField.getValueStr().equals(destField.getValueStr()) == false) {
						destField.cloneField(eachField);
						destField.setModified(true);
					}
				}

			} catch (Exception ex) {
				throw new Hinderance(ex, "Error when copying object : '" + this.getClass().getSimpleName() + "', field: '" + eachField.getFieldName() + "'");
			}
		}

		if (ObjectBase.ParentIsNotAtClaszYet(this)) {
			Clasz parntObject = this.getParentObjectByContext();
			Clasz parentSource = aSource.getParentObjectByContext();
			if (parntObject != null) {// its possible the parent class for this clszObject is abstract and still not at Clasz
				parntObject.copyValue(parentSource);
			}
		}
	}

	public static FieldType GetFieldType(Connection aConn, Class aClass, String aFieldName) throws Exception {
		FieldType fieldType = null;
		Clasz aClasz = ObjectBase.CreateObject(aConn, aClass);
		ConcurrentHashMap<String, Field> claszFields = aClasz.getTreeField();
		for(Field eachField : claszFields.values()) {
			if (eachField.getFieldName().equals(aFieldName)) {
				fieldType = eachField.getFieldType();
				break;
			}
		}

		if (fieldType == null) {
			throw new Hinderance("GetFieldType, no field: " + aFieldName + ", in class: " + aClass.getSimpleName());
		}

		return(fieldType);
	}

	public CopyOnWriteArrayList<Clasz> getParentObjectByDb(Connection aConn, Class aParent, String aFieldName) throws Exception {
		CopyOnWriteArrayList<Clasz> result = new CopyOnWriteArrayList<>();
		FieldType fieldType = GetFieldType(aConn, aParent, aFieldName);
		if (fieldType == FieldType.OBJECTBOX) { // check if this field at aParent is instant variable clasz field or fieldbox
			result = getAllParentOfMemberFieldBox(aConn, aParent, aFieldName);
		} else if (fieldType == FieldType.OBJECT) {
			result = getParentObjectByDbOfField(aConn, aParent, aFieldName);
		} else {
			throw new Hinderance("getParentObjectByDb, field: " + aParent.getSimpleName() + "." + aFieldName + "is not of OBJECT or OBJECTBOX type!");
		}
		return(result);
	}

	public CopyOnWriteArrayList<Clasz> getParentObjectByDbOfField(Connection aConn, Class aParent, String aFieldName) throws Exception {
		CannotBeInline(aParent, aFieldName);
		CopyOnWriteArrayList<Clasz> result = new CopyOnWriteArrayList<>();
		String memberTableName = Clasz.GetIvTableName(aParent); // get the iv_ table name
		Table memberTable = new Table(memberTableName); // create the iv_ table
		String parentPkName = CreatePkColName(aParent);
		memberTable.getMetaRec().createField(parentPkName, FieldType.LONG);
		Record whereRecord = new Record();
		whereRecord.createField(aFieldName, this.getObjectId());
		memberTable.fetch(aConn, whereRecord); 
		if (memberTable.totalRecord() == 1) { // can only have one or zero instant variable 
			Long parentOid = memberTable.getRecord(0).getFieldLong(parentPkName).getValueLong();
			Clasz parentClasz = ObjectBase.CreateObject(aConn, aParent);
			parentClasz.setObjectId(parentOid);
			result.add(parentClasz);
		}
		return(result);
	}

	public CopyOnWriteArrayList<Clasz> getAllParentOfMemberFieldBox(Connection aConn, Class aParent, String aFieldName) throws Exception {
		CannotBeInline(aParent, aFieldName);
		CopyOnWriteArrayList<Clasz> result = new CopyOnWriteArrayList<>();
		String boxMemberTableName = Clasz.GetIwTableName(aParent, aFieldName); // get the iv_ table name
		Table boxMemberTable = new Table(boxMemberTableName); // create the iv_ table
		String parentPkName = CreatePkColName(aParent);
		boxMemberTable.getMetaRec().createField(parentPkName, FieldType.LONG);
		boxMemberTable.getMetaRec().createField(ObjectBase.LEAF_CLASS, FieldType.STRING, ObjectBase.CLASS_NAME_LEN);
		Record whereRecord = new Record();
		whereRecord.createField(aFieldName, this.getObjectId());
		boxMemberTable.fetch(aConn, whereRecord); // fetches ALL parent entries containing this child clasz
		for(Record eachRec : boxMemberTable.getRecordBox().values()) {
			Long parentOid = eachRec.getFieldLong(parentPkName).getValueLong();
			Clasz eachClasz = ObjectBase.CreateObject(aConn, Class.forName(aParent.getName()));
			eachClasz.setObjectId(parentOid);
			result.add(eachClasz);
		}
		return(result);
	}

	public static boolean IsInlineMember(Class aParent, String aMemberName) throws Exception {
		boolean isInline = false;
		java.lang.reflect.Field reflectField = aParent.getField(aMemberName);
		ReflectField eachAnnotation = (ReflectField) reflectField.getAnnotation(ReflectField.class);
		if (eachAnnotation != null) {
			isInline = eachAnnotation.inline();
		} else {
			throw new Hinderance("Error at class: '" + aParent.getSimpleName() + "', fail to create field: '" + reflectField.getName() + "'");
		}
		return(isInline);
	}

	public static void CannotBeInline(Class aParent, String aMemberName) throws Exception {
		if (IsInlineMember(aParent, aMemberName)) {
			throw new Hinderance("The process is not applicable for inline type at, class: " + aParent.getSimpleName() + ", field: " + aMemberName);
		}
	}

	public static Clasz GetInheritanceObject(Clasz aLeafObject, Class aWantedClass) throws Exception {
		Clasz result = null;
		if (ObjectBase.ParentIsNotAtClaszYet(aLeafObject)) {
			Clasz parentObject = aLeafObject.getParentObjectByContext();
			if (parentObject != null ) { 
				if (parentObject.getClass() == aWantedClass) {
					result = parentObject;
				} else {
					result = GetInheritanceObject(parentObject, aWantedClass);
				}
			} else {
				throw new Hinderance("The object: " + aLeafObject.getClass().getName() + " do not inherit class: " + aWantedClass.getName());
			}
		}
		return(result);
	}

	public void removeFieldObjectBoxMember(Connection aConn, String aMemberName, Long aOid, String aClasz) throws Exception {
		boolean removeDone = false;
		this.getFieldObjectBox(aMemberName).fetchByObjectId(aConn, aOid, aClasz);
		while (this.getFieldObjectBox(aMemberName).hasNext(aConn)) {
			Clasz clasz = this.getFieldObjectBox(aMemberName).getNext();
			if (clasz.getObjectId().equals(aOid)) {
				clasz.setForDelete(true);
				removeDone = true;
				break;
			}
		}
		if (removeDone == false) {
			App.logWarn(this, "Attempting to remove member object that do not exist: " + this.getClass().getSimpleName() 
			+ "." + aMemberName + ", oid: " + aOid);
		}
	}

	public void removeFieldObjectBoxMember(Connection aConn, String aMemberName) throws Exception {
		this.getFieldObjectBox(aMemberName).resetIterator(); // always do a reset before starting to loop for the objects
		while (this.getFieldObjectBox(aMemberName).hasNext(aConn)) {
			Clasz clasz = this.getFieldObjectBox(aMemberName).getNext();
			clasz.setForDelete(true);
		}
	}

	public boolean isSame(Clasz aClasz) throws Exception {
		boolean  result = false;
		if (this.getClass().equals(aClasz.getClass())) {
			if (this.getObjectId().equals(aClasz.getObjectId())) {
				result = true;
			}
		}
		return(result);
	}

	// populate this clasz if it has aWantedMember
	// aWantedMember must be a FieldObject and properly populated with oid
	// if aWantedMember is polymorphic, then its polymorphic/leaf clasz must be use
	public boolean populateByMember(Connection aConn, FieldObject aWantedMember) throws Exception {
		boolean result = ObjectBase.PopulateIfGotMember(aConn, this, aWantedMember);
		return(result);
	}

	public boolean equalsCriteria(Connection aConn, Clasz aRealClasz) throws Exception {
		boolean result = false;
		if (this.compareForCriteria(aConn, aRealClasz) == 0) {
			result = true;
		}
		return(result);
	}

	public int compareForCriteria(Connection aConn, Clasz aRealClasz) throws Exception {
		int compareInt = -1;
		for(Field eachCriteria : this.getTreeField().values()) {
			if (eachCriteria.isModified()) {
				Field realField = aRealClasz.getField(eachCriteria.getFieldName());
				compareInt = eachCriteria.compareForCriteria(aConn, realField);
			}
		}
		return(compareInt);
	}

	public static void SetWhereField(Record recWhere, Field fieldName) throws Exception {
		recWhere.createField(fieldName.getFieldName(), fieldName.getFieldType(), fieldName.getFieldSize());
		recWhere.getField(fieldName.getFieldName()).setValueStr(fieldName.getValueStr());
	}

	private static String GetWherePermutation(List<String> aEachCondition, int aPosition2UniqueIt, List<Field> aBindList, List<Field> aPermutedBindList) throws Exception {
		String uniqueableExpression = "";
		for (int cntr = 0; cntr < aEachCondition.size(); cntr++) {
			String newWhereSection = aEachCondition.get(cntr);
			if (newWhereSection != null && newWhereSection.isEmpty() == false) {
				if (uniqueableExpression.isEmpty() == false) uniqueableExpression += " and ";
				if (cntr == aPosition2UniqueIt) {
					newWhereSection = newWhereSection.replaceAll(">=", ">");
					newWhereSection = newWhereSection.replaceAll("<=", "<");
				}
				uniqueableExpression += newWhereSection;
				aPermutedBindList.add(aBindList.get(cntr));
			}
		}
		return(uniqueableExpression);
	}

	public static FetchStatus FetchBySection(Connection aConn, Class aParentClass, List<String> aKeyField, List<String> aKeyValue, SortOrder aSortOrder, Record aWhere, FieldObjectBox aBox, int aPageSize) throws Exception {
		List<SortOrder> orderList = new CopyOnWriteArrayList<>();
		for(int cntr = 0; cntr < aKeyField.size(); cntr++) {
			orderList.add(aSortOrder);
		}

		return (Clasz.FetchByPageFromTable(aConn, aParentClass, aKeyField, aKeyValue, orderList, aWhere, aBox, "next", aPageSize));
	}

	// replace the name to FetchBySection after removing the deprecated one
	public static FetchStatus FetchByPageFromTable(Connection aConn, Class aParentClass, List<String> aKeyField, List<String> aKeyValue, List<SortOrder> aKeyOrder, Record aWhere, FieldObjectBox aBox, String aPageDirection, int aPageSize) throws Exception {
		int result = 0;
		FetchStatus fetchStatus;

		// get sql objects for full sql select statement
		Clasz typeClasz = aBox.getMetaObj();
		List<Object> fetchObject = Table.GetFetchObject(typeClasz.getTableName(), null, aWhere, null);
		String fullSqlStr = (String) fetchObject.get(0);
		List<Field> whereBindList = (List<Field>) fetchObject.get(1);

		// get sql objects for pagination clause
		List<String> fieldExpressionList = new CopyOnWriteArrayList<>();
		List<String> sortFieldList = new CopyOnWriteArrayList<>();
		List<Field> fieldBindList = new CopyOnWriteArrayList<>();
		//String tableName = aClasz.getTableName();
		String tableName = Clasz.CreateTableName(aParentClass);
		Table claszTable = new Table(tableName);
		claszTable.initMeta(aConn);
		for(int cntrField = 0; cntrField < aKeyField.size(); cntrField++) {
			String keyName = aKeyField.get(cntrField);
			String keyValue = aKeyValue.get(cntrField);
			SortOrder keyOrder = aKeyOrder.get(cntrField);
			List<Object> sqlPageObject = GetEachFieldExpression(claszTable, keyName, keyValue, keyOrder, aPageDirection); // get the where clause that restrict the search to a section

			String strWhereExpression = ((String) sqlPageObject.get(0));
			String strSortOrder = ((String) sqlPageObject.get(1));
			Field bindField = (Field) sqlPageObject.get(2);

			fieldExpressionList.add(strWhereExpression);
			sortFieldList.add(keyName + " " + strSortOrder);
			fieldBindList.add(bindField);
		}

		List<Field> fieldBindPermuted = new CopyOnWriteArrayList<>();
		List<String> wherePermutationList = new CopyOnWriteArrayList<>();
		for (int cntr = fieldExpressionList.size() - 1; cntr >= 0; cntr--) {
			String strExpression = fieldExpressionList.get(cntr);
			if (strExpression != null && strExpression.isEmpty()== false) {
				String eachExpression = GetWherePermutation(fieldExpressionList, cntr, fieldBindList, fieldBindPermuted);
				wherePermutationList.add(eachExpression);
			}
		}

		String whereClause = "";
		for(int cntr = 0; cntr < wherePermutationList.size(); cntr++) {
			String newWhereSection = wherePermutationList.get(cntr);
			if (whereClause.isEmpty() == false) whereClause += " or ";
			if (newWhereSection.isEmpty() == false) whereClause += "(" + newWhereSection + ")";
		}

		if (whereClause.isEmpty() == false) {
			whereClause = "(" + whereClause + ")";
		}

		String sortClause = "";
		for (int cntr = 0; cntr < fieldExpressionList.size(); cntr++) {
			String newSortSection = sortFieldList.get(cntr);
			if (sortClause.isEmpty() == false) sortClause += ",";
			sortClause += " " + newSortSection;
		}

			
		if (whereClause.isEmpty() == false) {
			if (fullSqlStr.indexOf("where") < 0) {
				fullSqlStr += " where";
			} else {
				fullSqlStr += " and";
			}
			fullSqlStr += " " + whereClause;
		}

		if (sortClause.isEmpty() == false) {
			fullSqlStr += " order by " + sortClause;
		}

		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = aConn.prepareStatement(fullSqlStr);
			Database.SetStmtValue(stmt, whereBindList);
			Database.SetStmtValue(stmt, fieldBindPermuted, whereBindList.size());
App.logDebg(Clasz.class, "Clasz.FetchPageFromTable, where bind count: " + whereBindList.size() + ", permuted bind count: " + fieldBindPermuted.size());
App.logDebg(Clasz.class, "Clasz.FetchPageFromTable: " + stmt);
			rs = stmt.executeQuery();
			while (rs.next()) {
				Clasz clasz = ObjectBase.CreateObject(aConn, typeClasz.getClass());
				clasz.populateObject(aConn, rs, false);
				aBox.addValueObject(clasz);
				result++;
				if (result >= aPageSize) break;
			}
		} finally {
			fetchStatus = FetchStatus.EOF;
			if (rs != null) {
				rs.close();
			}
			if (stmt != null) {
				stmt.close();
			}
		}

		aBox.clearAllSortKey();
		for(int cntrField = 0; cntrField < aKeyField.size(); cntrField++) {
			String sortField = aKeyField.get(cntrField);
			aBox.getMetaObj().getField(sortField).setSortKey(true);
			aBox.getMetaObj().getField(sortField).setSortOrder(aKeyOrder.get(cntrField));
		}
		if (aKeyField.size() > 0) aBox.sort();

		aBox.setFetchStatus(fetchStatus);
		return(fetchStatus);
	}

	/*
	 * This differs from the one in FieldObjectBox.GetEachFieldExpression, 
	 * as this is fetching directly from the table without objectindex.
	*/
	private static List<Object> GetEachFieldExpression(Table aClaszTable, String aFieldName, String aFieldValue, SortOrder aDisplayOrder, String aPageDirection) throws Exception {
		if (aDisplayOrder == SortOrder.DSC) {
			if (aPageDirection.equals("next")) { // reverse the direction if display by descending order
				aPageDirection = "prev";
			} else {
				aPageDirection = "next";
			}
		}

		String whereClause = "";
		String sortOrder = "asc";
		Field keyField = aClaszTable.getField(aFieldName);
		if (aFieldValue == null || aFieldValue.trim().isEmpty()) aFieldValue = null;  // for empty or null value, there'll be no where range clause
		if (keyField.getFieldType() == FieldType.STRING || keyField.getFieldType() == FieldType.INTEGER) {
			if (aPageDirection.equals("next")) {
				if (aFieldValue != null) {
					if (keyField.getFieldType() == FieldType.STRING) 
						whereClause += "lower(" + aFieldName + ") >= lower(?)";
					else
						whereClause += aFieldName + " >= ?";
					keyField.setValueStr(aFieldValue);
				}
				sortOrder = "asc";
			} else if (aPageDirection.equals("prev")) {
				if (aFieldValue != null) {
					if (keyField.getFieldType() == FieldType.STRING) 
						whereClause += "lower(" + aFieldName + ") <= lower(?)";
					else
						whereClause += aFieldName + " <= ?";
					keyField.setValueStr(aFieldValue);
				}
				sortOrder = "desc";
			} else if (aPageDirection.equals("first")) {
				sortOrder = "asc";
			} else if (aPageDirection.equals("last")) {
				sortOrder = "desc";
			}
		} else if (keyField.getFieldType() == FieldType.DATETIME || keyField.getFieldType() == FieldType.DATE) {
			if (aPageDirection.equals("next")) {
				if (aFieldValue != null) {
					whereClause += aFieldName + " >= ?";
					keyField.setValueStr(aFieldValue);
				}
				sortOrder = "asc";
			} else if (aPageDirection.equals("prev")) {
				if (aFieldValue != null) {
					whereClause += aFieldName + " <= ?";
					keyField.setValueStr(aFieldValue);
				}
				sortOrder = "desc";
			} else if (aPageDirection.equals("first")) {
				sortOrder = "asc";
			} else if (aPageDirection.equals("last")) {
				sortOrder = "desc";
			}
		} else {
			throw new Hinderance("The field type in table: " + aClaszTable + ", type: " + keyField.getFieldType() + ", is not supported!");
		}

		List<Object> result = new CopyOnWriteArrayList<>();
		result.add(whereClause.trim());
		result.add(sortOrder);
		if (aFieldValue == null) {
			result.add(null); // only got sort clause but no conditional clause
		} else {
			result.add(keyField); // only got sort clause but no conditional clause
		}
		return(result);
	}

	public static void FetchAll(Connection aConn, Class aClass, Callback2ProcessClasz aCallback) throws Exception {
		if (Clasz.class.isAssignableFrom(aClass) == false) {
			throw new Hinderance("Cannot perform FetchAll for non Clasz class from class: " + aClass.getSimpleName());
		}

		String pkColName = Clasz.CreatePkColName(aClass);
		String strSql = "select " + pkColName + " from " + Clasz.CreateTableName(aClass);
		PreparedStatement stmt = null;
		ResultSet rset = null;
		try {
			stmt = aConn.prepareStatement(strSql);
			rset = stmt.executeQuery();
			while (rset.next()) {
				long pk = rset.getLong(1);
				Clasz fetchMember = Clasz.Fetch(aConn, aClass, pk);
				if (aCallback != null) {
					if (aCallback.processClasz(aConn, fetchMember) == false) {
						break;
					}
				}
			}
		} catch (Exception ex) {
			if (stmt == null) {
				throw new Hinderance(ex, "FetchAll - Fail to populate object for type: " + aClass.getSimpleName());
			} else {
				throw new Hinderance(ex, "FetchAll - Fail to populate object: " + stmt.toString());
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

	/*
	public void fetchParentByMember(Connection aConn, String aMemberName, Clasz aMemberObject, ResultSetFetchIntf aCallback) throws Exception {
		if (this.getField(aMemberName) instanceof FieldObject) {
			FetchParentByMember(aConn, this.getClass(), aMemberName, aMemberObject, aCallback);
		} else if (this.getField(aMemberName) instanceof FieldObjectBox) {
			FetchParentByBoxMember(aConn, this.getClass(), aMemberName, aMemberObject, aCallback);
		} else {
			throw new Hinderance("Fail to fetchParentByMember, invalid member: " + this.getClass().getSimpleName() + "." + aMemberName + ", type: " + aMemberObject.getClass().getSimpleName());
		}
	}
	*/

	public static void FetchParentByBoxMember(Connection aConn, Class aClass, String aMemberName, Clasz aMemberObject, Callback2ProcessClasz aCallback) throws Exception {
		if (Clasz.class.isAssignableFrom(aClass) == false) {
			throw new Hinderance("Cannot perform FetchParentByMember for non Clasz class from class: " + aClass.getSimpleName());
		}

		if (aMemberObject.isPopulated() == false) {
			throw new Hinderance("Cannot do FetchParentByMember from non populated member: " + aMemberObject.getClass().getSimpleName());
		}

		String parentPkName = Clasz.CreatePkColName(aClass);
		String memberTableName = GetIwTableName(aClass, aMemberName);
		String memberFieldName = CreateDbFieldName(aMemberName);

		String sqlMember = "select " + parentPkName + " from " + memberTableName + " where " + memberFieldName + " = " + aMemberObject.getObjectId();
		sqlMember += " and leaf_class" + " = '" + aMemberObject.getClass().getName() + "'";

		PreparedStatement stmt = null;
		ResultSet rset = null;
		try {
			stmt = aConn.prepareStatement(sqlMember);
			rset = stmt.executeQuery();
			while (rset.next()) {
				long pk = rset.getLong(1);
				Clasz fetchMember = Clasz.Fetch(aConn, aClass, pk);
				if (aCallback != null) {
					if (aCallback.processClasz(aConn, fetchMember) == false) {
						break;
					}
				}
			}
		} catch (Exception ex) {
			if (stmt == null) {
				throw new Hinderance(ex, "FetchParentByMember - Fail to populate object for type: " + aClass.getSimpleName());
			} else {
				throw new Hinderance(ex, "FetchParentByMember - Fail to populate object: " + stmt.toString());
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

	public static void FetchParentByMember(Connection aConn, Class aParentClass, String aMemberName, Clasz aMemberObject, Callback2ProcessClasz aCallback) throws Exception {
		if (Clasz.class.isAssignableFrom(aParentClass) == false) {
			throw new Hinderance("Cannot perform FetchParentByMember for non Clasz class from class: " + aParentClass.getSimpleName());
		}

		if (aMemberObject.isPopulated() == false) {
			throw new Hinderance("Cannot do FetchParentByMember from non populated member: " + aMemberObject.getClass().getSimpleName());
		}

		String parentPkName = Clasz.CreatePkColName(aParentClass);
		String memberTableName = GetIvTableName(aParentClass);
		String memberFieldName = CreateDbFieldName(aMemberName);
		String memberFieldLeaf = CreateLeafClassColName(memberFieldName);

		Table memberTable = new Table(memberTableName);
		memberTable.initMeta(aConn);
		
		String sqlMember = "select " + parentPkName + " from " + memberTableName + " where " + memberFieldName + " = " + aMemberObject.getObjectId();
		if (memberTable.fieldExist(memberFieldLeaf)) {
			sqlMember += " and " + memberFieldLeaf + " = '" + aMemberObject.getClass().getName() + "'";
		}

		PreparedStatement stmt = null;
		ResultSet rset = null;
		try {
			stmt = aConn.prepareStatement(sqlMember);
			rset = stmt.executeQuery();
			while (rset.next()) {
				long pk = rset.getLong(1);
				Clasz fetchParent = Clasz.Fetch(aConn, aParentClass, pk);
				if (aCallback != null) {
					if (aCallback.processClasz(aConn, fetchParent) == false) {
						break;
					}
				}
			}
		} catch (Exception ex) {
			if (stmt == null) {
				throw new Hinderance(ex, "FetchParentByMember - Fail to populate object for type: " + aParentClass.getSimpleName());
			} else {
				throw new Hinderance(ex, "FetchParentByMember - Fail to populate object: " + stmt.toString());
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

	public Float getValueFloat(String aFieldName) throws Exception {
		Float result;
		if (this.fieldExist(aFieldName)) {
			result = this.getRecord().getValueFloat(aFieldName);
		} else {
			this.validateField(aFieldName);
			result = (this.getParentObjectByContext().getValueFloat(aFieldName));
		}
		return(result);
	}

	public void setValueFloat(String aFieldName, Float aFieldValue) throws Exception {
		if (this.fieldExist(aFieldName)) {
			this.getRecord().setValueFloat(aFieldName, aFieldValue);
		} else {
			this.validateField(aFieldName);
			this.getParentObjectByContext().setValueFloat(aFieldName, aFieldValue);
		}
	}

	//
	// renaming underlying dabase object due to renaming of Clasz name
	//
	public static String CreateTableName(String aClassSimpleName) {
		String result = Clasz.GetTableNamePrefix() + Database.Java2DbTableName(aClassSimpleName);
		return(result);
	}

	public static String GetIvTableName(String aChildSimpleName) {
		String ivName = Clasz.GetIvPrefix() + Database.Java2DbTableName(aChildSimpleName); // create the parent iv_ table
		return(ivName);
	}

	public static String CreateSequenceTableName(String aSimpleName) {
		String result = Clasz.GetSequenceNamePrefix() + Database.Java2DbTableName(aSimpleName);
		return(result);
	}

	public static String CreatePkColName(String aClassSimpleName) throws Exception {
		String result = CreateTableName(aClassSimpleName) + "_pk";
		return(result);
	}

	public static String CreateChildCountColName(String aClassSimpleName) throws Exception {
		String result = CreateTableName(aClassSimpleName) + Table.POSTFIX_FIELD_CHILD_COUNT;
		return(result);
	}

	public static String GetIwTableName(String aParent, String aFieldName) {
		String iwName = Clasz.GetIwPrefix() + Database.Java2DbTableName(aParent) + "_" + Database.Java2DbTableName(aFieldName); // create the parent iv_ table
		return(iwName);
	}

	// not use, use the string version below instead
	public static void RenameClaszDbObject(Connection aConn, Class aOldClass, String aNewClassSimpleName) throws Exception {
		String classFqn = aOldClass.getName();
		String packageName = classFqn.substring(0, classFqn.lastIndexOf("."));
		String oldClassSimpleName = classFqn.substring(classFqn.lastIndexOf(".") + 1);
		RenameClaszDbObject(aConn, packageName, oldClassSimpleName, aNewClassSimpleName);
	}

	public static void RenameClaszDbObject(Connection aConn, String aPackageName, String aOldClassSimpleName, String aNewClassSimpleName) throws Exception {
		String oldClassFqn = aPackageName + "." + aOldClassSimpleName;

		// ------------------------------------------------------------------------------------------
		// rename cz_
		// ------------------------------------------------------------------------------------------

		// create both new and old table name
		String tableNameOld = CreateTableName(aOldClassSimpleName);
		String tableNameNew = CreateTableName(aNewClassSimpleName);

		// rename cc, pk column in main table
		String pkColNameOld = CreatePkColName(aOldClassSimpleName);
		String pkColNameNew = CreatePkColName(aNewClassSimpleName);
		String ccColNameOld = CreateChildCountColName(aOldClassSimpleName);
		String ccColNameNew = CreateChildCountColName(aNewClassSimpleName);
		Table oldTable = new Table(tableNameOld);

		// rename main table name to the new name
		try {
			oldTable.renameTable(aConn, tableNameNew);
			App.logInfo("Completed renaming table of: " + aOldClassSimpleName);
		} catch (Exception ex) {
			App.logEror(ex, "Error when renaming table of: " + aOldClassSimpleName);
		}

		// rename new main table pk constraint/index name 
		Table newTable = new Table(tableNameNew);
		try {
			newTable.renamePrimaryKey(aConn, tableNameOld);
			App.logInfo("Completed renaming pk constraint of: " + aOldClassSimpleName);
		} catch (Exception ex) {
			App.logEror(ex, "Error when renaming pk constraint of: " + aOldClassSimpleName);
		}

		try {
			newTable.renameField(aConn, pkColNameOld, pkColNameNew);
			App.logInfo("Completed renaming pk column of: " + aOldClassSimpleName);
		} catch(Exception ex) {
			App.logEror(ex, "Error when renaming pk column of: " + aOldClassSimpleName);
		}

		try {
			newTable.renameField(aConn, ccColNameOld, ccColNameNew);
			App.logInfo("Completed renaming cc column of: " + aOldClassSimpleName);
		} catch(Exception ex) {
			App.logEror(ex, "Error when renaming cc column of: " + aOldClassSimpleName);
		}


		boolean gotFieldObject = false;
		try {
			// for each fieldobjectbox, rename it's table name and pk col name
			boolean gotError = false;
			Class oldClass = Class.forName(oldClassFqn);
			Clasz claszOld = ObjectBase.CreateObjectTransient(aConn, oldClass); // use transient to avoid re-creating the table when run multiple time
			List<Field> leafFieldList = claszOld.getLeafField();
			for(Field eachField : leafFieldList) {
				if (eachField instanceof FieldObjectBox) {
					App.logDebg("Processing fob field name: " + eachField.getFieldName());
					String iwBoxNameOld = GetIwTableName(oldClass, eachField.getFieldName()); // get the iv_ table name
					String iwBoxNameNew = GetIwTableName(aNewClassSimpleName, eachField.getFieldName());
					Table oldIvBoxTable = new Table(iwBoxNameOld);

					try { ((FieldObjectBox) eachField).renameBoxMemberUniqueIndex(aConn, oldClass, aNewClassSimpleName); } catch(Exception ex) {
						gotError = true;
						App.logEror(ex, "Error when renaming fob unique index");
					}
					try { oldIvBoxTable.renameTable(aConn, iwBoxNameNew); } catch(Exception ex) {
						gotError = true;
						App.logEror(ex, "Error when renaming fob table to its new name: " + iwBoxNameNew);
					}
					Table newIvBoxTable = new Table(iwBoxNameNew);
					try { newIvBoxTable.renameField(aConn, pkColNameOld, pkColNameNew); } catch(Exception ex) {
						gotError = true;
						App.logEror(ex, "Error when renaming fob pk field name: " + pkColNameOld + ", " + pkColNameNew);
					}
				} else 	if (eachField instanceof FieldObject) {
					gotFieldObject = true;
				}
			}
			if (gotError == false) App.logInfo("Completed renaming FieldObjectBox of: " + aOldClassSimpleName);
		} catch (Exception ex) {
			App.logEror(ex, "Error when renaming FieldObjectBox of: " + aOldClassSimpleName);
		}

		// ------------------------------------------------------------------------------------------
		// rename ih_ -- yet to implement this, need to do it...
		// ------------------------------------------------------------------------------------------

		// ------------------------------------------------------------------------------------------
		// rename ih_ -- yet to implement this, need to do it...
		// ------------------------------------------------------------------------------------------

		// ------------------------------------------------------------------------------------------
		// rename ih_ -- yet to implement this, need to do it...
		// ------------------------------------------------------------------------------------------

		// ------------------------------------------------------------------------------------------
		// rename ih_ -- yet to implement this, need to do it...
		// ------------------------------------------------------------------------------------------


		// ------------------------------------------------------------------------------------------
		// rename iv_
		// ------------------------------------------------------------------------------------------

		// rename pk column in iv table
		if (gotFieldObject == true) {
			// rename iv table
			String ivNameOld = GetIvTableName(aOldClassSimpleName);
			String ivNameNew = GetIvTableName(aNewClassSimpleName);
			try {
				Table oldIvTable = new Table(ivNameOld);
				oldIvTable.renameTable(aConn, ivNameNew);
				App.logInfo("Completed renaming iv_ table of: " + aOldClassSimpleName);
			} catch (Exception ex) {
				App.logEror(ex, "Error when renaming iv_ table of: " + aOldClassSimpleName);
			}

			Table newIvTable = new Table(ivNameNew);
			try {
				newIvTable.renameField(aConn, pkColNameOld, pkColNameNew);
				App.logInfo("Completed renaming pk of iv_ table of: " + aOldClassSimpleName);
			} catch (Exception ex) {
				App.logEror(ex, "Eror when renaming pk of iv_ table of: " + aOldClassSimpleName);
			}
		}


		// ------------------------------------------------------------------------------------------
		// rename sq_
		// ------------------------------------------------------------------------------------------

		// rename sequence table
		try {
			String sqNameOld = CreateSequenceTableName(aOldClassSimpleName);
			String sqNameNew = CreateSequenceTableName(aNewClassSimpleName);
			Table oldSqTable = new Table(sqNameOld);
			oldSqTable.renameTable(aConn, sqNameNew);
			App.logInfo("Completed renaming sq_ table of: " + aOldClassSimpleName);
		} catch (Exception ex) {
			App.logEror(ex, "Eror when renaming sq_ table of: " + aOldClassSimpleName);
		}
	}

	public static void RenameLeafClass(Connection aConn, String aPackageName, String aOldClassSimpleName, String aNewClassSimpleName, String aFobParent, String aFobFieldName) throws Exception {
		String oldIwBoxTableName = GetIwTableName(aFobParent, aFobFieldName);
		Clasz.RenameLeafClassByIvBoxName(aConn, oldIwBoxTableName, aPackageName, aOldClassSimpleName, aNewClassSimpleName);
	}

	public static void RenameLeafClassByIvBoxName(Connection aConn, String newIvBoxTableName, String aPackageName, String aOldClassSimpleName, String aNewClassSimpleName) throws Exception {
		Table newIvBoxTable = new Table(newIvBoxTableName);
		newIvBoxTable.initMeta(aConn);
		newIvBoxTable.forEachRecord(aConn, (Connection bConn, Record aRecord) -> {
			String leafClass = aRecord.getField(ObjectBase.LEAF_CLASS).getValueStr();
			String oldClassName = aPackageName + "." + aOldClassSimpleName;
			String oldClassSimpleName = aOldClassSimpleName;
			if (leafClass.equalsIgnoreCase(oldClassName)) {
				String newClassName = oldClassName.replace(oldClassSimpleName, aNewClassSimpleName);
				Record record2Update = new Record();
				record2Update.createField(ObjectBase.LEAF_CLASS, newClassName);
				newIvBoxTable.update(bConn, record2Update, aRecord);
				if (bConn.getAutoCommit() == false) bConn.commit();
			}
			return(true);
		});
	}

}