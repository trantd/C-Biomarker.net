package kcore.plugin.biomarker_parallel;

public class BiomarkerResult implements Comparable<BiomarkerResult> {
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
	@Override
	public int compareTo(BiomarkerResult bio1) {
		if(bio1.getrCore() > rCore) {
    		return 1;
    	}
    	else if(bio1.getrCore() < rCore) {
    		return -1;
    	}
    	else {
    		if(bio1.getHc() <= hc) {
    			return -1;
    		}
    		else {
    			return 1;
    		}
    	}
	}	
}
