package biz.shujutech.db.relational;

public enum SortOrder {
	ASC, 
	DSC;

	public static String AsString(SortOrder aOrder) {
		String result = "desc";
		if (aOrder == ASC) {
			result = "asc";
		}
		return(result);
	}

	public static SortOrder AsSortOrder(String aOrder) {
		SortOrder result = SortOrder.DSC;
		if (aOrder.trim().equals("asc")) {
			result = SortOrder.ASC;
		}
		return(result);
	}
}
