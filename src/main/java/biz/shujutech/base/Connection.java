/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.shujutech.base;

import biz.shujutech.db.relational.BaseDb;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 *
 * @author chairkb
 */
public class Connection implements java.sql.Connection {

	private BaseDb baseDb;
	private java.sql.Connection sqlConn;

	public java.sql.Connection getSqlConn() {
		return sqlConn;
	}

	public void setSqlConn(java.sql.Connection sqlConn) {
		this.sqlConn = sqlConn;
	}

	public BaseDb getBaseDb() {
		return baseDb;
	}

	public void setBaseDb(BaseDb baseDb) {
		this.baseDb = baseDb;
	}

	@Override   
	public Statement createStatement() throws SQLException {
		return(this.getSqlConn().createStatement());
	}   
	   
	@Override   
	public PreparedStatement prepareStatement(String string) throws SQLException {
		return(this.getSqlConn().prepareStatement(string));
	}   
	   
	@Override   
	public CallableStatement prepareCall(String string) throws SQLException {
		return(this.getSqlConn().prepareCall(string));
	}   
	   
	@Override   
	public String nativeSQL(String string) throws SQLException {
		return(this.getSqlConn().nativeSQL(string));
	}   
	   
	@Override   
	public void setAutoCommit(boolean bln) throws SQLException {
		this.getSqlConn().setAutoCommit(bln);
	}   
	   
	@Override   
	public boolean getAutoCommit() throws SQLException {
		return(this.getSqlConn().getAutoCommit());
	}   
	   
	@Override   
	public void commit() throws SQLException {
		this.getSqlConn().commit();
	}   
	   
	@Override   
	public void rollback() throws SQLException {
		this.getSqlConn().rollback();
	}   
	   
	@Override   
	public void close() throws SQLException {
		this.getSqlConn().close();
	}   
	   
	@Override   
	public boolean isClosed() throws SQLException {
		return(this.getSqlConn().isClosed());
	}   
	   
	@Override   
	public DatabaseMetaData getMetaData() throws SQLException {
		return(this.getSqlConn().getMetaData());
	}   
	   
	@Override   
	public void setReadOnly(boolean bln) throws SQLException {
		this.getSqlConn().setReadOnly(bln);
	}   
	   
	@Override   
	public boolean isReadOnly() throws SQLException {
		return(this.getSqlConn().isReadOnly());
	}   
	   
	@Override   
	public void setCatalog(String string) throws SQLException {
		this.getSqlConn().setCatalog(string);
	}   
	   
	@Override   
	public String getCatalog() throws SQLException {
		return(this.getSqlConn().getCatalog());
	}   
	   
	@Override   
	public void setTransactionIsolation(int i) throws SQLException {
		this.getSqlConn().setTransactionIsolation(i);
	}   
	   
	@Override   
	public int getTransactionIsolation() throws SQLException {
		return(this.getSqlConn().getTransactionIsolation());
	}   
	   
	@Override   
	public SQLWarning getWarnings() throws SQLException {
		return(this.getSqlConn().getWarnings());
	}   
	   
	@Override   
	public void clearWarnings() throws SQLException {
		this.getSqlConn().clearWarnings();
	}   
	   
	@Override   
	public Statement createStatement(int i, int i1) throws SQLException {
		return(this.getSqlConn().createStatement(i, i1));
	}   
	   
	@Override   
	public PreparedStatement prepareStatement(String string, int i, int i1) throws SQLException {
		return(this.getSqlConn().prepareStatement(string, i, i1));
	}   
	   
	@Override   
	public CallableStatement prepareCall(String string, int i, int i1) throws SQLException {
		return(this.getSqlConn().prepareCall(string, i, i1));
	}   
	   
	@Override   
	public Map<String, Class<?>> getTypeMap() throws SQLException {
		return(this.getSqlConn().getTypeMap());
	}   
	   
	@Override   
	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		this.getSqlConn().setTypeMap(map);
	}   
	   
	@Override   
	public void setHoldability(int i) throws SQLException {
		this.getSqlConn().setHoldability(i);
	}   
	   
	@Override   
	public int getHoldability() throws SQLException {
		return(this.getSqlConn().getHoldability());
	}   
	   
	@Override   
	public Savepoint setSavepoint() throws SQLException {
		return(this.getSqlConn().setSavepoint());
	}   
	   
	@Override   
	public Savepoint setSavepoint(String string) throws SQLException {
		return(this.getSqlConn().setSavepoint(string));
	}   
	   
	@Override   
	public void rollback(Savepoint svpnt) throws SQLException {
		this.getSqlConn().rollback(svpnt);
	}   
	   
	@Override   
	public void releaseSavepoint(Savepoint svpnt) throws SQLException {
		this.getSqlConn().releaseSavepoint(svpnt);
	}   
	   
	@Override   
	public Statement createStatement(int i, int i1, int i2) throws SQLException {
		return(this.getSqlConn().createStatement(i, i1, i2));
	}   
	   
	@Override   
	public PreparedStatement prepareStatement(String string, int i, int i1, int i2) throws SQLException {
		return(this.getSqlConn().prepareStatement(string, i, i1, i2));
	}   
	   
	@Override   
	public CallableStatement prepareCall(String string, int i, int i1, int i2) throws SQLException {
		return(this.getSqlConn().prepareCall(string, i, i1, i2));
	}   
	   
	@Override   
	public PreparedStatement prepareStatement(String string, int i) throws SQLException {
		return(this.getSqlConn().prepareStatement(string, i));
	}   
	   
	@Override   
	public PreparedStatement prepareStatement(String string, int[] ints) throws SQLException {
		return(this.getSqlConn().prepareStatement(string, ints));
	}   
	   
	@Override   
	public PreparedStatement prepareStatement(String string, String[] strings) throws SQLException {
		return(this.getSqlConn().prepareStatement(string, strings));
	}   
	   
	@Override   
	public Clob createClob() throws SQLException {
		return(this.getSqlConn().createClob());
	}   
	   
	@Override   
	public Blob createBlob() throws SQLException {
		return(this.getSqlConn().createBlob());
	}   
	   
	@Override   
	public NClob createNClob() throws SQLException {
		return(this.getSqlConn().createNClob());
	}   
	   
	@Override   
	public SQLXML createSQLXML() throws SQLException {
		return(this.getSqlConn().createSQLXML());
	}   
	   
	@Override   
	public boolean isValid(int i) throws SQLException {
		return(this.getSqlConn().isValid(i));
	}   
	   
	@Override   
	public void setClientInfo(String string, String string1) throws SQLClientInfoException {
		this.getSqlConn().setClientInfo(string, string1);
	}   
	   
	@Override   
	public void setClientInfo(Properties prprts) throws SQLClientInfoException {
		this.getSqlConn().setClientInfo(prprts);
	}   
	   
	@Override   
	public String getClientInfo(String string) throws SQLException {
		return(this.getSqlConn().getClientInfo(string));
	}   
	   
	@Override   
	public Properties getClientInfo() throws SQLException {
		return(this.getSqlConn().getClientInfo());
	}   
	   
	@Override   
	public Array createArrayOf(String string, Object[] os) throws SQLException {
		return(this.getSqlConn().createArrayOf(string, os));
	}   
	   
	@Override   
	public Struct createStruct(String string, Object[] os) throws SQLException {
		return(this.getSqlConn().createStruct(string, os));
	}   
	   
	@Override   
	public void setSchema(String string) throws SQLException {
		this.getSqlConn().setSchema(string);
	}   
	   
	@Override   
	public String getSchema() throws SQLException {
		return(this.getSqlConn().getSchema());
	}   
	   
	@Override   
	public void abort(Executor exctr) throws SQLException {
		this.getSqlConn().abort(exctr);
	}   
	   
	@Override   
	public void setNetworkTimeout(Executor exctr, int i) throws SQLException {
		this.getSqlConn().setNetworkTimeout(exctr, i);
	}   
	   
	@Override   
	public int getNetworkTimeout() throws SQLException {
		return(this.getSqlConn().getNetworkTimeout());
	}   
	   
	@Override   
	public <T> T unwrap(Class<T> type) throws SQLException {
		return(this.getSqlConn().unwrap(type));
	}   
	   
	@Override   
	public boolean isWrapperFor(Class<?> type) throws SQLException {
		return(this.getSqlConn().isWrapperFor(type));
	}   
}