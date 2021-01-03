package biz.shujutech.db.object;

import biz.shujutech.base.App;
import biz.shujutech.base.Connection;
import biz.shujutech.base.Hinderance;
import biz.shujutech.db.relational.Field;
import biz.shujutech.db.relational.FieldType;
import java.lang.reflect.Modifier;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public final class FieldObject extends FieldClasz {

	private Clasz obj = null;
	private Boolean forDelete = false;

	public FieldObject(String aName, Clasz aValue) throws Exception {
		this.setFieldName(aName);
		this.setFieldType(FieldType.OBJECT);
		this.setModified(false);
		this.setAtomic(false);
		this.setValueObject(aValue);
	}

	public void createNewObject(ObjectBase aDb) throws Exception {
		Connection conn = null;
		try {
			conn = aDb.getConnPool().getConnection();
			this.createNewObject(aDb, conn);
		} finally {
			if (conn != null) {
				aDb.getConnPool().freeConnection(conn);
			}
		}
	}

	public void createNewObject(ObjectBase aDb, Connection aConn) throws Exception {
		this.obj = CreateNewObject(aDb, aConn, this.getDeclareType(), this.isPolymorphic(), this.getMasterObject(), (FieldObject) this);
	}

	public static Clasz CreateNewObject(ObjectBase aDb, Connection aConn, String aType, boolean aPolymorphic, Clasz aMaster, FieldObject aChildField) throws Exception {
		Clasz obj = null;
		Class memberClass = Class.forName(aType);
		if (memberClass != Clasz.class) { // if only this is not a recursive field, then we populate it
			Class effectiveClass = memberClass;
			if (aPolymorphic) {
				//effectiveClass = ObjectBase.getPolymorphicClass(aConn, aMaster, aChildField );
				effectiveClass = ObjectBase.GetEffectiveClass(aConn, aMaster, aChildField );
			}
			obj = Clasz.CreateObject(aDb, aConn, effectiveClass);
		}
		return(obj);
	}

	public static Class GetEffectiveClass(Connection aConn, String aType, boolean aPolymorphic, Clasz aMaster, FieldObject aChildField) throws Exception {
		Class effectiveClass = Class.forName(aType);
		if (effectiveClass != Clasz.class) { // if only this is not a recursive field, then we populate it
			if (aPolymorphic) {
				//effectiveClass = ObjectBase.getPolymorphicClass(aConn, aMaster, aChildField);
				effectiveClass = ObjectBase.GetEffectiveClass(aConn, aMaster, aChildField);
			}
		}
		return(effectiveClass);
	}

	/*
	@Deprecated
	public boolean isLookup(Connection aConn) throws Exception {
		boolean result = false;
		boolean isLookupClass = false;

		Class effectiveClass = GetEffectiveClass(aConn, this.getDeclareType(), this.isPolymorphic(), this.getMasterObject(), (FieldObject) this);
		if (Lookup.class.isAssignableFrom(effectiveClass)) {
			isLookupClass = true;
		}

		if (this.lookup() && isLookupClass) {
			result = true;
		}

		return(result);
	}
	*/

	public boolean isLookup(Connection aConn) throws Exception {
		boolean result = false;

		Class effectiveClass = GetEffectiveClass(aConn, this.getDeclareType(), this.isPolymorphic(), this.getMasterObject(), (FieldObject) this);
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
	public String getValueStr() throws Exception {
		return(this.getValueObj().getValueStr());
	}

	@Override
	public String getValueStr(Connection aConn) throws Exception {
		return(this.getValueObj(aConn).getValueStr());
	}

	@Deprecated
	@Override
	public Clasz getValueObj(ObjectBase aDb) throws Exception {
		Connection conn = null;
		Clasz result = null;
		try {
			conn = aDb.getConnPool().getConnection();
			result = this.getValueObj(conn);
		} finally {
			if (conn != null) {
				aDb.getConnPool().freeConnection(conn);
			}
		}
		return(result);
	}

	@Override
	public Clasz getValueObj(Connection aConn) throws Exception {
		if (this.getFetchStatus() == FetchStatus.SOF && this.isModified() == false) {
			if (this.isInline()) {
				if (obj == null) {
					this.createNewObject(this.getMasterObject().getDb(), aConn);
					Clasz.PopulateInlineField(this.getMasterObject(), this, this.getFieldName(), "tree");
				}
			} else {
				this.fetch(aConn);
			}
		}
		return(obj);
	}

	@Deprecated
	@Override
	public Clasz getValueObj() throws Exception {
		if (this.getFetchStatus() == FetchStatus.SOF && this.isModified() == false && this.isPrefetch() == false) {
			this.getValueObj(this.getMasterObject().getDb());
		}
		return(obj);
	}

	public Clasz getObj() {
		return(this.obj);
	}

	public void setObj(Clasz aObj) {
		this.obj = aObj;
	}

	public void setValueObject(Clasz aObject) throws Exception {
		if (aObject == null && this.obj == null) {
			return;
		}

		if (aObject == this.obj) {
			return;
		}

		this.setModified(true);
		this.obj = aObject;
	}

	@Override
	public void copyValue(Field aSourceField) throws Exception {
		if ((aSourceField instanceof FieldObject) == false)  {
			throw new Hinderance("Error, cannot copy values of difference type, target field: " + this.getFieldName() + ", type: " + this.getFieldType().toString() + ", source: " + aSourceField.getFieldName() + ", type: " + aSourceField.getFieldType().toString());
		}

		Clasz sourceObj = ((FieldObject) aSourceField).getObj();
		this.getObj().copyAllFieldWithoutModifiedState(sourceObj);
	}

	@Override
	public void cloneField(Field aSourceField) throws Exception {
		if ((aSourceField instanceof FieldObject) == false)  {
			throw new Hinderance("Error, cannot copy values of difference type, target field: " + this.getFieldName() + ", source: " + aSourceField.getFieldName());
		}

		Clasz sourceObj = ((FieldObject) aSourceField).getObj();
		if (sourceObj != null) {
			this.getObj().copyAllFieldWithModifiedState(sourceObj);
		} else {
			this.setObj(null);
		}
	}

	public static int compare(FieldObject aLeft, FieldObject aRight) throws Exception {
		int result = 0;
		Clasz left = aLeft.getObj();
		Clasz right = aRight.getObj();

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

	@Override
	public int compareTo(Object aRight) {
		int result = 0;
		try {
			result = FieldObject.compare(this, (FieldObject) aRight);
		} catch (Exception ex) {
			App.logEror(ex);
		}
		return(result);
	}

	public Field getField(String aFieldName) throws Exception {
		return(this.getObj().getField(aFieldName));
	}

	public void fetch() throws Exception {
		Connection conn = this.getMasterObject().getDb().getConnPool().getConnection();
		try {
			this.fetch(conn);
		} finally {
			if (conn != null) {
				this.getMasterObject().getDb().getConnPool().freeConnection(conn);
			}
		}		
	}

	public void fetch(Connection aConn) throws Exception {
		Class memberClass = Class.forName(this.getDeclareType());
		if (memberClass != Clasz.class) { // if only this is not a recursive field, then we populate it
			Class effectiveClass = memberClass;
			if (this.isPolymorphic()) {
				//effectiveClass = ObjectBase.getPolymorphicClass(aConn, this.getMasterObject(), (FieldObject) this);
				effectiveClass = ObjectBase.GetEffectiveClass(aConn, this.getMasterObject(), (FieldObject) this);
			}

			Clasz fetchMember = FetchMemberOfObject(aConn, (FieldObject) this, this.getMasterObject(), effectiveClass);
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

	public static Clasz FetchMemberOfObject(Connection aConn, FieldObject aField, Clasz aMasterObj, Class aMemberClass) throws Exception {
		PreparedStatement stmt = null;
		ResultSet rset = null;
		try {
			String strSql = "select " + aField.getFieldName() + " from " + aMasterObj.getIvTableName() + " where " + aMasterObj.getPkName() + " = " + aMasterObj.getObjectId();
			stmt = aConn.prepareStatement(strSql);
			rset = stmt.executeQuery();
			if (rset.next()) {
				long pk = rset.getLong(1);
				if (rset.wasNull() == false) {
					Clasz fetchMember = Clasz.fetchObjectByPk(aMasterObj.getDb(), aConn, aMemberClass, pk);
					return (fetchMember);
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

	@Deprecated
	@Override
	public void setValueStr(String aValue) throws Exception {
		this.getValueObj().setValueStr(aValue);
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

	public static boolean IsAbstract(FieldObject aFieldObject) throws Exception {
		Class fieldClass = Class.forName(aFieldObject.getDeclareType());
		return(IsAbstract(fieldClass));
	}

	public static boolean IsAbstract(Class fieldClass) throws Exception {
		boolean result = false;
		if (Modifier.isAbstract(fieldClass.getModifiers())) {
			result = true;
		}
		return(result);
	}

	public Class getObjectClass(Connection aConn) throws Exception {
		Clasz clasz = this.getValueObj(aConn);
		Class result = clasz.getClass();
		return(result);
	}

	public Class getObjectClass() throws Exception {
		Class result = this.getObj().getClass();
		return(result);
	}

}