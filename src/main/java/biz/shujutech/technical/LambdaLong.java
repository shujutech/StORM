package biz.shujutech.technical;

public class LambdaLong {
	private Long value = 0L;

	public Long getLong() {
		return value;
	}

	public void setLong(Long value) {
		this.value = value;
	}

	public Long sum(Long aLong) {
		this.value = this.value + aLong;
		return(this.value);
	}
	
	public Long increment() {
		this.value = this.value + 1;
		return(this.value);
	}
}
