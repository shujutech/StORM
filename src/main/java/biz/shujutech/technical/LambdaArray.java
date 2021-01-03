package biz.shujutech.technical;

import java.util.concurrent.CopyOnWriteArrayList;

public class LambdaArray {
	CopyOnWriteArrayList<Object> theArry = new CopyOnWriteArrayList<>();

	public CopyOnWriteArrayList<Object> getTheArry() {
		return theArry;
	}

	public void setTheArry(CopyOnWriteArrayList<Object> theArry) {
		this.theArry = theArry;
	}

	public void addElement(Object aObject) {
		this.theArry.add(aObject);
	}

	public Object getElement(int aIndex) {
		return this.theArry.get(aIndex);
	}

	public int getSize() {
		return this.theArry.size();
	}
}
