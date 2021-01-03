package biz.shujutech.db.relational;

import biz.shujutech.base.Connection;

public class FieldBoolean extends Field {
	private Boolean valueBoolean;

	public FieldBoolean(String aName) {
		this.setFieldName(aName);
		this.setFieldType(FieldType.BOOLEAN);
	}

	public Boolean getValueBoolean() {
		return valueBoolean;
	}

	public void setValueBoolean(Boolean valueBoolean) {
		this.setModified(true); // remember for every set method, must set this modify flag
		this.valueBoolean = valueBoolean;
	}

	@Override
	public void setValue(Object value) {
		this.setValueBoolean((Boolean) value);
	}

	@Override
	public Object getValueObj() {
		return(valueBoolean);
	}

	@Override
	public Object getValueObj(Connection aConn) {
		return(valueBoolean);
	}

	public String getValueStr() {
		String result = "";
		if (this.getValueBoolean() != null) {
			result = String.valueOf(this.getValueBoolean());
		}
		return(result);
	}

	@Override
	public void setValueStr(String valueStr) throws Exception {
		if (valueStr == null || valueStr.isEmpty()) {
			this.setValueBoolean(null);
		} else {
			this.setModified(true);
			if (valueStr.trim().toLowerCase().equals("true")) { 
				this.setValueBoolean(true);
			} else if (valueStr.trim().toLowerCase().equals("false")) { 
				this.setValueBoolean(false);
			} else if (valueStr.equals("1")) { 
				this.setValueBoolean(true);
			} else if (valueStr.equals("0")) { 
				this.setValueBoolean(false);
			} else {
				throw new Exception("Invalid value: " + valueStr + ", for field of boolean type, field: " + this.getFieldName().toUpperCase());
			}
		}

	}
}
