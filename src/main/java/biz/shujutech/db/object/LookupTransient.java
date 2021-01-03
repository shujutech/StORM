package biz.shujutech.db.object;

import biz.shujutech.base.Connection;
import java.util.concurrent.CopyOnWriteArrayList;

public class LookupTransient extends Clasz implements Lookup {
	private String descr;
	private CopyOnWriteArrayList<Lookup> LookupList = null;

	@Override
	public String getDescr() throws Exception {
		return(this.descr);
	}

	@Override
	public void setDescr(String aDescr) throws Exception {
		this.descr = aDescr;
	}

	@Override
	public String getValueStr() throws Exception {
		return(this.getDescr());
	}

	@Override
	public CopyOnWriteArrayList<Lookup> getLookupList() {
		return this.LookupList;
	}

	public void setLookupList(CopyOnWriteArrayList<Lookup> aLookup) {
		this.LookupList = aLookup;
	}

	@Override
	public void initialize(Connection aConn) throws Exception {
		// do nothing
	}

}
