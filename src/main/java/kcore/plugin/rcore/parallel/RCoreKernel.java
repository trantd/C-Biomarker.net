package kcore.plugin.rcore.parallel;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

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
	private Map<String, Vector<String>> reachableList;
	//array list to store adjbuff
	private ArrayList<String> vertexBuff;
	private ArrayList<String> vertexList;
	private AtomicIntegerArray atomicRCore;
	
	public RCoreKernel(Set<String> keySet) {
//		super();
		vertexList = new ArrayList<String>(keySet);
	}

	@Override
	public void run() {
		if(getGlobalId() < vertexBuff.size()) {
			int index = getGlobalId();
			String vertex = vertexBuff.get(index);
//			for (Map.Entry<String, Vector<String>> entry : reachableList.entrySet()){
//				if(entry.getValue().contains(vertex)) {
//					String vert = entry.getKey();
//					int ind = vertexList.indexOf(vert);
//					int adjRea = atomicRCore.get(ind);
//					if(adjRea > l) {
//						adjRea = atomicRCore.decrementAndGet(ind);
//						if(adjRea == l) {
//							vertexBuff.add(vert);
//						}
//						if(adjRea < l) {
//							atomicRCore.incrementAndGet(ind);
//						}
//					}
//				}
//			}
			Vector<String> adjListV = adjList.get(vertex);
			for (String vert : adjListV) {
				int ind = vertexList.indexOf(vert);
				int adjRea = atomicRCore.get(ind);
				if(adjRea > l) {
//					adjRea--;
//					reachability.replace(vert, count.get());
					adjRea = atomicRCore.decrementAndGet(ind);
					if(adjRea == l) {
						vertexBuff.add(vert);
					}
					if(adjRea < l) {
//						++adjRea;
//						reachability.replace(vert, count.get());
						atomicRCore.incrementAndGet(ind);
					}
				}
			}
		}
	}
	

	public AtomicIntegerArray getAtomicRCore() {
		return atomicRCore;
	}

	public void setAtomicRCore(AtomicIntegerArray atomicRCore) {
		this.atomicRCore = atomicRCore;
	}

	public Map<String, Vector<String>> getReachableList() {
		return reachableList;
	}

	public void setReachableList(Map<String, Vector<String>> reachableList) {
		this.reachableList = reachableList;
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
