package kcore.plugin.biomarker;

public class BiomarkerResult {
	private String name;
	private int rCore;
	private double hc;
	
	public BiomarkerResult(String name, int rCore, double hc) {
		super();
		this.name = name;
		this.rCore = rCore;
		this.hc = hc;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getrCore() {
		return rCore;
	}
	public void setrCore(int rCore) {
		this.rCore = rCore;
	}
	public double getHc() {
		return hc;
	}
	public void setHc(double hc) {
		this.hc = hc;
	}
	
	
}
