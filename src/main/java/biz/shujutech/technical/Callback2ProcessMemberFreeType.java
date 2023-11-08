package biz.shujutech.technical;

import biz.shujutech.base.Connection;

@FunctionalInterface
public interface Callback2ProcessMemberFreeType {
	public boolean processClasz(Connection aConn, Object aClasz) throws Exception;
}

