package biz.shujutech.technical;

public class LambdaCounter {
	private Integer cntr = 0;

	public Integer getCntr() {
		return cntr;
	}

	public void setCntr(Integer cntr) {
		this.cntr = cntr;
	}

	public Integer increment() {
		this.cntr = this.cntr + 1;
		return(this.cntr);
	}

}
