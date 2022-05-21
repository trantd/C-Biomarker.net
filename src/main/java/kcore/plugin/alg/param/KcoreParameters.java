package kcore.plugin.alg.param;

import org.cytoscape.model.CyNetwork;

public class KcoreParameters {

//	private GTAAlgParametersAttrSelection cancerSelParam;
//	private GTAAlgParametersAttrSelection normalSelParam;
	private CyNetwork cyNetwork;
//	private boolean generateTScores;
//	private boolean generateDegrees;
//	private int levelOneSubnetSize, levelTwoSubnetSize;
	private int resultingNetworkCount;
//	public static final int SUBNETWORK_MAX_SIZE_MAX = 20;
//	public static final int SUBNETWORK_MAX_SIZE_DEFAULT = 10;
//	public static final int NUMBER_OF_GENERATED_NETWORKS_MAX = 100;
//	public static final int NUMBER_OF_GENERATED_NETWORKS_DEFAULT = 10;
	
	//
	//public static final int RESULTING_NETWORK_MAX_TRY_IF_NO_ANSWER = 10;
	
	private NetFilteringMethod netFilteringMethod;

	public KcoreParameters(CyNetwork cyNetwork, int resultingNetworkCount, NetFilteringMethod netFilteringMethod) {
		this.setCyNetwork(cyNetwork);
		this.resultingNetworkCount = resultingNetworkCount;
		this.netFilteringMethod = netFilteringMethod;
	}

	public CyNetwork getCyNetwork() {
		return cyNetwork;
	}

	public void setCyNetwork(CyNetwork cyNetwork) {
		this.cyNetwork = cyNetwork;
	}

	public int getResultingNetworkCount() {
		return resultingNetworkCount;
	}

	public void setResultingNetworkCount(int resultingNetworkCount) {
		this.resultingNetworkCount = resultingNetworkCount;
	}

	public NetFilteringMethod getNetFilteringMethod() {
		return netFilteringMethod;
	}
	
	public void setNetFilteringMethod(NetFilteringMethod netFilteringMethod) {
		this.netFilteringMethod = netFilteringMethod;
	}
	
}
