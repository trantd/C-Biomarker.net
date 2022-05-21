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
	private int i;
	// map to store adjacency list
	private Map<String, Vector<String>> adjList;
	// map to store degree
	private Map<String, Integer> degrees;
	//array list to store vertex list
	private ArrayList<String> vertexList;
	//array list to store adjbuff
	private ArrayList<String> vertexBuff;
	private int kCore[];
	private AtomicIntegerArray atomickCore;
	private CyclicBarrier ba; 
	
	public KCoreKernel(Map<String, Integer> degrees) {
//		super();
		this.degrees = degrees;
		Set<String> keySet = this.degrees.keySet();
		vertexList = new ArrayList<String>(keySet);
	}

	@Override
	public void run() {
//		ba = new CyclicBarrier(vertexBuff.size());
		if(getGlobalId() < vertexBuff.size()) {
			int index = getGlobalId();
			String vertex = vertexBuff.get(index);
			Vector<String> adjListV = adjList.get(vertex);
			for (String adjName : adjListV) {
				int ind = vertexList.indexOf(adjName);
				int adjDeg = atomickCore.get(ind);
				if(adjDeg > l) {
//					adjDeg = adjDeg - 1;
//					degrees.replace(adjName, adjDeg);
					adjDeg = atomickCore.decrementAndGet(ind);
					if(adjDeg == l) {
						vertexBuff.add(adjName);
					}
					if(adjDeg < l) {
//						adjDeg = adjDeg + 1;
//						degrees.replace(adjName, adjDeg);
						atomickCore.incrementAndGet(ind);
					}
				}
			}
		}
//		try {
//			ba.await();
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (BrokenBarrierException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	public CyclicBarrier getBa() {
		return ba;
	}

	public void setBa(CyclicBarrier ba) {
		this.ba = ba;
	}

	public AtomicIntegerArray getAtomickCore() {
		return atomickCore;
	}

	public void setAtomickCore(AtomicIntegerArray atomickCore) {
		this.atomickCore = atomickCore;
	}

	public int getVisitedVertex() {
		return vertexBuff.size();
	}

	public int[] getkCore() {
		return kCore;
	}

	public void setkCore(int[] kCore) {
		this.kCore = kCore;
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
