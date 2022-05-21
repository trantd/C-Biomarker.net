package kcore.plugin.biomarker_parallel;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.aparapi.Kernel;

public class RCoreKernel extends Kernel {
	//R-core
	private int l;
	// number of vertex
	private int i;
	// map to store adjacency list
	private Map<String, Vector<String>> adjList;
	// map to store reachability
	private Map<String, Integer> reachability;
	private Map<String, Set<String>> reachableList;
	//array list to store adjbuff
	private ArrayList<String> vertexBuff;
	private Set<String> vertexList;
	private int rCore[];
	
	public RCoreKernel() {
		super();
	}

	@Override
	public void run() {
		int index = getGlobalId();
		if(index < vertexBuff.size()) {
			String vertex = vertexBuff.get(index);
//			for (Map.Entry<String, Set<String>> entry : reachableList.entrySet()){
//				if(entry.getValue().contains(vertex)) {
//					String vert = entry.getKey();
//					int adjRea = reachability.get(vert);
//					if(adjRea > l) {
//						adjRea--;
//						reachability.replace(vert, adjRea);
//						if(adjRea == l) {
//							vertexBuff.add(vert);
//						}
//						if(adjRea < l) {
//							++adjRea;
//							reachability.replace(vert, adjRea);
//						}
//					}
//				}
//			}
			Vector<String> adjListV = adjList.get(vertex);
			for (String vert : adjListV) {
				int adjRea = reachability.get(vert);
				if(adjRea > l) {
					adjRea--;
					reachability.replace(vert, adjRea);
					if(adjRea == l) {
						vertexBuff.add(vert);
					}
					if(adjRea < l) {
						++adjRea;
						reachability.replace(vert, adjRea);
					}
				}
			}
		}
	}
	

	public Map<String, Set<String>> getReachableList() {
		return reachableList;
	}

	public void setReachableList(Map<String, Set<String>> reachableList) {
		this.reachableList = reachableList;
	}

	public Set<String> getVertexList() {
		return vertexList;
	}

	public void setVertexList(Set<String> vertexList) {
		this.vertexList = vertexList;
	}

	public int getVisitedVertex() {
		return vertexBuff.size();
	}

	public int getL() {
		return l;
	}

	public void setL(int l) {
		this.l = l;
	}

	public int getI() {
		return i;
	}

	public void setI(int i) {
		this.i = i;
	}

	public Map<String, Vector<String>> getAdjList() {
		return adjList;
	}

	public void setAdjList(Map<String, Vector<String>> adjList) {
		this.adjList = adjList;
	}

	public Map<String, Integer> getReachability() {
		return reachability;
	}

	public void setReachability(Map<String, Integer> reachability) {
		this.reachability = reachability;
	}

	public ArrayList<String> getVertexBuff() {
		return vertexBuff;
	}

	public void setVertexBuff(ArrayList<String> vertexBuff) {
		this.vertexBuff = vertexBuff;
	}
}
