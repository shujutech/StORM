
package biz.shujutech.technical;

public class LambdaFloat {
	private Float value = 0.0f;

	public Float getFloat() {
		return value;
	}

	public void setFloat(Float value) {
		this.value = value;
	}

	public Float sum(Float aFloat) {
		this.value = this.value + aFloat;
		return(this.value);
	}

	@Override
	public String toString() {
		return this.getFloat().toString();
	}

}
