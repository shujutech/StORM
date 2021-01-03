package biz.shujutech.db.relational;

import biz.shujutech.base.Connection;

public class FieldLong extends Field {

	private Long valueLong;

	public FieldLong(String aName) {
		this.setFieldName(aName);
		this.setFieldType(FieldType.LONG);
	}

	public FieldLong(String aName, Long aValue) {
		this(aName);
		this.setValueLong(aValue);
	}

	public Long getValueLong() {
		return valueLong;
	}

	public void setValueLong(Long valueLong) {
		this.setModified(true);
		this.valueLong = valueLong;
	}

	@Override
	public void setValue(Object value) {
		this.setModified(true);
		this.setValueLong((Long) value);
	}

	@Override
	public Object getValueObj() {
		return(valueLong);
	}

	@Override
	public Object getValueObj(Connection aConn) {
		return(valueLong);
	}

	public String getValueStr() {
		String result = "";
		if (this.getValueLong() != null) {
			result = String.valueOf(this.getValueLong());
		}
		return(result);
	}

	@Override
	public void setValueStr(String valueStr) throws Exception {
		if (valueStr == null || valueStr.isEmpty()) {
			this.setValueLong(null);
		} else {
			this.setValueLong(Long.valueOf(valueStr));
		}
	}
	
}
