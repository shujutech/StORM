package biz.shujutech.base;

import java.lang.reflect.Array;

public class Base {
	public int threadId = 0;

	public int getThreadId() {
		return threadId;
	}

	public void setThreadId(int threadId) {
		this.threadId = threadId;
	}

	public void setupApp(String args[]) throws Exception {
		String strPropFile = "";
		if (Array.getLength(args) != 1) {
			System.err.println("Usage: java -jar Table <PropertyFile>"); 
			if (strPropFile.isEmpty()) {
				strPropFile = "shujutech.properties";
			}
			App.Setup(strPropFile);
		} else {
			App.Setup(args[0]);
		} 
	}

}
