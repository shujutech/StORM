package biz.shujutech.reflect;

import biz.shujutech.db.relational.Field;
import biz.shujutech.db.relational.FieldType;
import java.util.List;

public class AttribField {
	public String fieldName;
	public FieldType fieldType;
	public int fieldSize;
	public String fieldMask;
	public Field createdField;
	public Boolean isInline;
	public int displayPosition;
	public boolean polymorphic;
	public boolean prefetch;
	public boolean uiMaster; 
	public boolean updateable; 
	public boolean changeable; 
	public boolean lookup;
	public List<AttribIndex> indexes;
}