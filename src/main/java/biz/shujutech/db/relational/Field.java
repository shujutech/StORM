package biz.shujutech.db.relational;

import biz.shujutech.base.App;
import biz.shujutech.base.Base;
import biz.shujutech.base.Connection;
import biz.shujutech.base.Hinderance;
import biz.shujutech.db.object.Clasz;
import biz.shujutech.db.object.FieldHtml;
import biz.shujutech.db.object.FieldObjectBox;
import biz.shujutech.db.object.ObjectBase;
import biz.shujutech.reflect.AttribIndex;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.joda.time.DateTime;

public class Field extends Base implements Comparable {
	public static final Long BASE64_MAXSIZE = 200000L;

	private String fieldName;
	private FieldType fieldType;
	private int fieldSize;
	private String fqName; // fully qualified field name during its object context

	// information on how to display this field in GUI
	private String displayName = ""; // the label name for this field in ui
	private boolean forDisplay = true; // set to true, so ui will display this field
	private boolean forRemove = true; // set to true, so it'll be remove by default and json sent to ui is without this field, unless explicity set t false
	private boolean flatten = false;
	private int displayPosition;

	// field system attributes
	private boolean isPrimaryKey = false;
	private int primaryKeySeq = 0;
	private boolean isUniqueKey = false;
	private boolean isModified = false;
	private boolean isAtomic = true;
	private boolean isInline = false;
	private boolean isChildCount = false;
	private boolean deleteAsMember = false;
	private boolean delayFetch = false;
	private boolean polymorphic = false;
	private boolean uiMaster = false; // if it is Ui master, o2web will draw each clasz into its own rectangle area, see FrmRef.setAreadEdit() 
	private boolean updateable = true;
	private boolean changeable = true;
	private boolean lookup = true;
	private String defaultValue = "";

	private String formulaStr = ""; // if this field contains value, then its use for inplace update e.g. to increment a field value, the value must be incremented by the database and NOT the application

	// sorting properties
	private boolean isSortKey = false;
	private SortOrder sortOrder = SortOrder.ASC;
	private int sortKeyNo = 0;

	// table index properties
	private boolean isIndexKey = false;
	private SortOrder indexKeyOrder = SortOrder.ASC;
	private int indexKeyNo = 0;

	// object index properties
	private boolean isObjectKey = false;
	private SortOrder objectKeyOrder = SortOrder.ASC;
	private int objectKeyNo = 0;

	public List<AttribIndex> indexes = new CopyOnWriteArrayList<>();

	private Clasz masterObject = null;

	private String fieldMask = "";

	public Field() {
		super();
	};

	public boolean isUpdateable() {
		return updateable;
	}

	public void setUpdateable(boolean updateable) {
		this.updateable = updateable;
	}

	public boolean isChangeable() {
		return changeable;
	}

	public void setChangeable(boolean changeable) {
		this.changeable = changeable;
	}

	public static Field CreateField(Field aField) throws Exception {
		Field result = CreateFieldByType(aField.getFieldName(), aField.getFieldType(), aField.getFieldSize());
		return(result);
	}

	public static Field CreateField(String aName, FieldType aType) throws Exception {
		Field result = CreateFieldByType(aName, aType, 0);
		return(result);
	}

	public static Field CreateField(String aName, FieldType aType, int aSize) throws Exception {
		if (aType == FieldType.STRING || aType == FieldType.HTML) {
			Field result = CreateFieldByType(aName, aType, aSize);
			return(result);
		} else {
			throw new Hinderance("Must be string/html type field call to this method, field name: " + aName.toUpperCase());
		}
	}

	public static Field CreateField(String aName, DateTime aValue) throws Exception {
		FieldDate result = (FieldDate) CreateFieldByType(aName, FieldType.DATE, 0);
		result.setValueDate(aValue);
		return(result);
	}

	public static Field CreateField(String aName, DateTime aValue, boolean useTime) throws Exception {
		FieldDateTime result = (FieldDateTime) CreateFieldByType(aName, FieldType.DATETIME, 0);
		result.setValueDateTime(aValue);
		return(result);
	}

	private static Field CreateFieldByType(String aName, FieldType aType, int aSize) throws Exception {
		Field result;
		String fieldName = aName.toLowerCase();
		if (aType == FieldType.STRING) {
			result = new FieldStr(fieldName, aSize);
		} else if (aType == FieldType.HTML) {
			result = new FieldHtml(fieldName, aSize);
		} else if (aType == FieldType.ENCRYPT) {
			result = (Field) new FieldEncrypt(fieldName);
		} else if (aType == FieldType.BASE64) {
			result = (Field) new FieldBase64(fieldName);
		} else if (aType == FieldType.LONG) {
			result = (Field) new FieldLong(fieldName);
		} else if (aType == FieldType.INTEGER) {
			result = (Field) new FieldInt(fieldName);
		} else if (aType == FieldType.BOOLEAN) {
			result = (Field) new FieldBoolean(fieldName);
		} else if (aType == FieldType.DATETIME) {
			result = (Field) new FieldDateTime(fieldName);
		} else if (aType == FieldType.DATE) {
			result = (Field) new FieldDate(fieldName);
		} else if (aType == FieldType.FLOAT) {
			result = (Field) new FieldFloat(fieldName);
		} else {
			throw new Hinderance("Fail to create field of unknown type, field: " + fieldName.toUpperCase() + ", type: " + aType);
		}
		return(result);
	}

	public String getDisplayName() {
		if (this.displayName.isEmpty()) {
			String tmp = this.getFieldName();
			tmp = tmp.replace("_", " ");
			tmp = capitalizeString(tmp);
			displayName = tmp;
		}
		return displayName;
	}

	public static String capitalizeString(String string) {
		char[] chars = string.toLowerCase().toCharArray();
		boolean found = false;
		for (int i = 0; i < chars.length; i++) {
		  if (!found && Character.isLetter(chars[i])) {
		    chars[i] = Character.toUpperCase(chars[i]);
		    found = true;
		  } else if (Character.isWhitespace(chars[i]) || chars[i]=='.' || chars[i]=='\'') { // You can add other chars here
		    found = false;
		  }
		}
		return String.valueOf(chars);
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public boolean forDisplay() {
		return forDisplay;
	}

	public void forDisplay(boolean forDisplay) {
		this.forDisplay = forDisplay;
	}

	public int displayPosition() {
		return this.displayPosition;
	}

	public void displayPosition(int aPosition) {
		this.displayPosition = aPosition;
	}

	public void displayPosition(int aPosition, boolean forDisplay) {
		this.displayPosition = aPosition;
		this.forDisplay = forDisplay;
	}

	public boolean forRemove() {
		return forRemove;
	}

	public void forRemove(boolean forRemove) {
		this.forRemove = forRemove;
	}

	public boolean isFlatten() {
		return flatten;
	}

	public void setFlatten(boolean flatten) {
		this.flatten = flatten;
	}

	public boolean isAtomic() {
		return isAtomic;
	}

	public void setAtomic(boolean aAtomic) {
		this.isAtomic = aAtomic;
	}

	public boolean isModified() {
		return isModified;
	}

	public void setModified(boolean aModified) {
		this.isModified = aModified;
	}

	public boolean isPrimaryKey() {
		return(this.isPrimaryKey);
	}

	public boolean isInline() {
		return(this.isInline);
	}

	public boolean isChildCount() {
		return(this.isChildCount);
	}
	
	public void setChildCount(boolean childCount) {
		this.isChildCount = childCount;
	}

	public boolean isDelayFetch() {
		return delayFetch;
	}

	public void setDelayFetch(boolean delayFetch) {
		this.delayFetch = delayFetch;
	}

	public void setPrimaryKey() {
		this.setPrimaryKey(0);
	}

	public void setPrimaryKey(int aPosition) {
		this.isPrimaryKey = true;
		this.primaryKeySeq = aPosition;
	}

	public int getPrimaryKeySeq() {
		return(this.primaryKeySeq);
	}

	public void setInline(boolean inline) {
		this.isInline = inline;
	}

	public void deleteAsMember(boolean value) {
		this.deleteAsMember = value;
	}

	public boolean deleteAsMember() {
		return(this.deleteAsMember);
	}

	public void setUniqueKey(boolean aValue) {
		this.isUniqueKey = aValue;
	}

	public boolean isUniqueKey() {
		return(this.isUniqueKey);
	}

	public int getFieldSize() {
		return fieldSize;
	}

	public void setFieldSize(int fieldSize) {
		this.fieldSize = fieldSize;
	}

	public String getValueStr() throws Exception {
		throw new Hinderance("The getValueStr method is not implemented in class: " + this.getMasterClassSimpleName() + ", fieid: "+ this.getFieldName());
	}

	public String getValueStr(Connection aConn) throws Exception {
		return(this.getValueStr());
	}

	public void setValue(Object value) throws Exception {
		throw new Hinderance("The setValue method is not implemented in class: " + this.getMasterClassSimpleName() + ", field: "+ this.getFieldName());
	}

	public void setValueStr(String value) throws Exception {
		throw new Hinderance("The setValueStr method is not implemented in class: " + this.getMasterClassSimpleName() + ", field: "+ this.getFieldName());
	}

	public Object getValueObj(ObjectBase aDb) throws Exception {
		throw new Hinderance("The getValueObj(ObjectBase) method is not implemented in class: " + this.getMasterClassSimpleName() + ", field: "+ this.getFieldName());
	}

	public Object getValueObj(Connection aConn) throws Exception {
		throw new Hinderance("The getValueObj(Connection) method is not implemented in class: " + this.getMasterClassSimpleName() + ", field: "+ this.getFieldName());
	}

	public Object getValueObj() throws Exception {
		throw new Hinderance("The getValueObj() method is not implemented in class: " + this.getMasterClassSimpleName() + ", field: "+ this.getFieldName());
	}

	public String getFieldName() {
		return fieldName;
	}

	public final void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public FieldType getFieldType() {
		return fieldType;
	}

	public final void setFieldType(FieldType fieldType) {
		this.fieldType = fieldType;
	}

	public String getFormulaStr() {
		return formulaStr;
	}

	public void setFormulaStr(String formulaStr) {
		this.formulaStr = formulaStr;
	}

	public void populateField(ResultSet rs) throws Exception {
		PopulateField(this, rs);
	}

	public static void PopulateField(Field eachField, ResultSet rs) throws Exception {
		if (eachField.getFieldType() == FieldType.STRING || eachField.getFieldType() == FieldType.HTML) {
			String value = rs.getString(eachField.getFieldName());
			((FieldStr) eachField).setValueStr(value);
		} else if (eachField.getFieldType() == FieldType.ENCRYPT) {
			String value = rs.getString(eachField.getFieldName());
			((FieldEncrypt) eachField).setEncryptedValue(value);
		} else if (eachField.getFieldType() == FieldType.BASE64) {
			byte[] byteArray = rs.getBytes(eachField.getFieldName());
			if (byteArray != null) {
				if (byteArray.length > Field.BASE64_MAXSIZE) {
					throw new Hinderance("Field BASE64 size exceed: " + BASE64_MAXSIZE + " bytes, field: " + eachField.getFieldName());
				}
				String value = Base64.getEncoder().encodeToString(byteArray);
				value = new String(Base64.getDecoder().decode(value));
				((FieldStr) eachField).setValueStr(value);
			}
		} else if (eachField.getFieldType() == FieldType.DATETIME) {
			FieldDateTime fieldDate = (FieldDateTime) eachField;
			java.sql.Timestamp tstampType = rs.getTimestamp(fieldDate.getFieldName());
			if (tstampType != null) {
				DateTime value = new DateTime(tstampType.getTime());
				fieldDate.setValueDateTime(value);
			} else {
				fieldDate.setValueDateTime(null);
			}
		} else if (eachField.getFieldType() == FieldType.DATE) {
			FieldDate fieldDate = (FieldDate) eachField;
			java.sql.Date tstampType = rs.getDate(fieldDate.getFieldName());
			if (tstampType != null) {
				DateTime value = new DateTime(tstampType.getTime());
				fieldDate.setValueDate(value);
			} else {
				fieldDate.setValueDate(null);
			}
		} else	if (eachField.getFieldType() == FieldType.LONG) {
			FieldLong fieldLong = (FieldLong) eachField;
			if (rs.getObject(eachField.getFieldName()) != null) {
				Long value = rs.getLong(fieldLong.getFieldName());
				if (rs.wasNull() == false) {
					fieldLong.setValueLong(value);
				} else {
					fieldLong.setValueLong(null);
				}
			} else {
				fieldLong.setValueLong(null);
			}
		} else	if (eachField.getFieldType() == FieldType.INTEGER) {
			Integer value = rs.getInt(eachField.getFieldName());
			if (rs.wasNull() == false) {
				((FieldInt) eachField).setValueInt(value);
			} else {
				((FieldInt) eachField).setValueInt(null);
			}
		} else	if (eachField.getFieldType() == FieldType.FLOAT) {
			Float value = rs.getFloat(eachField.getFieldName());
			if (rs.wasNull() == false) {
				((FieldFloat) eachField).setValueFloat(value);
			} else {
				((FieldFloat) eachField).setValueFloat(null);
			}
		} else	if (eachField.getFieldType() == FieldType.BOOLEAN) {
			Boolean value = rs.getBoolean(eachField.getFieldName());
			((FieldBoolean) eachField).setValueBoolean(value);
		} else {
			throw new Hinderance("Unknown type for field: " + eachField.getFieldName().toUpperCase() + " when attempting to fetch it from result set");
		}
		eachField.setModified(false); // when populating database, its assume is not modify
	}


	public void cloneField(Field aSourceField) throws Exception {
		this.copyValue(aSourceField);
		this.setModified(aSourceField.isModified()); // place back the original modify status
	}

	public void copyValueStrNull(Field aSourceField) throws Exception {
		if (this.getFieldType() == FieldType.STRING || this.getFieldType() == FieldType.HTML) {
			String value = ((FieldStr) aSourceField).getValueStrNull();
			this.setFieldSize(aSourceField.getFieldSize());
			((FieldStr) this).setValueStr(value);
		} else {
			copyValue(aSourceField);
		}
	}

	public void copyValue(Field aSourceField) throws Exception {
		if (this.getFieldType() == FieldType.STRING || this.getFieldType() == FieldType.HTML) {
			String value = ((FieldStr) aSourceField).getValueStrNull();
			this.setFieldSize(aSourceField.getFieldSize());
			((FieldStr) this).setValueStr(value);
		} else	if (this.getFieldType() == FieldType.ENCRYPT) {
			String value = ((FieldEncrypt) aSourceField).getValueStr();
			((FieldEncrypt) this).setValueStr(value);
		} else	if (this.getFieldType() == FieldType.BASE64) {
			String value = ((FieldEncrypt) aSourceField).getValueStr();
			((FieldEncrypt) this).setValueStr(value);
		} else if (this.getFieldType() == FieldType.DATETIME) {
			DateTime value = ((FieldDateTime) aSourceField).getValueDateTime();
			((FieldDateTime) this).setValueDateTime(value);
		} else if (this.getFieldType() == FieldType.DATE) {
			DateTime value = ((FieldDate) aSourceField).getValueDate();
			((FieldDate) this).setValueDate(value);
		} else	if (this.getFieldType() == FieldType.LONG) {
			Long value = ((FieldLong) aSourceField).getValueLong();
			((FieldLong) this).setValueLong(value);
		} else	if (this.getFieldType() == FieldType.INTEGER) {
			Integer value = ((FieldInt) aSourceField).getValueInt();
			((FieldInt) this).setValueInt(value);
		} else	if (this.getFieldType() == FieldType.FLOAT) {
			Float value = ((FieldFloat) aSourceField).getValueFloat();
		} else	if (this.getFieldType() == FieldType.BOOLEAN) {
			Boolean value = ((FieldBoolean) aSourceField).getValueBoolean();
			((FieldBoolean) this).setValueBoolean(value);
		} else {
			throw new Hinderance("Unknown type for field when attempting to copy from field: " + aSourceField.getFieldName().toUpperCase());
		}
		this.copyAttribute(aSourceField);
	}

	public void copyAttribute(Field aSourceField) {
		this.isInline = aSourceField.isInline;
		this.deleteAsMember = aSourceField.deleteAsMember;
		this.isPrimaryKey = aSourceField.isPrimaryKey;
		this.isUniqueKey = aSourceField.isUniqueKey;
		this.updateable = aSourceField.updateable;
		this.changeable = aSourceField.changeable;

		this.displayPosition = aSourceField.displayPosition;
		this.uiMaster = aSourceField.uiMaster;
		this.lookup = aSourceField.lookup;
		this.polymorphic = aSourceField.polymorphic;
		this.primaryKeySeq = aSourceField.primaryKeySeq;
		this.masterObject = aSourceField.masterObject;
		this.displayName = aSourceField.displayName;
		/*
		this.displayPosition(aSourceField.displayPosition());
		this.setUiMaster(aSourceField.getUiMaster());
		this.setLookup(aSourceField.lookup());
		this.setUpdateable(aSourceField.isUpdateable());
		this.setPolymorphic(aSourceField.isPolymorphic());
		if (aSourceField.isUniqueKey()) {
			this.setPrimaryKey(aSourceField.getPrimaryKeySeq());
		}
		this.setMasterObject(aSourceField.getMasterObject());
		this.setDisplayName(aSourceField.getDisplayName());
		*/
	}
	
	public boolean isSystemField() {
		boolean result = false;
		if (this.isPrimaryKey()) {
			result = true;
		} else if (this.isChildCount()) {
			result = true;
		}
		return(result);
	}

	public synchronized static int compare(Field aLeft, Field aRight) throws Exception {
		int result = 0;
		Field left = (Field) aLeft;
		Field right = (Field) aRight;

		//if (left.getFieldType() == FieldType.STRING) {
		if (left instanceof FieldStr) {
			result = ((FieldStr) left).getValueStr().toLowerCase().compareTo(((FieldStr) right).getValueStr().toLowerCase());
		} else if (left.getFieldType() == FieldType.DATETIME) {
			result = ((FieldDateTime) left).getValueDateTime().compareTo(((FieldDateTime) right).getValueDateTime());
		} else if (left.getFieldType() == FieldType.DATE) {
			result = ((FieldDate) left).getValueDate().compareTo(((FieldDate) right).getValueDate());
		} else	if (left.getFieldType() == FieldType.LONG) {
			result = (int) (((FieldLong) left).getValueLong() - ((FieldLong) right).getValueLong());
		} else	if (left.getFieldType() == FieldType.INTEGER) {
			result = (int) (((FieldInt) left).getValueInt() - ((FieldInt) right).getValueInt());
		}

		/*
		if (result == 0) {
			result = 1; // returning 0 would merge keys??? http://stackoverflow.com/questions/109383/how-to-sort-a-mapkey-value-on-the-values-in-java
		}

		if (aLeft.getSortOrder() == SortOrder.DSC) {
			result = result * -1;
		}
		*/
		
		return(result);
	}

	@Override
	public int compareTo(Object aRight) {
		int result = 0;
		try {
			result = (Field.compare(this, (Field) aRight));
		} catch(Exception ex) {
			App.logEror(ex);
		}
		return(result);
	}

	public int compareForCriteria(Connection aConn, Field aRight) throws Exception {
		Field right = (Field) aRight;
		Integer compareInt = -1;
		if (this instanceof FieldStr) {
			compareInt = ((FieldStr) this).getValueStr().compareTo(((FieldStr) right).getValueStr());
		} else if (this.getFieldType() == FieldType.DATETIME) {
			compareInt = ((FieldDateTime) this).getValueDateTime().compareTo(((FieldDateTime) right).getValueDateTime());
		} else if (this.getFieldType() == FieldType.DATE) {
			compareInt = ((FieldDate) this).getValueDate().compareTo(((FieldDate) right).getValueDate());
		} else	if (this.getFieldType() == FieldType.LONG) {
			compareInt = (int) (((FieldLong) this).getValueLong() - ((FieldLong) right).getValueLong());
		} else	if (this.getFieldType() == FieldType.INTEGER) {
			compareInt = (int) (((FieldInt) this).getValueInt() - ((FieldInt) right).getValueInt());
		} else	if (this.getFieldType() == FieldType.OBJECT) {
			Clasz claszCriteria = (Clasz) this.getValueObj(aConn);
			Clasz claszReal = (Clasz) (aRight).getValueObj(aConn);
			compareInt = claszCriteria.compareForCriteria(aConn, claszReal);
		} else	if (this.getFieldType() == FieldType.OBJECTBOX) {
			FieldObjectBox fobCriteria = (FieldObjectBox) this;
			FieldObjectBox fobReal = (FieldObjectBox) aRight;
			if (fobCriteria.getTotalMember() != fobReal.getTotalMember()) {
				compareInt = (int) (fobCriteria.getTotalMember() - fobReal.getTotalMember());
			} else {
				List<Integer> compareNum = new CopyOnWriteArrayList<>();
				fobCriteria.forEachMember(aConn, ((Connection bConn, Clasz aCriteriaMember) -> { 
					List<Integer> gotMatch = new CopyOnWriteArrayList<>();
					List<Integer> compareList = new CopyOnWriteArrayList<>();
					fobReal.forEachMember(bConn, ((Connection cConn, Clasz aRealMember) -> { 
						if (aCriteriaMember.equalsCriteria(cConn, aRealMember)) {
							gotMatch.add(0);
							return(false);
						} else {
							if (compareList.isEmpty()) {
								int cmpInt = aCriteriaMember.compareForCriteria(cConn, aRealMember); // match the compare int for the first element that do not compare to 0
								compareList.add(cmpInt);
							}
						}
						return(true); 
					}));
					if (gotMatch.isEmpty()) {
						compareNum.add(compareList.get(0));
						return(false);
					}
					return(true);
				}));
				if (compareNum.isEmpty()) {
					compareInt = 0;
				} else {
					compareInt = compareNum.get(0);
				}
			}
		} else {
			throw new Hinderance("Unsupported criteria comparison for field of type: " + this.getFieldType().name());
		}

		return(compareInt);
	}

	//
	// sorting related methods
	//

	public boolean isSortKey() {
		return isSortKey;
	}

	public void setSortKey(boolean isSortKey) {
		this.isSortKey = isSortKey;
	}

	public void setSortKey(Field aSourceField) {
		this.setSortKey(aSourceField.isSortKey());
		this.setSortKeyNo(aSourceField.getSortKeyNo());
		this.setSortOrder(aSourceField.getSortOrder());
	}

	public SortOrder getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(SortOrder sortOrder) {
		this.sortOrder = sortOrder;
	}

	public int getSortKeyNo() {
		return sortKeyNo;
	}

	public void setSortKeyNo(int sortKeyNo) {
		this.sortKeyNo = sortKeyNo;
	}

	public void setSortKey(int aKeyNo, SortOrder aOrder) {
		this.isSortKey = true;
		this.sortKeyNo = aKeyNo;
		this.sortOrder = aOrder;
	}

	public void clearSortKey() {
		this.isSortKey = false;
		this.sortKeyNo = 0;
		this.sortOrder = SortOrder.ASC;
	}

	//
	// start of table index methods
	//

	public boolean isIndexKey() {
		return isIndexKey;
	}

	public void setIndexKey(boolean isIndexKey) {
		this.isIndexKey = isIndexKey;
	}

	public SortOrder getIndexKeyOrder() {
		return indexKeyOrder;
	}

	public void setIndexKeyOrder(SortOrder indexKeyOrder) {
		this.indexKeyOrder = indexKeyOrder;
	}

	public int getIndexKeyNo() {
		return indexKeyNo;
	}

	public void setIndexKeyNo(int indexKeyNo) {
		this.indexKeyNo = indexKeyNo;
	}

	public void setIndexKey(Field aSourceField) {
		this.setIndexKey(aSourceField.isIndexKey());
		this.setIndexKeyNo(aSourceField.getIndexKeyNo());
		this.setIndexKeyOrder(aSourceField.getIndexKeyOrder());
	}

	//
	// end of table index methods
	//

	public boolean isObjectKey() {
		return isObjectKey;
	}

	public void setObjectKey(boolean isObjectKey) {
		this.isObjectKey = isObjectKey;
	}

	public void setObjectKey(Field aSourceField) {
		this.setObjectKey(aSourceField.isObjectKey());
		this.setObjectKeyNo(aSourceField.getObjectKeyNo());
		this.setObjectKeyOrder(aSourceField.getObjectKeyOrder());
	}

	public SortOrder getObjectKeyOrder() {
		return objectKeyOrder;
	}

	public void setObjectKeyOrder(SortOrder aOrder) {
		this.objectKeyOrder = aOrder;
	}

	public int getObjectKeyNo() {
		return objectKeyNo;
	}

	public void setObjectKeyNo(int aKeyNo) {
		this.objectKeyNo = aKeyNo;
	}

	public void setObjectKey(int aKeyNo, SortOrder aOrder) {
		this.isObjectKey = true;
		this.objectKeyNo = aKeyNo;
		this.objectKeyOrder = aOrder;
	}

	public void clearObjectKey() {
		this.isObjectKey = false;
		this.objectKeyNo = 0;
		this.objectKeyOrder = SortOrder.ASC;
	}

	//
	// end of sort and object indexes methods
	//

	public boolean isPolymorphic() {
		return polymorphic;
	}

	public void setPolymorphic(boolean polymorphic) {
		this.polymorphic = polymorphic;
	}

	public boolean isUiMaster() {
		return uiMaster;
	}

	public void setUiMaster(boolean uiMaster) {
		this.uiMaster = uiMaster;
	}

	public boolean getUiMaster() {
		return(this.uiMaster);
	}

	public void setLookup(boolean aLookup) {
		this.lookup = aLookup;
	}

	public boolean lookup() {
		return lookup;
	}

	public String getFqName() {
		return fqName;
	}

	public void setFqName(String fqName) {
		this.fqName = fqName;
	}

	public Clasz getMasterObject() {
		return masterObject;
	}

	public Class getMasterClass() {
		Class result = null;
		if (this.getMasterObject() != null) {
			result = this.getMasterObject().getClass();
		}
		return(result);
	}

	public void setMasterObject(Clasz masterObject) {
		this.masterObject = masterObject;
	}

	public String getMasterClassSimpleName() {
		String result = "";
		if (this.getMasterObject() != null) {
			result = this.getMasterObject().getClass().getSimpleName();
		}
		return(result);
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getFieldMask() {
		return fieldMask;
	}

	public void setFieldMask(String fieldMask) {
		this.fieldMask = fieldMask;
	}

	

}
