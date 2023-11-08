package biz.shujutech.db.relational;

import biz.shujutech.base.Base;
import biz.shujutech.base.Connection;
import biz.shujutech.base.Hinderance;
import biz.shujutech.db.object.Clasz;
import biz.shujutech.db.object.FieldObject;
import biz.shujutech.db.object.FieldObjectBox;
import java.sql.ResultSet;
import java.util.Collection;
import org.joda.time.DateTime;
import java.util.concurrent.ConcurrentHashMap;

public class Record extends Base {

	private ConcurrentHashMap<String, Field> fieldBox = new ConcurrentHashMap<>();

	public ConcurrentHashMap<String, Field> getFieldBox() {
		return fieldBox;
	}

	public Record() {}

	public Record(Record aRec) throws Exception {
		for (Field eachField : aRec.getFieldBox().values()) {
			if (eachField.getFieldSize() == 0) {
				this.createField(eachField.getDbFieldName(), eachField.getDbFieldType());
			} else {
				this.createField(eachField.getDbFieldName(), eachField.getDbFieldType(), eachField.getFieldSize());
			}
		}
	}

	public Collection<Field> getFields() throws Exception {
		return(this.getFieldBox().values());
	}

	public Field getFirstField() throws Exception {
		Field result = null;
		for(Field eachField: this.getFieldBox().values()) {
			result = eachField;
			break;
		}
		return(result);
	}

	public FieldInt createFieldInt(String aName, Integer aValue) throws Exception {
		FieldInt result = (FieldInt) createField(aName, FieldType.INTEGER);
		result.setValueInt(aValue);
		return result;
	}

	public FieldDate createFieldDate(String aName, DateTime aValue) throws Exception {
		FieldDate result = (FieldDate) createField(aName, FieldType.DATE);
		result.setValueDate(aValue);
		return result;
	}

	/**
	 * Factored create field inside a record. Note it places the created field
	 * into a fieldbox for tracking the fields belonging to this record.
	 * 
	 * @param aName
	 * @param aType
	 * @return
	 * @throws Exception 
	 */
	public Field createField(String aName, FieldType aType) throws Exception {
		String fieldName = aName.toLowerCase();
		this.checkField(fieldName);
		Field oneField = Field.CreateField(fieldName, aType);
		this.fieldBox.put(fieldName, oneField);
		return(oneField);
	}

	public Field createField(String aName, FieldType aType, int aSize, String aValue) throws Exception {
		Field oneField = this.createField(aName, aType, aSize);
		oneField.setValue(aValue);
		return(oneField);
	}

	public Field createField(String aName, FieldType aType, int aSize) throws Exception {
		this.checkField(aName);
		Field oneField = Field.CreateField(aName, aType, aSize);
		this.fieldBox.put(aName, oneField);
		return(oneField);
	}
	// TODO create individual call for each field type (now all field regardless its string, long, datetime are all in the Field class)

	public void createField(Connection aConn, Record aRec) throws Exception {
		for (Field eachField : aRec.getFieldBox().values()) {
				this.createField(aConn, eachField);
		}
	}

	public Field createField(Connection aConn, Field aField) throws Exception {
		Field result;
		if (aField.getDbFieldType() == FieldType.STRING) {
			result = this.createField(aField.getDbFieldName(), aField.getDbFieldType(), aField.getFieldSize());
		} else if (aField.getDbFieldType() == FieldType.LONG) {
			result = this.createField(aField.getDbFieldName(), 0L);
		} else if (aField.getDbFieldType() == FieldType.OBJECT) {
			Clasz<?> obj = (Clasz<?>) ((FieldObject<?>) aField).getValueObj(aConn);
			Clasz<?> newObj = (Clasz<?>) (obj.getClass()).getConstructor().newInstance();
			//Clasz newObj = (Clasz) obj.getClass().newInstance();
			result = this.createFieldObject(aField.getDbFieldName(), newObj);
		} else if (aField.getDbFieldType() == FieldType.OBJECTBOX) {
			FieldObjectBox<?> field = (FieldObjectBox<?>) aField;
			Clasz<?> newObj = (Clasz<?>) (Class.forName(field.getDeclareType())).getConstructor().newInstance();
			//Clasz newObj = (Clasz) Class.forName(field.getDeclareType()).newInstance();
			result = this.createFieldObject(aField.getDbFieldName(), newObj);
		} else {
			result = this.createField(aField.getDbFieldName(), aField.getDbFieldType());
		}
		return(result);
	}

	public Field createField(String aName, String aValue) throws Exception {
		FieldStr result = (FieldStr) this.createField(aName, FieldType.STRING);
		result.setValueStr(aValue);
		return(result);
	}

	public Field createField(String aName, long aValue) throws Exception {
		FieldLong result = (FieldLong) this.createField(aName, FieldType.LONG);
		result.setValueLong(aValue);
		return(result);
	}

	public Field createField(String aName, DateTime aValue) throws Exception {
		FieldDate result = (FieldDate) this.createField(aName, FieldType.DATE);
		result.setValueDate(aValue);
		return(result);
	}

	public Field createField(String aName, DateTime aValue, boolean useTime) throws Exception {
		FieldDateTime result = (FieldDateTime) this.createField(aName, FieldType.DATETIME);
		result.setValueDateTime(aValue);
		return(result);
	}

	public <Ty extends Clasz<?>> FieldObject<Ty> createFieldObject(String aName, Ty aObject) throws Exception {
		this.checkField(aName);
		FieldObject<Ty> oneField = new FieldObject<Ty>(aName.toLowerCase(), aObject);
		this.fieldBox.put(aName, oneField);
		return oneField;
	}

	public <Ty extends Clasz<?>> FieldObjectBox<Ty> createFieldFob(String aName, FieldObjectBox<Ty> aBox) throws Exception {
		this.checkField(aName);
		aBox.setDbFieldName(aName.toLowerCase());
		aBox.setDbFieldType(FieldType.OBJECTBOX);
		this.fieldBox.put(aName, aBox);
		return(aBox);
	}

	public void checkField(String aName) throws Exception {
		if (fieldExist(aName)) {
			throw new Hinderance("Trying to create an already exist field: " + aName.toUpperCase());
		}
	}

	public boolean fieldExist(String aName) {
		if (this.fieldBox.containsKey(aName.toLowerCase())) {
			return(true);
		} else {
			return(false);
		}
	}

	public Field getField(String aName) throws Exception {
		Field result = this.getFieldBox().get(aName.toLowerCase());
		if (result != null) {
			if (result instanceof FieldEncrypt) {
				result = (FieldEncrypt) result;
			} else if (result instanceof FieldLong) {
				result = (FieldLong) result;
			} else if (result instanceof FieldInt) {
				result = (FieldInt) result;
			} else if (result instanceof FieldDateTime) {
				result = (FieldDateTime) result;
			} else if (result instanceof FieldDate) {
				result = (FieldDate) result;
			} else if (result instanceof FieldBoolean) {
				result = (FieldBoolean) result;
			} else if (result instanceof FieldObject<?>) {
				result = (FieldObject<?>) result;
			} else if (result instanceof FieldObjectBox) {
				result = (FieldObjectBox<?>) result;
			} else if (result instanceof FieldBase64) {
				result = (FieldBase64) result;
			} else if (result instanceof FieldStr) { // many other field inherit from FieldStr, so place it last
				result = (FieldStr) result;
			} else if (result instanceof FieldFloat) {
				result = (FieldFloat) result;
			} else {
				throw new Hinderance("Unknown field type in record, field: " + aName.toUpperCase());
			}
		}
		return(result);
	}

	public FieldLong getFieldLong(String aName) throws Exception {
		FieldLong result = (FieldLong) this.getField(aName);
		return(result);
	}

	public FieldStr getFieldStr(String aName) throws Exception {
		FieldStr result = (FieldStr) this.getField(aName);
		return(result);
	}

	public FieldInt getFieldInt(String aName) throws Exception {
		FieldInt result = (FieldInt) this.getField(aName);
		return(result);
	}

	public Field getFieldObjectBox(String aName) throws Exception {
		Field result = (FieldObjectBox<?>) this.getField(aName);
		return(result);
	}

	public FieldDateTime getFieldDateTime(String aName) throws Exception {
		FieldDateTime result = (FieldDateTime) this.getField(aName);
		return(result);
	}

	public Boolean getValueBoolean(String aFieldName) throws Exception {
		FieldBoolean oneField = (FieldBoolean) this.getField(aFieldName);
		Boolean result = oneField.getValueBoolean();
		return(result);
	}

	public String getValueStr(String aFieldName) throws Exception {
		Field oneField = this.getField(aFieldName);
		String result = oneField.getValueStr();
		return(result);
	}

	public Integer getValueInt(String aFieldName) throws Exception {
		FieldInt oneField = (FieldInt) this.getField(aFieldName);
		Integer result = oneField.getValueInt();
		return(result);
	}

	public Long getValueLong(String aFieldName) throws Exception {
		FieldLong oneField = (FieldLong) this.getField(aFieldName);
		Long result = oneField.getValueLong();
		return(result);
	}

	public Clasz<?> getValueObject(Connection aConn, String aFieldName) throws Exception {
		FieldObject<?> oneField = (FieldObject<?>) this.getField(aFieldName);
		Clasz<?> result = oneField.getValueObj(aConn);
		return result;
	}

	public Clasz<?> getValueObject(String aFieldName) throws Exception {
		FieldObject<?> oneField = (FieldObject<?>) this.getField(aFieldName);
		Clasz<?> result = oneField.getObj();
		return result;
	}

	public Clasz<?> getValueObject(String aFieldName, long aIndex) throws Exception {
		FieldObjectBox<?> oneField = (FieldObjectBox<?>) this.getField(aFieldName);
		Clasz<?> result = oneField.getObject(aIndex);
		if (result == null) {
			throw new Hinderance("In array objects: "+ aFieldName.toUpperCase() + ", there is no object at index: " + aIndex);
		}
		return result;
	}

	public DateTime getValueDateTime(String aFieldName) throws Exception {
		FieldDateTime oneField = (FieldDateTime) this.getField(aFieldName);
		DateTime result = oneField.getValueDateTime();
		return(result);
	}

	public DateTime getValueDate(String aFieldName) throws Exception {
		FieldDate oneField = (FieldDate) this.getField(aFieldName);
		DateTime result = oneField.getValueDate();
		return(result);
	}

	public void setValueStr(String aFieldName, String aValue) throws Exception {
		FieldStr oneField = (FieldStr) this.getField(aFieldName);
		oneField.setValueStr(aValue);
	}

	public void setValueLong(String aFieldName, Long aValue) throws Exception {
		FieldLong oneField = (FieldLong) this.getField(aFieldName);
		oneField.setValueLong(aValue);
	}

	public void setValueInt(String aFieldName, Integer aValue) throws Exception {
		FieldInt oneField = (FieldInt) this.getField(aFieldName);
		oneField.setValueInt(aValue);
	}


	public void setValueBoolean(String aFieldName, Boolean aValue) throws Exception {
		FieldBoolean oneField = (FieldBoolean) this.getField(aFieldName);
		oneField.setValueBoolean(aValue);
	}

	public void setValueDateTime(String aFieldName, DateTime aValue) throws Exception {
		FieldDateTime oneField = (FieldDateTime) this.getField(aFieldName);
		oneField.setValueDateTime(aValue);
	}

	public void setValueDate(String aFieldName, DateTime aValue) throws Exception {
		FieldDate oneField = (FieldDate) this.getField(aFieldName);
		oneField.setValueDate(aValue);
	}

	/*
	@SuppressWarnings("unchecked")
	public void setValueObject(String aFieldName, Clasz<?> aValue) throws Exception {
		FieldObject oneField = (FieldObject<?>) this.getField(aFieldName);
		oneField.setValueObject(aValue);
	}

	@SuppressWarnings("unchecked")
	public void addValueObject(Connection aConn, String aFieldName, Clasz<?> aValue) throws Exception {
		FieldObjectBox fob2AddObj = (FieldObjectBox) this.getField(aFieldName);
		if (fob2AddObj.getMetaObj().getClaszName().equals("Clasz")) {
			Clasz metaObj = ObjectBase.CreateObject(aConn, aValue.getClass());
			fob2AddObj.setMetaObj(metaObj);
		}
		fob2AddObj.addValueObject(aValue);
	}
	*/

	public String getPkFieldName() {
		String result = "";
		for (Field eachField : this.getFieldBox().values()) {
			if (eachField.isPrimaryKey()) {
				result = eachField.getDbFieldName();
				break;
			}
		}
		return(result);
	}

	public int totalField() {
		return(this.getFieldBox().size());
	}

	public void cloneField(Connection aConn, Field aSourceField) throws Exception {
		Field destField = this.getField(aSourceField.getDbFieldName());
		if (destField == null) {
			destField = this.createField(aConn, aSourceField);
		}
		destField.cloneField(aConn, aSourceField);
	}

	public void copyValue(Connection aConn, Field aSourceField) throws Exception {
		Field destField = this.getField(aSourceField.getDbFieldName());
		if (destField == null) {
			destField = this.createField(aConn, aSourceField);
		}
		destField.copyValue(aConn, aSourceField);
	}

	public void clear() {
		this.fieldBox.clear();
	}
	
	public String toStr() throws Exception {
		String result = "";
		for (Field eachField : this.getFieldBox().values()) {
			result += "[" + eachField.getDbFieldName() + ":" + eachField.getValueStr() + "]";
		}
		return(result);
	}

	public boolean removeField(String aFieldName) {
		boolean result = true;
		if (this.fieldBox.remove(aFieldName) == null) result = false;
		return(result);
	}

	public void populateField(ResultSet rs) throws Exception {
		populateField(rs, this);
	}

	public static void populateField(ResultSet rs, Record aRec) throws Exception {
		for (Field eachField : aRec.getFieldBox().values()) {
			eachField.populateField(rs);
		}
	}

	public void copy(Connection aConn, Record aCopyFrom) throws Exception {
		for (Field targetField : this.getFieldBox().values()) {
			Field sourceField = aCopyFrom.getField(targetField.getDbFieldName());
			if (sourceField != null) {
				targetField.copyValue(aConn, sourceField);
			} else {
				throw new Hinderance("Source record do not have the field: " + targetField.getDbFieldName() + " to copy from!");
			}
		}
	}

	public void copyStrNull(Connection aConn, Record aCopyFrom) throws Exception { // check if copy method can copy null string and not blank string
		for (Field targetField : this.getFieldBox().values()) {
			Field sourceField = aCopyFrom.getField(targetField.getDbFieldName());
			if (sourceField != null) {
				targetField.copyValueStrNull(aConn, sourceField);
			} else {
				throw new Hinderance("Source record do not have the field: " + targetField.getDbFieldName() + " to copy from!");
			}
		}
	}
	
	public Float getValueFloat(String aFieldName) throws Exception {
		FieldFloat oneField = (FieldFloat) this.getField(aFieldName);
		Float result = oneField.getValueFloat();
		return(result);
	}

	public void setValueFloat(String aFieldName, Float aValue) throws Exception {
		FieldFloat oneField = (FieldFloat) this.getField(aFieldName);
		oneField.setValueFloat(aValue);
	}

	public Field createField(String aName, Record aValue) throws Exception {
		FieldRecord result = (FieldRecord) this.createField(aName, FieldType.RECORD);
		result.setValueRecord(aValue);
		return(result);
	}

}
