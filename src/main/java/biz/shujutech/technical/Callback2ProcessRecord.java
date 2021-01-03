package biz.shujutech.technical;

import biz.shujutech.base.Connection;
import biz.shujutech.db.relational.Record;

@FunctionalInterface
public interface Callback2ProcessRecord {
	public boolean processRecord(Connection aConn, Record aRecord) throws Exception;
	
}
