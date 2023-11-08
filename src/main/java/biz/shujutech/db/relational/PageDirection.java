package biz.shujutech.db.relational;

public enum PageDirection {
	PREVIOUS, 
	NEXT;

	public static String AsString(PageDirection aOrder) {
		String result = "next";
		if (aOrder == PREVIOUS) {
			result = "previous";
		}
		return(result);
	}

	public static PageDirection AsPageDirection(String aOrder) {
		PageDirection result = PageDirection.NEXT;
		if (aOrder.trim().equals("previous")) {
			result = PageDirection.PREVIOUS;
		}
		return(result);
	}
	
}
