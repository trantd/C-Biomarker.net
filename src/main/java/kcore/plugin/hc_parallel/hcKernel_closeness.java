package kcore.plugin.hc_parallel;

import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Vector;
import java.util.ArrayList;
import java.util.HashMap;

import com.aparapi.Kernel;

public class hcKernel_closeness extends Kernel {	
	private Map<String, Vector<String>> adjList;
	private Set<String> vertexList;
	
	private Map<String, Double> closeness = new HashMap<>();
	private Map<String, Set<String>> reachableList;
	
	private ArrayList<Vert> listVert;
	
	public hcKernel_closeness() {
		super.clone();
	}
	
	@Override
	public void run() {
//		super.clone();
		int index = getGlobalId();
//		ArrayList<Vert> vertList = new ArrayList<>();
		double c = closeNess(index, listVert);
		closeness.put(listVert.get(index).getName(), c);
	}
	
	public double closeNess(int index, ArrayList<Vert> vertList) {
//		initVertList(vertList);
		double path = 0;
		ShortestP(vertList.get(index));
		Set<String> rea = reachableList.get(vertList.get(index).getName());
			for (Vert v : vertList) {
				if(rea != null && rea.contains(v.getName())) {
					path += 1/v.getDist();
				}
				v.setDist(Double.MAX_VALUE);
				v.setVisited(false);
			}
		
		return path/(vertList.size()-1);
	}
	public void ShortestP(Vert sourceV) {
        sourceV.setDist(0);
        PriorityQueue<Vert> priorityQueue = new PriorityQueue<>();
        priorityQueue.add(sourceV);
        sourceV.setVisited(true);

        while (!priorityQueue.isEmpty()) {
            Vert actualVertex = priorityQueue.poll();
            for (Edge1 edge : actualVertex.getList()) {
                Vert v = edge.getTargetVert();

                if (!v.Visited()) {
                    double newDistance = actualVertex.getDist() + edge.getWeight();
                    if (newDistance < v.getDist()) {
                        priorityQueue.remove(v);
                        v.setDist(newDistance);
                        v.setPr(actualVertex);
                        priorityQueue.add(v);
                    }
                }
            }
            actualVertex.setVisited(true);
        }
    }
	public void initVertList(ArrayList<Vert> vertList) {
		for (String ver : vertexList) {
			Vert vert = new Vert(ver);
			vertList.add(vert);
		}
		for (Vert vert : vertList) {
			Vector<String> neighbour  = adjList.get(vert.getName());
			if(neighbour != null) {
				for (Vert v : vertList) {
					if(neighbour.contains(v.getName())) {
						vert.addNeighbour(new Edge1(1, vert, v));
					}
				}
			}
		}
	}

	public ArrayList<Vert> getListVert() {
		return listVert;
	}

	public void setListVert(ArrayList<Vert> listVert) {
		this.listVert = listVert;
	}

	public Map<String, Vector<String>> getAdjList() {
		return adjList;
	}

	public void setAdjList(Map<String, Vector<String>> adjList) {
		this.adjList = adjList;
	}

	public Set<String> getVertexList() {
		return vertexList;
	}

	public void setVertexList(Set<String> vertexList) {
		this.vertexList = vertexList;
	}

	public Map<String, Set<String>> getReachableList() {
		return reachableList;
	}

	public void setReachableList(Map<String, Set<String>> reachableList) {
		this.reachableList = reachableList;
	}

//	public ArrayList<Vert> getVertList() {
//		return vertList;
//	}
//
//	public void setVertList(ArrayList<Vert> vertList) {
//		this.vertList = vertList;
//	}

	public Map<String, Double> getCloseness() {
		return closeness;
	}

	public void setCloseness(Map<String, Double> closeness) {
		this.closeness = closeness;
	}
	
}

