package biz.shujutech.technical;

public final class LambdaInteger {
	private Integer cntr = 0;

	public LambdaInteger() {
	}

	public LambdaInteger(Integer aValue) {
		setInteger(aValue);
	}

	public Integer getInteger() {
		return cntr;
	}

	public void setInteger(Integer cntr) {
		this.cntr = cntr;
	}

	public Integer increment() {
		this.cntr = this.cntr + 1;
		return(this.cntr);
	}

}
