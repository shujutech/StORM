
package biz.shujutech.base;

import java.util.logging.Level;

public class Simple
{
	public Simple() {
		try {
			App.log(Level.INFO, "OS Name: " + System.getProperty("os.name"));
			App.log(Level.INFO, "OS Architecture: " + System.getProperty("os.arch"));
			App.log(Level.FINE, "OS Version: " + System.getProperty("os.version"));
			App.logInfo("Info OS Name: " + System.getProperty("os.name"));
			App.logInfo("Info OS Architecture: " + System.getProperty("os.arch"));
			App.logInfo("Info OS Version: " + System.getProperty("os.version"));
			App.logEror("Eror OS Name: " + System.getProperty("os.name"));
			App.logEror("Eror OS Architecture: " + System.getProperty("os.arch"));
			App.logEror("Eror OS Version: " + System.getProperty("os.version"));

			App.setLogLevel("debg");
			App.logInfo("Log level is at: " + App.AppLogger.getLevel().getName());
			App.logDebg("Debg OS Name: " + System.getProperty("os.name"));
			App.logDebg("Debg OS Architecture: " + System.getProperty("os.arch"));
			App.logDebg("Debg OS Version: " + System.getProperty("os.version"));

			App.log(Level.INFO, "JDBC Password: " + App.GetValue("Systm.jdbcPassword", ""));
		} catch (Exception ex) {
			App.log(Level.SEVERE, 0, new Hinderance(ex, this.getClass().getName() + " encounter fatal error, application is aborting...."));
		}
	};
}
