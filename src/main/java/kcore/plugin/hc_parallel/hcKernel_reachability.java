package kcore.plugin.hc_parallel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.aparapi.Kernel;
//import com.aparapi.Range;

public class hcKernel_reachability extends Kernel {
	private Map<String, Vector<String>> adjList;
	private String[] vertextList;
//	private List<String> visited;
	private Map<String, Integer> reachability;
	private Map<String, Set<String>> reachableList = new HashMap<>();
	
	public hcKernel_reachability() {
		this.reachability = new HashMap<>();
	}
	@Override
	public void run() {
		int index = getGlobalId();
		List<String> visited = new ArrayList<>();
		int n = countChildNode(vertextList[index],vertextList[index], visited);
		reachability.put(vertextList[index], n) ;
	}
	public int countChildNode(String node, String source, List<String> visited) {
		int count = 0;
		visited.add(node);
		if (adjList.get(node) != null && adjList.get(node).size() > 0) {
			for (String vertex : adjList.get(node)) {
				if (!visited.contains(vertex)) {
					if (adjList.get(vertex) != null && adjList.get(vertex).size() > 0) {
						count = count + countChildNode(vertex, source, visited);
					}
					count = count + 1;
					visited.add(vertex);
					pushMapS(reachableList, source, vertex);
				}
			}
		} else return 0;
		return count;
	}
	public void pushMapS(Map<String, Set<String>> adjList, String start, String end) {
		if (!adjList.containsKey(start)) {
			adjList.put(start, new HashSet<>());
		}
		adjList.get(start).add(end);
	}
	public Map<String, Set<String>> getReachableList() {
		return reachableList;
	}
	public void setReachableList(Map<String, Set<String>> reachableList) {
		this.reachableList = reachableList;
	}
	public Map<String, Vector<String>> getAdjList() {
		return adjList;
	}
	
	public void setAdjList(Map<String, Vector<String>> adjList) {
		this.adjList = adjList;
	}

//	public Set<String> getVisited() {
//		return visited;
//	}
//
//	public void setVisited(Set<String> visited) {
//		this.visited = visited;
//	}

	public String[] getVertextList() {
		return vertextList;
	}
	public void setVertextList(String[] vertextList) {
		this.vertextList = vertextList;
	}

	public Map<String, Integer> getReachability() {
		return reachability;
	}

	public void setReachability(Map<String, Integer> reachability) {
		this.reachability = reachability;
	}
	
	
}
