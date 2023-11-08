package biz.shujutech.db.object;

import biz.shujutech.base.App;
import biz.shujutech.base.Connection;
import biz.shujutech.base.Hinderance;
import biz.shujutech.db.relational.Database;
import biz.shujutech.db.relational.Field;
import biz.shujutech.db.relational.FieldType;
import biz.shujutech.db.relational.Record;
import biz.shujutech.db.relational.Table;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ObjectIndex {

	public static void DeleteIndex(Connection aConn, Clasz<?> aClasz2Update) throws Exception {
		Record whereRecord = new Record();
		whereRecord.createField(aClasz2Update.getPkName(), aClasz2Update.getObjectId());
		for(String eachIndexName : GetAllIndexName(aConn, aClasz2Update)) { // for each index
			Table indexTable = new Table(eachIndexName);
			indexTable.delete(aConn, whereRecord);
		}
	}

	public static void UpdateIndexAll(Connection aConn, Clasz<?> aClasz2Update) throws Exception {
		for(String eachIndexName : GetAllIndexName(aConn, aClasz2Update)) { // for each index
			UpdateIndex(aConn, aClasz2Update, eachIndexName, false);
		}
	}

	public static void UpdateIndex(Connection aConn, Clasz<?> aClasz2Update, String eachIndexName, boolean aItsPopulate) throws Exception {
		try {
			//List<String> indexColumn = GetIndexColumn(aConn, eachIndexName, objId, aClasz2Update.getPkName()); // the column name is use to determine which field is being index on
			List<String> indexColumn = GetIndexColumn(aConn, eachIndexName); // the column name is use to determine which field is being index on

			Record whereRec = new Record();
			String pkName = aClasz2Update.getPkName();
			whereRec.createField(pkName, aClasz2Update.getObjectId());

			Table indexTable = new Table(eachIndexName);
			Record indexRec = indexTable.createRecord(aConn);
//App.logDebg(ObjectIndex.class, "Updating objectindex: " + eachIndexName + ", of clasz: " + aClasz2Update.getClass().getName() + ", pkName: " + pkName + ", value: " + indexRec.getField(pkName));
			indexRec.getField(pkName).setValue(aClasz2Update.getObjectId()); 

			TraverseObjForUpdate(aConn, aClasz2Update, indexColumn, indexTable, indexRec, whereRec, "", aItsPopulate);
		} catch(Exception ex) {
			throw new Hinderance(ex, "Fail to update object index: " + eachIndexName + ", of clasz: " + aClasz2Update.getClass().getName());
		}
	}

	public static boolean IndexPopulated(Connection aConn, Record aIndexRec) throws Exception {
		boolean result = true;
		for(Field eachField : aIndexRec.getFields()) {
			if (eachField.getValueObj(aConn) == null) {
				result = false;
				break;
			}
		}
		return(result);
	}

	private static void TraverseForInsertOrUpdate(Connection bConn, Record whereRec, String colName, Clasz<?> eachClasz, Record newIndexRec, String leafClassColName, Table aIndexTable, List<String> aIndexColumn, Record aIndexRec, Record aWhereRec, boolean aItsPopulate) throws Exception {
		whereRec.getField(colName).setValue(eachClasz.getObjectId()); // as it traverse, collect the related objectid into the where rec
		newIndexRec.getField(colName).setValue(eachClasz.getObjectId());
		String leafClass = eachClasz.getClass().getName();
		whereRec.getField(leafClassColName).setValueStr(leafClass);
		newIndexRec.getField(leafClassColName).setValue(leafClass); 
		if (IndexPopulated(bConn, newIndexRec)) {
			InsertOrUpdateIndex(bConn, aIndexTable, newIndexRec, whereRec);
		} else {
			TraverseObjForUpdate(bConn, eachClasz, aIndexColumn, aIndexTable, newIndexRec, whereRec, colName, aItsPopulate);
		}
		newIndexRec.copyStrNull(bConn, aIndexRec); // reinit the index record to the before state for the next traverse
		whereRec.copyStrNull(bConn, aWhereRec);
	}

	@SuppressWarnings("unchecked")
	private static void TraverseObjForUpdate(Connection aConn, Clasz<?> aClasz, List<String> aIndexColumn, Table aIndexTable, Record aIndexRec, Record aWhereRec, String aFqFieldName, boolean aItsPopulate) throws Exception {
		for(Field eachField : aClasz.getTreeField().values()) { 
			String colName = GetFqFieldName(aFqFieldName, eachField.getDbFieldName());
			if (aIndexColumn.contains(colName)) {
				//if (eachField.getFieldType() == FieldType.OBJECT) {
				if (eachField instanceof FieldObject) {
					if (eachField.getValueObj(aConn) != null) {
						Long objId = ((FieldObject<?>) eachField).getValueObj(aConn).getObjectId();
						aWhereRec.createField(colName, objId); // as it traverse, collect the related objectid into the where rec
						aIndexRec.getField(colName).setValue(objId); // as it traverse, collect the related field value into the index rec
						//if (eachField.isPolymorphic()) {}
						String leafClass = ((FieldObject<?>) eachField).getValueObj(aConn).getClass().getName();
						String leafClassColName = Clasz.CreateLeafClassColName(colName);	
						aWhereRec.createField(leafClassColName, leafClass);
						aIndexRec.getField(leafClassColName).setValue(leafClass); // as it traverse, collect the related field value into the index rec
						if (IndexPopulated(aConn, aIndexRec)) { // if the index is fully populated, insert/update the index and its consider complete
							InsertOrUpdateIndex(aConn, aIndexTable, aIndexRec, aWhereRec);
							break;
						} else {
							Clasz<?> fieldClasz = (Clasz<?>) eachField.getValueObj(aConn);
							TraverseObjForUpdate(aConn, fieldClasz, aIndexColumn, aIndexTable, aIndexRec, aWhereRec, colName, aItsPopulate);
						}
					}
				//} else if (eachField.getFieldType() == FieldType.OBJECTBOX) {
				} else if (eachField instanceof FieldObjectBox<?>) {
					FieldObjectBox<Clasz<?>> fob = (FieldObjectBox<Clasz<?>>) eachField;
					aWhereRec.createField(colName, FieldType.LONG);
					String leafClassColName = Clasz.CreateLeafClassColName(colName);	
					aWhereRec.createField(leafClassColName, FieldType.STRING, ObjectBase.CLASS_NAME_LEN);
					Record newIndexRec = aIndexTable.createRecord(aConn); newIndexRec.copyStrNull(aConn, aIndexRec); // copy the state of this index record into a new index record
					Record whereRec = new Record(aWhereRec); whereRec.copyStrNull(aConn, aWhereRec);
					if (aItsPopulate) { // read all record and update the index
						//List<Exception> excptInLambda = new CopyOnWriteArrayList<>();
						fob.forEachMember(aConn, (Connection bConn, Clasz<?> eachClasz) -> { // loop thru fieldobjectbox and recursive call  
							try {
								TraverseForInsertOrUpdate(bConn, whereRec, colName, eachClasz, newIndexRec, leafClassColName, aIndexTable, aIndexColumn, aIndexRec, aWhereRec, aItsPopulate);
								return(true);
							} catch(Exception ex) {
								//excptInLambda.add(ex);
								//return(false);
								throw new Hinderance(ex, "Fail while object indexing: " + aClasz.getClass().getSimpleName() + ", field: " + aFqFieldName);
							}
						});
						/*
						// exception inside a lambda expression, we'll bombs out if its happen, couldn't handle this in lambda content
						if (excptInLambda.isEmpty() == false) {
							throw new Hinderance(excptInLambda.get(0), "Fail while object indexing: " + aClasz.getClass().getSimpleName() + ", field: " + aFqFieldName);
						}
						*/
					} else {
						for (Clasz<?> eachClasz : fob.getObjectMap().values()) { // update index of the populated object in the FieldObjectBox
							TraverseForInsertOrUpdate(aConn, whereRec, colName, eachClasz, newIndexRec, leafClassColName, aIndexTable, aIndexColumn, aIndexRec, aWhereRec, aItsPopulate);
						}
					} 
				} else {
					aIndexRec.getField(colName).setValue(eachField.getValueObj(aConn));
					if (IndexPopulated(aConn, aIndexRec)) {
						InsertOrUpdateIndex(aConn, aIndexTable, aIndexRec, aWhereRec);
					} 
				}
			}
		}
	}

	public static void InsertOrUpdateIndex(Connection aConn, Table aIndexTable, Record aIndexRec, Record aWhereRec) throws Exception {
		if (aIndexTable.update(aConn, aIndexRec, aWhereRec) == 0) {
			aIndexTable.insert(aConn, aIndexRec);
		}
	}

	/**
	 * "Object Index" is use as a index to access to a specific object instant 
	 * inside the database. The "Object Index" is to speed up search of the parent
	 * or child objects at any depth, this avoid traversing the child object and 
	 * determine if that is the wanted object.
	 * 
	 * To avoid complexity, the object index can index on only one child field 
	 * object at any one time, the data field on the child field object can be of
	 * any depth.
	 * 
	 * Structure of an "Object Index" is:-
	 * 
	 * Object Index
	 *	|
	 * 	- Object Id (object primary key)
	 * 	|
	 * 	--- Field 1 (parent object id)
	 * 	--- Field 2	(child field object id)
	 * 	--- Field 3	(child field object leaf class)
	 * 	--- Field 4 (child child field object id)
	 * 	--- Field 5 (child child field leaf class)
	 * 	--- Field 6 (targeted child child data field to index on)
	 * 
	 * For each data index field in the "Object Index", a physical index is 
	 * created. This enable access via a given value of the object, there's no 
	 * support for search using composite index, though this can be supported if
	 * needed.
	 * 
	 * @param aConn
	 * @param aClasz
	 * @return 
	 * @throws java.lang.Exception
	 */
	public static String GetObjectIndexName(Connection aConn, Clasz<?> aClasz) throws Exception {
		List<Field> aryFieldObjId= new CopyOnWriteArrayList<>();
		List<Field> aryFieldData = new CopyOnWriteArrayList<>();
		GetAllIndexKey(aryFieldObjId, aryFieldData, aClasz);
		String objIndexName = GetObjectIndexName(aConn, aClasz, aryFieldObjId);
		return(objIndexName);
	}

	public static String CreateObjectIndex(Connection aConn, Clasz<?> aClasz) throws Exception {
		List<Field> aryFieldObjId = new CopyOnWriteArrayList<>();
		List<Field> aryFieldData = new CopyOnWriteArrayList<>();
		GetAllIndexKey(aryFieldObjId, aryFieldData, aClasz);
		String objIndexName = GetObjectIndexName(aConn, aClasz, aryFieldObjId);
		CreateObjectIndexTable(aConn, objIndexName, aryFieldObjId, aryFieldData, aClasz);
		return(objIndexName);
	}

	private static void CreateObjectIndexTable(Connection aConn, String objIndexName, List<Field> aryFieldObjId, List<Field>aryFieldData, Clasz<?> aClasz) throws Exception {
		if (Database.TableExist(aConn, objIndexName) == false) {
			int pkSeq = 0;
			Table indexTable = new Table(objIndexName);
			String pkName = aClasz.getPkName();
			Field pkField = indexTable.createField(pkName, FieldType.LONG);
			pkField.setPrimaryKey(pkSeq++);

			for(Field eachField : aryFieldObjId) { // for all the object field for creating column to store the objects' id
				String colName = Database.Java2DbFieldName(eachField.getFqName());
				Field fieldObjPk = indexTable.createField(colName, FieldType.LONG);
				String leafClassColName = Clasz.CreateLeafClassColName(colName);	
				Field fieldObjLeaf = indexTable.createField(leafClassColName, FieldType.STRING, ObjectBase.CLASS_NAME_LEN);
				fieldObjPk.setPrimaryKey(pkSeq++);
				fieldObjLeaf.setPrimaryKey(pkSeq++);
			}

			List<Field> dataFieldForIndexing = new CopyOnWriteArrayList<>();
			for(Field eachField : aryFieldData) { // now create the data field for indexing
				String colName = Database.Java2DbFieldName(eachField.getFqName());
				if (eachField.getDbFieldType() == FieldType.STRING) {
					dataFieldForIndexing.add(indexTable.createField(colName, FieldType.STRING, eachField.getFieldSize()));
				} else {
					dataFieldForIndexing.add(indexTable.createField(colName, eachField.getDbFieldType()));
				}
			}

			Database.CreateTable(aConn, indexTable); // create the index table
			Database.CreatePrimaryKey(aConn, indexTable);

			// now create indexes for the data field
			for(Field eachField : dataFieldForIndexing) {
				eachField.setObjectKey(true);
				Database.createIndex(aConn, indexTable);
				eachField.setObjectKey(false);
			}
			App.logInfo(ObjectIndex.class, "Created object index table: " + objIndexName);

		} else {
			App.logWarn(ObjectIndex.class, "Object index: " + objIndexName + " already exist, ignoring call to create it"); 
		}
	}

	public static String GetObjectIndexName(Connection aConn, Clasz<?> aClasz, List<Field> colBox) throws Exception {
		String prefixName = Clasz.GetObjectIndexPrefix();
		String allFieldObjName = "";
		for(Field eachField : colBox) { // get all the marked field for indexing from the object
			if (allFieldObjName.isEmpty() == false) {
				allFieldObjName += "_";
			}
			allFieldObjName += eachField.getDbFieldName().toLowerCase();
		}
		String clsName = Database.Java2DbTableName(aClasz.getClass().getSimpleName());
		String idxName = prefixName + clsName;
		if (allFieldObjName.isEmpty() == false) {
			idxName += "_" + allFieldObjName;
		}
		return(idxName);
	}

	public static void GetAllIndexKey(List<Field> aryFieldObjId, List<Field> aryFieldData, Clasz<?> aClasz) throws Exception {
		GetAllIndexKey(aryFieldObjId, aryFieldData, aClasz, "");
	}

	private static void GetAllIndexKey(List<Field> aryFieldObjId, List<Field> aryFieldData, Clasz<?> aClasz, String aFqFieldName) throws Exception {
		for(Field eachField : aClasz.getTreeField().values()) { 
			String fqName = GetFqFieldName(aFqFieldName, eachField.getDbFieldName());
			if (eachField.isAtomic()) {
				if (eachField.isObjectKey()) {
					eachField.setFqName(fqName);
					aryFieldData.add(eachField);
				}
			} else {
				if (eachField.isObjectKey()) {
					if (eachField.getDbFieldType() == FieldType.OBJECT) {
						Clasz<?> clasz = ((FieldObject<?>) eachField).getObj();
						if (clasz != null) {
							eachField.setFqName(fqName);
							aryFieldObjId.add(eachField);
							GetAllIndexKey(aryFieldObjId, aryFieldData, clasz, fqName); // recursive call to clear the member object index keys
							break; // once get a field object for indexing, never look for others, only support one fieldobject at any one time
						}
					} else if (eachField.getDbFieldType() == FieldType.OBJECTBOX) {
						eachField.setFqName(fqName);
						aryFieldObjId.add(eachField);
						Clasz<?> clasz = ((FieldObjectBox<?>) eachField).getMetaObj();
						GetAllIndexKey(aryFieldObjId, aryFieldData, clasz, fqName); // recursive call to clear the object index keys
						break; // once get a field object for indexing, never look for others, only support one fieldobject at any one time
					} else {
						throw new Hinderance("Invalid field type in object while getting all its index key name: " + aClasz.getClaszName().toUpperCase());
					}
				}
			}
		}
	}

	public static String GetFqFieldName(String aNowStr, String aNewStr) {
		if (aNowStr.isEmpty() == false) {
			aNowStr += "$";
		}
		return(aNowStr + aNewStr);
	}

	public static void GetKeyValueForIndex(Connection aConn, String[] aFullFieldName, int aFieldAt, Field finalField, Map<String, String> colMap, String eachColName) throws Exception {
		if (aFieldAt < aFullFieldName.length) {
			String eachFieldName = aFullFieldName[aFieldAt];
			if (finalField.getDbFieldType() == FieldType.OBJECT) {
				Clasz<?> clasz = ((FieldObject<?>) finalField).getValueObj(aConn);
				if (clasz != null) {
					if (clasz.fieldExist(eachFieldName)) {
						finalField = clasz.getField(eachFieldName);
					} else {
						finalField = null;
					}
					GetKeyValueForIndex(aConn, aFullFieldName, aFieldAt + 1, finalField, colMap, eachColName);
				}
			} else if (finalField.getDbFieldType() == FieldType.OBJECTBOX) {
				FieldObjectBox<?> fobField = (FieldObjectBox<?>) finalField;
				for (Clasz<?> eachClasz : fobField.getObjectMap().values()) { // now update/insert all member variable
					if (eachClasz.fieldExist(eachFieldName)) {
						finalField = eachClasz.getField(eachFieldName);
						GetKeyValueForIndex(aConn, aFullFieldName, aFieldAt + 1, finalField, colMap, eachColName);
					} 
				}
			} else {
				throw new Hinderance("Object index should only index on FieldObject or FieldObjectBox type of field!");
			}
		} else { // already traverse to the end of the leaf field
			if (finalField != null) {
				if (finalField.getDbFieldType() == FieldType.OBJECT) {
					if (finalField.getValueObj(aConn) != null) {
						colMap.put(eachColName, finalField.getValueStr()); // with the final field, place inside the map for later sql/jdbc update
					} else {
						colMap.put(eachColName, ""); // with the final field, place inside the map for later sql/jdbc update
					}
				} else if (finalField.getDbFieldType() == FieldType.OBJECTBOX) {
					throw new Hinderance("Cannot index on FieldObjectBox type at the leaf field!");
				}
			} else {
				colMap.put(eachColName, "");
			}
		}
	}

	/*
	public static void updateIndex(Connection aConn, String aIndexName, Map<String, String> aCol2Update, String aPkName) throws Exception {
		Record whereRecord = new Record();
		whereRecord.createFieldObject(aPkName, (String) aCol2Update.get(aPkName));

		Record dataRecord = new Record();
		for(String eachColName : aCol2Update.keySet()) {
			dataRecord.createFieldObject(eachColName, (String) aCol2Update.get(eachColName));
		}

		Table indexTable = new Table(aIndexName);
		if (indexTable.update(aConn, dataRecord, whereRecord) == 0) {
			//dataRecord.CreateField(aPkName, (String) aCol2Update.get(aPkName));
			indexTable.insert(aConn, dataRecord);
		}
	}
	*/

	public static String CreateObjectOrFieldIndex(Connection aConn, Clasz<?> aClasz, List<Field> colBox, String newName) throws Exception {
		if (Database.TableExist(aConn, newName) == false) {
			Table indexTable = new Table(newName);
			List<Field> phyIdxs = new CopyOnWriteArrayList<>();
			for(Field eachField : colBox) { // get all the marked field for indexing from the object
				String colName = Database.Java2DbFieldName(eachField.getFqName());
				if (eachField.getDbFieldType() == FieldType.STRING) {
					phyIdxs.add(indexTable.createField(colName, FieldType.STRING, eachField.getFieldSize()));
				} else {
					phyIdxs.add(indexTable.createField(colName, FieldType.STRING, String.valueOf(Long.MAX_VALUE).length()));
				}
			}
			String pkName = aClasz.getPkName();
			Field pkField = indexTable.createField(pkName, FieldType.LONG);
//indexTable.getField(pkName).setPrimaryKey(); // KIV, may not be needed to create PK
			Database.CreateTable(aConn, indexTable); // create the index table
			pkField.setObjectKey(true);
			Database.createIndex(aConn, indexTable);
			pkField.setObjectKey(false);
//Database.CreatePrimaryKey(aConn, indexTable); // don't need to be PK, if everything is ok, for now let it be pk
			for(Field eachField : phyIdxs) {
				eachField.setObjectKey(true);
				Database.createIndex(aConn, indexTable);
				eachField.setObjectKey(false);
			}
			App.logInfo(ObjectIndex.class, "Created object index table: " + newName.toUpperCase());
		}
		return(newName);
	}

	public static String createObjectIndex(Connection aConn, Clasz<?> aClasz) throws Exception {
		List<Field> colBox = aClasz.getAllIndexKey();
		String newName = getObjectIndexName(aConn, aClasz, colBox);
		String result = CreateObjectOrFieldIndex(aConn, aClasz, colBox, newName);
		return(result);
	}

	public static String createFieldIndex(Connection aConn, Clasz<?> aMasterClasz, String aFieldName) throws Exception {
		Clasz<?> childClasz = aMasterClasz.getNonNullObject(aConn, aFieldName);
		List<Field> colBox = childClasz.getAllIndexKey();
		String newName = GetFieldIndexName(aConn, aMasterClasz, aFieldName, colBox);
		String result = CreateObjectOrFieldIndex(aConn, childClasz, colBox, newName);
		return(result);
	}

	public static List<String> GetIndexColumn(Connection aConn, String aIndexName) throws Exception {
		List<String> result = new CopyOnWriteArrayList<>();
		DatabaseMetaData metadata = aConn.getMetaData();
		ResultSet resultSet = metadata.getColumns(null, null, aIndexName, null);
		while (resultSet.next()) {
			String colName = resultSet.getString("COLUMN_NAME").toLowerCase();
			result.add(colName);
		}
		return(result);
	}

	@Deprecated
	public static List<String> GetIndexColumn(Connection aConn, String aIndexName, Long aObjId, String aPkName) throws Exception {
		List<String> result = new CopyOnWriteArrayList<>();
		PreparedStatement stmt = null;
		ResultSet rset = null;
		ResultSetMetaData rsmdSet;
		try {
			String sqlStr = "select * from " + aIndexName + " where " + aPkName + "= ?";
			stmt = aConn.prepareStatement(sqlStr);
			stmt.setLong(1, aObjId);
			rset = stmt.executeQuery();
			rsmdSet = rset.getMetaData();
			int colCount = rsmdSet.getColumnCount();

			for(int cntrCol = 1; cntrCol <= colCount; cntrCol++) {
				String colName = rsmdSet.getColumnName(cntrCol).toLowerCase();
				result.add(colName);
			}
		} finally {
			if (stmt != null) {
				stmt.close();
			}
			if (rset != null) {
				rset.close();
			}
		}
		return(result);
	}

	public static boolean IsObjectIndexForClasz(Connection aConn, String aIndexName, Clasz<?> aClasz) throws Exception {
		boolean result = false;
		DatabaseMetaData mdb = aConn.getMetaData();
		String[] onlyTable = { "TABLE" };
		int longestMatch = 0;
		String matchTableName = "";
		String tableName = Clasz.CreateTableName(Class.forName(aClasz.getClass().getName()));
		ResultSet rs = mdb.getTables(null, null, tableName + "%", onlyTable);
		while (rs.next()) {
			String eachTableName = rs.getString(3);
			String indexBaseName = eachTableName.replaceFirst(Clasz.TABLE_NAME_PREFIX, Clasz.GetObjectIndexPrefix());
			if (aIndexName.startsWith(indexBaseName)) { // if the index name have several clasz table name prefix, use the longest matching one
				if (longestMatch < indexBaseName.length()) {
					longestMatch = indexBaseName.length();
					matchTableName = eachTableName;
				}
			}
		}

		if (tableName.equals(matchTableName)) {
			result = true;
		}
		return(result);
	}

	public static List<String> GetAllIndexName(Connection aConn, Clasz<?> aClasz) throws Exception {
		List<String> result = new CopyOnWriteArrayList<>();
		DatabaseMetaData mdb = aConn.getMetaData();
		ResultSet rs;
		String[] onlyTable = { "TABLE" };
		rs = mdb.getTables(null, null, CreateIndexName(aClasz) + "%", onlyTable);
		while (rs.next()) {
			String idxName = rs.getString(3);
			if (IsObjectIndexForClasz(aConn, idxName, aClasz)) {
				result.add(idxName);
			}
		}
		return(result);
	}

	public static String CreateIndexName(Clasz<?> aClasz) {
		String result = Clasz.GetObjectIndexPrefix() + Database.Java2DbTableName(aClasz.getClass().getSimpleName());
		return(result);
	}

	public static String getObjectIndexName(Connection aConn, Clasz<?> aClasz, List<Field> colBox) throws Exception {
		String prefixName = Clasz.GetObjectIndexPrefix();
		String allColName = "";
		for(Field eachField : colBox) { // get all the marked field for indexing from the object
			if (allColName.isEmpty() == false) {
				allColName += "0";
			}
			allColName += eachField.getFqName();
		}
		String clsName = Database.Java2DbTableName(aClasz.getClass().getSimpleName());
		String idxName = prefixName + clsName + "_" + allColName;
		return(idxName);
	}

	public static String GetFieldIndexName(Connection aConn, Clasz<?> aMasterClasz, String aFieldName, List<Field> colBox) throws Exception {
		String prefixName = Clasz.GetFieldIndexPrefix();
		String allColName = "";
		for(Field eachField : colBox) { // get all the marked field for indexing from the object
			if (allColName.isEmpty() == false) {
				allColName += "0";
			}
			allColName += eachField.getFqName();
		}
		String clsName = Database.Java2DbTableName(aMasterClasz.getClass().getSimpleName());
		String idxName = prefixName + clsName + "_" + aFieldName + "_"+ allColName;
		return(idxName);
	}

	public static boolean IsNumeric(String str)  {  
		try  {  
			Double.parseDouble(str);  
		}  
		catch(NumberFormatException nfe)  {  
			return false;  
		}  
		return true;  
	}

	public static List<Field> GetIndexedField(Connection aConn, Clasz<?> aClasz) throws Exception {
		List<Field> aryFieldObjId= new CopyOnWriteArrayList<>();
		List<Field> aryFieldData = new CopyOnWriteArrayList<>();
		GetAllIndexKey(aryFieldObjId, aryFieldData, aClasz);
		return(aryFieldData);
	}

	/*
	public static void PopulateObjectIndex(Connection aConn, Class aClass, String aIndexName) throws Exception {
		String sqlToGetObjId = "select " + Clasz.CreatePkName(aClass) + " from " + Clasz.CreateTableName(aClass);
		Clasz.ForEachClasz(aConn, aClass, sqlToGetObjId, ((Connection conn, Clasz clasz) -> {
			UpdateIndex(conn, clasz, aIndexName, true);
			return(true);
		}));
	}
	*/

	public static void GetCriteriaByTable(Connection aConn, Clasz<?> aCriteria, Map<String, Record> aWhereCriteria, String aFqFieldName) throws Exception {
		GetCriteriaByTable(aConn, aCriteria, aWhereCriteria, null, aFqFieldName);
	}

	public static void GetCriteriaByTable(Connection aConn, Clasz<?> aCriteria, Map<String, Record> aWhereCriteria, Record aWhereRec, String aFqFieldName) throws Exception {
		for (Field eachField : aCriteria.getInstantRecord().getFieldBox().values()) { // for each populated field, build up the sql where clause
			String fqName = GetFqFieldName(aFqFieldName, eachField.getDbFieldName());
			if (eachField.isModified()) {
				if (eachField.getDbFieldType() == FieldType.OBJECT) { // if field is of object type, traverse it
					Clasz<?> fieldObject = ((FieldObject<?>) eachField).getObj(); // convert table type to Clasz type
					if (fieldObject.getClaszName().equals("Clasz") == false) {
						GetCriteriaByTable(aConn, fieldObject, aWhereCriteria, null, fqName);
					}
				} else if (eachField.getDbFieldType() == FieldType.OBJECTBOX) {
					for(Clasz<?> memberObject : ((FieldObjectBox<?>) eachField).getObjectMap().values()) {
						GetCriteriaByTable(aConn, memberObject, aWhereCriteria, null, fqName);
					}
				} else {
					Record whereRec;
					if (aWhereRec == null) {
						whereRec = new Record();
					} else {
						whereRec = aWhereRec; // FieldObjectBox, use back the same whereRec
					}
					eachField.setDbFieldName(fqName);
					whereRec.createField(aConn, eachField);
					whereRec.copyValue(aConn, eachField);
					aWhereCriteria.put(aCriteria.getTableName(), whereRec); // the objective is here, accumulate every where field for each clasz/table
				}
			}
		}

		if (ObjectBase.ParentIsNotAtClaszYet(aCriteria)) {
			Clasz<?> parentObject = aCriteria.getParentObjectByContext();
			if (parentObject != null && parentObject.getClass().equals(Clasz.class) == false) { // no parent clszObject because its an abstract class and no inherited normal class
				if (aCriteria.isPopulated() == false) {
					GetCriteriaByTable(aConn, aCriteria, aWhereCriteria, null, aFqFieldName);
				}
			}
		}
	}

	public static String GetIndexWhereCriteria(Connection aConn, String aObjIdxName, String aFqFieldName, Clasz<?> aCriteria, List<Field> aIndexField) throws Exception {
		String strWhere = new String();
		Map<String, Record> whereCriteria = new ConcurrentHashMap<>(); // each table name (string) and a record for the where fields (record)
		GetCriteriaByTable(aConn, aCriteria, whereCriteria, aFqFieldName); // the select criteria for this leaf clszObject, doesn't do the parent clszObject

		// get list of indexed fields
		int cntrTable = 0;
		for(String eachTableName: whereCriteria.keySet()) {
			cntrTable++;
			Record eachRec = whereCriteria.get(eachTableName);
			if (cntrTable > 1) {
				if (eachRec.getFields().size() == 1) {
						strWhere +=  " and ";
				}
			}

			int cntrField = 0;
			for(Field eachField: eachRec.getFields()) {
				cntrField++;
				if (eachRec.getFields().size() > 1) {
					if (cntrField == 1) {
						if (eachField.getFormulaStr().isEmpty()) {
							strWhere += aObjIdxName + "." + eachField.getDbFieldName() + " in (?";
						} else {
							strWhere += eachField.getFormulaStr() + " in (?";
						}
					} else {
						strWhere += ", ?";
					}
				} else {
					if (eachField.getFormulaStr().isEmpty()) {
						strWhere += aObjIdxName + "." + eachField.getDbFieldName() + " = ?";
					} else {
						strWhere += eachField.getFormulaStr();
					}
				}

				aIndexField.add(eachField);
			}
			if (eachRec.getFields().size() > 1) {
				strWhere +=  ")";
			}
		}

		return(strWhere);
	}

	public static String ObjectIndexOnFob(Connection aConn, Clasz<?> aClaszMaster, String aFobName, String aIndexFieldName) throws Exception {
		Clasz<?> claszMaster = ObjectBase.CreateObject(aConn, aClaszMaster.getClass());
		claszMaster.clearAllIndexKey();
		claszMaster.getFieldObjectBox(aFobName).setObjectKey(true);
		claszMaster.getFieldObjectBox(aFobName).getMetaObj().getField(aIndexFieldName).setObjectKey(true);

		String indexName = ObjectIndex.GetObjectIndexName(aConn, claszMaster);
		if (Database.TableExist(aConn, indexName) == false) {
			App.logInfo(aClaszMaster.getClass(), "Creating object index for: " + aClaszMaster.getClass().getName() + ", on payslip employee's name");
			ObjectIndex.CreateObjectIndex(aConn, claszMaster); // create field index by name
			ObjectIndex.ObjectIndexPopulate(aClaszMaster.getClass(), aConn, indexName);
		}
		return(indexName);
	}

	public static String CreateIndexNameOnFob(Connection aConn, Clasz<?> claszMaster, String aFobName, List<String> aFieldList) throws Exception {
		claszMaster.clearAllIndexKey();
		for(String eachFieldName : aFieldList) {
			claszMaster.getFieldObjectBox(aFobName).setObjectKey(true);
			claszMaster.getFieldObjectBox(aFobName).getMetaObj().getField(eachFieldName).setObjectKey(true);
		}

		String indexName = ObjectIndex.GetObjectIndexName(aConn, claszMaster);
		return indexName;
	}

	public static String ObjectIndexOnFob(Connection aConn, Clasz<?> aClaszMaster, String aFobName, List<String> aFieldList) throws Exception {
		//String indexName = ObjectIndex.GetObjectIndexName(aConn, claszMaster);
		Clasz<?> claszMaster = ObjectBase.CreateObject(aConn, aClaszMaster.getClass());
		String indexName = CreateIndexNameOnFob(aConn, claszMaster, aFobName, aFieldList);
		if (Database.TableExist(aConn, indexName) == false) {
			App.logInfo(aClaszMaster.getClass(), "Creating object index with composite fields for: " + aClaszMaster.getClass().getSimpleName());
			ObjectIndex.CreateObjectIndex(aConn, claszMaster); // create field index by name
			ObjectIndex.ObjectIndexPopulate(aClaszMaster.getClass(), aConn, indexName);
		}
		return(indexName);
	}

	public static void ObjectIndexPopulate(Class<?> aMasterClass, Connection aConn, String aIndexName) throws Exception {
			App.logInfo(aMasterClass, "Populating object index: " + aIndexName);
			String sqlGetMaster = "select " + Clasz.CreatePkColName(aMasterClass) + " from " + Clasz.CreateTableName(aMasterClass);
			Clasz.ForEachClaszFreeType(aConn, aMasterClass, sqlGetMaster, (Connection bConn, Object aEachClasz) -> {
				ObjectIndex.UpdateIndex(bConn, (Clasz<?>) aEachClasz, aIndexName, true);
				return(true);
			});
			App.logInfo(aMasterClass, "Completed populating object index: " + aIndexName);
	}

	public static void AddDataColumn2Index(Connection aConn, String aObjectIndexName, Clasz<?> aClasz2Index) throws Exception {
		if (Database.TableExist(aConn, aObjectIndexName)) {
			Table objectIndexTable = new Table(aObjectIndexName);

			// traverse aClasz and get the key for indexsing
			List<Field> aryFieldObjId = new CopyOnWriteArrayList<>();
			List<Field> aryFieldData = new CopyOnWriteArrayList<>();
			ObjectIndex.GetAllIndexKey(aryFieldObjId, aryFieldData, aClasz2Index);

			// create the missing field
			for (Field eachField : aryFieldData) {
				// now create the data field for indexing
				String colName = Database.Java2DbFieldName(eachField.getFqName());
				if (eachField.getDbFieldType() == FieldType.STRING) {
					objectIndexTable.createField(colName, FieldType.STRING, eachField.getFieldSize());
				} else {
					objectIndexTable.createField(colName, eachField.getDbFieldType());
				}
			}
			// add missing column into db
			Database.AddColumn(aConn, objectIndexTable);
		} else {
			App.logWarn(ObjectIndex.class, "Object index: " + aObjectIndexName + " already exist, will not add index column to it");
		}
	}
}

