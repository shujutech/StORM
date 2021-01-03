package biz.shujutech.db.relational;

import biz.shujutech.base.Connection;

public class FieldInt extends Field {
	private Integer valueInteger;

	public FieldInt(String aName) {
		this.setFieldName(aName);
		this.setFieldType(FieldType.INTEGER);
	}

	public Integer getValueInt() {
		return valueInteger;
	}

	public void setValueInt(Integer valueInteger) {
		this.setModified(true);
		this.valueInteger = valueInteger;
	}

	@Override
	public void setValue(Object value) {
		this.setModified(true);
		this.setValueInt((Integer) value);
	}

	@Override
	public Object getValueObj() {
		return(valueInteger);
	}

	@Override
	public Object getValueObj(Connection aConn) {
		return(valueInteger);
	}

	@Override
	public String getValueStr() {
		String result = "";
		if (this.getValueInt() != null) {
			result = String.valueOf(this.getValueInt());
		}
		return(result);
	}

	@Override
	public void setValueStr(String valueStr) throws Exception {
		if (valueStr == null || valueStr.isEmpty()) {
			this.setValueInt(null);
		} else {
			this.setValueInt(Integer.valueOf(valueStr));
		}
	}
	
	
}
