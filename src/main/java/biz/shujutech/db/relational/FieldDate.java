package biz.shujutech.db.relational;

import biz.shujutech.base.DateAndTime;
import biz.shujutech.base.Connection;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class FieldDate extends Field {
	private DateTime valueDate;

	public FieldDate(String aName) {
		this.setFieldName(aName);
		this.setFieldType(FieldType.DATE);
	}

	public FieldDate(String aName, DateTime aValue) throws Exception {
		this(aName);
		this.setValueDate(aValue);
	}

	public DateTime getValueDate() {
		return valueDate;
	}

	public void setValueDate(DateTime valueDate) throws Exception {
		this.setModified(true);
		this.valueDate = DateAndTime.SetZeroTime(valueDate); 
	}

	@Override
	public void setValue(Object value) throws Exception {
		this.setValueDate((DateTime) value);
	}

	@Override
	public Object getValueObj() {
		return(valueDate);
	}

	@Override
	public Object getValueObj(Connection aConn) {
		return(valueDate);
	}

	@Override
	public synchronized String getValueStr() throws Exception {
		String result = "";
		if (this.getValueDate() != null) {
			result = DateAndTime.FormatDisplayNoTime(this.getValueDate());
		}
		return(result);
	}

	@Override
	public synchronized void setValueStr(String valueStr) throws Exception {
		this.setModified(true);
		if (valueStr == null || valueStr.isEmpty() || valueStr.trim().equals("--")) {
			this.setValueDate(null);
		} else {
			this.setValueDate(DateAndTime.StrToDateTime(valueStr, DateAndTime.FORMAT_DISPLAY_NO_TIME));
		}
	}

	public static boolean IsTimeZero(DateTime aDate) throws Exception {
		String TIME_ONLY = "HH:mm:ss";
		boolean result = false;
		DateTimeFormatter dtf = DateTimeFormat.forPattern(TIME_ONLY);
		String strTime = dtf.print(aDate);
		if (strTime.equals("00:00:00")) result = true;
		return(result);
	}
	
}
