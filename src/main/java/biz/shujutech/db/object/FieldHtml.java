package biz.shujutech.db.object;

import biz.shujutech.db.relational.FieldStr;
import biz.shujutech.db.relational.FieldType;

/**
 *
 * @author chairkb
 */
public class FieldHtml extends FieldStr {

	public FieldHtml(String aName) {
		super(aName, 512);
		this.setFieldType(FieldType.HTML);
	}

	public FieldHtml(String aName, int aSize) {
		super(aName, aSize);
		this.setFieldType(FieldType.HTML);
	}
	
}
