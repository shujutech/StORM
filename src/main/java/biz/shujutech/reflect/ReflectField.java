package biz.shujutech.reflect;


import biz.shujutech.db.object.Clasz;
import biz.shujutech.db.relational.FieldType;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


@Retention(RUNTIME)
public @interface ReflectField {
	public boolean encrypt() default false;	
	public FieldType type() default FieldType.UNKNOWN;
	public int size() default 0;
	public String mask() default "";
	//public String clasz() default "";
	public Class<?> clasz() default Clasz.class;
	public boolean inline() default false;	
	public String[] value() default {};
	public boolean deleteAsMember() default false;
	public boolean delayFetch () default false;
	public int displayPosition() default 0;
	public boolean polymorphic() default false;
	public boolean uiMaster() default false;
	public boolean prefetch() default false;
	public boolean updateable() default true;
	public boolean changeable() default true;
	public boolean lookup() default false;
	public ReflectIndex[] indexes() default {};
}
