package kcore.plugin.biomarker_parallel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.aparapi.Kernel;

public class KCoreKernel extends Kernel {
	//k-core
	private int l;
	// number of vertex
	private int i;
	// map to store adjacency list
	private Map<String, Vector<String>> adjList;
	// map to store degree
	private Map<String, Integer> degrees;
	//array list to store vertex list
	private ArrayList<String> vertexList;
	//array list to store adjbuff
	private ArrayList<String> vertexBuff;
	private List<Integer> result;
	
	public KCoreKernel(Map<String, Integer> degrees) {
//		super();
		this.degrees = degrees;
		Set<String> keySet = this.degrees.keySet();
		vertexList = new ArrayList<String>(keySet);
	}

	@Override
	public void run() {
		if(getGlobalId() < vertexBuff.size()) {
			int index = getGlobalId();
			String vertex = vertexBuff.get(index);
			Vector<String> adjListV = adjList.get(vertex);
			for (String adjName : adjListV) {
				if(degrees.get(adjName) > l) {
					int adjDeg = degrees.get(adjName);
					adjDeg = adjDeg - 1;
					degrees.replace(adjName, adjDeg);
					if(degrees.get(adjName) == l) {
						vertexBuff.add(adjName);
					}
					if(degrees.get(adjName) < l) {
						adjDeg = adjDeg + 1;
						degrees.replace(adjName, adjDeg);
					}
				}
			}
		}
	}

	public int getVisitedVertex() {
		return vertexBuff.size();
	}

	public List<Integer> getResult() {
		return result;
	}

	public void setResult(List<Integer> result) {
		this.result = result;
	}

	public int getI() {
		return i;
	}

	public void setI(int i) {
		this.i = i;
	}

	public int getL() {
		return l;
	}

	public void setL(int l) {
		this.l = l;
	}

	public Map<String, Vector<String>> getAdjList() {
		return adjList;
	}

	public void setAdjList(Map<String, Vector<String>> adjList) {
		this.adjList = adjList;
	}

	public Map<String, Integer> getDegrees() {
		return degrees;
	}

	public void setDegrees(Map<String, Integer> degrees) {
		this.degrees = degrees;
	}

	public ArrayList<String> getVertexBuff() {
		return vertexBuff;
	}

	public void setVertexBuff(ArrayList<String> vertexBuff) {
		this.vertexBuff = vertexBuff;
	}
}
