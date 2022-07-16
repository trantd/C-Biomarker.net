package kcore.plugin.parallel;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicIntegerArray;

import com.aparapi.Kernel;

public class KCoreKernel extends Kernel {
	//k-core
	private int l;
	// number of vertex
	private AtomicInteger i;
	// map to store adjacency list
	private Map<String, Vector<String>> adjList;
	// map to store degree
	private Map<String, Integer> degrees;
	//array list to store vertex list
	private ArrayList<String> vertexList;
	//array list to store adjbuff
//	private ArrayList<String> vertexBuff;
	private int kCore[];
	private AtomicIntegerArray atomickCore;
	private Set<String> visitedList;
	
	public Set<String> getVisitedList() {
		return visitedList;
	}

	public void setVisitedList(Set<String> visitedList) {
		this.visitedList = visitedList;
	}

	public KCoreKernel(Map<String, Integer> degrees) {
//		super();
		this.degrees = degrees;
		Set<String> keySet = this.degrees.keySet();
		vertexList = new ArrayList<String>(keySet);
		i = new AtomicInteger(0);
	}

	@Override
	public void run() {
		int index = getGlobalId();
		ArrayList<String> vertexBuff = new ArrayList<>();
		if(atomickCore.get(index) == l && !visitedList.contains(vertexList.get(index))) {
			vertexBuff.add(vertexList.get(index));
			visitedList.add(vertexList.get(index));
		}
		if(vertexBuff.size() > 0) {
			int n = vertexBuff.size();
			for (int j = 0; j < n; j++) {
				Vector<String> adjListV = adjList.get(vertexBuff.get(j));
				for (String adj : adjListV) {
					int id = vertexList.indexOf(adj);
					if(atomickCore.get(id) > l) {
						atomickCore.decrementAndGet(id);
						if(atomickCore.get(id) == l) {
							if(!visitedList.contains(adj)) {
								vertexBuff.add(adj);
								visitedList.add(adj);
								n++;
							}
						}
						if(atomickCore.get(id) < l) {
							atomickCore.incrementAndGet(id);
						}
					}
				}
			}
			i.addAndGet(vertexBuff.size());
		}
	}

	public ArrayList<String> getVertexList() {
		return vertexList;
	}

	public void setVertexList(ArrayList<String> vertexList) {
		this.vertexList = vertexList;
	}

	public AtomicIntegerArray getAtomickCore() {
		return atomickCore;
	}

	public void setAtomickCore(AtomicIntegerArray atomickCore) {
		this.atomickCore = atomickCore;
	}

	public int getVisitedVertex() {
		return i.get();
	}

	public int[] getkCore() {
		return kCore;
	}

	public void setkCore(int[] kCore) {
		this.kCore = kCore;
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

//	public ArrayList<String> getVertexBuff() {
//		return vertexBuff;
//	}
//
//	public void setVertexBuff(ArrayList<String> vertexBuff) {
//		this.vertexBuff = vertexBuff;
//	}
}
