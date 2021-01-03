package biz.shujutech.db.relational;


import biz.shujutech.base.App;
import biz.shujutech.base.Base;
import biz.shujutech.base.Connection;
import biz.shujutech.base.ConnectionPool;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import biz.shujutech.db.object.FieldObjectBox;
import biz.shujutech.base.Hinderance;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import biz.shujutech.technical.Callback2ProcessRecord;

public class Table extends Base {
	public static final String POSTFIX_FIELD_CHILD_COUNT = "_cc";

	public static final String PREFIX_INHERITANCE = "ih_";
	public static final String PREFIX_MEMBER_OF = "iv_"; // instance variable
	public static final String PREFIX_MEMBER_BOX_OF = "iw_"; // member box instance variable
	public static final String PREFIX_POLYMORPHIC = "ip_"; // polymorphic table
	public static final String PREFIX_OBJECT_INDEX = "oi_"; 
	public static final String PREFIX_FIELD_INDEX = "fi_"; 
	public static final String PREFIX_NON_ORM = "zo_"; // so that it goes to the bottom


	private ConcurrentHashMap<Long, Record> recordBox = new ConcurrentHashMap<>();
	public String tableName;
	public String uniqueIndexName;

	public String pkColName;
	public long fetchLastPk;
	public long fetchPageSize = Long.MAX_VALUE;
	private Record metaRec = null;
	private Database db;


	public Table() {
		if (this.metaRec == null) {
			this.metaRec = new Record();
			//this.getRecordBox().put(new Long(0), metaRec); // first entry it the record array is the meta record
		}
	}

	public Table(Database aDb, String aName) {
		this(aName);
		this.db = aDb;
	}

	public Table(String aName) {
		this();
		this.setTableName(aName);
	}

	public Database getDb() {
		return db;
	}

	public void setDb(Database db) {
		this.db = db;
	}

	public String getUniqueIndexName() {
		return uniqueIndexName;
	}

	public final void setUniqueIndexName(String uniqueIndexName) {
		this.uniqueIndexName = uniqueIndexName;
	}

	public String getIndexName() {
		String result = this.getTableName();
		for (Field eachField : this.getMetaRec().getFieldBox().values()) {
			if (eachField.isObjectKey()) {
				result += "_" + Database.Java2DbFieldName(eachField.getFieldName());
			}
		}
		if (result.length() > 64) {
			result = result.substring(0, 64);
		}
		return(result);
	}

	public static String GetIndexName(String aTableName, List<Field> aIndexField) {
		String indexName = aTableName;
		for (Field eachField : aIndexField) {
			if (eachField.isIndexKey()) {
				indexName += "_" + Database.Java2DbFieldName(eachField.getFieldName());
			}
		}
		if (indexName.length() > 64) {
			indexName = indexName.substring(0, 64);
		}

		if (indexName.equals(aTableName)) indexName = "";
		return(indexName);
	}

	public Record getMetaRec() {
		return metaRec;
	}

	public Record getRecord(long aIndex) {
		return(this.getRecordBox().get(new Long(aIndex)));
	}

	public Field createField(String aName, FieldType aType) throws Exception {
		return(this.metaRec.createField(aName, aType));
	}

	public Field createField(String aName, FieldType aType, int aSize) throws Exception {
		return(this.metaRec.createField(aName, aType, aSize));
	}

	public final ConcurrentHashMap<Long, Record> getRecordBox() {
		return recordBox;
	}

	public String getTableName() {
		return tableName;
	}

	public final void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public boolean gotPrimaryKey() {
		String result = this.metaRec.getPkFieldName();
		if (result.isEmpty()) {
			return false;
		} else {
			return true;
		}
	}

	public String getPkName() throws Exception {
		String result = this.metaRec.getPkFieldName();
		if (result.isEmpty()) {
			throw new Hinderance("Fail to get the primary key name for table: " + this.getTableName().toUpperCase());
		}
		return(result);
	}

	public Field getField(String aName) throws Exception {
		Field result = this.metaRec.getField(aName);
		return(result);
	}
	
	public static String GetPkName(Connection aConn, String aTable) throws Exception {
		String result = "";
		ResultSet rsMeta = null;
		DatabaseMetaData dbMeta;
		Connection connMeta = aConn;
		try {
			dbMeta = connMeta.getMetaData();
			rsMeta = dbMeta.getPrimaryKeys(null, null, aTable.toLowerCase());
			while(rsMeta.next()) {
				result += rsMeta.getString("COLUMN_NAME");
			}
		} finally 		{
			if (rsMeta != null) rsMeta .close();
		}
		return(result.toLowerCase());
	}

	public Record createRecord(Connection aConn) throws Exception {
		Record result;
		if (this.metaRec.totalField() == 0) {
			this.initMeta(aConn);
			result = this.metaRec;
		} else {
			result = new Record();
			result.createField(this.metaRec); // create the structure of the meta rec to indexName
		}
		
		return(result);
	}

	public void initMeta(Database aDb) throws Exception {
		Connection conn = null;
		try {
			conn = aDb.getConnPool().getConnection();
			this.initMeta(conn);
		} finally {
			if (conn != null) {
				aDb.getConnPool().freeConnection(conn);
			}
		}

	}

	public void initMeta(Connection aConn) throws Exception {
		this.pkColName = GetPkName(aConn, this.tableName);
		PreparedStatement stmtMeta = null;
		ResultSet rsMeta = null;
		ResultSetMetaData rsmdSet;
		//Connection connMeta = aPool.ge
		Connection connMeta = aConn;
		try {
			String sqlMeta = "select * from " + this.tableName + " where 1 = 0";
			stmtMeta = connMeta.prepareStatement(sqlMeta);
			rsMeta = stmtMeta.executeQuery();
			rsmdSet = rsMeta.getMetaData();
			int colCount = rsmdSet.getColumnCount();
			for(int cntrCol = 1; cntrCol <= colCount; cntrCol++) {
				String colName = rsmdSet.getColumnName(cntrCol).toLowerCase();
				int colType = rsmdSet.getColumnType(cntrCol);
				Field createdField = this.createField(colName, Database.JavaSqlType2FieldType(colType));
				if (this.pkColName.equals(colName.toLowerCase())) createdField.setPrimaryKey();
			}
		} finally {
			if (stmtMeta != null) stmtMeta.close();
			if (rsMeta != null) rsMeta .close();
		}
	}

	public boolean fieldExist(String aName) {
		boolean result = false;
		if (this.metaRec.fieldExist(aName)) result = true;
		return(result);
	}

	/**
	 * The similar method to update but instead of using connection pool, it takes
	 * the connection argument. This call is use when transaction commit is needed
	 * base as several sql command is call using the same connection that is then 
	 * committed as a single transaction operation
	 * 
	 * @param aConn
	 * @param aRecordAt
	 * @throws Exception 
	 */
	public int update(Record aRecUpdField, Record aRecWhere) throws Exception {
		return(this.update(this.getDb(), aRecUpdField, aRecWhere));
	}

	public int update(Database aDb, Record aRecUpdField, Record aRecWhere) throws Exception {
		Connection conn = null;
		try {
			conn = aDb.getConnPool().getConnection();
			return(update(conn, this.tableName, aRecUpdField, aRecWhere));
		} finally {
			if (conn != null) aDb.getConnPool().freeConnection(conn);
		}
	}

	public int update(Connection aConn, Record aRecUpdField, Record aRecWhere) throws Exception {
		return(update(aConn, this.tableName, aRecUpdField, aRecWhere));
	}
	
	/**
	 * The static version to update the given record of the given table name. It
	 * assemble the sql update statement from all the fields in the record and 
	 * update the record using the primary key for this table.
	 * 
	 * @param aConn
	 * @param aThreadId
	 * @param aTableName
	 * @param aRecUpdField
	 * @param aRecWhere
	 * @return
	 * @throws Exception 
	 */
	public static int update(Connection aConn, String aTableName, Record aRecUpdField, Record aRecWhere) throws Exception {
		int result = 0;
		PreparedStatement stmtUpd = null;
		ResultSet rsUpd = null;
		try {
			StringBuffer strUpdate = new StringBuffer();
			List<Field> fieldUpdate = Database.GetFieldToUpdate(aConn, aTableName, aRecUpdField, strUpdate);
			String sqlFieldToUpdate = strUpdate.toString();

			StringBuffer strBuffer = new StringBuffer();
			List<Field> fieldWhere = Database.GetWhereClause(aTableName, aRecWhere, strBuffer);
			String sqlWhere = strBuffer.toString();

			// place in the fields values and set the sql string
			List<Field> fieldArr = new CopyOnWriteArrayList<>();  // combine the field to update and the where clause
			fieldArr.addAll(fieldUpdate);
			fieldArr.addAll(fieldWhere);

			String sqlUpd = "update " + aTableName + " set " + sqlFieldToUpdate + " where " + sqlWhere;
			stmtUpd = aConn.prepareStatement(sqlUpd);
			Database.SetStmtValue(stmtUpd, fieldArr);
			//App.logDebg(Table.class, "dml_update: " + stmtUpd.toString());

			result = stmtUpd.executeUpdate();
		} catch(Exception ex) {
			if (stmtUpd != null) {
				throw new Hinderance(ex, "Update error: " + stmtUpd.toString());
			} else {
				throw new Hinderance(ex, "Fail to update: " + aTableName);
			}
		} finally {
			if (stmtUpd != null) {
				stmtUpd.close();
			}
			if (rsUpd != null) {
				rsUpd .close();
			}
		}
		return(result);
	}
	
	/**
	 * Delete a record, this delete compare all the fields in the record and 
	 * delele it. 
	 * 
	 * @param aWhere
	 * @return 
	 * @throws Exception 
	 */
	public int delete(Record aWhere) throws Exception {
		Connection conn = null;
		try {
			conn = this.getDb().getConnPool().getConnection();
			return(delete(conn, aWhere));
		} finally {
			if (conn != null) this.getDb().getConnPool().freeConnection(conn);
		}
	}

	public int delete(Connection aConn, long aRecordAt) throws Exception {
		int result = delete(aConn, this.tableName, this.recordBox.get(aRecordAt));
		return(result);
	}

	public int delete(Connection aConn, Record aWhere) throws Exception {
		int result = delete(aConn, this.tableName, aWhere);
		return(result);
	}

	public static int delete(Connection aConn, String aTableName, Record aWhere) throws Exception {
		int result;
		PreparedStatement stmtRemove = null;
		Connection connRemove = null;
		try {
			StringBuffer strBuffer = new StringBuffer();
			List<Field> fieldArr = Database.GetWhereClause(aTableName, aWhere, strBuffer);
			String sqlWhere = strBuffer.toString();

			String strRemove = "delete from " + aTableName + " where " + sqlWhere;
			connRemove = aConn;
			stmtRemove = connRemove.prepareStatement(strRemove);
			Database.SetStmtValue(stmtRemove, fieldArr);
			App.logWarn(Table.class, "Deleting object: " + stmtRemove.toString()); // for any deletion, we log them
			result = stmtRemove.executeUpdate(); // if got child object, this delete will not happen, silently ignore
		} catch(Exception ex) {
			throw new Hinderance(ex, "Fail to delete: " + stmtRemove.toString());
		} finally {
			if (stmtRemove != null) stmtRemove.close();
		}
		return(result);
	}


	@Deprecated
	public void insert(Record aRecord) throws Exception {
		this.insert(this.getDb(), aRecord);
	}

	@Deprecated
	public void insert(Database aDb, Record aRecord) throws Exception {
		Connection conn = null;
		try {
			conn = aDb.getConnPool().getConnection();
			this.insert(conn, aRecord);
		} finally {
			if (conn != null) aDb.getConnPool().freeConnection(conn);
		}
	}

	@Deprecated
	public void insert(ConnectionPool aPool, long aRecordAt) throws Exception {
		Connection conn = null;
		try {
			conn = aPool.getConnection();
			this.insert(conn, aRecordAt);
		} finally {
			if (conn != null) aPool.freeConnection(conn);
		}
	}

	public void insert(Connection aConn, long aRecordAt) throws Exception {
		Insert(aConn, this.tableName, this.recordBox.get(aRecordAt));
	}

	public void insert(Connection aConn, Record aRecord) throws Exception {
		Insert(aConn, this.tableName, aRecord);
	}

	public static int Insert(Connection aConn, String aTableName, Record aRecord) throws Exception {
		int result = 0;
		PreparedStatement stmtIns = null;
		try {
			StringBuffer strField = new StringBuffer();
			StringBuffer strHolder = new StringBuffer();
			List<Field> fieldInsert = Database.GetFieldToInsert(aTableName, aRecord, strField, strHolder);
			String sqlField = strField.toString();
			String sqlHolder = strHolder.toString();

			String sqlIns = "insert into " + aTableName + "(" + sqlField + ") values (" + sqlHolder + ")";
			stmtIns = aConn.prepareStatement(sqlIns);
			Database.SetStmtValue(stmtIns, fieldInsert);
			//App.logDebg(Table.class, "dml_insert: " + stmtIns.toString());

			result = stmtIns.executeUpdate();
		} catch(Exception ex) {
			if (stmtIns != null) {
				throw new Hinderance(ex, "Fail to insert: " + stmtIns.toString());
			} else {
				throw new Hinderance(ex, "Fail to insert, table: " + aTableName);
			}
		} finally {
			if (stmtIns != null) {
				stmtIns.close();
			}
		}
		return(result);
	}

	public long fetch(Connection aConn, Record aWhere) throws Exception {
		if (this.getMetaRec().getFieldBox().isEmpty()) {
			throw new Hinderance("Table: " + this.getTableName() + ", fields is not defined, fetch fail");
		}

		Long result = new Long(this.getRecordBox().size());
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			rs = Fetch(aConn, stmt, this.getTableName(), this.getMetaRec(), aWhere);
			while(rs.next()) {
				Record recGot = new Record(this.getMetaRec());
				recGot.populateField(rs);
				this.getRecordBox().put(result, recGot);
				result++;
			}
		} finally {
			if (rs != null) {
				rs.close();
			}
			if (stmt != null) {
				stmt.close();
			}
		}
		return(result);
	}

	public long fetch(Connection aConn, String aSqlStr) throws Exception {
		if (this.getMetaRec().getFieldBox().isEmpty()) {
			throw new Hinderance("Table: " + this.getTableName() + ", fields is not defined, fetch fail");
		}

		Long result = new Long(this.getRecordBox().size());
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = aConn.prepareStatement(aSqlStr);
			rs = stmt.executeQuery();
			while(rs.next()) {
				Record recGot = new Record(this.getMetaRec());
				recGot.populateField(rs);
				this.getRecordBox().put(result, recGot);
				result++;
			}
		} finally {
			if (rs != null) {
				rs.close();
			}
			if (stmt != null) {
				stmt.close();
			}
		}
		return(result);
	}



	/**
	 * Fetch this table record into recSelect, using the aWhere record as
	 * the condition (the where clause) to fetch this record.
	 * 
	 * @param aConn
	 * @param aSelect
	 * @param aWhere
	 * @throws Exception 
	 */
	public int fetch(Connection aConn, Record aSelect, Record aWhere) throws Exception {
		int result = 0;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			rs = Fetch(aConn, stmt, this.getTableName(), aSelect, aWhere);
			if (rs.next()) {
				aSelect.populateField(rs);
				result++;
			}
			
		} finally {
			if (rs != null) {
				rs.close();
			}
			if (stmt != null) {
				stmt.close();
			}
		}
		return(result);
	}

	public static ResultSet Fetch(Connection aConn, PreparedStatement stmt, String aSqlToGetObjId, List<Field> aFieldValue) throws Exception {
		ResultSet rs;
		try {
			stmt = aConn.prepareStatement(aSqlToGetObjId);
			Database.SetStmtValue(stmt, aFieldValue);
			rs = stmt.executeQuery();
			return(rs);
		} catch(Exception ex) {
			throw new Hinderance(ex, "Fetch sql fail: " + aSqlToGetObjId);
		}		
	}

	public ResultSet fetch(Connection aConn, PreparedStatement stmt, Record aSelect, Record aWhere) throws Exception {
		return(Table.Fetch(aConn, stmt, this.getTableName(), aSelect, aWhere));
	}

	public static ResultSet Fetch(Connection aConn, PreparedStatement stmt, String aTableName, Record aSelect, Record aWhereRec) throws Exception {
		return Fetch(aConn, stmt, aTableName, aSelect, aWhereRec, null);
	}

	public static ResultSet Fetch(Connection aConn, PreparedStatement stmt, String aTableName, Record aSelect, Record aWhereRec, String aWhereStr) throws Exception {
		List<Object> objForSql = GetFetchObject(aTableName, aSelect, aWhereRec, aWhereStr);
		String sqlStr = (String) objForSql.get(0);
		List<Field> fieldArr = (List<Field>) objForSql.get(1);
		stmt = aConn.prepareStatement(sqlStr);
		Database.SetStmtValue(stmt, fieldArr);
		ResultSet rs = stmt.executeQuery();
		return(rs);
	}

	public static List<Object> GetFetchObject(String aTableName, Record aSelect, Record aWhereRec, String aWhereStr) throws Exception {
		List<Object> result = new CopyOnWriteArrayList<>();
		String sqlStr = "";
		try {
			int cntrSelect = 0;
			String sqlSelect = "";
			if (aSelect == null || aSelect.totalField() == 0) {
				sqlSelect = "*";
			} else {
				for (Field eachField : aSelect.getFieldBox().values()) {
					if (cntrSelect != 0) {
						sqlSelect += ", ";
					}
					if (eachField.getFieldType() == FieldType.ENCRYPT) {
						sqlSelect += Database.DdlForDecrypt(eachField.getFieldName());
					} else {
						sqlSelect += eachField.getFieldName();
					}
					cntrSelect++;
				}
			}

			StringBuffer strBuffer = new StringBuffer();
			List<Field> fieldArr = Database.GetWhereClause(aTableName, aWhereRec, strBuffer);
			String sqlWhere = strBuffer.toString();

			sqlStr = "select " + sqlSelect + " from " + aTableName + " where " + sqlWhere;
			if (aWhereStr != null && aWhereStr.isEmpty() == false) {
				sqlStr += " and " + aWhereStr;
			}

			result.add(sqlStr);
			result.add(fieldArr);
			return(result);
		} catch(Exception ex) {
			throw new Hinderance(ex, "Fetch sql fail: " + sqlStr);
		}		
	}	

	public long totalRecord() {
		return(this.getRecordBox().size());
	}

	/**
	 * Set a the field in this record as unique. When doing this, both meta
	 * record and data record is set altogether.
	 * 
	 * @param aFieldName
	 * @throws Exception 
	 */
	public void setUniqueKey(String aFieldName) throws Exception {

		Field metaField = this.getMetaRec().getField(aFieldName);
		if (metaField == null) throw new Hinderance("Fail to make field unique for table: " + this.getTableName() + ", no such field: " + aFieldName);
		metaField.setUniqueKey(true);

		for(Record dataRec : this.getRecordBox().values()) {
			Field dataField = dataRec.getField(aFieldName);
			if (dataField == null) throw new Hinderance("Fail to make field unique for table: " + this.getTableName() + ", no such field: " + aFieldName);
			dataField.setUniqueKey(true);
		}
	}

	public void sortAsc(String aFieldName) throws Exception {
		this.sortObjectBox(aFieldName, SortOrder.ASC);
	}

	public void sortDsc(String aFieldName) throws Exception {
		this.sortObjectBox(aFieldName, SortOrder.DSC);
	}

	private void sortObjectBox(String aFieldName, SortOrder aOrder) throws Exception {
		if ((this.getField(aFieldName) instanceof FieldObjectBox) == false) {
			throw new Hinderance("Only array of objects field can be sorted, cannot sort field: " + this.getField(aFieldName).getFieldName());
		}

		RecordComparator recordComparator = new RecordComparator(this.getRecordBox(), aFieldName, aOrder);
		TreeMap<Long, Record> sortedBox = new TreeMap<Long, Record>(recordComparator);
		sortedBox.putAll(this.getRecordBox());
		this.getRecordBox().clear();
		this.getRecordBox().putAll(sortedBox);
	}

	/**
	 * http://stackoverflow.com/questions/109383/how-to-sort-a-mapkey-value-on-the-values-in-java
	 * 
	 */
	class RecordComparator implements Comparator<Long> {
		ConcurrentHashMap<Long, Record> box2Compare;
		String field2Compare;
		SortOrder sortOrder;

		public RecordComparator(ConcurrentHashMap<Long, Record> aBox2Compare, String aFieldName, SortOrder aOrder) {
			this.box2Compare = aBox2Compare;
			this.field2Compare = aFieldName;
			this.sortOrder = aOrder;
		}

		@Override
		public int compare(Long leftKey, Long rightKey) {
			int result = 0;
			try {
				result = this.box2Compare.get(leftKey).getField(this.field2Compare).compareTo(this.box2Compare.get(rightKey).getField(this.field2Compare));
				if (this.sortOrder == SortOrder.DSC) {
					result = result * -1;
				}
			} catch(Exception ex) {
				App.logEror("Error when comparing field of Long type");
			}
			return(result);
		}
	}	

	public void renameTable(Connection aConn, String aNewName) throws Exception {
		Database.AlterTableRenameTable(aConn, this, aNewName);
	}

	public void renameField(Connection aConn, String aOldName, String aNewName) throws Exception {
		Database.AlterTableRenameField(aConn, this, aOldName, aNewName);
	}

	public void renamePrimaryKey(Connection aConn, String aOldTableName) throws Exception {
		Database.AlterIndexRenamePk(aConn, aOldTableName, this.getTableName());
	}

	public String getColumnDataType(Connection aConn, String aColName) throws Exception {
		String result = "";
		ResultSet rsMeta = null;
		DatabaseMetaData dbMeta;
		Connection connMeta = aConn;
		try {
			dbMeta = connMeta.getMetaData();
			//String dbName = connMeta.getCatalog();
			//connMeta.setCatalog(dbName);
			//rsMeta = dbMeta.getColumns(null, null, this.getTableName().toUpperCase(), null);
			rsMeta = dbMeta.getColumns(null, null, this.getTableName().toLowerCase(), null);
			while(rsMeta.next()) {
				String columnName = rsMeta.getString("COLUMN_NAME");
				if (columnName.equalsIgnoreCase(aColName)) {
					String columnType = rsMeta.getString("TYPE_NAME");
					int columnSize = rsMeta.getInt("COLUMN_SIZE");
					result = columnType + "(" + columnSize + ")";
				}
			}
		} finally 		{
			if (rsMeta != null) rsMeta .close();
		}
		return(result.toLowerCase());
	}

	public void forEachRecord(Connection aConn, Callback2ProcessRecord aCallback) throws Exception {
		PreparedStatement stmt = null;
		ResultSet rset = null;
		try {
			stmt = aConn.prepareStatement("select * from " + this.getTableName());
			rset = stmt.executeQuery();
			while (rset.next()) {
				if (aCallback != null) {
					Record record = new Record(this.getMetaRec());
					record.populateField(rset);
					if (aCallback.processRecord(aConn, record) == false) {
						break;
					}
				}
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

	/*
	public static void main(String args[]) {
		try {
			Database dbMaster = new Database();
			dbMaster.setupApp(args); // read property file, such as jdbc configuration, logging, etc
			dbMaster.setupDb(); // setup connection pooling

			Table tableA = new Table("rk_user");
			tableA.createField("login_id", FieldType.STRING, 10);
			tableA.createField("password", FieldType.ENCRYPT);
			tableA.createField("create_date", FieldType.DATETIME);
			tableA.createField("last_login", FieldType.DATETIME);
			tableA.createField("first_login", FieldType.DATETIME);
			tableA.createField("last_attempt", FieldType.INTEGER);
			tableA.createField("status", FieldType.INTEGER);
			if (dbMaster.tableExist(tableA.getTableName()) == false) dbMaster.createTable(tableA);
		} catch(Exception ex) {
			App.logEror(0, new Hinderance(ex, "Application encounter fatal error, application is aborting...."));
		}
	}
	*/

}


	/*
	public String getColumnDataType(Connection aConn, String aColName) throws Exception {
		String result = null;
		Statement stmt = null;
		ResultSet rs = null;
		ResultSetMetaData rsmd = null;
		try {
			//stmt = aConn.prepareStatement("select * from " + this.getTableName() + " where 1 < 0");
			stmt = aConn.createStatement();
			rs = stmt.executeQuery("select * from " + this.getTableName());
			rsmd = rs.getMetaData();
			for(int cntr = 1; cntr <= rsmd.getColumnCount(); cntr++) {
				if (rsmd.getColumnName(cntr).equalsIgnoreCase(aColName)) {
					String columnType = rsmd.getColumnTypeName(cntr);
					int columnSize = rsmd.getColumnDisplaySize(cntr);
					result = columnType + "(" + columnSize + ")";
				}
			}
		} finally {
			if (rs != null) {
				rs.close();
			}
			if (stmt != null) {
				stmt.close();
			}
		}
		return(result);
	}
	*/