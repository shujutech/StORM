package biz.shujutech.db.object;

import biz.shujutech.base.App;
import biz.shujutech.base.Connection;
import biz.shujutech.base.Hinderance;
import biz.shujutech.db.relational.Field;
import biz.shujutech.db.relational.FieldType;
import java.lang.reflect.Modifier;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public final class FieldObject<Ty extends Clasz<?>> extends FieldClasz {

	private Ty obj = null;
	private Boolean forDelete = false;

	public FieldObject(String aName, Ty aValue) throws Exception {
		this.setDbFieldName(aName);
		this.setDbFieldType(FieldType.OBJECT);
		this.setModified(false);
		this.setAtomic(false);
		this.setValueObject(aValue);
	}

	public void createNewObject(Connection aConn) throws Exception {
		this.obj = CreateNewObject(aConn, this.getDeclareType(), this.isPolymorphic(), this.getMasterObject(), (FieldObject<Ty>) this);
	}

	@SuppressWarnings("unchecked")
	public static <Ty extends Clasz<?>> Ty CreateNewObject(Connection aConn, String aType, boolean aPolymorphic, Clasz<?> aMaster, FieldObject<Ty> aChildField) throws Exception {
		Ty obj = null;
		Class<?> memberClass = Class.forName(aType);
		if (memberClass != Clasz.class) { // if only this is not a recursive field, then we populate it
			Class<?> effectiveClass = memberClass;
			if (aPolymorphic) {
				effectiveClass = ObjectBase.GetEffectiveClass(aConn, aMaster, aChildField);
			}
			obj = (Ty) Clasz.CreateObjectFromAnyClass(aConn, effectiveClass);
		}
		return(obj);
	}

	public static Class<?> GetEffectiveClass(Connection aConn, String aType, boolean aPolymorphic, Clasz<?> aMaster, FieldObject<?> aChildField) throws Exception {
		Class<?> effectiveClass = Class.forName(aType);
		if (effectiveClass != Clasz.class) { 
			if (aPolymorphic) {
				Class<?> tmpClasz = ObjectBase.GetEffectiveClass(aConn, aMaster, aChildField);
				if (tmpClasz != null) {
					effectiveClass = tmpClasz;
				}
			}
		}
		return(effectiveClass);
	}


	public boolean isLookup(Connection aConn) throws Exception {
		boolean result = false;
		Class<?> effectiveClass = GetEffectiveClass(aConn, this.getDeclareType(), this.isPolymorphic(), this.getMasterObject(), (FieldObject<Ty>) this);
		if (Lookup.class.isAssignableFrom(effectiveClass)) {
			result = true;
		}
		return(result);
	}

	public Boolean isForDelete() {
		return forDelete;
	}

	public void setForDelete(Boolean forDelete) {
		this.forDelete = forDelete;
	}

	@Override
	public String getValueStr(Connection aConn) throws Exception {
		return(this.getValueObj(aConn).getValueStr());
	}

	@Override
	public Ty getValueObj(Connection aConn) throws Exception {
		if (this.getFetchStatus() == FetchStatus.SOF && this.isModified() == false) {
			if (this.isInline()) {
				if (obj == null) {
					this.createNewObject(aConn);
					Clasz.PopulateInlineField(aConn, this.getMasterObject(), this, this.getDbFieldName(), "tree");
				}
			} else {
				this.fetch(aConn);
			}
		}
		return(obj);
	}

	public Ty getObj() {
		return(this.obj);
	}

	public void setObj(Ty aObj) {
		this.obj = aObj;
	}

	public void setValueObject(Ty aObject) throws Exception {
		if (aObject == null && this.obj == null) {
			this.setModified(true); // when obj is null and modified, we remove it as object member if persisting
			return;
		}

		if (aObject == this.obj) {
			return;
		}

		this.setModified(true);
		this.obj = aObject;
	}

	public void setValueObjectFreeType(Object aObject) throws Exception {
		if (aObject instanceof Clasz) {
			@SuppressWarnings("unchecked")
			Ty castedObject = (Ty) aObject;
			setValueObject(castedObject);
		} else {
			throw new Hinderance("Cannot setValueObject() from an incompatible object type: " + aObject.getClass().getSimpleName());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void copyValue(Connection aConn, Field aSourceField) throws Exception {
		if ((aSourceField instanceof FieldObject<?>) == false)  {
			throw new Hinderance("Error, cannot copy values of difference type, target field: " + this.getDbFieldName() + ", type: " + this.getDbFieldType().toString() + ", source: " + aSourceField.getDbFieldName() + ", type: " + aSourceField.getDbFieldType().toString());
		}

		Ty sourceObj = ((FieldObject<Ty>) aSourceField).getObj();
		this.getObj().copyAllFieldWithoutModifiedState(aConn, sourceObj);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void cloneField(Connection aConn, Field aSourceField) throws Exception {
		if ((aSourceField instanceof FieldObject<?>) == false)  {
			throw new Hinderance("Error, cannot copy values of difference type, target field: " + this.getDbFieldName() + ", source: " + aSourceField.getDbFieldName());
		}

		Ty sourceObj = ((FieldObject<Ty>) aSourceField).getObj();
		if (sourceObj != null) {
			this.getObj().copyAllFieldWithModifiedState(aConn, sourceObj);
		} else {
			this.setObj(null);
		}
	}

	public static int compare(FieldObject<?> aLeft, FieldObject<?> aRight) throws Exception {
		int result = 0;
		Clasz<?> left = aLeft.getObj();
		Clasz<?> right = aRight.getObj();

		if (left.getClass().getSimpleName().equals(right.getClass().getSimpleName()) == false) {
			throw new Hinderance("Cannot compare object of different type: " + left.getClass().getSimpleName().toUpperCase() + ", " + right.getClass().getSimpleName().toUpperCase());
		}

		if ((left.getClass().getSimpleName().equals("Clasz")) || (right.getClass().getSimpleName().equals("Clasz"))) {
			//throw new Hinderance("Cannot compare object Clasz type between field: " + aLeft.getFieldName().toUpperCase() + ", " + aRight.getFieldName().toUpperCase());
		} else {
			result = left.compareTo(right); // this call the Clasz class to handle the compare
		}

		return(result);
	}

	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(Field aRight) {
		int result = 0;
		try {
			result = FieldObject.compare(this, (FieldObject<Ty>) aRight);
		} catch (Exception ex) {
			App.logEror(ex);
		}
		return(result);
	}

	public Field getField(String aFieldName) throws Exception {
		return(this.getObj().getField(aFieldName));
	}

	@SuppressWarnings("unchecked")
	public void fetch(Connection aConn) throws Exception {
		Class<Ty> memberClass = (Class<Ty>) Class.forName(this.getDeclareType());
		//if (memberClass != Clasz.class) { // if only this is not a recursive field, then we populate it
		if (memberClass.getName().equals(Clasz.class.getName()) == false) { // if only this is not a recursive field, then we populate it
			Class<Ty> effectiveClass = memberClass;
			if (this.isPolymorphic()) {
				//effectiveClass = ObjectBase.getPolymorphicClass(aConn, this.getMasterObject(), (FieldObject) this);
				Class<?> anyClass = ObjectBase.GetEffectiveClass(aConn, this.getMasterObject(), (FieldObject<Ty>) this);
				effectiveClass = (Class<Ty>) anyClass;
			}

			Ty fetchMember = (Ty) FetchMemberOfObject(aConn, (FieldObject<Ty>) this, this.getMasterObject(), effectiveClass);
			if (fetchMember == null) {
				// do nothing
				this.setFetchStatus(FieldClasz.FetchStatus.EOF);
			} else if (fetchMember.isPopulated()) {
				this.setFetchStatus(FieldClasz.FetchStatus.EOF);
				this.setValueObject(fetchMember);
			} else {
				this.setValueObject(null);
				this.setModified(false);
			}
		}
	}

	public static <Ty extends Clasz<?>> Ty FetchMemberOfObject(Connection aConn, FieldObject<Ty> aField, Clasz<?> aMasterObj, Class<Ty> aMemberClass) throws Exception {
		PreparedStatement stmt = null;
		ResultSet rset = null;
		try {
			String strSql = "select " + aField.getDbFieldName() + " from " + aMasterObj.getIvTableName() + " where " + aMasterObj.getPkName() + " = " + aMasterObj.getObjectId();
			stmt = aConn.prepareStatement(strSql);
			rset = stmt.executeQuery();
			if (rset.next()) {
				long pk = rset.getLong(1);
				if (rset.wasNull() == false) {
					Clasz<?> fetchMember = Clasz.FetchObjectByPk(aConn, aMemberClass, pk);
					//return (fetchMember);
					return aMemberClass.cast(fetchMember);
				}
			}
		} catch (Exception ex) {
			String strSql = "<PreparedStatement is null>";
			if (stmt != null) strSql = stmt.toString();
			throw new Hinderance(ex, "Fail to fetch: " + strSql);
		} finally {
			if (rset != null) {
				rset.close();
			}
			if (stmt != null) {
				stmt.close();
			}
		}
		return (null);
	}

	public void setValueStr(Connection aConn, String aValue) throws Exception {
		this.getValueObj(aConn).setValueStr(aValue);
	}

	public boolean isAbstract() throws Exception {
		return(IsAbstract(this));
	}

	public static boolean IsAbstract(String aClassName) throws Exception {
		return(IsAbstract(Class.forName(aClassName)));
	}

	public static boolean IsAbstract(FieldObject<?> aFieldObject) throws Exception {
		Class<?> fieldClass = Class.forName(aFieldObject.getDeclareType());
		return(IsAbstract(fieldClass));
	}

	public static boolean IsAbstract(Class<?> fieldClass) throws Exception {
		boolean result = false;
		if (Modifier.isAbstract(fieldClass.getModifiers())) {
			result = true;
		}
		return(result);
	}

	public Class<?> getObjectClass() throws Exception {
		Class<?> result = this.getObj().getClass();
		return(result);
	}

	public Class<?> getObjectClass(Connection aConn) throws Exception {
		Class<?> result;
		if (this.getObj() == null) {
			Ty clasz = CreateNewObject(aConn, this.getDeclareType(), this.isPolymorphic(), this.getMasterObject(), (FieldObject<Ty>) this);
			result = clasz.getClass();
		} else {
			Ty clasz = this.getValueObj(aConn);
			result = clasz.getClass();
		}
		return(result);
	}

}