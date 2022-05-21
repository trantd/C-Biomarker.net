package kcore.plugin.alg.param;

public enum NetFilteringMethod {
	FILTER_BY_KCORE("K-Core"),
	FILTER_BY_RCORE("R-Core"),
	FILTER_BY_HC("Hierarchical closeness"),
	FILTER_BY_BIO("Biomarker genes");
	
	private String text;
	
	private NetFilteringMethod(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
	
	public String toString() {
		return text;
	}
	
}
