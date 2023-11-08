package biz.shujutech.db.object;

import biz.shujutech.base.App;
import biz.shujutech.base.Connection;
import biz.shujutech.base.DateAndTime;
import biz.shujutech.base.Hinderance;
import biz.shujutech.db.relational.Database;
import biz.shujutech.db.relational.Database.DbType;
import biz.shujutech.db.relational.Field;
import biz.shujutech.db.relational.FieldType;
import biz.shujutech.db.relational.Record;
import biz.shujutech.db.relational.SortOrder;
import biz.shujutech.db.relational.Table;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import biz.shujutech.technical.LambdaGeneric;
import biz.shujutech.util.Generic;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import biz.shujutech.technical.Callback2ProcessMember;

public class FieldObjectBox<Ty extends Clasz<?>> extends FieldClasz {

	private ConcurrentHashMap<Long, Ty> objectMap = new ConcurrentHashMap<>();
	private Iterator<Entry<Long, Ty>> iterator = null;
	private Long key;
	private Ty metaObj;
	private int fetchSize = 5;

	public FieldObjectBox() {
		this.setAtomic(false);
	}

	public FieldObjectBox(Ty aMetaObj) throws Exception {
		this();
		this.metaObj = aMetaObj;
	}

	@Override
	public String getValueStr() throws Exception {
		String result = "";
		for(Ty eachClasz : this.getObjectMap().values()) {
			result = eachClasz.getValueStr();
			if (result.isEmpty() == false) {
				break;
			}
		}
		return(result);
	}

	public Ty getMetaObj() {
		return metaObj;
	}

	public void setMetaObj(Ty aMetaObj) {
		this.metaObj = aMetaObj;
	}

	public void setMetaObj(Object aMetaObj) throws Hinderance {
		if (aMetaObj instanceof Clasz) {
			Clasz<?> castedObj = (Clasz<?>) aMetaObj;
			setMetaObj(castedObj);
		} else {
			throw new Hinderance("Cannot setMetaObj() from an incompatible object type: " + aMetaObj.getClass().getSimpleName());
		}
	}

	public String getMetaType() {
		return(this.metaObj.getClass().getName()) ;
	}

	public Class<?> getMemberClass() {
		return(this.metaObj.getClass());
	}

	public ConcurrentHashMap<Long, Ty> getObjectMap() {
		return objectMap;
	}

	public void setObjectMap(ConcurrentHashMap<Long, Ty> objectMap) {
		this.objectMap = objectMap;
	}

	public Ty getObject(long aIndex) throws Exception {
		this.mustFetch();
		Ty result = this.getObjectMap().get(aIndex); 
		return(result);
	}

	public synchronized void addValueObject(Ty aObject) throws Exception {
		this.setModified(true);
		long index = this.getObjectMap().size();
		aObject.setMasterObject((Clasz<?>) this.getMasterObject());
		this.getObjectMap().put(index, aObject);
	}

	public synchronized void addValueObjectFreeType(Object aObject) throws Exception {
		if (aObject instanceof Clasz) {
			@SuppressWarnings("unchecked")
			Ty castedObject = (Ty) aObject;
			addValueObject(castedObject);
		} else {
			throw new Hinderance("Cannot addValueObject() from an incompatible object type: " + aObject.getClass().getSimpleName());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void cloneField(Connection aConn, Field aSourceField) throws Exception {
		FieldObjectBox<Ty> sourceField = (FieldObjectBox<Ty>) aSourceField;
		this.copyFieldObjectBox(aConn,  sourceField, false);
		this.copyAttribute(aSourceField);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void copyValue(Connection aConn, Field aSourceField) throws Exception {
		this.copyFieldObjectBox(aConn, (FieldObjectBox<Ty>) aSourceField, true);
	}


	public void copyByRef(FieldObjectBox<Ty> aSourceFob) throws Exception {
		for (Entry<Long, Ty> eachMember : aSourceFob.getObjectMap().entrySet()) {
			Ty obj = eachMember.getValue();
			this.addValueObject(obj);
		}
	}

	@SuppressWarnings("unchecked")
	public void copyFieldObjectBox(Connection aConn, FieldObjectBox<Ty> aSourceField, boolean isCopy) throws Exception {
		for(Entry<Long, Ty> recEach : aSourceField.getObjectMap().entrySet()) {
			// for each object in the object box of aSourceField
			Long idx = recEach.getKey();
			Ty obj = recEach.getValue();
			Ty targetObj = this.getObject(idx);
			if (targetObj == null) {
				//targetObj = (Ty) Class.forName(aSourceField.getMetaType()).newInstance();
				targetObj = (Ty) Class.forName(aSourceField.getMetaType()).getConstructor().newInstance();
			}
			if (isCopy) {
				targetObj.copyAllFieldWithoutModifiedState(aConn, obj);
			} else {
				targetObj.copyAllFieldWithModifiedState(aConn, obj);
			}
		}
	}

	public boolean containObjectId(Long aObjId) throws Exception {
		boolean result = false;
		for(Ty obj : this.getObjectMap().values()) {
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
		Map<Long, Ty> sortedBox = SortByComparator(this.getObjectMap());
		this.getObjectMap().clear();
		sortedBox.entrySet().forEach((entry) -> {
			this.getObjectMap().put(entry.getKey(), entry.getValue());
		});
	}

	/**
	 * Comparing all the objects in this container with another set of objects
	 * in another container.
	 * 
	 * @param <Ty>
	 * @param aLeft
	 * @param aRight
	 * @return
	 * @throws Exception 
	 */
	public static <Ty extends Clasz<?>> int compare(FieldObjectBox<Ty> aLeft, FieldObjectBox<Ty> aRight) throws Exception {
		int result = 0;
		int smallest = Integer.MIN_VALUE;
		int largest = Integer.MAX_VALUE;
		int setCompare;

		for (Ty left : aLeft.getObjectMap().values()) {
			for (Ty right : aRight.getObjectMap().values()) {
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

	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(Field aRight) {
		int result = 0;
		try {
			/*
			if (aRight instanceof FieldObjectBox) {
			} else {
				throw new Hinderance("Cannot compare field of different type: " + this.getClass().getSimpleName() + ", " + aRight.getClass().getSimpleName());
			}
			*/
			FieldObjectBox<Ty> rightFob = (FieldObjectBox<Ty>) aRight;
			result = FieldObjectBox.compare(this, rightFob);
		} catch (Exception ex) {
			//App.logEror(ex);
			throw new RuntimeException(ex);
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

	private void clearAllSortKey(Clasz<?> aClasz2Clear) throws Exception {
		for(Field eachField : aClasz2Clear.getTreeField().values()) {
			if (eachField.isAtomic()) {
				if (eachField.isSortKey()) {
					eachField.clearSortKey();
				}
			} else {
				if (eachField.getDbFieldType() == FieldType.OBJECT) {
					Clasz<?> clasz = ((FieldObject<?>) eachField).getObj();
					if (clasz != null) {
						clearAllSortKey(clasz); // recursive call to clear the member object sort keys
					}
				} else if (eachField.getDbFieldType() == FieldType.OBJECTBOX) {
					Clasz<?> clasz = ((FieldObjectBox<?>) eachField).getMetaObj();
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

	/**
	 * Assigned the sort keySeq properties to all of the data object in this
 	 * container. This is required in Clasz compare method. The Clasz object
	 * that're being compare uses the keys as the sort criteria.
	 * 
	 * @param aClaszMeta
	 * @param aClaszData
	 * @throws Exception 
	 */
	private void setSortKey2Data() throws Exception {
		for(Ty dataClasz : this.getObjectMap().values()) {
			setSortKey2Data(this.getMetaObj(), dataClasz);
		}
	}

	@SuppressWarnings("unchecked")
	private static <Ty extends Clasz<?>> void setSortKey2Data(Ty aClaszMeta, Ty aClaszData) throws Exception {
		for(Field metaField : aClaszMeta.getTreeField().values()) {
			Field dataField = aClaszData.getField(metaField.getDbFieldName());
			if (metaField.isAtomic()) {
				if (metaField.isSortKey()) {
					dataField.setSortKey(metaField);
				}
			} else {
				//if (metaField.getFieldType() == FieldType.OBJECT) {
				if (metaField instanceof FieldObject) {
					Ty claszMeta = ((FieldObject<Ty>) metaField).getObj();
					Ty claszData = ((FieldObject<Ty>) dataField).getObj();
					if (claszMeta != null) {
						if (claszData != null && (claszData.getClass().getSimpleName().equals("Clasz")) == false) { 
							setSortKey2Data(claszMeta, claszData); // recursive call to clear the member object sort keys
						}
					}
				//} else if (metaField.getFieldType() == FieldType.OBJECTBOX) {
				} else if (metaField instanceof FieldObjectBox) {
					Ty claszMeta = ((FieldObjectBox<Ty>) metaField).getMetaObj();
					if (claszMeta != null) {
						for(Ty eachClasz : ((FieldObjectBox<Ty>) dataField).getObjectMap().values()) {
							if ((eachClasz.getClass().getSimpleName().equals("Clasz")) == false) { 
								setSortKey2Data(claszMeta, eachClasz);
							}
						}
					}
				} else {
					throw new Hinderance("Invalid field type in object: " + aClaszMeta.getClaszName().toUpperCase());
				}
			}
		}
	}
	
	public long getTotalMemberInList() {
		return(this.getObjectMap().size());
	}

	public long getTotalMemberFromDb(Connection aConn) throws Exception {
		long result = 0;
		String strSql = "select count(*)"
		+ " from " + this.getMasterObject().getIwTableName(this.getDbFieldName()) 
		+ " where " + this.getMasterObject().getPkName() + " = " + this.getMasterObject().getObjectId();
		PreparedStatement stmt = null;
		ResultSet rset = null;
		try {
			stmt = aConn.prepareStatement(strSql);
			rset = stmt.executeQuery();
			while (rset.next()) {
				result = rset.getLong(1);
			}
		} catch (Exception ex) {
			if (stmt == null) {
				throw new Hinderance(ex, "getTotalMemberInList - Fail to count total member, stmt is null");
			} else {
				throw new Hinderance(ex, "getTotalMemberInList - Fail to count total member: " + stmt.toString());
			}
		} finally {
			if (rset != null) {
				rset.close();
			}
			if (stmt != null) {
				stmt.close();
			}
		}
		return result;
	}

	public Ty fetchByObjectIdAndAppend2List(Connection aConn, Long aObjId, Class<Ty> aClass) throws Exception {
		Ty result = fetchByObjectIdAndAppend2List(aConn, aObjId, aClass.getName());
		return(aClass.cast(result));
	}

	public Ty fetchByObjectIdAndAppend2List(Connection aConn, Long aObjId, String aObjClasz) throws Exception {
		return this.fetchByObjectId(aConn, String.valueOf(aObjId), aObjClasz, false);
	}

	public Ty fetchByObjectId(Connection aConn, Long aObjId, Class<Ty> aClass) throws Exception {
		Ty result = fetchByObjectId(aConn, aObjId, aClass.getName());
		return(aClass.cast(result));
	}

	public Ty fetchByObjectId(Connection aConn, Long aObjId, String aObjClasz) throws Exception {
		return(this.fetchByObjectId(aConn, String.valueOf(aObjId), aObjClasz));
	}

	public Ty fetchByObjectId(Connection aConn, String aObjId, String aObjClasz) throws Exception {
		return fetchByObjectId(aConn, aObjId, aObjClasz, true);
	}

	public Ty fetchByObjectId(Connection aConn, String aObjId, String aObjClasz, Boolean aClearList) throws Exception {
		List<Ty> aryFetchMember = new CopyOnWriteArrayList<>(); // for pass by ref only	
		String strSql = "select " + this.getDbFieldName() + ", " + ObjectBase.LEAF_CLASS 
		+ " from " + this.getMasterObject().getIwTableName(this.getDbFieldName()) 
		+ " where " + this.getMasterObject().getPkName() + " = " + this.getMasterObject().getObjectId()
		+ " and " + this.getDbFieldName() + " = " + aObjId; // getFieldName must convert to low level db field name?
		if (this.isPolymorphic()) {
			strSql += " and " + ObjectBase.LEAF_CLASS + " = " + "'" + aObjClasz + "'";
		}
		Ty fetchMember = null;
		try {
			FetchStatus status = FieldObjectBox.FetchMember(aConn, this, this.getMasterObject(), strSql, aryFetchMember, null, aClearList);
			this.setFetchStatus(status);
			fetchMember = aryFetchMember.get(0);
		} catch(Exception ex) {
			throw new Hinderance(ex, "Fail to fetchByObjectId, sql: " + strSql);
		}
		return(fetchMember);
	}

	public void fetchByCustomSql(Connection aConn, String aCustomSql) throws Exception {
		FetchStatus status = FetchMember(aConn, this, this.getMasterObject(), aCustomSql);
		this.setFetchStatus(status);
	}

	public void fetchByWhereRec(Connection aConn, Record recWhere) throws Exception {
		String claszTableName = this.getMetaObj().getTableName();
		String claszTablePk = this.getMetaObj().getPkName();
		StringBuffer strBuffer = new StringBuffer();
		Database.GetWhereClause(aConn, claszTableName, recWhere, strBuffer);
		String strSqlAnd = "";
		if (strBuffer.toString().trim().isBlank() == false) strSqlAnd = " and " + strBuffer.toString();

		String boxMemberTableName = this.getMasterObject().getIwTableName(this.getDbFieldName());
		String strJoinSql = boxMemberTableName + "." + this.getDbFieldName() + " = " + claszTableName + "." + claszTablePk;
		String strSql = "select " + this.getDbFieldName() + ", " + ObjectBase.LEAF_CLASS 
		+ " from " + boxMemberTableName  + ", " + claszTableName
		+ " where " + boxMemberTableName + "." + this.getMasterObject().getPkName() + " = " + this.getMasterObject().getObjectId()
		+ " and " + strJoinSql;
		if (strSqlAnd.isBlank() == false) strSql = strSql + strSqlAnd;

		App.logDebg(strSql);
		this.fetchByCustomSql(aConn, strSql);
	}

	public void fetchAll(Connection aConn) throws Exception {
		if (this.getTotalMemberInList() == 0) {
			String strSql = "select " + this.getDbFieldName() + ", " + ObjectBase.LEAF_CLASS 
			+ " from " + this.getMasterObject().getIwTableName(this.getDbFieldName()) 
			+ " where " + this.getMasterObject().getPkName() + " = " + this.getMasterObject().getObjectId();
			FetchStatus status = FetchMember(aConn, this, this.getMasterObject(), strSql);
			this.setFetchStatus(status);
		} else {
			if (this.getMasterObject().getObjectId() != Clasz.NOT_INITIALIZE_OBJECT_ID) { // if this is new object, do nothing actually, don't need do fetchAll, don't need warn user
				//throw new Hinderance("There must be no member when doing fetchaAll, total member: " + this.getTotalMember());
			}
		}
	}

	public static <Ty extends Clasz<?>> FetchStatus FetchMember(Connection aConn, FieldObjectBox<Ty> aResultFob, Clasz<?> aMasterObj, String strSql) throws Exception {
		List<Ty> aryFetchMember = new CopyOnWriteArrayList<>(); // for pass by ref only	
		FetchStatus status = FieldObjectBox.FetchMember(aConn, aResultFob, aMasterObj, strSql, aryFetchMember, null);
		return(status);
	}

	public boolean gotMember(Connection aConn, Ty aMember) throws Exception {
		boolean result = false;
		String strSql = "select " + this.getDbFieldName() + ", " + ObjectBase.LEAF_CLASS 
		+ " from " + this.getMasterObject().getIwTableName(this.getDbFieldName()) 
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

	/*
	public void forEachClasz(Connection aConn, Callback2ProcessClasz aCallback) throws Exception {
		this.forEachMember(aConn, "all", (Callback2ProcessMember) aCallback);
	}
	*/

	public void forEachMember(Connection aConn, Callback2ProcessMember<Ty> aCallback) throws Exception {
		this.forEachMember(aConn, "all", aCallback);
	}

	public void forEachMember(Connection aConn, Ty aCriteria, Callback2ProcessMember<Ty> aCallback) throws Exception {
		forEachMember(aConn, aCriteria, null, null, aCallback);
	}

	public void forEachMember(Connection aConn, Ty aCriteria, List<String> aSortField, List<SortOrder> aSortOrder, Callback2ProcessMember<Ty> aCallback) throws Exception {
		String strSql;
		if (this.getMemberClass().equals(aCriteria.getClass()) == false) throw new Hinderance("FieldObjectBox.forEachMember search criteria can only be FOB type");
		Multimap<String, Record> whereBox = Multimaps.synchronizedListMultimap(ArrayListMultimap.create());
		ObjectBase.GetMemberBoxByMemberCriteria(aConn, this, aCriteria, whereBox); // the select criteria for this leaf clszObject, doesn't do the parent clszObject
		List<Field> aryWhere = null;
		if (whereBox.isEmpty() == false) {
			StringBuffer strBuffer = new StringBuffer();
			aryWhere = Database.GetWhereClause(aConn, whereBox, strBuffer); // convert the where record into array list
			String mainTableName = this.getMasterObject().getIwTableName(this.getDbFieldName());
			strSql = "select " + this.getDbFieldName() + ", " + ObjectBase.LEAF_CLASS;
			String strFrom = Database.GetFromClause(mainTableName, whereBox);
			String strWhere = strBuffer.toString();

			String iwTableName = this.getMasterObject().getIwTableName(this.getDbFieldName());
			if (strFrom.contains(iwTableName) == false) {
				if (strFrom.isEmpty() == false) strFrom += ", ";
				strFrom += iwTableName;
			}
			String link2Parent = this.getMasterObject().getPkName() + " = " + this.getMasterObject().getObjectId();
			if (strWhere.isEmpty() == false) strWhere += " and ";
			strWhere += "(" + link2Parent + ")";

			strSql = strSql + " from " + strFrom + " where " + strWhere;
		} else {
			strSql = "select " + this.getDbFieldName() + ", " + ObjectBase.LEAF_CLASS 
			+ " from " + this.getMasterObject().getIwTableName(this.getDbFieldName()) 
			+ " where " + this.getMasterObject().getPkName() + " = " + this.getMasterObject().getObjectId();
		}

		String strOrder = "";
		if (aSortField != null && aSortOrder != null) {
			for(int cntrField = 0; cntrField < aSortField.size(); cntrField++) {
				String sortField = aSortField.get(cntrField).toLowerCase();
				SortOrder sortOrder = aSortOrder.get(cntrField);
				if (strOrder.isEmpty() == false) strOrder += ", ";
				strOrder += sortField + " " + SortOrder.AsString(sortOrder);
			}
			strSql += " order by " + strOrder;
		}

		FetchStatus status = FieldObjectBox.FetchMember(aConn, this, this.getMasterObject(), strSql, null, aCallback, aryWhere);
		this.setFetchStatus(status);
	}

	public void forEachMember(Connection aConn, String csvMemberList, Callback2ProcessMember<Ty> aCallback) throws Exception {
		String strSql = "select " + this.getDbFieldName() + ", " + ObjectBase.LEAF_CLASS 
		+ " from " + this.getMasterObject().getIwTableName(this.getDbFieldName()) 
		+ " where " + this.getMasterObject().getPkName() + " = " + this.getMasterObject().getObjectId();
		if (csvMemberList.equals("all") == false) {
			//strSql += " and " + this.getFieldName() + " in (" + csvMemberList + ")";
			strSql += " and (";
			String selectedPair = "";
			String[] employmentPair = csvMemberList.split(",");
			for(int cntr = 0; cntr < employmentPair.length; cntr+=2) {
				if (!selectedPair.isEmpty()) selectedPair += " or ";
				selectedPair += "(";
				selectedPair += this.getDbFieldName() + " = " + employmentPair[cntr].trim();
				selectedPair += " and " + ObjectBase.LEAF_CLASS + " = '" + employmentPair[cntr + 1].trim() + "'";
				selectedPair += ")";
			}
			strSql += selectedPair;
			strSql += ")";
		}	
		FetchStatus status = FieldObjectBox.FetchMember(aConn, this, this.getMasterObject(), strSql, null, aCallback);
		this.setFetchStatus(status);
	}

	public static <Ty extends Clasz<?>> void RemoveStaleMember(Connection aConn, Clasz<?> aMasterObj, FieldObjectBox<Ty> aFob, Long aMemberOid) throws Exception {
		String strSql = "delete from " + aMasterObj.getIwTableName(aFob.getDbFieldName()) 
		+ " where " + aMasterObj.getPkName() + " = " + aMasterObj.getObjectId()
		+ " and " + aFob.getDbFieldName() + " = " + aMemberOid;
		PreparedStatement stmt = null;
		Connection conn = null;
		try {
			App.logWarn(FieldObjectBox.class, "Removing stale member, sql: " + strSql);
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

	public static <Ty extends Clasz<?>> FetchStatus FetchMember(Connection aConn, FieldObjectBox<Ty> aResultFob, Clasz<?> aMasterObj, String strSql, List<Ty> aMemberContainer, Callback2ProcessMember<Ty> aCallback) throws Exception {
		return FetchMember(aConn, aResultFob, aMasterObj, strSql, aMemberContainer, aCallback, true);
	}

	public static <Ty extends Clasz<?>> FetchStatus FetchMember(Connection aConn, FieldObjectBox<Ty> aResultFob, Clasz<?> aMasterObj, String strSql, List<Ty> aMemberContainer, Callback2ProcessMember<Ty> aCallback, Boolean aClearList) throws Exception {
		return(FieldObjectBox.FetchMember(aConn, aResultFob, aMasterObj, strSql, aMemberContainer, aCallback, null, aClearList));
	}

	public static <Ty extends Clasz<?>> FetchStatus FetchMember(Connection aConn, FieldObjectBox<Ty> aResultFob, Clasz<?> aMasterObj, String strSql, List<Ty> aMemberContainer, Callback2ProcessMember<Ty> aCallback, List<Field> aAryWhere) throws Exception {
		return FetchMember(aConn, aResultFob, aMasterObj, strSql, aMemberContainer, aCallback, aAryWhere, true);
	}

	@SuppressWarnings("unchecked")
	public static <Ty extends Clasz<?>> FetchStatus FetchMember(Connection aConn, FieldObjectBox<Ty> aResultFob, Clasz<?> aMasterObj, String strSql, List<Ty> aMemberContainer, Callback2ProcessMember<Ty> aCallback, List<Field> aAryWhere, Boolean aClearList) throws Exception {
		FetchStatus result = FetchStatus.SOF;
		PreparedStatement stmt = null;
		ResultSet rset = null;
		try {
			stmt = aConn.prepareStatement(strSql);
			if(aAryWhere != null) Database.SetStmtValue(aConn, stmt, aAryWhere);
			rset = stmt.executeQuery();
			if (aClearList) aResultFob.getObjectMap().clear();
			while (rset.next()) {
				long pk = rset.getLong(1);
				Class<?> leafClass = Class.forName(aResultFob.getDeclareType());
				String polymorphicName = rset.getString(2);
				if (polymorphicName != null && polymorphicName.isEmpty() == false) {
					leafClass = Class.forName(polymorphicName); // if its a polymorphic member field
				}
				Ty fetchMember = Clasz.FetchObjectByPk(aConn, (Class<Ty>) leafClass, pk);
				if (fetchMember.getObjectId().equals(Clasz.NOT_INITIALIZE_OBJECT_ID)) {
					RemoveStaleMember(aConn, aMasterObj, aResultFob, pk);
				} else {
					if (aMemberContainer != null) {
						aMemberContainer.add(fetchMember);
					}
					aResultFob.addValueObject(fetchMember);  // now put the new member into the field object box
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
				throw new Hinderance(ex, "FetchMember - Fail to populate object array: null");
			} else {
				throw new Hinderance(ex, "FetchMember - Fail to populate object array: " + stmt.toString());
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

	public FetchStatus fetchSectionByObjectIndex(Connection aConn, String aPageDirection, int aFetchSize
	, String aSortField, String aSortValue, String aIndexName) throws Exception {
		return(this.fetchSectionByObjectIndex(aConn, aPageDirection, aFetchSize, aSortField, aSortValue, aIndexName, ""));
	}

	public FetchStatus fetchSectionByObjectIndex(Connection aConn, String aPageDirection, int aFetchSize
	, String aSortField, String aSortValue, String aIndexName, String aWhereClause) throws Exception {
		List<String> sortFieldList = new CopyOnWriteArrayList<>();
		List<String> sortValueList = new CopyOnWriteArrayList<>();
		List<SortOrder> sortOrderList = new CopyOnWriteArrayList<>();
		sortFieldList.add(aSortField);
		sortValueList.add(aSortValue);
		sortOrderList.add(SortOrder.ASC);
		FetchStatus result = this.fetchSectionByObjectIndex(aConn, aPageDirection, aFetchSize, sortFieldList, sortValueList, sortOrderList, aIndexName, aWhereClause);
		return(result);
	}

	public FetchStatus fetchSectionByObjectIndex(Connection aConn, String aPageDirection, int aFetchSize
	, String aSortField, String aSortValue, String aIndexName, SortOrder aDisplayOrder) throws Exception {
		List<String> sortFieldList = new CopyOnWriteArrayList<>();
		List<String> sortValueList = new CopyOnWriteArrayList<>();
		List<SortOrder> sortOrderList = new CopyOnWriteArrayList<>();
		sortFieldList.add(aSortField);
		sortValueList.add(aSortValue);
		sortOrderList.add(aDisplayOrder);
		return(this.fetchSectionByObjectIndex(aConn, aPageDirection, aFetchSize, sortFieldList, sortValueList, sortOrderList, aIndexName, ""));
	}

	public FetchStatus fetchSectionByObjectIndex(Connection aConn, String aPageDirection, int aFetchSize
	, List<String> aSortFieldList, List<String> aSortValueList, List<SortOrder> aSortOrderList
	, String aIndexName) throws Exception {
		return(this.fetchSectionByObjectIndex(aConn, aPageDirection, aFetchSize, aSortFieldList, aSortValueList, aSortOrderList, aIndexName, ""));
	}

	private FetchStatus fetchSectionByObjectIndex(Connection aConn, String aPageDirection, int aFetchSize
	, List<String> aSortFieldList, List<String> aSortValueList, List<SortOrder> aSortOrderList
	, String aIndexName, String aWhereClause) throws Exception {
		Clasz<?> masterObj = this.getMasterObject();
		FetchStatus result = fetchSectionByObjectIndex(aConn, masterObj, this, aPageDirection, aFetchSize, aSortFieldList, aSortValueList, aSortOrderList, aIndexName, aWhereClause);
		this.setFetchStatus(result);
		return(result);
	}

	public static <Ty extends Clasz<?>> String SqlStrOfObjectIndex(Class<?> aClass, String aIndexName, String aWhereClause, String aOrderClause, FieldObjectBox<Ty> aField, Clasz<?> aMasterObj) throws Exception {
		String result = "";
		if (Clasz.class.isAssignableFrom(aClass)) {
			String strIndexLeaf = Clasz.CreateLeafClassColName(aField.getDbFieldName());	
			String sqlSelect = "select" + " mom_child."+ aField.getDbFieldName() + ", " + "mom_child." + ObjectBase.LEAF_CLASS 
			+ " from " + aIndexName + ", " 
			+ "(select " + aField.getDbFieldName() + ", " + ObjectBase.LEAF_CLASS 
			+ " from " + aMasterObj.getIwTableName(aField.getDbFieldName()) 
			+ " where " + aMasterObj.getPkName() + " = " + aMasterObj.getObjectId() + ") mom_child";
			String sqlWhere = " where (mom_child." + aField.getDbFieldName() + " = " + aIndexName + "." + aField.getDbFieldName() 
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

	static final class PopulateMemberObjectThreadPk<Ty extends Clasz<?>> extends Thread {
		Integer[] cntrThread;
		Connection conn;
		Class<Ty> leafClass;
		Long pk;

		FieldObjectBox<Ty> parentField;

		public PopulateMemberObjectThreadPk(Integer[] aCntrThread, Connection aConn, FieldObjectBox<Ty> aParent, Class<Ty> aClass, Long aPk) throws Exception {
			this.cntrThread = aCntrThread;
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
				Ty fetchMember = Clasz.FetchObjectByPk(this.conn, (Class<Ty>) this.leafClass, this.pk);
				if (fetchMember.getObjectId().equals(Clasz.NOT_INITIALIZE_OBJECT_ID)) {
					RemoveStaleMember(this.conn, this.parentField.getMasterObject(), this.parentField, this.pk); // if stale member, never add them into the Fob, coz they're not a member already, they're stale
				} else {
					this.parentField.addValueObject(fetchMember);  // now put the new member into the field object box
				}
			} catch (Exception ex) {
				//App.logEror(ex, "Fail thread: " + this.cntrThread[0] + ", in FetchMemberThread for class: " + this.leafClass.getSimpleName() + ", of pk: " + this.pk);
				throw new RuntimeException(ex);
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

	public interface GetFetchChildSql<Ty> {
		Object execute(Ty aParam) throws Exception;
	}

	public interface AfterPopulate<Ty> {
			Object execute(Ty aParam) throws Exception;
	}

	public Iterator<Entry<Long, Ty>> getIterator() {
		return iterator;
	}

	public void setIterator(Iterator<Entry<Long, Ty>> iterator) {
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
		for (Map.Entry<Long, Ty> entry : this.getObjectMap().entrySet()) {
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
		this.mustFetch(); // should remove and assume items already in map? need to check may break other areas
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

	public Ty getNext() throws Exception {
		this.mustFetch(); // should remove and assume items already in map? need to check may break other areas

		if (this.getKey() == null ) {
			this.setKey(-1L);
		}

		Long largestIndex = this.getLargestIndex();
		while (this.getKey().compareTo(largestIndex) <= 0) {
			this.setKey(this.getKey() + 1);
			Ty result = this.getObjectMap().get(this.getKey());
			if (result != null) {
				return(result);
			} else {
				this.setKey(this.getKey() + 1);
			}
		}
		throw new Hinderance("Error, already at the end of iteration, field: " + this.getDbFieldName() + ", key: " + this.getKey());
	}

	public Ty getNextByIterator() throws Exception {
		this.mustFetch(); // should remove and assume items already in map? need to check may break other areas

		if (this.getIterator() == null ) {
			this.resetIterator();
		}

		if (this.getIterator().hasNext()) {
			Map.Entry<Long, Ty> mapEntry = this.getIterator().next();
			this.setKey(mapEntry.getKey());
			return(mapEntry.getValue());
		} else {
			throw new Hinderance("Error, already at the end of iteration, field: " + this.getDbFieldName());
		}
	}

	public void setKey(Long aKey) throws Exception {
		this.key = aKey;
	}

	public Long getKey() throws Exception {
		this.mustFetch(); // should remove and assume items already in map? need to check may break other areas

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
		this.mustFetch(); // should remove and assume items already in map? need to check may break other areas
		this.getObjectMap().remove(aKey);
	}

	public void remove(Ty aToRemove) throws Exception {
		Set<Entry<Long, Ty>> entrySet = this.getObjectMap().entrySet();
		for (Iterator<Entry<Long, Ty>> iterateClasz = entrySet.iterator(); iterateClasz.hasNext();) {
			Entry<Long, Ty> entry = iterateClasz.next();
			Ty value = entry.getValue();
			if(value == aToRemove) {
				iterateClasz.remove(); //Removing Entry from map
				break;
			}    
		}
	}

	public Ty getValue(Long aIndex) throws Exception {
		this.mustFetch(); // should remove and assume items already in map? need to check may break other areas
		return(this.getObjectMap().get(aIndex));
	}
	
	public boolean containsKey(Long aIndex) throws Exception {
		this.mustFetch(); // should remove and assume items already in map? need to check may break other areas

		if (this.getObjectMap().containsKey(aIndex)) {
			return (true);
		}
		return (false);
	}

	public void mustFetch() throws Exception {
		if (this.getFetchStatus() == FetchStatus.SOF && this.isModified() == false) { // SOF mean there's no attempt to Fetch from db at all, if this field is modified...
			Connection conn = this.getMasterObject().getDb().getConnPool().getConnection();
			try {
				this.mustFetch(conn); // should remove and assume items already in map? need to check may break other areas
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
	
	private static <Ty extends Clasz<?>> Map<Long, Ty> SortByComparator(Map<Long, Ty> unsortMap) throws Exception {
		List<Map.Entry<Long, Ty>> list = new LinkedList<>(unsortMap.entrySet()); // Convert Map to List
 
		// Sort list with comparator, to compare the Map values
		Collections.sort(list, (Map.Entry<Long, Ty> o1, Map.Entry<Long, Ty> o2) -> (o1.getValue()).compareTo(o2.getValue()));
 
		// Convert sorted map back to a Map
		Long cntr = 0L;
		Map<Long, Ty> sortedMap = new LinkedHashMap<>();
		for (Map.Entry<Long, Ty> entry : list) {
			sortedMap.put(cntr++, entry.getValue());
		}
		return(sortedMap);
	}

	public Ty addObject(Connection aConn, Class<Ty> aClass) throws Exception {
		Ty clasz = ObjectBase.CreateObject(aConn, aClass);
		this.addValueObject(clasz);
		return(clasz);
	}

	public Ty getFirstMember() throws Exception {
		return(this.objectMap.get(0L));
	}

	public boolean gotMember(Connection aConn) throws Exception {
		LambdaGeneric<Boolean> result = new LambdaGeneric<>(false);
		this.forEachMember(aConn, (Connection bConn, Ty aClasz) -> { // uses for each member will ensure we fetched the right polymorphic member
			result.setValue(true);
			return(false);
		});
		
		return result.getValue();
	}

	public Ty fetchFirstMember(Connection aConn, Ty aCriteria) throws Exception {
		List<Ty> result = new CopyOnWriteArrayList<>();
		// uses for each member will ensure we fetched the right polymorphic member
		this.forEachMember(aConn, (Connection bConn, Ty aClasz) -> { 
			try {
				if (aCriteria.equalsCriteria(bConn, aClasz)) {
					result.add((Ty) aClasz);
					return(false);
				}
				return(true);
			} catch(Exception ex) {
				//App.logEror(ex, "Fail, exception thrown in FieldObjectBox.fetchFirsMember method.");
				//return false ; // discontinue and get out
				throw new Hinderance(ex, "Fail, exception thrown in FieldObjectBox.fetchFirsMember method.");
			}
		});
		
		if (result.size() > 0) {
			return(result.get(0));
		} else {
			return(null);
		}
	}

	@SuppressWarnings("unchecked")
	public Ty fetchUniqueMember(Connection aConn, Ty aCriteria, String aIndexName) throws Exception {
		Ty theMember = null;
		Table oiTable = new Table(aIndexName);
		oiTable.initMeta(aConn);
		//List<Field> indexedField = ObjectIndex.GetIndexedField(aConn, this.getMasterObject()); // get the indexed field of this master object
		List<Field> indexField = new CopyOnWriteArrayList<>();
		String whereClause = ObjectIndex.GetIndexWhereCriteria(aConn, aIndexName, this.getDbFieldName(), aCriteria, indexField);
		Class<Ty> fieldClass = (Class<Ty>) Class.forName(this.getDeclareType());
		String sqlStr = SqlStrOfObjectIndex(fieldClass, aIndexName, whereClause, "", this, this.getMasterObject());
		PreparedStatement stmt = null; 
		ResultSet rs = null;
		try {
			stmt = aConn.prepareStatement(sqlStr);
			Database.SetStmtValue(aConn, stmt, indexField);
			rs = stmt.executeQuery();
			int cntr = 0;
			while (rs.next()) {
				cntr++;
				if (cntr >= 2) {
					throw new Hinderance("Fetching object for unique member return non unique result: " + this.getClass().getSimpleName() + ", index: " + aIndexName);
				}
				long pk = rs.getLong(1);
				Class<Ty> leafClass = fieldClass;
				String polymorphicName = rs.getString(2);
				if (polymorphicName != null && polymorphicName.isEmpty() == false) {
					leafClass = (Class<Ty>) Class.forName(polymorphicName); // if its a polymorphic member field, get its polymorphic class name
				}
				theMember = (Ty) Clasz.FetchObjectByPk(aConn, leafClass, pk);
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
	
	private static List<Object> SqlPagingForObjectIndex(Connection aConn, String aIndexName, List<String> aKeyField, List<String> aKeyValue, List<SortOrder> aKeyOrder, String aPageDirection) throws Exception {
		if (aKeyField.size() != aKeyValue.size()) {
			throw new Hinderance("When SqlPagingForObjectIndex, its total key clasz, key field and key value must be the same!");
		}

		List<String> sameOrderKeyField = new CopyOnWriteArrayList<>();
		List<String> sameOrderKeyValue = new CopyOnWriteArrayList<>();
		List<SortOrder> sameOrderKeyOrder = new CopyOnWriteArrayList<>();

		String resultWhereExpression = "";
		String resultSortOrder = "";
		String firstSetWhereExpression = "";
		int sortOrderBatchCntr = 0;
		boolean allValueIsEmpty = true;
		for(int cntrSort = 0; cntrSort < aKeyOrder.size(); cntrSort++) {
			SortOrder currentSortOrder = aKeyOrder.get(cntrSort);
			SortOrder nextSortOrder = null;
			if (cntrSort + 1 < aKeyOrder.size()) {
				nextSortOrder = aKeyOrder.get(cntrSort + 1);
			}

			sameOrderKeyField.add(aKeyField.get(cntrSort));
			sameOrderKeyValue.add(aKeyValue.get(cntrSort));
			sameOrderKeyOrder.add(aKeyOrder.get(cntrSort));

			// different sort order, start getting the sql
			if (nextSortOrder == null || currentSortOrder.equals(nextSortOrder) == false) { 
				List<Object> sameSortOrder = GetSqlPagingForObjectIndex(aConn, aIndexName, sameOrderKeyField, sameOrderKeyValue, sameOrderKeyOrder, aPageDirection);
				String strWhereExpression = ((String) sameSortOrder.get(0)).trim();
				String strSortOrder = ((String) sameSortOrder.get(1)).trim();
				boolean isEmptyValue = (boolean) sameSortOrder.get(2);
				if (isEmptyValue == false) allValueIsEmpty = false; 

				// no equal for last where str to ensure fetch is next haven't display record
				if (cntrSort == aKeyOrder.size() - 1 && aPageDirection.equals("seek") == false) {
					strWhereExpression = strWhereExpression.replaceAll(">=", ">");
					strWhereExpression = strWhereExpression.replaceAll("<=", "<");
				}

				// after doing set section, need to or into set with primary key set and no other keys set
				if (sortOrderBatchCntr == 0) {
					firstSetWhereExpression = strWhereExpression;
					if (aPageDirection.equals("seek") == false) {
						firstSetWhereExpression = firstSetWhereExpression.replaceAll(">=", ">");
						firstSetWhereExpression = firstSetWhereExpression.replaceAll("<=", "<");
					}
				} 

				// place there where and sort clause into result
				if (strWhereExpression.isEmpty() == false) {
					if (resultWhereExpression.isEmpty() == false) resultWhereExpression += " and ";
					resultWhereExpression += strWhereExpression;
				}
				if (strSortOrder.isEmpty() == false) {
					if (resultSortOrder.isEmpty() == false) resultSortOrder += ", ";
					resultSortOrder += strSortOrder;
				}

				// start with new sort batch
				sortOrderBatchCntr++;
				sameOrderKeyField.clear();
				sameOrderKeyValue.clear();
				sameOrderKeyOrder.clear();
			}
		}

		if (allValueIsEmpty == true) {
			resultWhereExpression = "";
		} else {
			resultWhereExpression = "(" + resultWhereExpression + ")" + " or " + "(" + firstSetWhereExpression + ")";
		}

		List<Object> result = new CopyOnWriteArrayList<>();
		result.add(resultWhereExpression);
		result.add(resultSortOrder);
		return result;
	}

	private static List<Object> GetSqlPagingForObjectIndex(Connection aConn, String aIndexName, List<String> aKeyField, List<String> aKeyValue, List<SortOrder> aSortOrder, String aPageDirection) throws Exception {
		String concatFieldName = "";
		String concatFieldValue = "";
		String strOrder = "";

		String objectIndexTableName = aIndexName.toLowerCase();
		Table objectIndexTable = new Table(objectIndexTableName);
		objectIndexTable.initMeta(aConn);
		for(int cntrField = 0; cntrField < aKeyField.size(); cntrField++) {
			String keyName = aKeyField.get(cntrField).toLowerCase();
			String keyValue = aKeyValue.get(cntrField);
			SortOrder sortOrder = aSortOrder.get(cntrField);
			if (keyValue == null) keyValue = "";

			// concat field, concat value
			if (concatFieldName.isEmpty() == false) {
				if (Database.GetDbType(aConn) == DbType.MYSQL) {
					concatFieldName += ", ";
				} else {
					concatFieldName += " || ";
				}
			}
			//if (concatFieldValue.isEmpty() == false) concatFieldValue += " ";

			// pad column and value according to field type
			Field keyField = objectIndexTable.getField(keyName);
			if (keyField.getDbFieldType() == FieldType.DATETIME) {
				String dateForSort = DateAndTime.FormatDateTimeForSort(keyValue);
				concatFieldValue += dateForSort;
				concatFieldName += Database.DateTimeForSort(aConn, objectIndexTableName + "." + keyName);
			} else if (keyField.getDbFieldType() == FieldType.DATE) {
				String dateForSort = DateAndTime.FormatDateForSort(keyValue);
				concatFieldValue += dateForSort;
				concatFieldName += Database.DateForSort(aConn, objectIndexTableName + "." + keyName);
			} else if (keyField.getDbFieldType() == FieldType.INTEGER || keyField.getDbFieldType() == FieldType.LONG) {
				String digitForSort = Generic.PadDigitForSort(keyValue);
				concatFieldValue += digitForSort;
				String sqlDigit2Str = Database.Num2StrSql(aConn, objectIndexTableName + "." + keyName);
				concatFieldName += Database.LeftPadSql(aConn, sqlDigit2Str, digitForSort.length(), " ");
			} else if (keyField.getDbFieldType() == FieldType.FLOAT) {
				String digitForSort = Generic.PadFloatForSort(keyValue);
				concatFieldValue += digitForSort;
				String sqlDigit2Str = Database.Float2StrSql(aConn, objectIndexTableName + "." + keyName);
				concatFieldName += Database.LeftPadSql(aConn, sqlDigit2Str, digitForSort.length(), " ");
			} else {
				String strForSort = Generic.PadStrForSort(keyValue.toLowerCase(), keyField.getFieldSize());
				concatFieldValue += strForSort;
				concatFieldName += Database.RightPadSql(aConn, objectIndexTableName + "." + keyName, strForSort.length(), " ");
			}

			// handle sort order
			SortOrder newSortOrder = sortOrder;
			if (aPageDirection.equals("prev")) {
				newSortOrder = SortOrder.ReverseOrder(sortOrder);
			} 
			if (strOrder.isEmpty() == false) strOrder += ", ";
			strOrder += objectIndexTableName + "." + keyName + " " + SortOrder.AsString(newSortOrder);
		}

		if (concatFieldName.isEmpty() == false && aKeyField.size() == 1) {
			if (Database.GetDbType(aConn) == DbType.MYSQL) {
				concatFieldName += ", ''";
			} else {
				concatFieldName += " || ''";
			}
		}

		// for mysql only
		if (concatFieldName.isEmpty() == false) {
			if (Database.GetDbType(aConn) == DbType.MYSQL) {
				concatFieldName = "concat(" + concatFieldName + ")";
			}
		}

		SortOrder primarySortOrder = aSortOrder.get(0);
		String strWhereExpression = Clasz.GetPagingCondition(concatFieldName, concatFieldValue, primarySortOrder, aPageDirection);

		List<Object> result = new CopyOnWriteArrayList<>();
		result.add(strWhereExpression);
		result.add(strOrder);
		result.add(concatFieldValue.trim().isEmpty());
		return result;
	}

	private static <Ty extends Clasz<?>> void FetchMemberOfBoxSpawner(Connection aConn, List<Thread> threadPool, FieldObjectBox<Ty> aFieldBox, Integer[] cntrThreadPassAsRef, Class<Ty> leafClass, long pk) throws Exception {
		if (App.getMaxThread() == 1) { // no threading
			cntrThreadPassAsRef[0]++;
			App.logDebg(FieldObjectBox.class, "Spawning thread sequentially: " + cntrThreadPassAsRef[0]);
			//(new PopulateMemberObjectThreadPk(cntrThreadPassAsRef, aMasterObj, aConn, aFieldBox, leafClass, pk)).join();
			(new PopulateMemberObjectThreadPk<>(cntrThreadPassAsRef, aConn, aFieldBox, leafClass, pk)).join();
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
						App.ShowThreadingStatus(FieldObjectBox.class, "FetchMemberOfBoxSpawner", cntrThreadPassAsRef[0], App.getMaxThread(), cntrAttempt, maxAttempt);
						Thread.sleep(App.MAX_THREAD_OR_CONN_WAIT); // if no db connection, wait x second and continue the loop
					} else { 
						cntrThreadPassAsRef[0]++;
						App.logDebg(FieldObjectBox.class, "Spawning thread parallelly: " + cntrThreadPassAsRef[0]);
						//Thread theThread = new PopulateMemberObjectThreadPk(cntrThreadPassAsRef, aMasterObj, conn, aFieldBox, leafClass, pk);
						Thread theThread = new PopulateMemberObjectThreadPk<>(cntrThreadPassAsRef, conn, aFieldBox, leafClass, pk);
						threadPool.add(theThread);
						break;
					}
				} catch(Exception ex) {
					App.ShowThreadingStatus(FieldObjectBox.class, "FetchMemberOfBoxSpawner", cntrThreadPassAsRef[0], App.getMaxThread(), cntrAttempt, maxAttempt);
					if (cntrAttempt >= maxAttempt) {
						throw new Hinderance(ex, "[fetchMemberSection] Give up threading due to insufficent db connection");
					} else {
						Thread.sleep(App.MAX_THREAD_OR_CONN_WAIT); // wait for other db conn to free up
					}
				} 
			}
		}
	}

	@SuppressWarnings("unchecked")
	private FetchStatus fetchSectionByObjectIndex(Connection aConn, Clasz<?> aMasterObj, FieldObjectBox<Ty> aFieldBox, String aPageDirection, int aPageSize
	, List<String> aSortFieldList, List<String> aSortValueList, List<SortOrder> aSortOrderList, String aIndexName, String aWhereClause) throws Exception {

		int totalField = aSortFieldList.size();
		if (aSortValueList.size() != totalField || aSortOrderList.size() != totalField) {
			throw new Hinderance("[fetchSectionByObjectIndex] The pass in array of fields to sort, its value and its sort key is not the same!");
		}

		FetchStatus result = FetchStatus.EOF;
		PreparedStatement stmt = null;
		ResultSet rset = null;

		try {
			// get sql objects for pagination clause
			List<Object> keyPageObject = SqlPagingForObjectIndex(aConn, aIndexName, aSortFieldList, aSortValueList, aSortOrderList, aPageDirection); 
			String whereClause = ((String) keyPageObject.get(0));
			String sortClause = ((String) keyPageObject.get(1));

			// handle where clause
			if (whereClause.isEmpty() == false) {
				whereClause = "(" + whereClause + ")";
			}
			if (aWhereClause != null && !aWhereClause.isEmpty()) {
				if (whereClause.isEmpty() == false) whereClause += " and ";
				whereClause += " " + aWhereClause.trim();
			}

			// compose the sql for retrieve object index records
			Class<Ty> fieldClass = (Class<Ty>) Class.forName(aFieldBox.getDeclareType());
			String strSqlObjectIndex = SqlStrOfObjectIndex(fieldClass, aIndexName, whereClause, sortClause, aFieldBox, aMasterObj);
			stmt = aConn.prepareStatement(strSqlObjectIndex);

			Table oiTable = new Table(aIndexName);
			oiTable.initMeta(aConn);

			rset = stmt.executeQuery();
			//App.logDebg(this, "ObjectIndex sql: " + stmt.toString());
			aFieldBox.getObjectMap().clear();
			int cntrRow = 0;
			Integer[] cntrThreadPassAsRef = {0};
			List<Thread> threadPool = new CopyOnWriteArrayList<>();
			while (rset.next()) {
				App.logDebg(this, "Result set fetch cntr: " + cntrRow + ", pageSize: " + aPageSize);
				if (cntrRow < aPageSize) {
					cntrRow++;
					long pk = rset.getLong(1);
					Class<Ty> leafClass = fieldClass;
					String polymorphicName = rset.getString(2);
					if (polymorphicName != null && polymorphicName.isEmpty() == false) {
						leafClass = (Class<Ty>) Class.forName(polymorphicName); // if its a polymorphic member field, get its polymorphic class name
					}

					// can refactor as a spawner pattern in future
					FetchMemberOfBoxSpawner(aConn, threadPool, aFieldBox, cntrThreadPassAsRef, leafClass, pk);

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
				throw new Hinderance(ex, "fetchSectionByObjectIndex - [" + aIndexName + "], fail to populate object array: null");
			} else {
				throw new Hinderance(ex, "fetchSectionByObjectIndex - [" + aIndexName + "], fail to populate object array: " + stmt.toString());
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

	public static <Ty extends Clasz<?>> void MarkMemberFieldForRemove(Connection aConn, FieldObjectBox<Ty> aFob, String aFieldName) throws Exception {
		aFob.resetIterator();
		while (aFob.hasNext(aConn)) {
			Ty eachClasz = aFob.getNext();
			eachClasz.getField(aFieldName).forRemove(true);
		}
	}

	public void markAllMemberFieldForKeep(Connection aConn) throws Exception {
		MarkAllMemberFieldForKeep(aConn, this);
	}

	public static <Ty extends Clasz<?>> void MarkAllMemberFieldForKeep(Connection aConn, FieldObjectBox<Ty> aFob) throws Exception {
		aFob.resetIterator(); 
		while(aFob.hasNext(aConn)) {
			Ty eachClasz = aFob.getNext();
			eachClasz.markAllFieldForKeep();
		}
	}

	public void removeMarkMemberField(Connection aConn) throws Exception {
		RemoveMarkMemberField(aConn, this);
	}

	public static <Ty extends Clasz<?>> void RemoveMarkMemberField(Connection aConn, FieldObjectBox<Ty> aFob) throws Exception {
		aFob.resetIterator(); 
		while(aFob.hasNext(aConn)) {
			Ty eachClasz = aFob.getNext();
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
	public static void CreateBoxMemberTable(Connection aConn, Class<?> aParent, String aLinkField) throws Exception {
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

	public void renameBoxMemberUniqueIndex(Connection aConn, Class<?> aParent, String aNewClassName) throws Exception {
		String oldTableName = Clasz.GetIwTableName(aParent, this.getDbFieldName());
		String newTableName = Clasz.GetIwTableName(aNewClassName, this.getDbFieldName());
		Table linkTable = new Table(oldTableName);
		String oldIndexName = linkTable.getIndexName() + "_unq";
		String newIndexName = newTableName + "_unq";
		Database.AlterTableRenameIndex(aConn, linkTable.getTableName(), oldIndexName, newIndexName);
	}

	public FetchStatus fetchBySection(Connection aConn, String aKeyField, String aKeyValue, SortOrder aSortOrder, String aPageDirection, int aPageSize) throws Exception {
		List<String> keyField = new CopyOnWriteArrayList<>();
		keyField.add(aKeyField);

		List<String> keyValue = new CopyOnWriteArrayList<>();
		keyValue.add(aKeyValue); 

		List<SortOrder> orderList = new CopyOnWriteArrayList<>();
		orderList.add(aSortOrder);

		return this.fetchBySection(aConn, keyField, keyValue, orderList, aPageDirection, aPageSize);
	}

	@SuppressWarnings("unchecked")
	public FetchStatus fetchBySection(Connection aConn, List<String> aKeyField, List<String> aKeyValue, List<SortOrder> aSortOrder, String aPageDirection, int aPageSize) throws Exception {
		if (this.isInline()) {
			throw new Hinderance("Fob fetchBySection cannot be use for inline fob field: " + this.getMasterClassSimpleName() + "." + this.getCamelCaseName());
		}

		Multimap<String, Record> whereBox = Multimaps.synchronizedListMultimap(ArrayListMultimap.create());
		String iwBoxTableName = Clasz.GetIwTableName(this);
		Record linkIwBox2Member = new Record();
		linkIwBox2Member.createField(this.getDbFieldName(), "");
		String memberTableName = Clasz.CreateTableName(this.getMemberClass());
		String memberPkName = Clasz.CreatePkColName(this.getMemberClass());
		linkIwBox2Member.getField(this.getDbFieldName()).setFormulaStr(iwBoxTableName + "." + this.getDbFieldName() + " = " + memberTableName + "." + memberPkName);
		whereBox.put(iwBoxTableName, linkIwBox2Member);

		Record linkParent2Member = new Record();
		String parentPkName = Clasz.CreatePkColName(this.getMasterClass());
		linkParent2Member.createField(parentPkName, this.getMasterObject().getObjectId());
		whereBox.put(iwBoxTableName, linkParent2Member);

		Class<Ty> memberClass = (Class<Ty>) this.getMemberClass();
		this.removeAll();
		FieldObjectBox<Ty> fobResult = this;
		FetchStatus result;
		if (this.isPolymorphic() == false) {
			result = Clasz.FetchBySection(aConn, memberClass, aKeyField, aKeyValue, aSortOrder, whereBox, fobResult, aPageDirection, aPageSize, null);
		} else {
			result = Clasz.FetchBySection(aConn, memberClass, aKeyField, aKeyValue, aSortOrder, whereBox, fobResult, aPageDirection, aPageSize, (Connection bConn, ResultSet aRs) -> {
				// get the memberClass oid and polymorphic class type and fetch the object directly from member cz table
				Long oid = aRs.getLong(this.getDbFieldName());
				String leafClassName = aRs.getString(ObjectBase.LEAF_CLASS);
				Class<Ty> leafClass = (Class<Ty>) Class.forName(leafClassName);
				Ty clasz = ObjectBase.CreateObject(bConn, (Class<Ty>) leafClass);
				clasz.setObjectId(oid);
				if (clasz.populate(bConn)) {
					fobResult.addValueObject(clasz);
				} else {
					throw new Hinderance("Fail to populate during fetchBySection, class: " + leafClass + ", oid: " + oid);
				}
				return(true);
			});
		}
		return(result);
	}
}
