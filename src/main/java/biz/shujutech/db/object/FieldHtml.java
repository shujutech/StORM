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
		this.setDbFieldType(FieldType.HTML);
	}

	public FieldHtml(String aName, int aSize) {
		super(aName, aSize);
		this.setDbFieldType(FieldType.HTML);
	}
	
	@Override
	public void setValueStr(String valueString) throws Exception {
		String cleanStr;
		if (valueString != null && valueString.equals("<br>")) {
			cleanStr = null;
		} else {
			cleanStr = valueString;
		}
		super.setValueStr(cleanStr);
	}
}
