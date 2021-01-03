package biz.shujutech.db.object;

import biz.shujutech.db.relational.Field;

/**
 *
 */
public class FieldClasz extends Field {
	public enum FetchStatus {
		SOF, // start of fetch
		MOF, // middof of fetch
		EOF; // end of fetch
	};

	private String defineType; // this is the type declared in the annotation, for polymorpich declaration its different from its object type
	private boolean prefetch = true;
	private FetchStatus fetchStatus = FetchStatus.SOF;

	public String getDeclareType() {
		return defineType;
	}

	public void setDeclareType(String objectType) {
		this.defineType = objectType;
	}

	/* don't use this cause if u pass to a Class param then call this method, getClass() return class instead of the original/leaf class
	public void setDeclareType(Class objectType) {
		this.defineType = objectType.getClass().getName();
	}
	*/

	public boolean isPrefetch() {
		return prefetch;
	}

	public void setPrefetch(boolean prefetch) {
		this.prefetch = prefetch;
	}

	public FetchStatus getFetchStatus() {
		return fetchStatus;
	}

	public void setFetchStatus(FetchStatus fetchStatus) {
		this.fetchStatus = fetchStatus;
	}
}
