package biz.shujutech.db.relational;

/**
 *
 */
public class FieldRecord extends Field {
	
	private Record record = null;

	public FieldRecord(String aName) {
		this.setDbFieldName(aName);
		this.setDbFieldType(FieldType.RECORD);
	}

	public Record getRecord() {
		return record;
	}

	public void setValueRecord(Record record) {
		this.record = record;
	}


}
