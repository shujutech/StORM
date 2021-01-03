package biz.shujutech.db.relational;

import biz.shujutech.base.Connection;

public class FieldStr extends Field {
	private String valueString;

	public FieldStr(String aName, int aSize) {
		this.setFieldName(aName);
		this.setFieldType(FieldType.STRING);
		this.setFieldSize(aSize);
	}

	@Override
	public String getValueStr() throws Exception {
		String result = valueString;
		if (result == null) {
			result = "";
		}
		return(result);
	}

	public String getValueStrNull() {
		String result = valueString;
		return(result);
	}

	@Override
	public void setValueStr(String valueString) throws Exception {
		this.setModified(true); 
		this.valueString = valueString;
	}

	@Override
	public void setValue(Object value) throws Exception {
		this.setValueStr((String) value); // must call this set method so setModified is call
	}

	@Override
	public Object getValueObj() throws Exception {
		return(valueString);
	}

	@Override
	public Object getValueObj(Connection aConn) throws Exception {
		return(valueString);
	}
	
}
