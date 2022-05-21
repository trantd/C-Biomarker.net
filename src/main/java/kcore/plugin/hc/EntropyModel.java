package kcore.plugin.hc;

public class EntropyModel {
	public String key;
	public Double value;
	
	public EntropyModel(){
		
	}
	
	public EntropyModel(String key, Double value){
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Double getValue() {
		return value;
	}

	public void setValue(Double value) {
		this.value = value;
	}
	
}
