package biz.shujutech.db.relational;

import biz.shujutech.base.Connection;

public class FieldFloat extends Field {
	private Float valueFloat;

	public FieldFloat(String aName) {
		this.setDbFieldName(aName);
		this.setDbFieldType(FieldType.FLOAT);
	}

	public Float getValueFloat() {
		return valueFloat;
	}

	public void setValueFloat(Float valueFloat) {
		this.setModified(true);
		this.valueFloat = valueFloat;
	}

	@Override
	public void setValue(Object value) {
		this.setModified(true);
		this.setValueFloat((Float) value);
	}

	/*
	@Override
	public Object getValueObj() {
		return(valueFloat);
	}
	*/

	@Override
	public Object getValueObj(Connection aConn) {
		return(valueFloat);
	}

	@Override
	public String getValueStr() {
		String result = "";
		if (this.getValueFloat() != null) {
			result = String.valueOf(this.getValueFloat());
		}
		return(result);
	}

	@Override
	public void setValueStr(String valueStr) throws Exception {
		if (valueStr == null || valueStr.isEmpty()) {
			this.setValueFloat(null);
		} else {
			this.setValueFloat(Float.valueOf(valueStr));
		}
	}
	
	
}