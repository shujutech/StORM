package biz.shujutech.db.relational;

public class FieldBase64 extends FieldStr {
	private static final int MAX_BASE64_SIZE = Integer.MAX_VALUE; // should only need 43 bytes, https://stackoverflow.com/questions/13378815/base64-length-calculation

	public FieldBase64(String aName) {
		super(aName, MAX_BASE64_SIZE);
		this.setFieldType(FieldType.BASE64);
	}
	
}
