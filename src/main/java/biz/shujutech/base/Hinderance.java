/*
 * Decided to create our own Throwable for our own application, the 
 * architectural for this is to enable classes coded by ourselves to 
 * throw their exceptions all the time. This classes are catch and 
 * thrown up to the root calling class that will then use the method 
 * ExToStr() to display the error messages. Notice all the messages 
 * from the error stack are displayed to facilitate error diagnosing. 
 * To preserve the stack messages, this classes store the messages in
 * a private variable.
 *
 */

package biz.shujutech.base;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Hinderance extends Exception {
	private ConcurrentLinkedQueue<String> exQueue = new ConcurrentLinkedQueue<>();	// to preserve messages from the error stack
	private String javaExStr = "";
	private static String lineFeed;

	public Hinderance(String aMsg) {
		Hinderance.lineFeed = System.getProperty("line.separator");
		Exception ex = new Exception();
		ex.setStackTrace(Thread.currentThread().getStackTrace());
		this.exQueue.add(aMsg + Hinderance.lineFeed + Hinderance.ExToStr(ex));
	}

	public Hinderance(Throwable aEx) {
		Hinderance.lineFeed = System.getProperty("line.separator");
		if (aEx.getClass().getName().equals(this.getClass().getName())) {
			Hinderance Ex = (Hinderance) aEx;
			this.javaExStr = Ex.getJavaExStr();
			this.exQueue = ((Hinderance) aEx).getMsgQueue();
		} else {
			StringWriter sWriter = new StringWriter();
			PrintWriter pWriter = new PrintWriter(sWriter);
			aEx.printStackTrace(pWriter);
			pWriter.flush();
			this.exQueue.add(sWriter.toString());
		}
	}

	public Hinderance(Throwable aEx, String aMsg) {
		Hinderance.lineFeed = System.getProperty("line.separator");
		if (aEx.getClass().getName().equals(this.getClass().getName())) {
			this.javaExStr = ((Hinderance) aEx).getJavaExStr();
			this.exQueue = ((Hinderance) aEx).getMsgQueue();
			this.exQueue.add(aMsg.concat(Hinderance.lineFeed));
		} else {
			StringWriter sWriter = new StringWriter();
			PrintWriter pWriter = new PrintWriter(sWriter);
			aEx.printStackTrace(pWriter);
			pWriter.flush();
			String tmpStr = sWriter.toString();
			tmpStr = tmpStr.replaceAll("\t", "");
			this.javaExStr = tmpStr;
			this.exQueue.add(aMsg.concat(Hinderance.lineFeed));
		}
	}

	public ConcurrentLinkedQueue<String> getMsgQueue() {
		return(this.exQueue);
	}

	public String getJavaExStr() {
		return(this.javaExStr);
	}

	public String getExMsg() {
		String result = "";
		//while(this.exQueue.isEmpty() == false) {
		//	result += this.exQueue.poll();
		//}
		Object[] aryObj = this.exQueue.toArray();
		for(Object eachObj : aryObj) {
			result += (String) eachObj;
		}
		result += this.javaExStr;
		return(result);
	}

	public static String ExToStr(Throwable aEx) {
		if (aEx.getClass().getName().equals(new Hinderance(new Throwable()).getClass().getName())) {
			Hinderance Ex = (Hinderance) aEx;
			return Ex.getExMsg();
		} else {
			Hinderance Ex = new Hinderance(aEx);
			return Ex.getExMsg();
		}
	}

	public static String ExToStr(Throwable aEx, String aMsg) {
		if (aEx.getClass().getName().equals(new Hinderance(new Throwable()).getClass().getName())) {
			Hinderance Ex = (Hinderance) aEx;
			return aMsg.concat(Hinderance.lineFeed).concat(Ex.getExMsg());
		} else {
			Hinderance Ex = new Hinderance(aEx, aMsg);
			return Ex.getExMsg();
		}
	}

	public static String ExStackToStr(Throwable aEx, String aMsg) {
		String ReturnStr = aMsg.concat(Hinderance.lineFeed);

		StringWriter sWriter = new StringWriter();
		PrintWriter pWriter = new PrintWriter(sWriter);
		aEx.printStackTrace(pWriter);
		pWriter.flush();
		String strQueue = sWriter.toString();
		StringTokenizer STkz = new StringTokenizer(strQueue, Hinderance.lineFeed);
		while (STkz.hasMoreTokens()) {
			ReturnStr.concat(STkz.nextToken().trim());
		}

		return ReturnStr;
	}

	public ConcurrentLinkedQueue<String> getExQueue() {
		return this.exQueue;
	}
}
