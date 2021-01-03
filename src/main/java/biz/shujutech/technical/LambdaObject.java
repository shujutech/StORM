package biz.shujutech.technical;

public final class LambdaObject {
	Object theObject = null;

	public LambdaObject(Object aObject) {
		this.setTheObject(aObject);
	}

	public LambdaObject() {
		this.setTheObject(null);
	}

	public Object getTheObject() {
		return theObject;
	}

	public void setTheObject(Object theObject) {
		this.theObject = theObject;
	}

}
