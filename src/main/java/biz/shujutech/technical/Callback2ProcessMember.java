package biz.shujutech.technical;

import biz.shujutech.base.Connection;
import biz.shujutech.db.object.Clasz;

@FunctionalInterface
public interface Callback2ProcessMember<Ty extends Clasz<?>> {
	public boolean processClasz(Connection aConn, Ty aClasz) throws Exception;
}

