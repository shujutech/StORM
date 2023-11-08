package biz.shujutech.db.object;

import biz.shujutech.base.Connection;
import biz.shujutech.db.relational.Database;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.CopyOnWriteArrayList;

public interface Lookup {
	public String getValueStr() throws Exception;
	public String getDescr() throws Exception;
	public void setDescr(String aDescr) throws Exception;
	public CopyOnWriteArrayList<Lookup> getLookupList() throws Exception;
	public void initialize(Connection aConn) throws Exception;

	public static Lookup InsertDefaultList(Connection aConn, Lookup aInstant, Class<?> aClass, String aDescr, CopyOnWriteArrayList<Lookup> aLookupList) throws Exception {
		if (aInstant == null) {
			aInstant = GetListItem(aDescr, aLookupList); // if item is already in list, no need db operation
			if (aInstant == null) {
				Lookup criteria = (Lookup) ObjectBase.CreateObjectFromAnyClass(aConn, aClass);
				criteria.setDescr(aDescr);
				aInstant = (Lookup) ObjectBase.FetchObject(aConn, (Clasz<?>) criteria);
				if (aInstant == null) {
					criteria.initialize(aConn);
					ObjectBase.PersistCommit(aConn, (Clasz<?>) criteria);
					aInstant = criteria;
				} else {
					aInstant.initialize(aConn); // do whatever needed after existing object is Fetch from db
				}
				Add2LookupList(aLookupList, aInstant);
			} else {
				aInstant.initialize(aConn); 
			}
		}
		return(aInstant);
	}

	public static Lookup GetListItem(String aDescr, CopyOnWriteArrayList<Lookup> aLookupList) throws Exception {
		Lookup result = null;
		for (Lookup eachItem : aLookupList) {
			if (eachItem.getDescr().equals(aDescr)) {
				result = eachItem;
			}
		}
		return(result);
	}

	public static void ClearAndLoadList(Connection aConn, Class<?> aClass, CopyOnWriteArrayList<Lookup> aLookupList) throws Exception {
		PreparedStatement stmt = null;
		ResultSet rset = null;
		Lookup objFetched;
		aLookupList.clear();
		try {
			rset = ObjectBase.FetchAllByChrono(aConn, aClass, stmt);
			while((objFetched = (Lookup) ObjectBase.FetchNext(aConn, aClass, rset)) != null) {
				objFetched.initialize(aConn);
				Add2LookupList(aLookupList, objFetched);
			}
		} finally {
			if (rset != null) {
				rset.close();
			}
			if (stmt != null) {
				stmt.close();
			}
		}
	}

	public static void Add2LookupList(Lookup aThis) throws Exception {
		if (aThis.getLookupList().contains(aThis) == false) {
			aThis.getLookupList().add(aThis);
		}
	}

	public static void Add2LookupList(CopyOnWriteArrayList<Lookup> aLookupList, Lookup aToBeAdded) throws Exception {
		if (aLookupList.contains(aToBeAdded) == false) {
			aLookupList.add(aToBeAdded);
		}
	}

	public static Lookup GetSelectedLookup(CopyOnWriteArrayList<Lookup> aLookupList, String aLookupName) throws Exception {
		Lookup result = null;
		for(Lookup eachLookup : aLookupList) {
			if (eachLookup.getDescr().equalsIgnoreCase(aLookupName)) {
				result = eachLookup;
				break;
			}
		}

		return(result);
	}

	public static <Ty extends Clasz<?>> Clasz<?> CreateLookupClasz(Connection aConn, String aFieldName, Class<Ty> aLookupClass) throws Exception {
		Clasz<?> masterClasz = ObjectBase.CreateObject(aConn, Master.class);
		Ty memberLookup = ObjectBase.CreateObject(aConn, aLookupClass);
		String dbFieldName = Database.Java2DbFieldName(aFieldName);
		FieldObject<Ty> memberField = masterClasz.createFieldObject(dbFieldName, memberLookup);
		memberField.setLookup(true);
		memberField.setDeclareType(aLookupClass.getName());
		masterClasz.populateLookupField(aConn);
		return masterClasz;
	}

}
