package biz.shujutech.base;

import java.util.logging.LogRecord;

public class LogFormatter extends java.util.logging.Formatter {
	public LogFormatter() throws Exception {
	}

	@Override
	public String format(LogRecord record) {
		return(record.getMessage());
	}

}
