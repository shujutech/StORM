package biz.shujutech.technical;

public class LambdaGeneric<Ty> {
	private Ty value = null;

	public LambdaGeneric() {
	}

	public LambdaGeneric(Ty value) {
		this.value = value;
	}

	public Ty getValue() {
		return value;
	}

	public void setValue(Ty value) {
		this.value = value;
	}
	
}
