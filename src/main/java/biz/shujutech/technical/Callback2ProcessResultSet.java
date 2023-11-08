package biz.shujutech.technical;

import biz.shujutech.base.Connection;
import java.sql.ResultSet;

@FunctionalInterface
public interface Callback2ProcessResultSet {
	public boolean processResultSet(Connection aConn, ResultSet aResultSet) throws Exception;
}
