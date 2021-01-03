package biz.shujutech.db.object;

import biz.shujutech.base.App;
import biz.shujutech.base.Connection;
import biz.shujutech.base.DateAndTime;
import biz.shujutech.base.Hinderance;
import biz.shujutech.db.relational.Database;
import biz.shujutech.db.relational.Field;
import biz.shujutech.db.relational.FieldDate;
import biz.shujutech.db.relational.FieldDateTime;
import biz.shujutech.db.relational.FieldType;
import biz.shujutech.db.relational.Record;
import biz.shujutech.db.relational.SortOrder;
import biz.shujutech.db.relational.Table;
import biz.shujutech.technical.LambdaCounter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.joda.time.DateTime;
import biz.shujutech.technical.Callback2ProcessClasz;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class FieldObjectBox extends FieldClasz {

	private ConcurrentHashMap<Long, Clasz> objectMap = new ConcurrentHashMap<>();
	private Iterator iterator = null;
	private Long key;
	private Clasz metaObj;
	private int fetchSize = 5;

	public FieldObjectBox() {
		this.setAtomic(false);
	}

	public FieldObjectBox(Clasz aStoreType) throws Exception {
		this();
		this.metaObj = aStoreType;
	}

	@Override
	public String getValueStr() throws Exception {
		String result = "";
		for(Clasz eachClasz : this.getObjectMap().values()) {
			result = eachClasz.getValueStr();
			if (result.isEmpty() == false) {
				break;
			}
		}
		return(result);
	}

	public Clasz getMetaObj() {
		return metaObj;
	}

	public void setMetaObj(Clasz metaObj) {
		this.metaObj = metaObj;
	}

	public String getMetaType() {
		return(this.metaObj.getClass().getName()) ;
	}

	public Class getMemberClass() {
		return(this.metaObj.getClass());
	}

	public ConcurrentHashMap<Long, Clasz> getObjectMap() {
		return objectMap;
	}

	public void setObjectMap(ConcurrentHashMap<Long, Clasz> objectMap) {
		this.objectMap = objectMap;
	}

	public Clasz getObject(long aIndex) throws Exception {
		this.mustFetch();
		Clasz result = this.getObjectMap().get(aIndex); 
		return(result);
	}

	public synchronized void addValueObject(Clasz aObject) throws Exception {
		this.setModified(true);
		long index = this.getObjectMap().size();
		aObject.setMasterObject(this.getMasterObject());
		this.getObjectMap().put(index, aObject);
	}

	@Override
	public void cloneField(Field aSourceField) throws Exception {
		this.copyFieldObjectBox(aSourceField, false);
		this.copyAttribute(aSourceField);
	}

	@Override
	public void copyValue(Field aSourceField) throws Exception {
		this.copyFieldObjectBox(aSourceField, true);
	}


	public void copyByRef(FieldObjectBox aSourceField) throws Exception {
		for (Entry eachMember : aSourceField.getObjectMap().entrySet()) {
			Clasz obj = (Clasz) eachMember.getValue();
			this.addValueObject(obj);
		}
	}

	public void copyFieldObjectBox(Field aSourceField, boolean isCopy) throws Exception {

		if ((aSourceField instanceof FieldObjectBox) == false) { // validate the field type
			throw new Hinderance("Error, can only copy between array fields source: " + aSourceField.getFieldName() + ", target: " + this.getMetaType());
		}
		
		FieldObjectBox source = (FieldObjectBox) aSourceField;
		if (source.getMetaType().equalsIgnoreCase(this.getMetaType()) == false) { // validate the class type
			throw new Hinderance("Error, copy array fields must be of same type, source type: " + source.getMetaType() + ", target type: " + this.getMetaType());
		}

		for (Entry recEach : source.getObjectMap().entrySet()) {
			// for each object in the object box of aSourceField
			Long idx = (Long) recEach.getKey();
			Clasz obj = (Clasz) recEach.getValue();
			Clasz targetObj = this.getObject(idx);
			if (targetObj == null) {
				targetObj = (Clasz) Class.forName(source.getMetaType()).newInstance();
			}
			if (isCopy) {
				targetObj.copyAllFieldWithoutModifiedState(obj);
			} else {
				targetObj.copyAllFieldWithModifiedState(obj);
			}
		}
	}

	public boolean containObjectId(Long aObjId) throws Exception {
		boolean result = false;
		for(Clasz obj : this.getObjectMap().values()) {
			//App.logInfo("Comparing field objectbox id: " + obj.getObjectId() + ", with: " + aObjId);
			if (obj.getObjectId().equals(aObjId)) {
				result = true;
				break;
			}
		}
		return(result);
	}

	
	public void sort() throws Exception {
		this.sortObjectBox();
	}

	/**
	 * Sort the objects in this FieldObjectBox according to the fields that has
	 * been mark as sorting keys. 
	 * 
	 * http://www.mkyong.com/java/how-to-sort-a-map-in-java/
	 * 
	 * @throws Exception 
	 */
	public void sortObjectBox() throws Exception {
		this.setSortKey2Data();
		Map<Long, Clasz> sortedBox = SortByComparator(this.getObjectMap());
		this.getObjectMap().clear();
		sortedBox.entrySet().forEach((entry) -> {
			this.getObjectMap().put(entry.getKey(), entry.getValue());
		});
	}

	/**
	 * Comparing all the objects in this container with another set of objects
	 * in another container.
	 * 
	 * @param aLeft
	 * @param aRight
	 * @return
	 * @throws Exception 
	 */
	public static int compare(FieldObjectBox aLeft, FieldObjectBox aRight) throws Exception {
		int result = 0;
		int smallest = Integer.MIN_VALUE;
		int largest = Integer.MAX_VALUE;
		int setCompare;

		for (Clasz left : aLeft.getObjectMap().values()) {
			for (Clasz right : aRight.getObjectMap().values()) {
				setCompare = left.compareTo(right);
				if (setCompare > largest) {
					largest = setCompare;
				}
				if (setCompare < smallest) {
					smallest = setCompare;
				}
				// can do break to avoid redundant processing??
			}
		}

		// if sort order is ASC? not sure, need to test it
		if (smallest != Integer.MIN_VALUE) {
			result = smallest;
		}
		return(result);
	}

	@Override
	public int compareTo(Object aRight) {
		int result = 0;
		try {
			result = FieldObjectBox.compare(this, (FieldObjectBox) aRight);
		} catch (Exception ex) {
			App.logEror(ex);
		}
		return(result);
	}
	
	/**
	 * Clears all the sort keys that was set previously. This include both 
	 * inherited fields and member variable fields.
	 * 
	 * @throws Exception 
	 */
	public void clearAllSortKey() throws Exception {
		clearAllSortKey(this.getMetaObj());
	}

	private static void clearAllSortKey(Clasz aClasz2Clear) throws Exception {
		for(Field eachField : aClasz2Clear.getTreeField().values()) {
			if (eachField.isAtomic()) {
				if (eachField.isSortKey()) {
					eachField.clearSortKey();
				}
			} else {
				if (eachField.getFieldType() == FieldType.OBJECT) {
					Clasz clasz = ((FieldObject) eachField).getObj();
					if (clasz != null) {
						clearAllSortKey(clasz); // recursive call to clear the member object sort keys
					}
				} else if (eachField.getFieldType() == FieldType.OBJECTBOX) {
					Clasz clasz = ((FieldObjectBox) eachField).getMetaObj();
					clearAllSortKey(clasz); // recursive call to clear the meta object sort keys
				} else {
					throw new Hinderance("Invalid field type in object: " + aClasz2Clear.getClaszName().toUpperCase());
				}
			}
		}
	}
	
	public Field getMetaField(String aFieldName) throws Exception {
		return(this.getMetaObj().getField(aFieldName));
	}

	public Clasz getMetaObject(String aFieldName) throws Exception {
		Clasz result = ((FieldObject) this.getMetaObj().getField(aFieldName)).getObj();
		if (result == null) {
			Connection conn = null;
			ObjectBase db = this.getMasterObject().getDb();
			String className = this.getMetaObj().getFieldObj(aFieldName).getDeclareType();
			try {
				conn = db.getConnPool().getConnection();
				result = Clasz.CreateObject(db, conn, Class.forName(className));
			} finally {
				if (conn != null) {
					db.getConnPool().freeConnection(conn);
				}
			}
		}
		return(result);
	}

	/**
	 * Assigned the sort keySeq properties to all of the data object in this
 container. This is required in Clasz compare method. The Clasz object
	 * that're being compare uses the keys as the sort criteria.
	 * 
	 * @param aClaszMeta
	 * @param aClaszData
	 * @throws Exception 
	 */
	private void setSortKey2Data() throws Exception {
		for(Clasz dataClasz : this.getObjectMap().values()) {
			setSortKey2Data(this.getMetaObj(), dataClasz);
		}
	}

	private static void setSortKey2Data(Clasz aClaszMeta, Clasz aClaszData) throws Exception {
		for(Field metaField : aClaszMeta.getTreeField().values()) {
			Field dataField = aClaszData.getField(metaField.getFieldName());
			if (metaField.isAtomic()) {
				dataField.setSortKey(metaField);
			} else {
				if (metaField.getFieldType() == FieldType.OBJECT) {
					Clasz claszMeta = ((FieldObject) metaField).getObj();
					Clasz claszData = ((FieldObject) dataField).getObj();
					if (claszData != null && (claszData.getClass().getSimpleName().equals("Clasz")) == false) { 
						setSortKey2Data(claszMeta, claszData); // recursive call to clear the member object sort keys
					}
				} else if (metaField.getFieldType() == FieldType.OBJECTBOX) {
					Clasz claszMeta = ((FieldObjectBox) metaField).getMetaObj();
					for(Clasz eachClasz : ((FieldObjectBox) dataField).getObjectMap().values()) {
						if ((eachClasz.getClass().getSimpleName().equals("Clasz")) == false) { 
							setSortKey2Data(claszMeta, eachClasz);
						}
					}
				} else {
					throw new Hinderance("Invalid field type in object: " + aClaszMeta.getClaszName().toUpperCase());
				}
			}
		}
	}
	
	public long getTotalMember() {
		return(this.getObjectMap().size());
	}

	public Clasz fetchByObjectId(Connection aConn, Long aObjId, String aObjClasz) throws Exception {
		return(this.fetchByObjectId(aConn, String.valueOf(aObjId), aObjClasz));
	}

	public Clasz fetchByObjectId(Connection aConn, String aObjId, String aObjClasz) throws Exception {
		List<Clasz> aryFetchMember = new CopyOnWriteArrayList<>(); // for pass by ref only	
		String strSql = "select " + this.getFieldName() + ", " + ObjectBase.LEAF_CLASS 
		+ " from " + this.getMasterObject().getIwTableName(this.getFieldName()) 
		+ " where " + this.getMasterObject().getPkName() + " = " + this.getMasterObject().getObjectId()
		+ " and " + this.getFieldName() + " = " + aObjId; // getFieldName must convert to low level db field name?
		if (this.isPolymorphic()) {
			strSql += " and " + ObjectBase.LEAF_CLASS + " = " + "'" + aObjClasz + "'";
		}
		Clasz fetchMember = null;
		try {
			FetchStatus status = FetchMemberOfBoxObject(aConn, this, this.getMasterObject(), strSql, aryFetchMember, null);
			this.setFetchStatus(status);
			fetchMember = aryFetchMember.get(0);
		} catch(Exception ex) {
			throw new Hinderance(ex, "Fail to fetchByObjectId, sql: " + strSql);
		}
		return(fetchMember);
	}

	public void fetchByCustomSql(Connection aConn, String aCustomSql) throws Exception {
		FetchStatus status = FetchMemberOfBoxObject(aConn, this, this.getMasterObject(), aCustomSql);
		this.setFetchStatus(status);
	}

	public void fetchByWhereRec(Connection aConn, Record recWhere) throws Exception {
		String claszTableName = this.getMetaObj().getTableName();
		String claszTablePk = this.getMetaObj().getPkName();
		StringBuffer strBuffer = new StringBuffer();
		List<Field> aryWhere = Database.GetWhereClause(claszTableName, recWhere, strBuffer);
		String strSqlAnd = " and " + strBuffer.toString();

		String boxMemberTableName = this.getMasterObject().getIwTableName(this.getFieldName());
		String strJoinSql = boxMemberTableName + "." + this.getFieldName() + " = " + claszTableName + "." + claszTablePk;
		String strSql = "select " + this.getFieldName() + ", " + ObjectBase.LEAF_CLASS 
		+ " from " + boxMemberTableName  + ", " + claszTableName
		+ " where " + boxMemberTableName + "." + this.getMasterObject().getPkName() + " = " + this.getMasterObject().getObjectId()
		+ " and " + strJoinSql
		+ strSqlAnd;

		App.logDebg(strSql);
		this.fetchByCustomSql(aConn, strSql);
	}

	public void fetchAll(Connection aConn) throws Exception {
		if (this.getTotalMember() == 0) {
			String strSql = "select " + this.getFieldName() + ", " + ObjectBase.LEAF_CLASS 
			+ " from " + this.getMasterObject().getIwTableName(this.getFieldName()) 
			+ " where " + this.getMasterObject().getPkName() + " = " + this.getMasterObject().getObjectId();
			FetchStatus status = FetchMemberOfBoxObject(aConn, this, this.getMasterObject(), strSql);
			this.setFetchStatus(status);
		} else {
			if (this.getMasterObject().getObjectId() != Clasz.NOT_INITIALIZE_OBJECT_ID) { // if this is new object, do nothing actually, don't need do fetchAll, don't need warn user
				//throw new Hinderance("There must be no member when doing fetchaAll, total member: " + this.getTotalMember());
			}
		}
	}

	public static FetchStatus FetchMemberOfBoxObject(Connection aConn, FieldObjectBox aField, Clasz aMasterObj, String strSql) throws Exception {
		List<Clasz> aryFetchMember = new CopyOnWriteArrayList<>(); // for pass by ref only	
		FetchStatus status = FetchMemberOfBoxObject(aConn, aField, aMasterObj, strSql, aryFetchMember, null);
		return(status);
	}

	public boolean gotMember(Connection aConn, Clasz aMember) throws Exception {
		boolean result = false;
		String strSql = "select " + this.getFieldName() + ", " + ObjectBase.LEAF_CLASS 
		+ " from " + this.getMasterObject().getIwTableName(this.getFieldName()) 
		+ " where " + this.getMasterObject().getPkName() + " = " + this.getMasterObject().getObjectId();
		PreparedStatement stmt = null;
		ResultSet rset = null;
		try {
			stmt = aConn.prepareStatement(strSql);
			rset = stmt.executeQuery();
			while (rset.next()) {
				long pk = rset.getLong(1);
				String leaf = rset.getString(2);
				if (aMember.getObjectId().equals(pk) && leaf.equals(aMember.getClass().getName())) {
					result = true;
					break;
				}
			}
		} catch (Exception ex) {
			if (stmt == null) {
				throw new Hinderance(ex, "Fail to check if member exist for type: " + aMember.getClass().getSimpleName());
			} else {
				throw new Hinderance(ex, "Fail to check if member exist: " + stmt.toString());
			}
		} finally {
			if (rset != null) {
				rset.close();
			}
			if (stmt != null) {
				stmt.close();
			}
		}
		return(result);
	}

	public void forEachMember(Connection aConn, Callback2ProcessClasz aCallback) throws Exception {
		this.forEachMember(aConn, "all", aCallback);
	}

	public void forEachMember(Connection aConn, Clasz aCriteria, Callback2ProcessClasz aCallback) throws Exception {
		String strSql;
		//Map<String, Record> whereBox = new ConcurrentHashMap<>(); // each table name (string) and a record for the where fields (record)
		Multimap<String, Record> whereBox = Multimaps.synchronizedListMultimap(ArrayListMultimap.create());
		ObjectBase.GetMemberBoxByMemberCriteria(aConn, this, aCriteria, whereBox); // the select criteria for this leaf clszObject, doesn't do the parent clszObject
		List<Field> aryWhere = null;
		if (whereBox.isEmpty() == false) {
			StringBuffer strBuffer = new StringBuffer();
			aryWhere = Database.GetWhereClause(whereBox, strBuffer); // convert the where record into array list
			String mainTableName = this.getMasterObject().getIwTableName(this.getFieldName());
			strSql = "select " + this.getFieldName() + ", " + ObjectBase.LEAF_CLASS 
			+ " from " + Database.GetFromClause(mainTableName, whereBox);
			strSql += " where " + strBuffer.toString();
		} else {
			strSql = "select " + this.getFieldName() + ", " + ObjectBase.LEAF_CLASS 
			+ " from " + this.getMasterObject().getIwTableName(this.getFieldName()) 
			+ " where " + this.getMasterObject().getPkName() + " = " + this.getMasterObject().getObjectId();
		}
		FetchStatus status = FetchMemberOfBoxObject(aConn, this, this.getMasterObject(), strSql, null, aCallback, aryWhere);
		this.setFetchStatus(status);
	}

	public void forEachMember(Connection aConn, String csvMemberList, Callback2ProcessClasz aCallback) throws Exception {
		String strSql = "select " + this.getFieldName() + ", " + ObjectBase.LEAF_CLASS 
		+ " from " + this.getMasterObject().getIwTableName(this.getFieldName()) 
		+ " where " + this.getMasterObject().getPkName() + " = " + this.getMasterObject().getObjectId();
		if (csvMemberList.equals("all") == false) {
			//strSql += " and " + this.getFieldName() + " in (" + csvMemberList + ")";
			strSql += " and (";
			String selectedPair = "";
			String[] employmentPair = csvMemberList.split(",");
			for(int cntr = 0; cntr < employmentPair.length; cntr+=2) {
				if (!selectedPair.isEmpty()) selectedPair += " or ";
				selectedPair += "(";
				selectedPair += this.getFieldName() + " = " + employmentPair[cntr].trim();
				selectedPair += " and " + ObjectBase.LEAF_CLASS + " = '" + employmentPair[cntr + 1].trim() + "'";
				selectedPair += ")";
			}
			strSql += selectedPair;
			strSql += ")";
		}	
		FetchStatus status = FetchMemberOfBoxObject(aConn, this, this.getMasterObject(), strSql, null, aCallback);
		this.setFetchStatus(status);
	}

	public static void RemoveStaleMember(Connection aConn, Clasz aMasterObj, FieldObjectBox aFob, Long aMemberOid) throws Exception {
		String strSql = "delete from " + aMasterObj.getIwTableName(aFob.getFieldName()) 
		+ " where " + aMasterObj.getPkName() + " = " + aMasterObj.getObjectId()
		+ " and " + aFob.getFieldName() + " = " + aMemberOid;
		PreparedStatement stmt = null;
		Connection conn = null;
		try {
			App.logWarn(FieldObjectBox.class, "Removing stale member, sql: " + strSql);
			//conn = aConn.getBaseDb().getConnPool().getConnection(); // need new connection for this so it'll not interfere with aConn processing, this is one of the reason min 2 conn is needed for each session
			//conn.setAutoCommit(true);
			conn = aConn;
			stmt = conn.prepareStatement(strSql);
			stmt.executeUpdate();
		} catch (Exception ex) {
			App.logWarn(FieldObjectBox.class, ex, "Fail to remove stale member, sql: " + strSql);
		} finally {
			if (stmt != null) {
				stmt.close();
			}
			if (conn != null) {
				//aConn.getBaseDb().getConnPool().freeConnection(conn); 
			}
		}
	}

	public static FetchStatus FetchMemberOfBoxObject(Connection aConn, FieldObjectBox aFob, Clasz aMasterObj, String strSql, List<Clasz> aMemberContainer, Callback2ProcessClasz aCallback) throws Exception {
		return(FetchMemberOfBoxObject(aConn, aFob, aMasterObj, strSql, aMemberContainer, aCallback, null));

	}

	public static FetchStatus FetchMemberOfBoxObject(Connection aConn, FieldObjectBox aFob, Clasz aMasterObj, String strSql, List<Clasz> aMemberContainer, Callback2ProcessClasz aCallback, List<Field> aAryWhere) throws Exception {
		FetchStatus result = FetchStatus.SOF;
		PreparedStatement stmt = null;
		ResultSet rset = null;
		try {
			stmt = aConn.prepareStatement(strSql);
			if(aAryWhere != null) Database.SetStmtValue(stmt, aAryWhere);
			rset = stmt.executeQuery();
			aFob.getObjectMap().clear();
			while (rset.next()) {
				long pk = rset.getLong(1);
				Class leafClass = Class.forName(aFob.getDeclareType());
				String polymorphicName = rset.getString(2);
				if (polymorphicName != null && polymorphicName.isEmpty() == false) {
					leafClass = Class.forName(polymorphicName); // if its a polymorphic member field
				}
				Clasz fetchMember = Clasz.fetchObjectByPk(aMasterObj.getDb(), aConn, leafClass, pk);
				if (fetchMember.getObjectId().equals(Clasz.NOT_INITIALIZE_OBJECT_ID)) {
					RemoveStaleMember(aConn, aMasterObj, aFob, pk);
				} else {
					if (aMemberContainer != null) {
						aMemberContainer.add(fetchMember);
					}
					aFob.addValueObject(fetchMember);  // now put the new member into the field object box
					if (aCallback != null) {
						if (aCallback.processClasz(aConn, fetchMember) == false) {
							break;
						}
					}
				}
			}
			result = FetchStatus.EOF;
		} catch (Exception ex) {
//App.logEror(FieldObjectBox.class, ex);
			if (stmt == null) {
				throw new Hinderance(ex, "FetchMemberOfBoxObject - Fail to populate object array: null");
			} else {
				throw new Hinderance(ex, "FetchMemberOfBoxObject - Fail to populate object array: " + stmt.toString());
			}
		} finally {
			if (rset != null) {
				rset.close();
			}
			if (stmt != null) {
				stmt.close();
			}
		}
		return(result);
	}

	public FetchStatus fetchByObjectIndex(Connection aConn, String aSortField, int aBatchSize, String aIndexName, String aWhereClause) throws Exception {
		return(this.fetchBySection(aConn, "next", aBatchSize, aSortField, null, aIndexName, aWhereClause));
	}

	public FetchStatus fetchBySection(Connection aConn, String aPageDirection, String aSortField, String aSortValue, String aIndexName) throws Exception {
		return(this.fetchBySection(aConn, aPageDirection, aSortField, aSortValue, aIndexName, ""));
	}

	public FetchStatus fetchBySection(Connection aConn, String aPageDirection, String aSortField, String aSortValue, String aIndexName, String aWhereClause) throws Exception {
		int pageSize = this.getFetchSize();
		return(this.fetchBySection(aConn, aPageDirection, pageSize, aSortField, aSortValue, aIndexName, aWhereClause));
	}

	public FetchStatus fetchBySection(Connection aConn, String aPageDirection, int aFetchSize, String aSortField, String aSortValue, String aIndexName) throws Exception {
		return(this.fetchBySection(aConn, aPageDirection, aFetchSize, aSortField, aSortValue, aIndexName, ""));

	}

	public FetchStatus fetchBySection(Connection aConn, String aPageDirection, int aFetchSize, String aSortField, String aSortValue, String aIndexName, String aWhereClause) throws Exception {
		//FetchStatus result = this.fetchBySection(aConn, aPageDirection, aFetchSize, aSortField, aSortValue, aIndexName, SortOrder.ASC, aWhereClause);
		List<String> sortFieldList = new CopyOnWriteArrayList<>();
		List<String> sortValueList = new CopyOnWriteArrayList<>();
		List<SortOrder> sortOrderList = new CopyOnWriteArrayList<>();
		sortFieldList.add(aSortField);
		sortValueList.add(aSortValue);
		sortOrderList.add(SortOrder.ASC);
		FetchStatus result = this.fetchBySection(aConn, aPageDirection, aFetchSize, sortFieldList, sortValueList, sortOrderList, aIndexName, aWhereClause);
		return(result);
	}

	public FetchStatus fetchBySection(Connection aConn, String aPageDirection, int aFetchSize, String aSortField, String aSortValue, String aIndexName, SortOrder aDisplayOrder) throws Exception {
		List<String> sortFieldList = new CopyOnWriteArrayList<>();
		List<String> sortValueList = new CopyOnWriteArrayList<>();
		List<SortOrder> sortOrderList = new CopyOnWriteArrayList<>();
		sortFieldList.add(aSortField);
		sortValueList.add(aSortValue);
		sortOrderList.add(aDisplayOrder);
		//return(this.fetchBySection(aConn, aPageDirection, aFetchSize, aSortField, aSortValue, aIndexName, aDisplayOrder, ""));
		return(this.fetchBySection(aConn, aPageDirection, aFetchSize, sortFieldList, sortValueList, sortOrderList, aIndexName, ""));
	}

	@Deprecated
	private FetchStatus fetchBySection(Connection aConn, String aPageDirection, int aFetchSize, String aSortField, String aSortValue, String aIndexName, SortOrder aDisplayOrder, String aWhereClause) throws Exception {
		Clasz masterObj = this.getMasterObject();
		FetchStatus result = fetchMemberOfBoxObjectBySection(aConn, masterObj, this, aPageDirection, aFetchSize, aSortField, aSortValue, aIndexName, aDisplayOrder, aWhereClause);
		this.setFetchStatus(result);
		return(result);
	}

	public FetchStatus fetchBySection(Connection aConn, String aPageDirection, int aFetchSize, List<String> aSortFieldList, List<String> aSortValueList, List<SortOrder> aSortOrderList, String aIndexName) throws Exception {
		return(this.fetchBySection(aConn, aPageDirection, aFetchSize, aSortFieldList, aSortValueList, aSortOrderList, aIndexName, ""));
	}

	private FetchStatus fetchBySection(Connection aConn, String aPageDirection, int aFetchSize, List<String> aSortFieldList, List<String> aSortValueList, List<SortOrder> aSortOrderList, String aIndexName, String aWhereClause) throws Exception {
		Clasz masterObj = this.getMasterObject();
		FetchStatus result = fetchMemberOfBoxObjectBySection(aConn, masterObj, this, aPageDirection, aFetchSize, aSortFieldList, aSortValueList, aSortOrderList, aIndexName, aWhereClause);
		this.setFetchStatus(result);
		return(result);
	}

	/**
	 * 
	 * @param aConn - database connection
	 * @param aFieldBox - the fieldobject box where member instances is place into
	 * @param aMasterObj - the parent clasz which contain the member field box
	 * @param aPageDirection - either the first page, next page, previous page or last page
	 * @param aPageSize - the number of record to retrieve in one go/batch
	 * @param aSortField - the keySeq field
	 * @param aSortValue - the last or first keySeq field value from the client's page
	 * @param aIndexName - the "Object Index" name to be use for batch retrieval
	 * @throws Exception 
	 */
	@Deprecated
	private FetchStatus fetchMemberOfBoxObjectBySection(Connection aConn, Clasz aMasterObj, FieldObjectBox aFieldBox, String aPageDirection, int aPageSize, String aSortField, String aSortValue, String aIndexName, SortOrder aDisplayOrder, String aWhereClause) throws Exception {
		FetchStatus result = FetchStatus.EOF;
		PreparedStatement stmt = null;
		ResultSet rset = null;
		try {
			if (aDisplayOrder == SortOrder.DSC) {
				if (aPageDirection.equals("next")) { // reverse the direction if display by descending order
					aPageDirection = "prev";
				} else {
					aPageDirection = "next";
				}
			}

			String whereClause = "";
			String sortOrder = "asc";
			Table oiTable = new Table(aIndexName);
			oiTable.initMeta(aConn);
			Field keyField = oiTable.getField(aSortField);
			if (aSortValue == null || aSortValue.trim().isEmpty()) aSortValue = null;  // for empty or null value, there'll be no where range clause
			if (keyField.getFieldType() == FieldType.STRING) {
				if (aPageDirection.equals("next")) {
					if (aSortValue != null) whereClause += "lower(" + aSortField + ") > lower('" + aSortValue + "')";
					sortOrder = "asc";
				} else if (aPageDirection.equals("prev")) {
					if (aSortValue != null) whereClause += "lower(" + aSortField + ") < lower('" + aSortValue + "')";
					sortOrder = "desc";
				} else if (aPageDirection.equals("first")) {
					sortOrder = "asc";
				} else if (aPageDirection.equals("last")) {
					sortOrder = "desc";
				}
			} else if (keyField.getFieldType() == FieldType.DATETIME || keyField.getFieldType() == FieldType.DATE) {
				if (aPageDirection.equals("next")) {
					if (aSortValue != null) whereClause += aSortField + " > ?";
					sortOrder = "asc";
				} else if (aPageDirection.equals("prev")) {
					if (aSortValue != null) whereClause += aSortField + " < ?";
					sortOrder = "desc";
				} else if (aPageDirection.equals("first")) {
					sortOrder = "asc";
				} else if (aPageDirection.equals("last")) {
					sortOrder = "desc";
				}
			} else {
				throw new Hinderance("The field type in object index: " + aIndexName + ", type: " + keyField.getFieldType() + ", is not supported!");
			}
			
			if (aWhereClause != null && !aWhereClause.isEmpty()) {
				if (whereClause.isEmpty() == false) whereClause += " and ";
				whereClause += " " + aWhereClause.trim();
			}

			Class fieldClass = Class.forName(aFieldBox.getDeclareType());
			String strSql = SqlStrOfObjectIndex(fieldClass, aIndexName, whereClause, aSortField + " " + sortOrder, aFieldBox, aMasterObj);
			stmt = aConn.prepareStatement(strSql);
			if (aSortValue != null) {
				if ((keyField.getFieldType() == FieldType.DATETIME || keyField.getFieldType() == FieldType.DATE) && whereClause.isEmpty() == false) {
					java.sql.Timestamp dateValue;
					Field fieldDt = (Field) keyField;
					fieldDt.setValueStr(aSortValue);
					String dbDate;
					if (fieldDt instanceof FieldDateTime) {
						dbDate = DateAndTime.FormatForJdbcTimestamp(((FieldDateTime) fieldDt).getValueDateTime());
					} else { // this is instance of FieldDate with no time
						FieldDate castedDate = (FieldDate) fieldDt;
						DateTime dateNoTime = castedDate.getValueDate();
						//DateTime endOfDay = DateAndTime.GetDayEnd(dateNoTime);
						DateTime endOfDay = DateAndTime.GetDayStart(dateNoTime);
						dbDate = DateAndTime.FormatForJdbcTimestamp(endOfDay);
					}
					dateValue = java.sql.Timestamp.valueOf(dbDate); // format must be in "2005-04-06 09:01:10"
					stmt.setTimestamp(1, dateValue);
				}
			}
			rset = stmt.executeQuery();
			App.logDebg(this, "ObjectIndex sql: " + stmt.toString());
			aFieldBox.getObjectMap().clear();
			int cntrRow = 0;
			Integer[] cntrThreadPassAsRef = {0};
			List<Thread> threadPool = new CopyOnWriteArrayList<>();
Clasz.StartDebug = false;
Clasz.StartTime = new GregorianCalendar();
			while (rset.next()) {
				App.logDebg(this, "Result set fetch cntr: " + cntrRow + ", pageSize: " + aPageSize);
				if (cntrRow < aPageSize) {
					cntrRow++;
					long pk = rset.getLong(1);
					Class leafClass = fieldClass;
					String polymorphicName = rset.getString(2);
					if (polymorphicName != null && polymorphicName.isEmpty() == false) {
						leafClass = Class.forName(polymorphicName); // if its a polymorphic member field, get its polymorphic class name
					}

					// can refactor as a spawner pattern in future
					if (App.getMaxThread() == 1) { // no threading
						cntrThreadPassAsRef[0]++;
						App.logDebg(this, "Spawning thread sequentially: " + cntrThreadPassAsRef[0]);
						(new PopulateMemberObjectThreadPk(cntrThreadPassAsRef, aMasterObj, aConn, aFieldBox, leafClass, pk)).join();
					} else {
						int cntrAttempt = 0;
						int maxAttempt = App.MAX_GET_CONNECTION_ATTEMPT;
						Connection conn = null;
						while(true) {
							try {
								cntrAttempt++;
								conn = aConn.getBaseDb().getConnPool().getConnection(); // here will throw exception from connection pool, if too many attempts
								if (cntrThreadPassAsRef[0] >= App.getMaxThread()) { 
									aConn.getBaseDb().getConnPool().freeConnection(conn); 
									App.ShowThreadingStatus(FieldObjectBox.class, "fetchMemberOfBoxObjectBySection", cntrThreadPassAsRef[0], App.getMaxThread(), cntrAttempt, maxAttempt);
									Thread.sleep(App.MAX_THREAD_OR_CONN_WAIT); // if no db connection, wait x second and continue the loop
								} else { 
									cntrThreadPassAsRef[0]++;
									App.logDebg(this, "Spawning thread parallelly: " + cntrThreadPassAsRef[0]);
									Thread theThread = new PopulateMemberObjectThreadPk(cntrThreadPassAsRef, aMasterObj, conn, aFieldBox, leafClass, pk);
									threadPool.add(theThread);
									break;
								}
							} catch(Exception ex) {
								App.ShowThreadingStatus(FieldObjectBox.class, "fetchMemberOfBoxObjectBySection", cntrThreadPassAsRef[0], App.getMaxThread(), cntrAttempt, maxAttempt);
								if (cntrAttempt >= maxAttempt) {
									throw new Hinderance(ex, "[fetchMemberOfBoxObjectBySection] Give up threading due to insufficent db connection");
								} else {
									Thread.sleep(App.MAX_THREAD_OR_CONN_WAIT); // wait for other db conn to free up
								}
							} 
						}
					}
					// end of spawner

					result = FetchStatus.EOF;
				} else {
					result = FetchStatus.MOF;
					App.logDebg(this, "Completed ObjectIndex parallel fetch, total record: " + cntrRow);
					break;
				}
			}

			App.logDebg(this, "Waiting for all spawned thread to complete, total thread: " + threadPool.size());
			for(Thread eachThread : threadPool) {
				eachThread.join(); // for each spawn thread, call join to wait for them to complete
			}
Clasz.StartDebug = false;

			// threading result is not sorted, now sort it
			String sortField = aSortField.substring(aSortField.lastIndexOf("$") + 1);
			aFieldBox.clearAllSortKey();
			aFieldBox.getMetaObj().getField(sortField).setSortKey(true);
			aFieldBox.getMetaObj().getField(sortField).setSortOrder(aDisplayOrder);
			aFieldBox.sort();
			App.logDebg(this, "Completed all PopulateMemberObjectThreadPk");
		} catch (Exception ex) {
			if (stmt == null) {
				throw new Hinderance(ex, "fetchMemberOfBoxObjectBySection - Fail to populate object array: null");
			} else {
				throw new Hinderance(ex, "fetchMemberOfBoxObjectBySection - Fail to populate object array: " + stmt.toString());
			}
		} finally {
			if (rset != null) {
				rset.close();
			}
			if (stmt != null) {
				stmt.close();
			}
		}
		return(result);
	}

	public static String SqlStrOfObjectIndex(Class aClass, String aIndexName, String aWhereClause, String aOrderClause, FieldObjectBox aField, Clasz aMasterObj) throws Exception {
		String result = "";
		if (Clasz.class.isAssignableFrom(aClass)) {
			String strIndexLeaf = Clasz.CreateLeafClassColName(aField.getFieldName());	
			String sqlSelect = "select" + " mom_child."+ aField.getFieldName() + ", " + "mom_child." + ObjectBase.LEAF_CLASS 
			+ " from " + aIndexName + ", " 
			+ "(select " + aField.getFieldName() + ", " + ObjectBase.LEAF_CLASS 
			+ " from " + aMasterObj.getIwTableName(aField.getFieldName()) 
			+ " where " + aMasterObj.getPkName() + " = " + aMasterObj.getObjectId() + ") mom_child";
			String sqlWhere = " where (mom_child." + aField.getFieldName() + " = " + aIndexName + "." + aField.getFieldName() 
			+ " and " + aIndexName + "." + aMasterObj.getPkName() + " = " + aMasterObj.getObjectId() 
			+ " and " + aIndexName + "." + strIndexLeaf + " = " + "mom_child." + ObjectBase.LEAF_CLASS
			+ ")";
			String sqlOrderBy = "";
			if (aWhereClause.isEmpty() == false) {
				sqlWhere += " and " + aWhereClause;
			}
			if (aOrderClause.isEmpty() == false) {
				sqlOrderBy = " order by " + aOrderClause;
			}
			result = sqlSelect + sqlWhere + sqlOrderBy;
		} else {
			throw new Hinderance("Cannot fetch objects from database for " + aClass.getSimpleName().toUpperCase() + ", because it is not CLASZ type");
		}

		return(result);
	}

	static final class PopulateMemberObjectThreadPk extends Thread {
		Integer[] cntrThread;
		Clasz masterObject;
		Connection conn;
		Class leafClass;
		Long pk;

		FieldObjectBox parentField;

		public PopulateMemberObjectThreadPk(Integer[] aCntrThread, Clasz aMasterObject, Connection aConn, FieldObjectBox aParent, Class aClass, Long aPk) throws Exception {
			this.cntrThread = aCntrThread;
			this.masterObject = aMasterObject;
			this.conn = aConn;
			this.parentField = aParent;
			this.leafClass = aClass;
			this.pk = aPk;
			this.startThread();
		}

		public void startThread() throws Exception {
			start();
		}

		@Override
		public void run() {
			try {
				Clasz fetchMember = Clasz.fetchObjectByPk(this.masterObject.getDb(), this.conn, this.leafClass, this.pk);
				if (fetchMember.getObjectId().equals(Clasz.NOT_INITIALIZE_OBJECT_ID)) {
					RemoveStaleMember(this.conn, this.parentField.getMasterObject(), this.parentField, this.pk); // if stale member, never add them into the Fob, coz they're not a member already, they're stale
				} else {
					this.parentField.addValueObject(fetchMember);  // now put the new member into the field object box
				}
			} catch (Exception ex) {
				App.logEror(ex, "Fail thread: " + this.cntrThread[0] + ", in FetchMemberThread for class: " + this.leafClass.getSimpleName() + ", of pk: " + this.pk);
			} finally {
				if (App.getMaxThread() != 1) {
					this.conn.getBaseDb().getConnPool().freeConnection(this.conn);
				}
				decreThreadCount(this.cntrThread);
			}
		}

		public synchronized void decreThreadCount(Integer[] aCntrThread) {
			aCntrThread[0]--;
		}
	}

	public interface GetFetchChildSql<T> {
		Object execute(T aParam) throws Exception;
	}

	public interface AfterPopulate<T> {
			Object execute(T aParam) throws Exception;
	}

	/*
	static final class PopulateMemberObjectThread extends Thread {
		Integer[] cntrThread;
		ObjectBase dbase;
		Connection conn;
		Clasz masterObj;
		String childFieldName;
		String fetchChildSql = "";
		AfterPopulate afterPopulate;

		public PopulateMemberObjectThread(Integer[] aCntrThread, ObjectBase aDb, Connection aConn, Clasz aMasterObj, String aChildFieldName, String aFetchSql) throws Exception {
			this(aCntrThread, aDb, aConn, aMasterObj, aChildFieldName, aFetchSql, null);
		}

		public PopulateMemberObjectThread(Integer[] aCntrThread, ObjectBase aDb, Connection aConn, Clasz aMasterObj, String aChildFieldName, String aFetchSql, AfterPopulate<Clasz> aAfterPopulate) throws Exception {
			this.cntrThread = aCntrThread;
			this.dbase = aDb;
			this.conn = aConn;
			this.masterObj = aMasterObj;
			this.childFieldName = aChildFieldName;
			this.fetchChildSql = aFetchSql;
			this.afterPopulate = aAfterPopulate;
			this.startThread();
		}

		public void startThread() throws Exception {
			start();
		}

		@Override
		public void run() {
			try {
				if (this.fetchChildSql.isEmpty()) {
					this.masterObj.getFieldObjectBox(this.childFieldName).fetchAll(this.conn);
				} else {
					this.masterObj.getFieldObjectBox(this.childFieldName).fetchByCustomSql(this.conn, this.fetchChildSql);
				}
				this.masterObj.getFieldObjectBox(this.childFieldName).resetIterator();
				while(this.masterObj.getFieldObjectBox(this.childFieldName).hasNext(this.conn)) {
					Clasz eachObj = this.masterObj.getFieldObjectBox(this.childFieldName).getNext();
					if (this.afterPopulate != null) this.afterPopulate.execute(eachObj);
				}
			} catch (Exception ex) {
				App.logEror(ex, "Fail thread: " + this.cntrThread[0] + ", when populating value for: " + this.masterObj.getClass().getSimpleName());
			} finally {
				decreThreadCount(this.cntrThread);
				this.dbase.getConnPool().freeConnection(this.conn);
			}
		}

		public synchronized void decreThreadCount(Integer[] aCntrThread) {
			aCntrThread[0]--;
		}
	}
	*/

	public Iterator getIterator() {
		return iterator;
	}

	public void setIterator(Iterator iterator) {
		this.iterator = iterator;
	}

	public void resetIterator() throws Exception {
		this.setKey(-1L);
	}

	public void resetByIterator() throws Exception {
		this.setIterator(this.getObjectMap().entrySet().iterator());
	}

	public Long getLargestIndex() throws Exception {
		Long result = 0L;
		for (Map.Entry<Long, Clasz> entry : this.getObjectMap().entrySet()) {
			Long keySeq = entry.getKey();
			if (keySeq.compareTo(result) > 0) {
				result = keySeq;
			}
		}
		return(result);
	}

	public boolean hasNext() throws Exception {
		boolean result = false;
		Long largestIndex = this.getLargestIndex();
		if (this.getKey() != null) {
			Long nextIndex = this.getKey() + 1;
			while (nextIndex.compareTo(largestIndex) <= 0) {
				if (this.getObjectMap().get(nextIndex) != null) {
					result = true;
					break;
				} else {
					nextIndex++;
				}
			}
		} else {
			result = false;
		}
		return(result);
	}

	public boolean hasNext(Connection conn) throws Exception {
		this.mustFetch(conn);
		return(this.hasNext());
	}

	public boolean hasNextByIterator() throws Exception {
		this.mustFetch();
		if (this.getIterator() == null ) {
			this.resetIterator();
		}
		return(this.getIterator().hasNext());
	}

	public boolean hasNextByIterator(Connection conn) throws Exception {
		this.mustFetch(conn);
		if (this.getIterator() == null ) {
			this.resetIterator();
		}
		return(this.getIterator().hasNext());
	}

	public Clasz getNext() throws Exception {
		this.mustFetch();

		if (this.getKey() == null ) {
			this.setKey(-1L);
		}

		Long largestIndex = this.getLargestIndex();
		while (this.getKey().compareTo(largestIndex) <= 0) {
			this.setKey(this.getKey() + 1);
			Clasz result = this.getObjectMap().get(this.getKey());
			if (result != null) {
				return(result);
			} else {
				this.setKey(this.getKey() + 1);
			}
		}
		throw new Hinderance("Error, already at the end of iteration, field: " + this.getFieldName() + ", key: " + this.getKey());
	}

	public Clasz getNextByIterator() throws Exception {
		this.mustFetch();

		if (this.getIterator() == null ) {
			this.resetIterator();
		}

		if (this.getIterator().hasNext()) {
			Map.Entry<Long, Clasz> mapEntry = (Map.Entry) this.getIterator().next();
			this.setKey(mapEntry.getKey());
			return((Clasz) mapEntry.getValue());
		} else {
			throw new Hinderance("Error, already at the end of iteration, field: " + this.getFieldName());
		}
	}

	public void setKey(Long aKey) throws Exception {
		this.key = aKey;
	}

	public Long getKey() throws Exception {
		this.mustFetch();

		return(this.key);
	}

	public int getFetchSize() throws Exception {
		//this.mustFetch();
		return fetchSize;
	}

	public void setFetchSize(int fetchSize) throws Exception {
		this.fetchSize = fetchSize;
	}

	public void removeAll() throws Exception {
		this.setFetchStatus(FetchStatus.SOF);
		this.getObjectMap().clear();
	}

	public void remove(Long aKey) throws Exception {
		this.mustFetch();
		this.getObjectMap().remove(aKey);
	}

	public void remove(Clasz aToRemove) throws Exception {
		Set<Entry<Long, Clasz>> entrySet = this.getObjectMap().entrySet();
		for (Iterator iterateClasz = entrySet.iterator(); iterateClasz.hasNext();) {
			Entry<Long, Clasz> entry = (Entry<Long, Clasz>) iterateClasz.next();
			Clasz value = entry.getValue();
			if(value == aToRemove) {
				iterateClasz.remove(); //Removing Entry from map
				break;
			}    
		}
	}

	public Clasz getValue(Long aIndex) throws Exception {
		this.mustFetch();
		return(this.getObjectMap().get(aIndex));
	}
	
	public boolean containsKey(Long aIndex) throws Exception {
		this.mustFetch();

		if (this.getObjectMap().containsKey(aIndex)) {
			return (true);
		}
		return (false);
	}

	public void mustFetch() throws Exception {
		if (this.getFetchStatus() == FetchStatus.SOF && this.isModified() == false) { // SOF mean there's no attempt to Fetch from db at all, if this field is modified...
			Connection conn = this.getMasterObject().getDb().getConnPool().getConnection();
			try {
				this.mustFetch(conn);
			} finally {
				if (conn != null) {
					this.getMasterObject().getDb().getConnPool().freeConnection(conn);
				}
			}		
		}
	}

	public void mustFetch(Connection conn) throws Exception {
		if (this.getFetchStatus() == FetchStatus.SOF && this.isModified() == false) { // SOF mean there's no attempt to Fetch from db at all, if this field is modified...
			//App.logWarn("Fully fetching all members in the field, field is: " + this.getFieldName());
			this.fetchAll(conn);
			this.resetIterator(); // after fetching into the map, previous iterateClasz is not valid anymore, so reset it
		}
	}
	
	private static Map<Long, Clasz> SortByComparator(Map<Long, Clasz> unsortMap) throws Exception {
		List<Map.Entry<Long, Clasz>> list = new LinkedList<>(unsortMap.entrySet()); // Convert Map to List
 
		// Sort list with comparator, to compare the Map values
		Collections.sort(list, (Map.Entry<Long, Clasz> o1, Map.Entry<Long, Clasz> o2) -> (o1.getValue()).compareTo(o2.getValue()));
 
		// Convert sorted map back to a Map
		Long cntr = 0L;
		Map<Long, Clasz> sortedMap = new LinkedHashMap<>();
		for (Map.Entry<Long, Clasz> entry : list) {
			sortedMap.put(cntr++, entry.getValue());
		}
		return(sortedMap);
	}

	public Clasz addObject(Connection aConn, Class aClass) throws Exception {
		Clasz clasz = (Clasz) ObjectBase.CreateObject(aConn, aClass);
		this.addValueObject((Clasz) clasz);
		return(clasz);
	}

	public Clasz getFirstMember() throws Exception {
		return(this.objectMap.get(0L));
	}

	public Clasz fetchFirstMember(Connection aConn, Clasz aCriteria) throws Exception {
		List<Clasz> result = new CopyOnWriteArrayList<>();
		this.forEachMember(aConn, ((Connection bConn, Clasz aClasz) -> { // uses for each member will ensure we fetched the right polymorphic member
			if (aCriteria.equalsCriteria(bConn, aClasz)) {
				result.add(aClasz);
				return(false);
			}
			return(true);
		}));
		
		if (result.size() > 0) {
			return(result.get(0));
		} else {
			return(null);
		}
	}

	public Clasz fetchUniqueMember(Connection aConn, Clasz aCriteria, String aIndexName) throws Exception {
		Clasz theMember = null;
		Table oiTable = new Table(aIndexName);
		oiTable.initMeta(aConn);
		//List<Field> indexedField = ObjectIndex.GetIndexedField(aConn, this.getMasterObject()); // get the indexed field of this master object
		List<Field> indexField = new CopyOnWriteArrayList<>();
		String whereClause = ObjectIndex.GetIndexWhereCriteria(aIndexName, this.getFieldName(), aCriteria, indexField);
		Class fieldClass = Class.forName(this.getDeclareType());
		String sqlStr = SqlStrOfObjectIndex(fieldClass, aIndexName, whereClause, "", this, this.getMasterObject());
		PreparedStatement stmt = null; 
		ResultSet rs = null;
		try {
			stmt = aConn.prepareStatement(sqlStr);
			Database.SetStmtValue(stmt, indexField);
			rs = stmt.executeQuery();
			int cntr = 0;
			while (rs.next()) {
				cntr++;
				if (cntr >= 2) {
					throw new Hinderance("Fetching object for unique member return non unique result: " + this.getClass().getSimpleName() + ", index: " + aIndexName);
				}
				long pk = rs.getLong(1);
				Class leafClass = fieldClass;
				String polymorphicName = rs.getString(2);
				if (polymorphicName != null && polymorphicName.isEmpty() == false) {
					leafClass = Class.forName(polymorphicName); // if its a polymorphic member field, get its polymorphic class name
				}
				theMember = Clasz.fetchObjectByPk(this.getMasterObject().getDb(), aConn, leafClass, pk);
			}
		} catch (Exception ex) {
			if (stmt != null) {
				throw new Hinderance(ex, "Fail Get Member: " + stmt.toString());
			} else {
				throw new Hinderance(ex, "Fail Get Member");
			}
		} finally {
			if (rs != null) {
				rs.close();
			}
			if (stmt != null) {
				stmt.close();
			}
		}
		return(theMember);
	}

	public FetchStatus searchBySection(Connection aConn, String aPageDirection, String aSortField, String aSortValue, String aIndexName, String searchCriteria) throws Exception {
		return(this.fetchBySection(aConn, aPageDirection, aSortField, aSortValue, aIndexName, searchCriteria));
	}

	private static void GetSectionWhereClause(Connection aConn, List<String> aResult, String aIndexName, String aSortField, String aSortValue, SortOrder aDisplayOrder, String aPageDirection) throws Exception {
		if (aDisplayOrder == SortOrder.DSC) {
			if (aPageDirection.equals("next")) { // reverse the direction if display by descending order
				aPageDirection = "prev";
			} else {
				aPageDirection = "next";
			}
		}

		String whereClause = "";
		String sortOrder = "asc";
		Table oiTable = new Table(aIndexName);
		oiTable.initMeta(aConn);
		Field keyField = oiTable.getField(aSortField);
		if (aSortValue == null || aSortValue.trim().isEmpty()) aSortValue = null;  // for empty or null value, there'll be no where range clause
		if (keyField.getFieldType() == FieldType.STRING) {
			if (aPageDirection.equals("next")) {
				if (aSortValue != null) whereClause += "lower(" + aSortField + ") >= lower('" + aSortValue + "')";
				sortOrder = "asc";
			} else if (aPageDirection.equals("prev")) {
				if (aSortValue != null) whereClause += "lower(" + aSortField + ") <= lower('" + aSortValue + "')";
				sortOrder = "desc";
			} else if (aPageDirection.equals("first")) {
				sortOrder = "asc";
			} else if (aPageDirection.equals("last")) {
				sortOrder = "desc";
			}
		} else if (keyField.getFieldType() == FieldType.DATETIME || keyField.getFieldType() == FieldType.DATE) {
			if (aPageDirection.equals("next")) {
				if (aSortValue != null) whereClause += aSortField + " >= ?";
				sortOrder = "asc";
			} else if (aPageDirection.equals("prev")) {
				if (aSortValue != null) whereClause += aSortField + " <= ?";
				sortOrder = "desc";
			} else if (aPageDirection.equals("first")) {
				sortOrder = "asc";
			} else if (aPageDirection.equals("last")) {
				sortOrder = "desc";
			}
		} else {
			throw new Hinderance("The field type in object index: " + aIndexName + ", type: " + keyField.getFieldType() + ", is not supported!");
		}

		aResult.clear();
		aResult.add(whereClause);
		aResult.add(sortOrder);
	}

	private static void SetStmtDateValue(PreparedStatement stmt, LambdaCounter aFieldPosition, String whereClause, Field keyField, String aSortValue) throws Exception {
		if (aSortValue == null || aSortValue.trim().isEmpty()) aSortValue = null;  // for empty or null value, there'll be no where range clause
		if (aSortValue != null) {
			if ((keyField.getFieldType() == FieldType.DATETIME || keyField.getFieldType() == FieldType.DATE) && whereClause.isEmpty() == false) {
				java.sql.Timestamp dateValue;
				Field fieldDt = (Field) keyField;
				fieldDt.setValueStr(aSortValue);
				String dbDate;
				if (fieldDt instanceof FieldDateTime) {
					dbDate = DateAndTime.FormatForJdbcTimestamp(((FieldDateTime) fieldDt).getValueDateTime());
				} else { // this is instance of FieldDate with no time
					FieldDate castedDate = (FieldDate) fieldDt;
					DateTime dateNoTime = castedDate.getValueDate();
					//DateTime endOfDay = DateAndTime.GetDayEnd(dateNoTime);
					DateTime endOfDay = DateAndTime.GetDayStart(dateNoTime);
					dbDate = DateAndTime.FormatForJdbcTimestamp(endOfDay);
				}
				dateValue = java.sql.Timestamp.valueOf(dbDate); // format must be in "2005-04-06 09:01:10"
				Integer fieldPosition = aFieldPosition.getCntr();
				stmt.setTimestamp(fieldPosition, dateValue);
				aFieldPosition.increment();
			}
		}
	}

	private static void FetchMemberOfBoxSpawner(Connection aConn, List<Thread> threadPool, Clasz aMasterObj, FieldObjectBox aFieldBox, Integer[] cntrThreadPassAsRef, Class leafClass, long pk) throws Exception {
		if (App.getMaxThread() == 1) { // no threading
			cntrThreadPassAsRef[0]++;
			App.logDebg(FieldObjectBox.class, "Spawning thread sequentially: " + cntrThreadPassAsRef[0]);
			(new PopulateMemberObjectThreadPk(cntrThreadPassAsRef, aMasterObj, aConn, aFieldBox, leafClass, pk)).join();
		} else {
			int cntrAttempt = 0;
			int maxAttempt = App.MAX_GET_CONNECTION_ATTEMPT;
			Connection conn;
			while(true) {
				try {
					cntrAttempt++;
					conn = aConn.getBaseDb().getConnPool().getConnection(); // here will throw exception from connection pool, if too many attempts
					if (cntrThreadPassAsRef[0] >= App.getMaxThread()) { 
						aConn.getBaseDb().getConnPool().freeConnection(conn); 
						App.ShowThreadingStatus(FieldObjectBox.class, "fetchMemberOfBoxObjectBySection", cntrThreadPassAsRef[0], App.getMaxThread(), cntrAttempt, maxAttempt);
						Thread.sleep(App.MAX_THREAD_OR_CONN_WAIT); // if no db connection, wait x second and continue the loop
					} else { 
						cntrThreadPassAsRef[0]++;
						App.logDebg(FieldObjectBox.class, "Spawning thread parallelly: " + cntrThreadPassAsRef[0]);
						Thread theThread = new PopulateMemberObjectThreadPk(cntrThreadPassAsRef, aMasterObj, conn, aFieldBox, leafClass, pk);
						threadPool.add(theThread);
						break;
					}
				} catch(Exception ex) {
					App.ShowThreadingStatus(FieldObjectBox.class, "fetchMemberOfBoxObjectBySection", cntrThreadPassAsRef[0], App.getMaxThread(), cntrAttempt, maxAttempt);
					if (cntrAttempt >= maxAttempt) {
						throw new Hinderance(ex, "[fetchMemberOfBoxObjectBySection] Give up threading due to insufficent db connection");
					} else {
						Thread.sleep(App.MAX_THREAD_OR_CONN_WAIT); // wait for other db conn to free up
					}
				} 
			}
		}
	}

	/*
		Permutate the where expression as shown in the 2 SQL below:-

		select mom_child.generated_payslips, mom_child.leaf_class 
		from oi_payslip_run_generated_payslips, (
		select generated_payslips, leaf_class 
		from iv_payslip_run_generated_payslips 
		where cz_payslip_run_pk = 39) mom_child 
		where (mom_child.generated_payslips = oi_payslip_run_generated_payslips.generated_payslips 
		and oi_payslip_run_generated_payslips.cz_payslip_run_pk = 39 
		and oi_payslip_run_generated_payslips.generated_payslips_leaf_class = mom_child.leaf_class) 
		-- start permutating the 2 line below
		and ((lower(generated_payslips$employee) >= lower('Demo1 Silverston') and generated_payslips$period_from < '2020-07-01 00:00:00+08') 
		or (lower(generated_payslips$employee) >= lower('Demo1 Silverston') and generated_payslips$period_from < '2020-07-01 00:00:00+08')) 
		--
		order by  generated_payslips$employee asc, generated_payslips$period_from desc;
					 
		select mom_child.generated_payslips, mom_child.leaf_class 
		from oi_payslip_run_generated_payslips, (
		select generated_payslips, leaf_class 
		from iv_payslip_run_generated_payslips 
		where cz_payslip_run_pk = 38) mom_child 
		where (mom_child.generated_payslips = oi_payslip_run_generated_payslips.generated_payslips 
		and oi_payslip_run_generated_payslips.cz_payslip_run_pk = 38 
		and oi_payslip_run_generated_payslips.generated_payslips_leaf_class = mom_child.leaf_class) 
		-- start permutating the 2 line below
		and ((lower(generated_payslips$employee) >= lower('Demo5 Silverston') and generated_payslips$period_from < '2020-02-01 00:00:00+08')
		or (lower(generated_payslips$employee) > lower('Demo5 Silverston') and generated_payslips$period_from <= '2020-02-01 00:00:00+08'))
		--
		order by  generated_payslips$employee asc, generated_payslips$period_from desc	;	  
	*/
	public static String GetWherePermutation(List<String> aEachCondition, int aPosition2UniqueIt) throws Exception {
		String uniqueableExpression = "";
		for (int cntr = 0; cntr < aEachCondition.size(); cntr++) {
			String newWhereSection = aEachCondition.get(cntr);
			if (newWhereSection != null && newWhereSection.isEmpty() == false) {
				if (uniqueableExpression.isEmpty() == false) uniqueableExpression += " and ";
				if (cntr == aPosition2UniqueIt) {
					newWhereSection = newWhereSection.replaceAll(">=", ">");
					newWhereSection = newWhereSection.replaceAll("<=", "<");
				}
				uniqueableExpression += newWhereSection;
			}
		}
		return(uniqueableExpression);
	}

	private FetchStatus fetchMemberOfBoxObjectBySection(Connection aConn, Clasz aMasterObj, FieldObjectBox aFieldBox, String aPageDirection, int aPageSize, List<String> aSortFieldList, List<String> aSortValueList, List<SortOrder> aSortOrderList, String aIndexName, String aWhereClause) throws Exception {
		FetchStatus result = FetchStatus.EOF;
		PreparedStatement stmt = null;
		ResultSet rset = null;

		int totalField = aSortFieldList.size();
		if (aSortValueList.size() != totalField || aSortOrderList.size() != totalField) {
			throw new Hinderance("[fetchMemberOfBoxObjectBySection] The pass in array of fields to sort, its value and its sort key is not the same!");
		}

		try {
			List<String> fieldExpressionList = new CopyOnWriteArrayList<>();
			List<String> sortFieldList = new CopyOnWriteArrayList<>();
			for(int cntr = 0; cntr < aSortFieldList.size(); cntr++) {
				String sortField = aSortFieldList.get(cntr);
				String sortValue = aSortValueList.get(cntr);
				SortOrder sortOrder = aSortOrderList.get(cntr);

				List<String> sectionWhere = new CopyOnWriteArrayList<>();
				GetSectionWhereClause(aConn, sectionWhere, aIndexName, sortField, sortValue, sortOrder, aPageDirection);

				String newWhereSection = sectionWhere.get(0);
				String newSortSection = sectionWhere.get(1);

				fieldExpressionList.add(newWhereSection);
				sortFieldList.add(sortField + " " + newSortSection);
			}

			List<String> wherePermutationList = new CopyOnWriteArrayList<>();
			for (int cntr = fieldExpressionList.size() - 1; cntr >= 0; cntr--) {
				String eachExpression = GetWherePermutation(fieldExpressionList, cntr);
				wherePermutationList.add(eachExpression);
			}

			String whereClause = "";
			for(int cntr = 0; cntr < wherePermutationList.size(); cntr++) {
				String newWhereSection = wherePermutationList.get(cntr);
				if (whereClause.isEmpty() == false) whereClause += " or ";
				if (newWhereSection.isEmpty() == false) whereClause += "(" + newWhereSection + ")";
			}

			if (whereClause.isEmpty() == false) {
				whereClause = "(" + whereClause + ")";
			}

			if (aWhereClause != null && !aWhereClause.isEmpty()) {
				if (whereClause.isEmpty() == false) whereClause += " and ";
				whereClause += " " + aWhereClause.trim();
			}

			String sortClause = "";
			for (int cntr = 0; cntr < fieldExpressionList.size(); cntr++) {
				String newSortSection = sortFieldList.get(cntr);
				if (sortClause.isEmpty() == false) sortClause += ",";
				sortClause += " " + newSortSection;
			}
			
			Class fieldClass = Class.forName(aFieldBox.getDeclareType());
			String strSql = SqlStrOfObjectIndex(fieldClass, aIndexName, whereClause, sortClause, aFieldBox, aMasterObj);
			stmt = aConn.prepareStatement(strSql);

			Table oiTable = new Table(aIndexName);
			oiTable.initMeta(aConn);
			LambdaCounter dateFieldPosition = new LambdaCounter();
			dateFieldPosition.setCntr(1);
			for(int cntrExpr = 0; cntrExpr < wherePermutationList.size(); cntrExpr++) {
				for(int cntr = 0; cntr < aSortFieldList.size(); cntr++) {
					String sortField = aSortFieldList.get(cntr);
					String sortValue = aSortValueList.get(cntr);

					Field keyField = oiTable.getField(sortField);
					SetStmtDateValue(stmt, dateFieldPosition, whereClause, keyField, sortValue);
				}
			}
			rset = stmt.executeQuery();
			App.logDebg(this, "ObjectIndex sql: " + stmt.toString());
			aFieldBox.getObjectMap().clear();
			int cntrRow = 0;
			Integer[] cntrThreadPassAsRef = {0};
			List<Thread> threadPool = new CopyOnWriteArrayList<>();
			while (rset.next()) {
				App.logDebg(this, "Result set fetch cntr: " + cntrRow + ", pageSize: " + aPageSize);
				if (cntrRow < aPageSize) {
					cntrRow++;
					long pk = rset.getLong(1);
					Class leafClass = fieldClass;
					String polymorphicName = rset.getString(2);
					if (polymorphicName != null && polymorphicName.isEmpty() == false) {
						leafClass = Class.forName(polymorphicName); // if its a polymorphic member field, get its polymorphic class name
					}

					// can refactor as a spawner pattern in future
					FetchMemberOfBoxSpawner(aConn, threadPool, aMasterObj, aFieldBox, cntrThreadPassAsRef, leafClass, pk);

					result = FetchStatus.EOF;
				} else {
					result = FetchStatus.MOF;
					App.logDebg(this, "Completed ObjectIndex parallel fetch, total record: " + cntrRow);
					break;
				}
			}

			App.logDebg(this, "Waiting for all spawned thread to complete, total thread: " + threadPool.size());
			for(Thread eachThread : threadPool) {
				eachThread.join(); // for each spawn thread, call join to wait for them to complete
			}

			// threading result is not sorted, now sort it
			aFieldBox.clearAllSortKey();
			for(int cntr = 0; cntr < aSortFieldList.size(); cntr++) {
				String sortField = aSortFieldList.get(cntr);
				SortOrder theOrder = aSortOrderList.get(cntr);
				String sortFieldName = sortField.substring(sortField.lastIndexOf("$") + 1);
				aFieldBox.getMetaObj().getField(sortFieldName).setSortKey(true);
				aFieldBox.getMetaObj().getField(sortFieldName).setSortKeyNo(cntr);
				aFieldBox.getMetaObj().getField(sortFieldName).setSortOrder(theOrder);
			}
			aFieldBox.sort();

			App.logDebg(this, "Completed all PopulateMemberObjectThreadPk");
		} catch (Exception ex) {
			if (stmt == null) {
				throw new Hinderance(ex, "fetchMemberOfBoxObjectBySection - [" + aIndexName + "], fail to populate object array: null");
			} else {
				throw new Hinderance(ex, "fetchMemberOfBoxObjectBySection - [" + aIndexName + "], fail to populate object array: " + stmt.toString());
			}
		} finally {
			if (rset != null) {
				rset.close();
			}
			if (stmt != null) {
				stmt.close();
			}
		}
		return(result);
	}


	public void markMemberFieldForRemove(Connection aConn, String aFieldName) throws Exception {
		MarkMemberFieldForRemove(aConn, this, aFieldName);
	}

	public static void MarkMemberFieldForRemove(Connection aConn, FieldObjectBox aFob, String aFieldName) throws Exception {
		aFob.resetIterator();
		while (aFob.hasNext(aConn)) {
			Clasz eachClasz = aFob.getNext();
			eachClasz.getField(aFieldName).forRemove(true);
		}
	}

	public void markAllMemberFieldForKeep(Connection aConn) throws Exception {
		MarkAllMemberFieldForKeep(aConn, this);
	}

	public static void MarkAllMemberFieldForKeep(Connection aConn, FieldObjectBox aFob) throws Exception {
		aFob.resetIterator(); 
		while(aFob.hasNext(aConn)) {
			Clasz eachClasz = aFob.getNext();
			eachClasz.markAllFieldForKeep();
		}
	}

	public void removeMarkMemberField(Connection aConn) throws Exception {
		RemoveMarkMemberField(aConn, this);
	}

	public static void RemoveMarkMemberField(Connection aConn, FieldObjectBox aFob) throws Exception {
		aFob.resetIterator(); 
		while(aFob.hasNext(aConn)) {
			Clasz eachClasz = aFob.getNext();
			eachClasz.removeMarkField();
		}
	}

	/**
	 * Similar to createMemberOfTable method, but this, creates array of members. In
	 * summary, member of relationship is denoted in one iv_[class name] table.
	 * For arrays of objects relationship, for each array object variable, the
	 * table iv_[class name]_[variable name] is represents the array of
	 * [variable_name] of that class. Hence there will be multiple of such table
	 * if there is multiple "array instant variable" for a class.
	 *
	 * @param aConn
	 * @param aParent
	 * @param aLinkField
	 * @throws Exception
	 */
	public static void CreateBoxMemberTable(Connection aConn, Class aParent, String aLinkField) throws Exception {
		String relationType = Clasz.GetIwTableName(aParent, aLinkField);
		Table linkTable = new Table(relationType);
		if (Database.TableExist(aConn, relationType) == false) {
			String parentPkName = Clasz.CreatePkColName(aParent);

			linkTable.createField(parentPkName, FieldType.LONG); // create the parent field
			linkTable.createField(aLinkField, FieldType.LONG); // create the child field in the iv_ table
			linkTable.createField(ObjectBase.LEAF_CLASS, FieldType.STRING, ObjectBase.CLASS_NAME_LEN);

			linkTable.getField(parentPkName).setUniqueKey(true);
			linkTable.getField(aLinkField).setUniqueKey(true);
			linkTable.getField(ObjectBase.LEAF_CLASS).setUniqueKey(true);

			linkTable.getField(parentPkName).setIndexKeyNo(0);
			linkTable.getField(aLinkField).setIndexKeyNo(1);
			linkTable.getField(ObjectBase.LEAF_CLASS).setIndexKeyNo(2);

			Database.CreateTable(aConn, linkTable);
			Database.CreateUniqueIndex(aConn, linkTable);
		}
	}

	public void renameBoxMemberUniqueIndex(Connection aConn, Class aParent, String aNewClassName) throws Exception {
		String oldTableName = Clasz.GetIwTableName(aParent, this.getFieldName());
		String newTableName = Clasz.GetIwTableName(aNewClassName, this.getFieldName());
		Table linkTable = new Table(oldTableName);
		String oldIndexName = linkTable.getIndexName() + "_unq";
		String newIndexName = newTableName + "_unq";
		Database.AlterTableRenameIndex(aConn, linkTable.getTableName(), oldIndexName, newIndexName);
	}
}
