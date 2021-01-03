package biz.shujutech.example;

import biz.shujutech.base.DateAndTime;
import biz.shujutech.db.object.Clasz;
import biz.shujutech.db.relational.FieldType;
import biz.shujutech.reflect.ReflectField;
import org.joda.time.DateTime;

public class LeaveForm extends Clasz {

	@ReflectField(type=FieldType.STRING, size=16, displayPosition=10) public static String LeaveType; // is leave type actually, using leave type as the lookup
	@ReflectField(type=FieldType.DATETIME, displayPosition=30) public static String LeaveStart;
	@ReflectField(type=FieldType.DATETIME, displayPosition=40) public static String LeaveEnd;
	@ReflectField(type=FieldType.BOOLEAN, displayPosition=65) public static String HalfDay;
	@ReflectField(type=FieldType.DATETIME, displayPosition=50) public static String DateApplied;
	@ReflectField(type=FieldType.DATETIME, displayPosition=60) public static String DateApproved;
	@ReflectField(type=FieldType.STRING, size=32, displayPosition=70) public static String ApprovedBy;
	@ReflectField(type=FieldType.STRING, size=32, displayPosition=80) public static String AttachFileName;
	@ReflectField(type=FieldType.BASE64, displayPosition=85) public static String Attachment;
	@ReflectField(type=FieldType.HTML, size=255, displayPosition=90) public static String Remark;


	public String getLeaveType() throws Exception {
		return(this.getValueStr(LeaveType));
	}

	public void setLeaveType(String aLeaveType) throws Exception {
		this.setValueStr(LeaveType, aLeaveType);
	}

	public DateTime getLeaveStart() throws Exception {
		return((DateTime) this.getValueDateTime(LeaveStart));
	}

	public void setLeaveStart(DateTime aLeaveStart) throws Exception {
		this.setValueDateTime(LeaveStart, aLeaveStart);
	}

	public DateTime getLeaveEnd() throws Exception {
		return((DateTime) this.getValueDateTime(LeaveEnd));
	}

	public void setLeaveEnd(DateTime aLeaveEnd) throws Exception {
		this.setValueDateTime(LeaveEnd, aLeaveEnd);
	}

	public boolean getHalfDay() throws Exception {
		return(this.getValueBoolean(HalfDay));
	}

	public DateTime getDateApplied() throws Exception {
		return((DateTime) this.getValueDateTime(DateApplied));
	}

	public void setDateApplied(DateTime aDateApplied) throws Exception {
		this.setValueDateTime(DateApplied, aDateApplied);
	}

	public DateTime getDateApproved() throws Exception {
		return((DateTime) this.getValueDateTime(DateApproved));
	}

	public void setDateApproved(DateTime aDateApproved) throws Exception {
		this.setValueDateTime(DateApproved, aDateApproved);
	}

	public Float getDaysAppliedWithoutHoliday() throws Exception {
		Float result = 0.0f;
		if (this.getHalfDay()) {
			result = 0.5f;
		} else {
			//App.logInfo("Start: " + this.getLeaveStart() + ", end: " + this.getLeaveEnd());
			DateTime startDate = this.getLeaveStart().withTimeAtStartOfDay();
			DateTime endDate = this.getLeaveEnd().withTimeAtStartOfDay();
			for (DateTime date = startDate; DateAndTime.IsBeforeOrEqual(date, endDate); date = date.plusDays(1)) {
				result = result + 1.0f;
			}
		}
		return(result);
	}

	public boolean IsLeaveDateSpanYear() throws Exception {
		boolean result = false;
		if (this.getLeaveStart().getYear() != this.getLeaveEnd().getYear()) {
			result = true;
		}
		return(result);
	}

}