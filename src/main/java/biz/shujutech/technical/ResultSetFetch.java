package biz.shujutech.technical;

import biz.shujutech.base.Connection;
import biz.shujutech.base.Hinderance;
import biz.shujutech.db.object.Clasz;
import biz.shujutech.db.relational.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class ResultSetFetch {
	private final PreparedStatement stmt = null;
	private ResultSet rs = null;
	private Connection conn;

	public <Ty extends Clasz<?>> void forEachFetch(Connection aConn, Class<Ty> aClass, String aSqlToGetObjId, List<Field> aPositionalParamValue, Callback2ProcessMember<Ty> aCallback) throws Exception {
		this.conn = aConn;
		String pkColName = Clasz.CreatePkColName(aClass);
		this.rs = Clasz.Fetch(this.conn, this.stmt, aSqlToGetObjId, aPositionalParamValue);
		try {
			while(this.rs.next()) {
				Long objId = this.rs.getLong(pkColName);
				//Clasz<?> clasz = Clasz.Fetch(this.conn, this.fetchClass, objId);
				Clasz<?> clasz = Clasz.Fetch(this.conn, aClass, objId);
				aCallback.processClasz(aConn, aClass.cast(clasz));
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

	public <Ty extends Clasz<?>> void forEachFetchFreeType(Connection aConn, Class<?> aClass, String aSqlToGetObjId, List<Field> aPositionalParamValue, Callback2ProcessMemberFreeType aCallback) throws Exception {
		this.conn = aConn;
		String pkColName = Clasz.CreatePkColName(aClass);
		this.rs = Clasz.Fetch(this.conn, this.stmt, aSqlToGetObjId, aPositionalParamValue);
		try {
			while(this.rs.next()) {
				Long objId = this.rs.getLong(pkColName);
				Clasz<?> clasz = Clasz.FetchFreeType(this.conn, aClass, objId);
				aCallback.processClasz(aConn, aClass.cast(clasz));
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
}
