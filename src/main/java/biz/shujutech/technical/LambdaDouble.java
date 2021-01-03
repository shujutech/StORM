package biz.shujutech.technical;

public class LambdaDouble {
	private Double value = 0D;

	public Double getDouble() {
		return value;
	}

	public void setDouble(Double value) {
		this.value = value;
	}

	public Double sum(Double aDouble) {
		this.value = this.value + aDouble;
		return(this.value);
	}

	@Override
	public String toString() {
		return this.getDouble().toString();
	}

}
