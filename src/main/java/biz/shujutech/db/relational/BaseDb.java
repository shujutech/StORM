package biz.shujutech.db.relational;

import biz.shujutech.base.App;
import biz.shujutech.base.Base;
import biz.shujutech.base.ConnectionPool;

public class BaseDb extends Base {

	public ConnectionPool connPool = null; // connection pool
	public String jdbcDriver = "com.mysql.jdbc.Driver";
	public String jdbcUrl = "jdbc:mysql://localhost/test";
	public String jdbcUser = "root";
	public String jdbcPassword = "bpmail";
	public String jdbcPoolTimeOut = "30";
	public String jdbcInitConn = "16";
	public String jdbcMaxConn = "32";

	public boolean alreadySetup = false;

	public ConnectionPool getConnPool() {
		return connPool;
	}

	public void setConnPool(ConnectionPool aConnPool) {
		this.connPool = aConnPool;
	}

	public void setupDb() throws Exception {
		this.jdbcDriver = App.GetValue("Systm.jdbcDriver", this.jdbcDriver);
		this.jdbcUrl = App.GetValue("Systm.jdbcUrl", this.jdbcUrl);
		this.jdbcUser = App.GetValue("Systm.jdbcUser", this.jdbcUser);
		this.jdbcPassword = App.GetValue("Systm.jdbcPassword", this.jdbcPassword);
		this.jdbcPoolTimeOut = App.GetValue("Systm.jdbcPoolTimeOut", this.jdbcPoolTimeOut);
		this.jdbcInitConn = App.GetValue("Systm.jdbcInitConn", this.jdbcInitConn);
		this.jdbcMaxConn = App.GetValue("Systm.jdbcMaxConn", this.jdbcMaxConn);

		// connect to the database
		this.connPool = new ConnectionPool(this.jdbcDriver, this.jdbcUrl, this.jdbcUser, this.jdbcPassword, this.jdbcInitConn, this.jdbcMaxConn, this.jdbcPoolTimeOut, this);		
		this.alreadySetup = true;
	}

	public boolean alreadySetup() {
		return(this.alreadySetup);
	}
}

