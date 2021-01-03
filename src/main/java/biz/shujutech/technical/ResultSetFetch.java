package biz.shujutech.technical;

import biz.shujutech.base.Connection;
import biz.shujutech.base.Hinderance;
import biz.shujutech.db.object.Clasz;
import biz.shujutech.db.relational.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class ResultSetFetch{
	private final PreparedStatement stmt = null;
	private ResultSet rs = null;
	private Connection conn;
	private Class fetchClass;

	public void forEachFetch(Connection aConn, Class aClass, String aSqlToGetObjId, List<Field> aPositionalParamValue, Callback2ProcessClasz aCallback) throws Exception {
		this.conn = aConn;
		this.fetchClass = aClass;
		String pkColName = Clasz.CreatePkColName(this.fetchClass);
		this.rs = Clasz.Fetch(this.conn, this.stmt, aSqlToGetObjId, aPositionalParamValue);
		try {
			while(this.rs.next()) {
				Long objId = this.rs.getLong(pkColName);
				Clasz clasz = Clasz.Fetch(this.conn, this.fetchClass, objId);
				aCallback.processClasz(aConn, clasz);
			}
		} catch (Exception ex) {
			throw new Hinderance(ex, "Fail sql resultsetFetch: " + aSqlToGetObjId);
		} finally {
			if (this.rs != null) {
				this.rs.close();
			}
			if (stmt != null) {
				this.stmt.close();
			}
		}
	}

	/*
	private Table table;
	public void forEachFetch(Connection aConn, Class aClass, Record aWhere, ResultSetFetchIntf aCallback) throws Exception {
		this.conn = aConn;
		this.clasz = aClass;
		this.table = new Table(Clasz.CreateTableName(this.clasz.getSimpleName()));
		Record selectRec = new Record();
		String pkColName = Clasz.CreatePkName(this.clasz.getSimpleName());
		selectRec.createFieldObject(pkColName, FieldType.LONG);
		this.rs = Clasz.Fetch(this.conn, this.stmt, this.table.getTableName(), selectRec, aWhere);
		try {
			while(this.rs.next()) {
				Long objId = this.rs.getLong(pkColName);
				Clasz clasz = Clasz.Fetch(this.conn, this.clasz.getClass(), objId);
				aCallback.callback(clasz);
			}
		} catch (Exception ex) {
			throw new Hinderance(ex, "Fail sql resultsetFetch: " + this.stmt.toString());
		} finally {
			if (this.rs != null) {
				this.rs.close();
			}
			if (stmt != null) {
				this.stmt.close();
			}
		}
	}
	*/
}
