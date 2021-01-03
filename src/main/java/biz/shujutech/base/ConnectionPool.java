/**
 * 
 * This class create and maintain a pool of jdbc connections, purpose is
 * to avoid repeatetively connecting to the database. Instead after 
 * connections are made they are kept and reuse by other classes when they 
 * request for it. The requested connection must be free by the user to avoid
 * running out of connections. During development, the best practice is to set
 * maximum connection to one, so you can ensure all used connection is free 
 * up.
 */

package biz.shujutech.base;


import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.StringTokenizer;
import biz.shujutech.db.relational.BaseDb;
import biz.shujutech.db.relational.Database;
import biz.shujutech.db.relational.Database.DbType;
import biz.shujutech.db.relational.FieldType;
import biz.shujutech.db.relational.Table;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ConnectionPool {
	public String cpUrl;
	private String cpUser;
	private String cpPassword;
	private int cpInitConn;
	private int cpMaxConn;
	private int cpTimeOut;
	
	private int cpCheckOut = 0;
	private	List<Connection> cpFreeConn = new CopyOnWriteArrayList<>(); // for pass by ref only	
	private	List<Driver> cpDriver = new CopyOnWriteArrayList<>(); // for pass by ref only	

	private BaseDb baseDb;

	/**
	 * A ConnectionPool consist of several jdbc connections, this
	 * connections must be of a cpUser to a particular database 
	 * specified by its jdbc cpUrl
	 * 
	 * @param aDriver
	 * @param aUrl
	 * @param aUser
	 * @param aPassword
	 * @param aInitConn
	 * @param aMaxConn
	 * @param aTimeOut
	 * @param aDb
	 * @throws Exception 
	 */
	public ConnectionPool(String aDriver, String aUrl, String aUser, String aPassword, String aInitConn, String aMaxConn, String aTimeOut, BaseDb aDb) throws Exception {
		// set attributes
		this.cpUrl = aUrl;
		this.cpUser = aUser;
		this.cpPassword = aPassword;
		this.cpInitConn = Integer.parseInt(aInitConn);
		this.cpMaxConn = Integer.parseInt(aMaxConn);
		this.cpTimeOut = Integer.parseInt(aTimeOut) > 0 ? (Integer.parseInt(aTimeOut)) : 5;
		this.baseDb = aDb;

		// config information
		App.logInfo("Total db connection available for threading: " + this.cpInitConn);
		App.logConf("Jdbc, connecting with url = " + this.cpUrl);
		App.logConf("Jdbc, connecting with user = " + this.cpUser);
		App.logConf("Jdbc, connecting with password = *********");
		App.logConf("Jdbc, connecting with init conn = " + this.cpInitConn);
		App.logConf("Jdbc, connecting with max conn = " + this.cpMaxConn);
		App.logConf("Jdbc, connecting with time out= " + this.cpTimeOut);

		// load and register all drivers from aDriver
		StringTokenizer St = new StringTokenizer(aDriver);
		while (St.hasMoreElements()) {
			String driverName = St.nextToken().trim();
			try {
				Driver loadDriver = (Driver) Class.forName(driverName).newInstance();
				DriverManager.registerDriver(loadDriver);
				cpDriver.add(loadDriver);
			} catch (Throwable ex) {
				throw new Hinderance(ex, "Fail to register Jdbc driver: " + driverName);
			}
		}
		this.initPool(this.cpInitConn);
	}

	public BaseDb getBaseDb() {
		return baseDb;
	}

	public void setBaseDb(BaseDb baseDb) {
		this.baseDb = baseDb;
	}

	public int getMaxConn() {
		return(this.cpMaxConn);
	}

	/**
	 * Each time this class is instantiated, it'll establish the connection to 
	 * the specify database, at the same time each established connection will 
	 * be added to the 'cpFreeConn' array to indicate they are not used by any
	 * client yet
	 * 
	 * @param aTotal
	 * @throws SQLException 
	 */
	private void initPool(int aTotal) throws SQLException {
		for (int i = 0; i < aTotal; i++) {
			Connection newConn = this.newConnection();
			this.cpFreeConn.add(newConn);
		}
	}

	/**
	 * Here the method will call a public 'getConnection()' method and it'll 
	 * get the connection from a pool of 'connections', if all connections
	 * are used one have to wait
	 * 
	 * @return
	 * @throws Exception
	 * @throws SQLException 
	 */
	public Connection getConnection() throws Exception, SQLException {
		try {
			if (this.cpFreeConn.size() < 10) {
				App.logWarn(this, "Getting jdbc connection, available free connection to get from: " + this.cpFreeConn.size());
			}
			Connection conn = this.getConnection((long) cpTimeOut * 1000, 2);
			if (conn.getTransactionIsolation() != Connection.TRANSACTION_READ_COMMITTED) {
				App.logDebg(this, "Converting transaction isolation level from: " + conn.getTransactionIsolation() + " to: " + Connection.TRANSACTION_READ_COMMITTED);
				conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
			}
			return conn;
		} catch (SQLException ex) {
			throw new Hinderance(ex, "Fail to get database connection, probaly maximum connections have exceeded");
		}
	}

	/**
	 * This is where the real method that obtain connection from 
	 * the pool, the method will wait until a another client release
	 * a connection if there isn't any free connection, notice
	 * that 'getPooledConnection' is actually do the calling here
	 * 
	 * @param aTimeOut
	 * @return
	 * @throws Exception
	 * @throws SQLException 
	private synchronized Connection getConnection(long aTimeOut) throws Exception, SQLException {
		return(this.getConnection((long) aTimeOut, 2));
	}
	*/

	private synchronized Connection getConnection(long aTimeOut, int aTotalRetry) throws Exception, SQLException {
		// Get a pooled Connection from the cache or a new one.
		int cntr = 1;
		long startTime = System.currentTimeMillis();
		long remaining = aTimeOut;
		long sleepQuanta = aTimeOut/aTotalRetry; // aTimeout shouldbe divided by the number of retry we want before timeout
		Connection resultConn = null;
		while ((resultConn = this.getPooledConnection()) == null) { // wait if all are checked out and the max limit has been reached.
			try {
				App.logWarn(this, "No free connection from connection pool, attempt no: " + cntr++ + ", will timeout after: " + remaining/1000 + " second, next retry after " + sleepQuanta/1000 + " second...");
				Thread.sleep(sleepQuanta);
			} catch (InterruptedException ex) { }
			remaining = aTimeOut - (System.currentTimeMillis() - startTime);

			if (remaining < 1000) {
				throw new Hinderance(new Exception(), "Timeout, no free sql connection from pool, time out set at: " + aTimeOut/1000 + " sec, max connection in pool: " + this.cpMaxConn + App.ERROR_COMPLAIN); // Timeout has expired
			}
		}

		if (!this.IsConnectionOK(resultConn)) { // check if the Connection is still OK
			this.cpCheckOut--;
			App.logWarn(this, "Detected spoilt connection, removing it from its pool"); // was bad. Try again with the remaining timeout
			App.logWarn(this, "Continuing trying to obtain connection from connection pool");
			return this.getConnection((long) remaining, aTotalRetry);
		}

		return resultConn;
	}

	public void CheckForDual(Connection aConn) throws Exception {
		if (Database.GetDbType(aConn) == DbType.MYSQL || Database.GetDbType(aConn) == DbType.ORACLE) {
			// do nothing
		} else {
			Table dualTable = new Table("dual");
			if (Database.TableExist(aConn, dualTable.getTableName()) == false) {
				dualTable.createField("col1", FieldType.INTEGER);
				Database.CreateTable(aConn, dualTable);
			}
		}
	}

	/**
	 * Pass in a Connection to test if its a working connection by calling 
	 * Connection.createStatement() method
	 * 
	 * @param aConn
	 * @return
	 * @throws Exception 
	 */
	public boolean IsConnectionOK(Connection aConn) throws Exception {
		Statement testStmt = null;
		try {
			if (!aConn.isClosed()) {
				CheckForDual(aConn);
				testStmt = aConn.createStatement();
				testStmt.executeQuery("select count(*) from dual"); // try to createStatement to see if it's really alive
				testStmt.close();
			} else {
				return false;
			}
		} catch (SQLException ex) {
			//App.logWarn(new Hinderance(ex, "Fail to validate SQL connection, also make sure the database support 'select count(*) from dual' statement"));
			App.logWarn(this, "Found stale SQL connection, also make sure the database support 'select count(*) from dual' statement");
			if (testStmt != null) {
				try {
					testStmt.close();
				} catch (SQLException se) { }
			}
			return false;
		}
		return true;
	}

	/**
	 * This method simply return a connection if available * otherwise if the 
	 * cpMaxConn haven't been exceeded than it'll establish more new connections
	 * 
	 * @return
	 * @throws SQLException 
	 */
	private synchronized Connection getPooledConnection() throws Exception {
		Connection resultConn = null;
		if (this.cpFreeConn.size() > 0) {
			resultConn = (Connection) this.cpFreeConn.get(0); // pick the first Connection in the array to get round-robin usage
			this.cpCheckOut++;
			this.cpFreeConn.remove(0);
		} else if (this.cpMaxConn == 0 || this.cpCheckOut < this.cpMaxConn) {
			App.logWarn(this, "Creating new connection, exhaused init conn: " + this.cpInitConn + ", max is: " + this.cpMaxConn + ", set max to 0 for infinite new connections");
			resultConn = this.newConnection();
			this.cpCheckOut++;
		} else {
			throw new Hinderance("Cannot create anymore new connection, exceeded maximum: " + this.cpMaxConn + ", set max to more or 0 for infinite new connections");
		}
		return resultConn;
	}

	/*
	 * Establish a new Jdbc connection by calling 
	 * DriverManager.getConnection method
	 *
	 */
	private Connection newConnection() throws SQLException {
		java.sql.Connection sqlConn;
		biz.shujutech.base.Connection stConn = new biz.shujutech.base.Connection();
		if (cpUser == null) {
			sqlConn = DriverManager.getConnection(this.cpUrl);
		} else {
			sqlConn = DriverManager.getConnection(this.cpUrl, this.cpUser, this.cpPassword);
		}
		stConn.setSqlConn(sqlConn);
		stConn.setBaseDb(this.getBaseDb());
		return(stConn);
	}

	/*
	 * Takes a Connection object from a client and put them back to
	 * ConnectionPool. Client should call this method after completing
	 * using the connection, to ensure higher connection availability
	 * to all client
	 *
	 */
	public synchronized void freeConnection(Connection aConn) {
		this.cpFreeConn.add(aConn);
		this.cpCheckOut--;
		try {
			if (aConn.getAutoCommit() == false) aConn.rollback();
		} catch(Exception ex) {
			App.logEror(this, ex, "Error when freeing sql connection!");
		}
		notifyAll();
		App.logDebg(this, "Released jdbc connection, total free connection: " + this.cpFreeConn.size());
	}

	/**
	 * Call this method when you no longer need to use the ConnectionPool,
	 * this will close the jdbc connection and remove each of this Connection
	 * object from the array 
	 * 
	 * @return 
	 */
	public synchronized String release() {
		int cntr = 0;
		String resultMsg = "";

		for (Connection closeConn : this.cpFreeConn) {
			try {
				closeConn.close();
				cntr++;
			} catch (SQLException ex) {
				resultMsg.concat(Hinderance.exStackToStr(ex, "Fail to close database connection: " + cntr));
			}
		}
		App.logInfo(this, "Closed all connection and removed from connection pool");
		this.cpFreeConn.clear();
		this.cpCheckOut = 0;

		// release the drivers
		for (Driver driver : cpDriver) {
			try {
				DriverManager.deregisterDriver(driver);
			} catch (SQLException ex) {
				resultMsg.concat(Hinderance.exStackToStr(ex, "Fail to deregister Jdbc driver: " + driver.getClass().getName()));
			}
		}

		return resultMsg;
	}

	public String getStats() {
		return "Connection pool status, total: " + (this.cpFreeConn.size() + this.cpCheckOut) + 
		" available: " + this.cpFreeConn.size() + " used: " + this.cpCheckOut;
	}

}
