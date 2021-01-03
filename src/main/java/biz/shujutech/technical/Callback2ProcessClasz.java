package biz.shujutech.technical;

import biz.shujutech.base.Connection;
import biz.shujutech.db.object.Clasz;

@FunctionalInterface
public interface Callback2ProcessClasz {
	public boolean processClasz(Connection aConn, Clasz aClasz) throws Exception;
}

