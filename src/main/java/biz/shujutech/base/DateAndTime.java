package biz.shujutech.base;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class DateAndTime {
	public static final String FORMAT_DISPLAY_WITH_TIME = "dd-MMM-yyyy HH:mm:ss";
	public static final String FORMAT_DISPLAY_NO_TIME = "dd-MMM-yyyy";
	public static final String FORMAT_DAY_END = "dd-MMM-yyyy HH:mm:ss.SSS";
	public static final String FORMAT_JDBC_TIMESTAMP = "yyyy-MM-dd HH:mm:ss";
	public static final String FORMAT_JDBC_DATE = "yyyy-MM-dd";
	public static final String FORMAT_TIMEZONE = "dd-MMM-yyyy HH:mm:ss Z";
	public static final String FORMAT_COMPARE_NO_TIME = "yyyyMMdd";
	public static final String FORMAT_MONTH = "MMM, yyyy";
	public static final String FORMAT_FOR_SORT_DATETIME = "yyyyMMddHHmmss";
	public static final String FORMAT_FOR_SORT_DATE= "yyyyMMdd";

	public DateTime CreateDateAndTime(long aLong) {
		java.util.Date date = new java.util.Date(aLong);
		return(new DateTime(date));
	}

	public static DateTime MinusDay(DateTime aDateTime, int aDay) {
		if (aDateTime == null) return(null);
		DateTime result = aDateTime.minusDays(aDay);
		return(result);
	}

	public static String FormatForJdbcTimestamp(DateTime aDateTime) throws Exception {
		if (aDateTime == null) return(null);
		DateTimeFormatter dtf = DateTimeFormat.forPattern(FORMAT_JDBC_TIMESTAMP);
		String result = dtf.print(aDateTime);
		return(result);
	}

	public static String FormatForJdbcDate(DateTime aDateTime) throws Exception {
		if (aDateTime == null) return(null);
		DateTimeFormatter dtf = DateTimeFormat.forPattern(FORMAT_JDBC_DATE);
		String result = dtf.print(aDateTime);
		return(result);
	}

	public static String FormatForDisplayWithTime(DateTime aDateTime) throws Exception {
		if (aDateTime == null) return(null);
		DateTimeFormatter dtf = DateTimeFormat.forPattern(FORMAT_DISPLAY_WITH_TIME);
		String result = dtf.print(aDateTime);
		return(result);
	}

	public static String FormatForTimezone(DateTime aDateTime) throws Exception {
		if (aDateTime == null) return(null);
		DateTimeFormatter dtf = DateTimeFormat.forPattern(FORMAT_TIMEZONE);
		String result = dtf.print(aDateTime);
		return(result);
	}

	public static String FormatDisplayNoTime(DateTime aDateTime) throws Exception {
		if (aDateTime == null) return(null);
		DateTimeFormatter dtf = DateTimeFormat.forPattern(FORMAT_DISPLAY_NO_TIME);
		String result = dtf.print(aDateTime); // this will return current date if aDateTime is null, check again!! idiot joda
		return(result);
	}

	private static String FormatForSort(DateTime aDateTime, String aSortFormat) throws Exception {
		if (aDateTime == null) return("");
		DateTimeFormatter dtf = DateTimeFormat.forPattern(aSortFormat);
		String result = dtf.print(aDateTime);
		return(result);
	}

	public static String FormatDateTimeForSort(String aDateTime) throws Exception {
		if (aDateTime == null) return("");
		DateTime dateTime = DisplayStrToDateTime(aDateTime);
		String result = FormatForSort(dateTime, FORMAT_FOR_SORT_DATETIME);
		return(result);
	}

	public static String FormatDateForSort(String aDateTime) throws Exception {
		if (aDateTime == null) return("");
		DateTime dateTime = DisplayStrToDateTime(aDateTime);
		String result = FormatForSort(dateTime, FORMAT_FOR_SORT_DATE);
		return(result);
	}

	public static String FormatDateForSort(DateTime aDateTime) throws Exception {
		if (aDateTime == null) return("");
		String result = FormatForSort(aDateTime, FORMAT_FOR_SORT_DATE);
		return(result);
	}

	public static DateTime StrToDateTime(String aStr, String aFormat) throws Exception {
		if (aStr == null || aStr.isEmpty()) return(null);
		if (aFormat.equals(FORMAT_DISPLAY_NO_TIME)) {
			if (aStr.length() > aFormat.length()) {
				if (aStr.contains(" ")) { // this is for macos, its Sep is Sept
					aStr = aStr.substring(0, aStr.indexOf(" "));
				} else {
					App.logWarn("Removing time from datetime of string: " + aStr + ", is not possible");
				}
			}
		}
		DateTimeFormatter formatter = DateTimeFormat.forPattern(aFormat);
		DateTime result = formatter.parseDateTime(aStr);		
		return(result);
	}

	public static DateTime DisplayStrToDateTime(String aStr) throws Exception {
		if (aStr == null || aStr.isEmpty()) return(null);
		if (aStr.length() < FORMAT_DISPLAY_WITH_TIME.length()) {
			return(StrToDateTime(aStr, FORMAT_DISPLAY_NO_TIME));
		} else {
			if (aStr.length() > FORMAT_DISPLAY_WITH_TIME.length()) {
				return(StrToDateTime(aStr, FORMAT_TIMEZONE));
			} else {
				return(StrToDateTime(aStr, FORMAT_DISPLAY_WITH_TIME));
			}
		}
	}

	public static DateTime GetMonthStart(DateTime aDateTime) throws Exception {
		if (aDateTime == null) return(null);
		DateTime monthStart = aDateTime.dayOfMonth().withMinimumValue();
		return(monthStart);
	}

	public static DateTime GetMonthEnd(DateTime aDateTime) throws Exception {
		if (aDateTime == null) return(null);
		DateTime monthEnd = aDateTime.dayOfMonth().withMaximumValue();
		return(monthEnd);
	}

	public static Integer GetTotalDaysInMonth(DateTime aDateTime) throws Exception {
		DateTime monthEnd = GetMonthEnd(aDateTime);
		Integer result = monthEnd.getDayOfMonth();
		return(result);
	}

	public static DateTime GetDayEnd(DateTime aDateTime) throws Exception {
		return(SetTime(aDateTime, "23:59:59.999"));
	}

	public static DateTime GetDayStart(DateTime aDateTime) throws Exception {
		return(SetTime(aDateTime, "00:00:00.000"));
	}

	public static DateTime SetTime(DateTime aDateTime, String aTime) throws Exception {
		if (aDateTime == null) return(null);
		String strDateTime = FormatDisplayNoTime(aDateTime);
		DateTime result = StrToDateTime(strDateTime + " " + aTime, FORMAT_DAY_END);
		return(result);
	}

	public static DateTime SetZeroTime(DateTime aDateTime) throws Exception {
		if (aDateTime == null) return(null);
		String strDateTime = FormatDisplayNoTime(aDateTime);
		DateTime result = StrToDateTime(strDateTime, FORMAT_DISPLAY_NO_TIME);
		return(result);
	}

	public static boolean IsEqualWithoutTime(DateTime aDateLeft, DateTime aDateRight) throws Exception {
		return (DateTimeComparator.getDateOnlyInstance().compare(aDateLeft, aDateRight) == 0);
	}

	public static boolean IsDifferentMonthAndYear(DateTime aDateLeft, DateTime aDateRight) throws Exception {
		boolean result = true;
		if (aDateLeft.year().get() == aDateRight.year().get() && 
		aDateLeft.monthOfYear().get() == aDateRight.monthOfYear().get()) {
			result = false;
		}
		return(result);
	}

	public static boolean IsFullMonth(DateTime aDateStart, DateTime aDateEnd) throws Exception {
		boolean result = false;
		DateTime dateStart = SetZeroTime(aDateStart);
		DateTime dateEnd = SetZeroTime(aDateEnd);
		if (dateStart.equals(GetMonthStart(dateStart)) && dateEnd.equals(GetMonthEnd(dateEnd))) {
			result = true;
		}
		return(result);
	}

	public static DateTime CreateDateTime(int aYear, int aMonth, int aDay) throws Exception {
		DateTime dateOfMonth = new DateTime((new GregorianCalendar(aYear, aMonth - 1, aDay)).getTime().getTime()); // first day of the month, the month starts from 0
		return(SetZeroTime(dateOfMonth));
	}

	public static DateTime CreateDateTime(int aYear, int aMonth, int aDay, int a24Hour, int aMin, int aSec) throws Exception {
		DateTime result = CreateDateTime(aYear, aMonth, aDay);
		result = result.withTime(a24Hour, aMin, aSec, 0);
		return(result);
	}

	public static String FormatForCompareNoTime(DateTime aDateTime) throws Exception {
		if (aDateTime == null) return(null);
		DateTimeFormatter dtf = DateTimeFormat.forPattern(FORMAT_COMPARE_NO_TIME);
		String result = dtf.print(aDateTime);
		return(result);
	}

	public static String FormatForMonth(DateTime aDateTime) throws Exception {
		if (aDateTime == null) return(null);
		DateTimeFormatter dtf = DateTimeFormat.forPattern(FORMAT_MONTH);
		String result = dtf.print(aDateTime);
		return(result);
	}

	public static boolean IsBetween(DateTime aTarget, DateTime aFrom, DateTime aTo) throws Exception {
		boolean result = false;
		if ((aTarget.isEqual(aFrom) || aTarget.isAfter(aFrom))
		&& (aTarget.isEqual(aTo) || aTarget.isBefore(aTo))
		) {
			result = true;
		}
		return(result);
	}

	public static boolean IsOverlap(DateTime aStart, DateTime aEnd, DateTime aFrom, DateTime aTo) throws Exception {
		boolean result = false;
		if (DateAndTime.IsBetween(aStart, aFrom, aTo) || DateAndTime.IsBetween(aEnd, aFrom, aTo)
		|| DateAndTime.IsBetween(aFrom, aStart, aEnd)) {
			result = true;
		}
		return(result);
	}

	public static String AsString(DateTime aDateTime, String aFormat) throws Exception {
		if (aDateTime == null) return(null);
		DateTimeFormatter dtf = DateTimeFormat.forPattern(aFormat);
		String result = dtf.print(aDateTime);
		return(result);
	}

	public static DateTime GetYearStart(int aYear) throws Exception {
		DateTime result = CreateDateTime(aYear, 1, 1);
		return(result);
	}

	public static DateTime GetYearEnd(int aYear) throws Exception {
		DateTime result = CreateDateTime(aYear, 12, 31);
		return(result);
	}

	public static boolean IsValidYearMonth(Integer aYear, Integer aMonth) {
		String dateString = aYear.toString() + "-" + aMonth.toString() + "-" + "01";
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		sdf.setLenient(false);
		try {
			sdf.parse(dateString);
			return true;
		} catch(ParseException ex) {
			return false;
		}
	}

	public static boolean IsAfterOrEqual(DateTime aLeftDate, DateTime aRightDate) {
		return aLeftDate.isAfter(aRightDate) || aLeftDate.equals(aRightDate);
	}
	
	public static boolean IsBeforeOrEqual(DateTime aLeftDate, DateTime aRightDate) {
		return aLeftDate.isBefore(aRightDate) || aLeftDate.equals(aRightDate);
	}

	public static boolean DateInRange(DateTime aRangeStart, DateTime aRangeEnd, DateTime aStartDate) throws Exception {
		return(DateInRange(aRangeStart, aRangeEnd, aStartDate, aStartDate));
	}

	public static boolean DateInRange(DateTime aRangeStart, DateTime aRangeEnd, DateTime aStartDate, DateTime aEndDate) throws Exception {
		return (IsAfterOrEqual(aStartDate, aRangeStart)  && IsBeforeOrEqual(aStartDate, aRangeEnd)) || ((IsAfterOrEqual(aRangeStart, aStartDate) && IsBeforeOrEqual(aRangeStart, aEndDate)));
	}
}

