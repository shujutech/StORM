package biz.shujutech.db.object;

import biz.shujutech.base.Connection;
import java.util.concurrent.CopyOnWriteArrayList;

public class LookupList extends Clasz implements Lookup {
	private String descr;
	private final CopyOnWriteArrayList<Lookup> LookupList = new CopyOnWriteArrayList<>();

	@Override
	public String getDescr() throws Exception {
		return(this.descr);
	}

	@Override
	public void setDescr(String aDescr) throws Exception {
		this.descr = aDescr;
	}

	@Override
	public CopyOnWriteArrayList<Lookup> getLookupList() throws Exception {
		return(this.LookupList);
	}

	@Override
	public void initialize(Connection aConn) throws Exception {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public String getValueStr() throws Exception {
		return(this.getDescr());
	}

	public void addItem(LookupTransient aLookupItem) throws Exception {
		aLookupItem.setLookupList(this.getLookupList());
		this.getLookupList().add(aLookupItem);
	}
}
