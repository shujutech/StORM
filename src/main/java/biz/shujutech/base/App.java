/**
 * Class that implement logging, properties reading and writting. 
 * Will create properties and log file * automatically if it doesn't exist
 * (i.e. App.log and App.properties). Log can also automatically switch
 * to new file when it cyclically expires.  Minimum class needed: 
 * LogFormatter.java
 * 
 */

package biz.shujutech.base;

import biz.shujutech.util.CryptoRsa;
import biz.shujutech.util.Generic;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.Calendar;
import org.joda.time.DateTime;
import java.util.GregorianCalendar;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class App {
	public static final String PROPERTY_APP = "shujutechapp.properties";

	public static final String OS = System.getProperty("os.name").toLowerCase();
	public static final String DATE_STYLE_STR = "ddMMMyyyy HH:mm:ss";
	public static final String ENCRYPT_PREFIX = "ENCRYPT:";

	public static Logger AppLogger = null; // should not be final, we init it later
	public static DateTimeFormatter DateStyleObj;
	public static String PropertyFileName = "App.properties";
	public static Properties AppProps = null;
	public static FileHandler LogHandler = null;
	public static String LineFeed = "\n";

	public static String KeyRawLogName = "Systm.logName";
	public static String KeyLogLocation = "Systm.logLocation";
	public static String KeyLogOnConsole = "Systm.logOnConsole";
	public static String KeyLogThreadId = "Systm.logThreadId";
	public static String KeyLogWrapLen = "Systm.logWrapLen";
	public static String KeyLogSwitchLocation = "Systm.logSwitchLocation";
	public static String KeyMaxThread = "Systm.maxThread";

	public static String PropRawLogName = "App.[yyyyMM].log";
	public static String PropLogOnConsole = "true";
	public static boolean PropLogThreadId = true;
	public static int PropLogWrapLen = Integer.MAX_VALUE;

	public static String PropLogName = "";
	public static String PropLogNamePrefix = "";
	public static String PropLogNameDatePart = "";
	public static String PropLogNamePostfix= "";
	public static String SwitchDuration = "YEA";
	public static String PropMaxThread = "";
	public static DateTime SwitchDateBefore = new DateTime();
	public static DateTime SwitchDateAfter;
	public static boolean OnlyConsoleLog = false;

	public static LogFormatter CustomLogFormatter = null;
	public static FileInputStream propsInStream = null;
	public static int ThreadId = -1;

	public static NumberFormat NfThreadId = null;
	public static int MaxThreadIdDigit = 2;

	public static int MaxThread = 5;

	public static String KeyClassLogging = "Systm.logClass";
	public static ConcurrentHashMap<String, Level> ClassLogging = new ConcurrentHashMap<String, Level>();
	public static int MAX_CLASS_LOGGING = 5;

	public static Level JavaLogLevel = Level.ALL;

	public static final String ERROR_COMPLAIN = ", please contact: &nbsp<a href='mailto:shujutech@gmail.com'>shujutech@gmail.com</a>";
	public static final String CONTACT_US = "&nbsp <a href='mailto:shujutech@gmail.com'>shujutech@gmail.com</a>";
	public static final String PATH_JAR = NormalizePath(App.class.getProtectionDomain().getCodeSource().getLocation().getPath());
	public static final String PATH_DOCROOT = GetDocRoot(PATH_JAR);
	public static final String PATH_HTML = PATH_DOCROOT;
	public static final String PATH_WEBINF = PATH_DOCROOT + File.separator + "WEB-INF";
	public static String LogPathWebInf = PATH_WEBINF + File.separator +  "log";
	public static String ConfigPathWebInf = PATH_WEBINF + File.separator + "config";

	public static boolean AlreadySetup = false;
	public static int MAX_GET_CONNECTION_ATTEMPT = 3;
	public static long MAX_THREAD_OR_CONN_WAIT = 8000;

	public static void Setup() throws Exception {
		Setup(-1, App.PropertyFileName, false); 
	}

	public static void Setup(String aPropsFile) throws Exception {
		Setup(-1, aPropsFile, false); // constructor calling constructor
	}

	public static void Setup(String aPropsFile, boolean aOnlyConsoleLog) throws Exception {
		Setup(-1, aPropsFile, aOnlyConsoleLog); // constructor calling constructor
	}

	public static void Setup(int aThreadId, String aPropsFile) throws Exception {
		Setup(aThreadId, aPropsFile, false); // constructor calling constructor
	}

	public static void ShowStartupInfo(String strPropFile) throws Exception {
		String logLevel = App.GetValue("Systm.logLevel", "INFO");
		App.setLogLevel(logLevel);

		App.logInfo("Working directory: " + System.getProperty("user.dir"));
		App.logInfo("Configuration from: " + GetPropFullName(strPropFile));
		App.logInfo("Log file: " + GetLogFullName(App.PropLogName));
		App.logInfo("Log on console: " + App.PropLogOnConsole);
		App.logInfo("Log next switch at: " + App.SwitchDateAfter);
		App.logInfo("Maximum thread: " + GetMaxThread());

		for (Entry entry : App.ClassLogging.entrySet()) {
			// for each of the table in the inheritance tree
			String className = (String) entry.getKey();
			Level level = (Level) entry.getValue();
			App.logInfo("Class logging enable at class: " + className + ", log level: " + App.logLevel2Str(level));
		}

		App.logConf("OS name: " + System.getProperty("os.name"));
		App.logConf("OS architecture: " + System.getProperty("os.arch"));
		App.logConf("OS version: " + System.getProperty("os.version"));
		App.logConf("Java classpath: " + System.getProperty("java.class.path"));
	}

	public static void MakeDir(String aNewDir) {
		File directory = new File(aNewDir);
		if (!directory.exists()){
			directory.mkdir();
		}	
	}

	public static File FindFileOnWebInf(String aFileName) throws Exception {
		File result = Generic.FindFileInSubDirectory(PATH_WEBINF, aFileName);
		if (result != null && result.exists()) {
			return(result);
		} else {
			return(null);
		}
	}

	public static void Setup(int aThreadId, String aPropsFile, boolean aOnlyConsoleLog) throws Exception {
		if (App.AlreadySetup) return;

		// print the following compulsory to console
		String propFullName = GetPropFullName(aPropsFile);
		App.logInfo("Configuration is from: " + propFullName);
		App.logInfo("Working directory: " + System.getProperty("user.dir"));
		App.logInfo("If log path is not specify, it will be defaulted to: " + System.getProperty("java.io.tmpdir"));
		App.logInfo("Java classpath: " + System.getProperty("java.class.path"));

		if (App.CustomLogFormatter == null) {
			App.CustomLogFormatter = new LogFormatter(); // a custom log formatter
		} 

		App.AlreadySetup = true;
		App.OnlyConsoleLog = aOnlyConsoleLog;
		if (aThreadId < 0) {
			App.PropLogThreadId = false;
		} else {
			App.ThreadId = aThreadId;
		}
		if (System.getProperty("os.name").startsWith("Win")) {
			App.LineFeed = "\r\n";
		}
		App.DateStyleObj = DateTimeFormat.forPattern(DATE_STYLE_STR);
		App.PropertyFileName = aPropsFile;
		App.NfThreadId = NumberFormat.getInstance();

		if (App.AppProps == null) { // create or open properties file
			File propsFile = new File(propFullName);
			if (propsFile.exists() == false) { // if no property file, create one
				System.out.println("Fail to locate property file: " + propFullName + ", creating it....");
				try {
					propsFile.createNewFile();
				} catch (IOException ex) {
					throw new Hinderance(ex, "Fail to create property file: " + propFullName);
				}
			}
			propsInStream = new FileInputStream(propFullName);
			AppProps = new Properties();
			AppProps.load(propsInStream);
		}

		if (App.AppLogger == null) { // with the properties we can open the log file 
			App.PropRawLogName = App.GetValue(App.KeyRawLogName, App.PropRawLogName);
			App.AppLogger = Logger.getLogger("global");	 // create the logger now
			App.removeAllLogHandlers(); // remove it first to avoid getValue method from putting msg into console

			if (App.OnlyConsoleLog == false) {
				App.getLogName(new DateTime()); // to get the App.SwitchDuration
				App.OpenLogFile();
			}
			App.AppLogger.setLevel(Level.ALL);

			if (App.OnlyConsoleLog == true) {
				App.ConsoleLogEnable();
			} else {
				App.PropLogOnConsole = App.GetValue(KeyLogOnConsole, App.PropLogOnConsole); // control console logging
				if ((App.PropLogOnConsole.toLowerCase()).equals("false")) {
					App.consoleLogDisable();
				} else {
					App.ConsoleLogEnable();
				}
			}
			App.ShowStartupInfo(App.PropertyFileName);

			for(int cntr = 1; cntr < MAX_CLASS_LOGGING; cntr++) {
				String key = KeyClassLogging + cntr;
				String value = App.GetValue(key, "", false); 
				if (value != null && value.isEmpty() == false) {
					String className = "";
					String classLogLevel = "debg";

					String[] strArr = value.split(":");
					if (strArr.length > 0) {
						className = strArr[0].toLowerCase();
					}
					if (strArr.length > 1) {
						classLogLevel = strArr[1];
					}
					Level level = str2LogLevel(0, classLogLevel);
					ClassLogging.put(className, level);
				}
			}
		}
		App.PropMaxThread = App.GetValue(App.KeyMaxThread, App.MaxThread);
		if (App.PropMaxThread.isEmpty() == false) {
			App.MaxThread = Integer.parseInt(App.PropMaxThread);
		}
	}

	public static DateTime getLogSwitchDateBefore(int aCalDuration) throws Exception {
		DateTime switchDateNow = null;

		switch (aCalDuration) {
			case Calendar.HOUR:
				switchDateNow = new DateTime().withMinuteOfHour(0).withSecondOfMinute(0);
				break;
			case Calendar.DATE:
				switchDateNow = new DateTime().withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0);
				break;
			case Calendar.WEEK_OF_MONTH:
				switchDateNow = new DateTime().withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0);
				break;
			case Calendar.MONTH:
				switchDateNow = new DateTime().withDayOfMonth(1).withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0);
				break;
			case Calendar.YEAR:
				switchDateNow = new DateTime().withMonthOfYear(1).withDayOfMonth(1).withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0);
				break;
			default:
				throw new Hinderance("Invalid log file switching criteria: " + aCalDuration);
		}

		return(switchDateNow);
	}

	public static DateTime getLogSwitchDateAfter(DateTime aDateBefore, int aCalDuration) throws Exception {
		DateTime dateAfter;

		switch (aCalDuration) {
			case Calendar.HOUR:
				dateAfter = aDateBefore.plusHours(1);
				break;
			case Calendar.DATE:
				dateAfter = aDateBefore.plusDays(1);
				break;
			case Calendar.WEEK_OF_MONTH:
				dateAfter = aDateBefore.plusWeeks(1);
				break;
			case Calendar.MONTH:
				dateAfter = aDateBefore.plusMonths(1);
				break;
			case Calendar.YEAR:
				dateAfter = aDateBefore.plusYears(1);
				break;
			default:
				throw new Hinderance("Invalid log file switching criteria: " + aCalDuration);
		}

		return(dateAfter);
	}

	public static String NormalizePath(String path) {
		if (path.startsWith("/") || path.startsWith("\\")) {
			if (IsWindows()) {
				path = path.substring(1);
	 		} else {
				// do nothing
			}
		} else if (path.startsWith("file:")) {
			path = path.substring("file:".length());
		}

		return(path);
	}

	public static String GetDocRoot(String aFileName) {
		String result= aFileName;
		int startAt = aFileName.indexOf("WEB-INF");
		if (startAt >= 0) {
			result = aFileName.substring(0, startAt - 1);
		} 
		return(result);
	}

	public static String GetLogFullName(String aFileName) {
		String logPath;
		File logFile = new File(App.PropRawLogName);
		if (aFileName.startsWith("WEB-INF")) {
			MakeDir(App.LogPathWebInf);
			String[] fileName = aFileName.split(":");
			logPath = Paths.get(App.LogPathWebInf, fileName[1]).toString();
		} else {
			if (logFile.getParent() == null) {
				String tmpDir = System.getProperty("java.io.tmpdir");
				logPath = tmpDir + File.separator + aFileName;
			} else {
				logPath = aFileName;
			}
		}
		return(logPath);
	}

	public static String GetMaxThread() throws Exception {
		App.PropMaxThread = App.GetValue(App.KeyMaxThread, App.MaxThread);
		return(App.PropMaxThread);
	}

	public static String GetPropFullName(String aPropsFile) throws Exception {
		File propFile = Generic.FindFileAtUserDir(aPropsFile);
		if (propFile == null) {
			App.logInfo("Property file: " + aPropsFile + ", is not in working directory: " + System.getProperty("user.dir"));
			propFile = FindFileOnWebInf(aPropsFile);
			if (propFile == null) {
				App.logInfo("Property file: " + aPropsFile + ", is not in WEB-INF: " + PATH_WEBINF);
				propFile = Generic.FindFileOnClassPath(aPropsFile);
				if (propFile == null) {
					App.logInfo("Property file: " + aPropsFile + ", is not in any classpath sub-directories: " + System.getProperty("java.class.path"));
					throw new Hinderance("Fail to locate property file: " + aPropsFile);
				} 
			}
		} 
		App.logInfo("Found property file at: " + propFile.getAbsolutePath());
		return(propFile.getAbsolutePath());

		/*
		if (propFile != null) {
			String fullFileNameWithPath = propFile.getAbsolutePath();
			App.logInfo("Found property file at: " + fullFileNameWithPath);
		} else {
			App.logEror("Fail to locate property file: " + aPropsFile);
			return;
		}

		String configPath = aFileName; 
		File configFile = new File(aFileName);
		if (configFile.getParent() == null) {
			configPath = Paths.get(App.ConfigPath, aFileName).toString();
		}
		return(configPath);
		*/
	}

	public static void OpenLogFile() throws Exception {
		App.PropLogName = App.PropRawLogName;

		if (App.SwitchDuration.startsWith("MIN")) { // determine the next switch date 
			App.SwitchDateBefore = App.getLogSwitchDateBefore(Calendar.MINUTE);
			App.SwitchDateAfter = App.getLogSwitchDateAfter(App.SwitchDateBefore, Calendar.MINUTE);
		}
		else if (App.SwitchDuration.startsWith("HOU")) {
			App.SwitchDateBefore = App.getLogSwitchDateBefore(Calendar.HOUR);
			App.SwitchDateAfter = App.getLogSwitchDateAfter(App.SwitchDateBefore, Calendar.HOUR);
		}
		else if (App.SwitchDuration.startsWith("DAY")) {
			App.SwitchDateBefore = App.getLogSwitchDateBefore(Calendar.DATE);
			App.SwitchDateAfter = App.getLogSwitchDateAfter(App.SwitchDateBefore, Calendar.DATE);
		}
		else if (App.SwitchDuration.startsWith("MON")) {
			App.SwitchDateBefore = App.getLogSwitchDateBefore(Calendar.MONTH);
			App.SwitchDateAfter = App.getLogSwitchDateAfter(App.SwitchDateBefore, Calendar.MONTH);
		}
		else if (App.SwitchDuration.startsWith("YEA")) {
			App.SwitchDateBefore = App.getLogSwitchDateBefore(Calendar.YEAR);
			App.SwitchDateAfter = App.getLogSwitchDateAfter(App.SwitchDateBefore, Calendar.YEAR);
		}

		App.getLogName(App.SwitchDateBefore); // create the next log file name

 		// close and remove the previous log file handler if exist
		if (App.LogHandler != null) {
			App.logInfo(App.ThreadId, "Switching log file to: " + App.PropLogName + "........");
			App.LogHandler.close();
			Logger parLogger = App.AppLogger.getParent(); 
			Handler[] delHandler = parLogger.getHandlers();
			for (Handler delHandler1 : delHandler) {
				if (delHandler1 == App.LogHandler) {
					parLogger.removeHandler(delHandler1);
					break;
				}
			}
		}
		
 		// open and create the next log file
		try {
			App.LogHandler = new FileHandler(GetLogFullName(App.PropLogName), true);
			App.LogHandler.setFormatter(App.CustomLogFormatter);
			App.AppLogger.addHandler(App.LogHandler);
		}
		catch (IOException | SecurityException ex) {
			throw new Hinderance(ex, "Fail to create or open log file: " + App.PropLogName);
		}
	}

	public static boolean isInteger(String i) {
		try {
			Integer.parseInt(i);
			return true;
		}
		catch(NumberFormatException nfe) {
			return false;
		}
	}

	public static void getLogName(DateTime aDate) throws Exception {
		App.PropLogNamePrefix = App.PropRawLogName;
		App.PropLogNameDatePart = "";
		App.PropLogNamePostfix = "";

		int bracketStart = App.PropRawLogName.indexOf("[");
		if (bracketStart >= 0) { // user specify date part, assemble it to the log name
			String datePart = App.PropRawLogName.substring(bracketStart + 1);
			int bracketEnd = datePart.indexOf("]");
			if (bracketEnd == -1) {
				throw new Exception("Invalid date part in property " + App.KeyRawLogName + ", value: " + App.PropRawLogName);
			}
			datePart = datePart.substring(0, bracketEnd);
			//SimpleDateFormat dateStyleForFile = new SimpleDateFormat(datePart);
			DateTimeFormatter dateStyleForFile = DateTimeFormat.forPattern(datePart);

			if (datePart.trim().endsWith("mm")) {
				App.SwitchDuration = "MIN";
			} else if (datePart.trim().endsWith("HH")) {
				App.SwitchDuration = "HOUR";
			} else if (datePart.trim().endsWith("dd")) {
				App.SwitchDuration = "DAY";
			} else if (datePart.trim().endsWith("MM")) {
				App.SwitchDuration = "MONTH";
			} else if (datePart.trim().endsWith("yy")) {
				App.SwitchDuration = "YEAR";
			} else {
				throw new Exception("Invalid date part in property " + App.KeyRawLogName + ", value: " + App.PropRawLogName);
			}

			App.PropLogNamePrefix = App.PropRawLogName.substring(0, bracketStart);
			App.PropLogNameDatePart = dateStyleForFile.print(aDate);
			App.PropLogNamePostfix = App.PropRawLogName.substring(bracketStart + bracketEnd + 2);
			App.PropLogName = App.PropLogNamePrefix + App.PropLogNameDatePart + App.PropLogNamePostfix;
		}
		else { // user did not specify date part, no log switching
			App.PropLogName = App.PropRawLogName;
		}
	}

	public static void setLogLevel(String aLevel) throws Exception {
		App.setLogLevel(-1, aLevel);
	}

	public enum LogLevel {
		ERROR, WARN, INFO, COFG, DEBG, NONE;
	}

	public static LogLevel getLogLevel() throws Exception {
		LogLevel result;
		if (App.JavaLogLevel == Level.SEVERE) {
			result = LogLevel.ERROR; 
		} else if (App.JavaLogLevel == Level.WARNING) {
			result = LogLevel.WARN; 
		} else if (App.JavaLogLevel == Level.INFO) {
			result = LogLevel.INFO; 
		} else if (App.JavaLogLevel == Level.CONFIG) {
			result = LogLevel.COFG; 
		} else if (App.JavaLogLevel == Level.ALL) {
			result = LogLevel.DEBG; 
		} else if (App.JavaLogLevel == Level.OFF) {
			result = LogLevel.NONE; 
		} else {
			throw new Hinderance("Log level is not set!");
		}
		return(result);
	}

	@Deprecated
	public static void setLogLevel(int aThreadId, String aLevel) throws Exception {
		App.JavaLogLevel = Level.ALL;
		App.AppLogger.setLevel(Level.ALL);
		App.log(Level.INFO, aThreadId, "Log level is at: " + aLevel.toUpperCase());
		App.JavaLogLevel = str2LogLevel(aThreadId, aLevel);
	}

	public static Level str2LogLevel(int aThreadId, String aLevel) throws Exception {
		Level result;
		String strLevel = aLevel.toLowerCase();
		switch (strLevel.toLowerCase()) {
			case "eror":
				result = Level.SEVERE;
				break;
			case "warn":
				result = Level.WARNING;
				break;
			case "info":
				result = Level.INFO;
				break;
			case "cofg":
				result = Level.CONFIG;
				break;
			case "debg":
				result = Level.ALL;
				break;
			case "none":
				result = Level.OFF;
				break;
			default:
				throw new Hinderance("The set log level is not valid, please use EROR, WARN, INFO, COFG or DEBG level: " + aLevel.toUpperCase());
		}
		return(result);
	}

	public static String logLevel2Str(Level aLevel) throws Exception {
		String strLevel;
		if (aLevel == Level.SEVERE) {
			strLevel = "eror";
		} else if (aLevel == Level.WARNING) {
			strLevel = "warn";
		} else if (aLevel == Level.INFO) {
			strLevel = "info";
		} else if (aLevel == Level.CONFIG) {
			strLevel = "confg";
		} else if (aLevel == Level.ALL) {
			strLevel = "debg";
		} else if (aLevel == Level.OFF) {
			strLevel = "none";
		} else {
			throw new Hinderance("The set log level is not valid, please use EROR, WARN, INFO, COFG or DEBG level: " + aLevel.getName());
		}
		return(strLevel.toUpperCase());
	}

	public static synchronized void removeAllLogHandlers() {
		Logger parLogger = App.AppLogger.getParent(); 
		Handler[] delHandler = parLogger.getHandlers();
		for (Handler delHandler1 : delHandler) {
			parLogger.removeHandler(delHandler1);
		}
	}

	public static synchronized void consoleLogDisable() {
		Logger parLogger = App.AppLogger.getParent(); 
		Handler[] delHandler = parLogger.getHandlers();
		for (Handler delHandler1 : delHandler) {
			if (delHandler1 instanceof ConsoleHandler) {
				parLogger.removeHandler(delHandler1);
			}
		}
	}

	public static synchronized void ConsoleLogEnable() {
		consoleLogDisable(); // make sure only one output console 
		ConsoleHandler conHandler = new ConsoleHandler();
		conHandler.setFormatter(App.CustomLogFormatter);
		conHandler.setLevel(Level.ALL);
		App.AppLogger.addHandler(conHandler);
	}

	public static synchronized void log(Level aLevel, Throwable ex) {
		App.log(aLevel, Hinderance.exToStr(ex));
	};

	public static synchronized void log(Level aLevel, int aThreadId, Throwable ex) {
		App.log(aLevel, aThreadId, Hinderance.exToStr(ex));
	}

	public static void logInfo(String aMessage) {
		App.log(Level.INFO, aMessage);
	}

	public static void logInfo(Class aSource, String aMessage) {
		App.log(Level.INFO, "[" + aSource.getSimpleName() + "]" + " " + aMessage);
	}

	public static void logInfo(Object aSource, String aMessage) {
		App.log(Level.INFO, "[" + aSource.getClass().getSimpleName() + "]" + " " + aMessage);
	}

	public static void logInfo(int aThreadId, String aMessage) {
		App.log(Level.INFO, aThreadId, aMessage);
	}

	public static void logInfo(Throwable ex) {
		App.log(Level.INFO, Hinderance.exToStr(ex));
	}

	public static void logInfo(int aThreadId, Throwable ex) {
		App.log(Level.INFO, aThreadId, Hinderance.exToStr(ex));
	}

	public static void logEror(String aMessage) {
		App.log(Level.SEVERE, aMessage);
	}

	public static void logEror(Object aSource, String aMessage) {
		App.log(Level.SEVERE, "[" + aSource.getClass().getSimpleName() + "]" + " " + aMessage);
	}

	public static void logEror(int aThreadId, String aMessage) {
		App.log(Level.SEVERE, aThreadId, aMessage);
	}

	public static void logEror(Throwable ex) {
		App.log(Level.SEVERE, Hinderance.exToStr(ex));
	}

	public static void logEror(Object aSource, Throwable ex) {
		App.log(Level.SEVERE, "[" + aSource.getClass().getSimpleName() + "]" + " " + Hinderance.exToStr(ex));
	}

	public static void logEror(Throwable ex, String aMessage) {
		App.log(Level.SEVERE, aMessage + App.LineFeed + Hinderance.exToStr(ex));
	}

	public static void logEror(Object aSource, Throwable ex, String aMessage) {
		App.log(Level.SEVERE, "[" + aSource.getClass().getSimpleName() + "]" + " " + aMessage + App.LineFeed + Hinderance.exToStr(ex));
	}

	public static void logEror(int aThreadId, Throwable ex) {
		App.log(Level.SEVERE, aThreadId, Hinderance.exToStr(ex));
	}

	public static void logWarn(String aMessage) {
		App.log(Level.WARNING, aMessage);
	}

	public static void logWarn(Class aSource, String aMessage) {
		App.log(Level.WARNING, "[" + aSource.getSimpleName() + "]" + " " + aMessage);
	}

	public static void logWarn(Object aSource, String aMessage) {
		App.log(Level.WARNING, "[" + aSource.getClass().getSimpleName() + "]" + " " + aMessage);
	}

	public static void logWarn(int aThreadId, String aMessage) {
		App.log(Level.WARNING, aThreadId, aMessage);
	}

	public static void logWarn(int aThreadId, Throwable ex) {
		App.log(Level.WARNING, aThreadId, Hinderance.exToStr(ex));
	}

	public static void logWarn(Throwable ex) {
		App.log(Level.WARNING, Hinderance.exToStr(ex));
	}

	public static void logWarn(Class aSource, Throwable ex) {
		App.log(Level.WARNING, "[" + aSource.getSimpleName() + "]" + " " + Hinderance.exToStr(ex));
	}

	public static void logWarn(Object aSource, Throwable ex) {
		App.log(Level.WARNING, "[" + aSource.getClass().getSimpleName() + "]" + " " + Hinderance.exToStr(ex));
	}

	public static void logWarn(Object aSource, Throwable ex, String aMessage) {
		App.log(Level.WARNING, "[" + aSource.getClass().getSimpleName() + "]" + " " + aMessage + App.LineFeed + Hinderance.exToStr(ex));
	}

	public static void logDebg(String aMessage) {
		App.log(Level.FINE, aMessage);
	}

	public static void logDebg(int aThreadId, String aMessage) {
		App.log(Level.FINE, aThreadId, aMessage);
	}

	public static void logDebg(Object aSource, String aMessage) {
		App.log(Level.FINE, "[" + aSource.getClass().getSimpleName() + "]" + " " + aMessage);
	}

	public static void logDebg(Class aSource, String aMessage) {
		App.log(Level.FINE, "[" + aSource.getSimpleName() + "]" + " " + aMessage);
	}

	public static void logDebg(int aThreadId, Throwable ex) {
		App.log(Level.FINE, aThreadId, Hinderance.exToStr(ex));
	}

	public static void logDebg(Object aSource, Throwable ex, String aMessage) {
		App.log(Level.FINE, "[" + aSource.getClass().getSimpleName() + "]" + " " + aMessage + App.LineFeed + Hinderance.exToStr(ex));
	}

	public static void logConf(String aMessage) {
		App.log(Level.CONFIG, aMessage);
	}

	public static void logConf(Object aSource, String aMessage) {
		App.log(Level.CONFIG, "[" + aSource.getClass().getSimpleName() + "]" + " " + aMessage);
	}

	public static void logConf(int aThreadId, String aMessage) {
		App.log(Level.CONFIG, aThreadId, aMessage);
	}

	public static synchronized void log(Level aLevel, String aMessage) { 
		App.log(aLevel, -1, aMessage);
	};

	public static String getCurrentClass() {
		String className = "";
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		for (StackTraceElement eachStackElement : stack) {
			className = eachStackElement.getClassName().toLowerCase();
			if (className.contains("biz.shujutech")) {
				if (className.contains("biz.shujutech.base.app") == false && className.contains("biz.shujutech.base.hinderance") == false) {
					break;
				}
			}
		}
		return(className);
	}

	public static synchronized void log(Level aLevel, int aThreadId, String aMessage) {
		try {
			if (App.AppLogger == null || (App.LogHandler == null && App.OnlyConsoleLog == false)) {
				System.err.println(aMessage);
				return;
			}
			
			if (ClassLogging.isEmpty() == false) {
				String nowClass = getCurrentClass(); // if user says log this class, then continue, else check for system wide logging settings
				Level level =  ClassLogging.get(nowClass); // map contain class logging properties
				if (level != null) {
					if (aLevel.intValue() < level.intValue()) {
						return; // system wide logging level checking
					} 
				} else {
					if (aLevel.intValue() < App.JavaLogLevel.intValue()) {
						return; // system wide logging level checking
					} 
				}
			} else {
				if ((aLevel.intValue() < App.JavaLogLevel.intValue()) || aLevel.equals(Level.OFF)) {
					return; // system wide logging level checking
				} 
			}

			if (App.OnlyConsoleLog == false) {
				//if ((new DateTime()).after(App.SwitchDateAfter)) {
				if ((new DateTime()).isAfter(App.SwitchDateAfter)) {
					App.OpenLogFile(); // log switching
				}
			}
			String logString = "";
			String logPrefix;
			String strLevel;
			//App.DateStyleObj = new SimpleDateFormat(DATE_STYLE_STR);
			App.DateStyleObj = DateTimeFormat.forPattern(DATE_STYLE_STR);
			DateTime timeNow = new DateTime();
	
	    if (aLevel == Level.SEVERE) {
				strLevel = "EROR";
			} else if (aLevel == Level.INFO) {
				strLevel = "INFO";
			} else if (aLevel == Level.FINE) {
				strLevel = "DEBG";
			} else if (aLevel == Level.CONFIG) {
				strLevel = "COFG";
			} else if (aLevel == Level.WARNING) {
				strLevel = "WARN";
			} else {
				strLevel = "    ";
			}
			
			logPrefix = App.DateStyleObj.print(timeNow) + " " + strLevel + " ";
	
			if (App.PropLogThreadId && aThreadId != -1) { // should thread identifer be logged 
				if (aThreadId < 0) {
					App.NfThreadId.setMinimumIntegerDigits(App.MaxThreadIdDigit - 1);
				} else {
					App.NfThreadId.setMinimumIntegerDigits(App.MaxThreadIdDigit);
				}
				logPrefix = logPrefix + NfThreadId.format(aThreadId) + " ";
			}
	
			int linePos = aMessage.indexOf('\n');
			if (linePos >= aMessage.length() || linePos < 0) {
				logString = aMessage;
				logString = App.splitEqualLen(logString, App.PropLogWrapLen, App.LineFeed);
			}
			else { // multiple line message
				int posStart;
				int posEnd;
	
				posStart = 0;
				while (true) {
					posEnd = aMessage.indexOf('\n', posStart);
					if (posEnd >= 0) {
						posEnd++;
						logString = logString + App.splitEqualLen(aMessage.substring(posStart, posEnd), App.PropLogWrapLen, App.LineFeed);
						if (posEnd >= aMessage.length()) {
							break;
						} else {
							logString = logString.concat(App.LineFeed);
						}
						posStart = posEnd;
					}
					else { // last line
						logString = logString + App.splitEqualLen(aMessage.substring(posStart), App.PropLogWrapLen, App.LineFeed);
						break;
					}
				}
			}
	
			String prefixSpace = App.LineFeed; // base on the prefix length, pad space for second line onwards
			for (int i = 0; i < logPrefix.length(); i++) {
				prefixSpace = prefixSpace.concat(" ");
			}
			logString = logString.replaceAll(App.LineFeed, prefixSpace);
			logString = logString.concat(App.LineFeed);
	
			App.AppLogger.log(aLevel, logPrefix + logString);
			//App.AppLogger.log(aLevel, "{0}{1}", new Object[]{logPrefix, logString});
		}
		catch (Exception ex) {
			ex.printStackTrace(System.err); // last resort, logging not working so just print it to console
		}
	}

	public static String splitEqualLen(String aMessage, int maxLen, String lineFeed) { 
		String fmtString = aMessage;
		if (aMessage.length() > maxLen) {
			String tmpString = "";
			int startPos = 0;
			int endPos = maxLen - 1;
			int posSpace;
			int cutPos;

			while(true) {
				cutPos = endPos;
				posSpace = fmtString.indexOf(' ', endPos); // cannot have maxLen smaller then 8
				if (posSpace >= 0)  {
					endPos = posSpace; // found the space char
				}

				if (endPos - startPos >= maxLen + 8) { // brute cut the line
					endPos = cutPos;
				}

				if (endPos + 2 >= fmtString.length()) { // avoid 2 char orphan sentence
					endPos = fmtString.length();
				}

				tmpString = tmpString.concat((fmtString.substring(startPos, endPos)).trim() + App.LineFeed);
				startPos = endPos;

				endPos = endPos + maxLen;

				if (endPos > fmtString.length()) {
					endPos = fmtString.length();
				}

				if (startPos >= fmtString.length()) {
					break;
				}
			}
			fmtString = tmpString;
		}
		return(fmtString.trim());
	}

	public static synchronized String GetValue(String aKeyName, int aDefaultValue) throws Exception {
		String strDefaultValue = String.valueOf(aDefaultValue);
		return(App.GetValue(aKeyName, strDefaultValue));
	}

	public static synchronized String GetValue(String aKeyName, String aDefaultValue) throws Exception {
		String result = App.GetValue(aKeyName, aDefaultValue, true);
		return(result);
	}

	public static synchronized String GetValue(String aKeyName, String aDefaultValue, boolean createMissingKey) throws Exception {
		String keyValue = App.AppProps.getProperty(aKeyName); // get the key's value
		if (keyValue == null && createMissingKey) {
			keyValue = App.AppProps.getProperty(aKeyName, aDefaultValue); // get the key's value with a default
			if (App.AppLogger != null) {
				String errMsg = "Creating property '" + aKeyName + "', default: '" + aDefaultValue + "'";
				App.log(Level.WARNING, errMsg);
			}
			App.AppProps.setProperty(aKeyName, aDefaultValue);
			FileOutputStream propsOutStream = new FileOutputStream(GetPropFullName(App.PropertyFileName)); // open in append mode
			App.AppProps.store(propsOutStream, null);
		}

		if (keyValue != null && keyValue.startsWith(ENCRYPT_PREFIX)) {
			CryptoRsa cryptoRsa = new CryptoRsa();
			String encryptedValue = keyValue.substring(ENCRYPT_PREFIX.length());
			String decryptedValue = cryptoRsa.decryptText(encryptedValue, CryptoRsa.BASE64_PRIVATE_KEY);
			keyValue = decryptedValue;
		}

		return(keyValue);
	}

	public static String decrypt(int aThreadId, String aSzHex, String aSeed) {
		int dummy;
		int dummy1;
		int l = aSzHex.length();
		int index = 0;
		int j = 0;
		int L = aSeed.length();
		String strDecrypted = "";
		for(int i = 0; i < l;) {
			if(aSzHex.charAt(i) >= 'A') {
				dummy = aSzHex.charAt(i) - 55;
			} else {
				dummy = aSzHex.charAt(i) - 48;
			}
			dummy1 = dummy * 16;
			if(i + 1 != l) {
				if(aSzHex.charAt(i + 1) >= 'A') {
					dummy = aSzHex.charAt(i + 1) - 55;
				} else {
					dummy = aSzHex.charAt(i + 1) - 48;
				}
			} else {
				dummy = -48;
			}
			dummy1 += dummy;
			int temp1 = 0 - ((255 - dummy1) + 1);
			int temp2 = dummy1 ^ aSeed.charAt(j);
			strDecrypted = strDecrypted.concat(String.valueOf((char)temp2));
			if(L == j + 1) {
				j = 0;
			} else {
				j++;
			}
			i += 2;
			index++;
		}

		return strDecrypted;
	}

	public static String DoubleQuote(String aStr) {
		String result = "\"" + aStr + "\"";
		return(result);
	}

	public static String Unquote(String s) {
 		if (s != null && ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")))) {
  		s = s.substring(1, s.length() - 1);
 		}
		return s;
	}

	public static void ShowThreadingStatus(Class aClassName, String aModule, Integer aThreadNum, Integer aMaxThread, int aCntrAttempt, int aMaxAttempt) {
		if (aThreadNum >= aMaxThread) {
			App.logWarn(aClassName, "[" + aModule + "] " + "Maximum thread reach: " + aMaxThread + ", will wait for other thread to complete...");
		} else {
			App.logWarn(aClassName, "[" + aModule + "] " + "Waiting for free db connection, attempt no: " + aCntrAttempt + ", max attempt: " + aMaxAttempt);
		}
	}

	public static int getMaxThread() {
		return MaxThread;
	}

	public static void setMaxThread(int maxThread) {
		App.MaxThread = maxThread;
	}

	public static String GetLapseTime(GregorianCalendar aStart) throws Exception {
		String result = Long.toString(((new GregorianCalendar()).getTimeInMillis() - aStart.getTimeInMillis())/1000);
		return(result);
	}

	public static boolean IsWindows() {
		return OS.contains("win");
	}

	public static boolean IsMac() {
		return OS.contains("mac");
	}

	public static boolean IsUnix() {
		return (OS.contains("nix") || OS.contains("nux") || OS.contains("aix"));
	}

	public static boolean IsSolaris() {
		return OS.contains("sunos");
	}
	public static String GetOS(){
		if (IsWindows()) {
			return "win";
		} else if (IsMac()) {
			return "osx";
		} else if (IsUnix()) {
			return "uni";
		} else if (IsSolaris()) {
			return "sol";
		} else {
			return "err";
		}
	}

	public static void main(String[] args) {
		String tempDir = System.getProperty("java.io.tmpdir");
		App.ConfigPathWebInf = tempDir;
		App.LogPathWebInf = tempDir;

		// this demonstrate App can be call from other external class, the external class is "Simple"
		try {
			App.Setup("shujutech.properties"); 
			Simple simple = new Simple();
		} catch (Exception ex) {
			App.log(Level.SEVERE, new Hinderance(ex, "App encounter fatal error, application is aborting...."));
		}
		
		// simplest method to use App 
		try {
			App app = new App(); 
			App.log(Level.INFO, "Server starting.....");
			App.setLogLevel("debg");
			App.log(Level.INFO, "This should got no thread id"); 
			App.logWarn("This is warning log message"); 
			App.logDebg("This is debug log message"); 
			App.log(Level.INFO, "Next exception will be thrown and log beautifully"); 
		} catch (Exception ex) {
			App.log(Level.SEVERE, new Hinderance(ex, "Application encounter fatal error, application is aborting...."));
		}
	};
}