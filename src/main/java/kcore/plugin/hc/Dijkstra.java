package kcore.plugin.hc;

import java.util.*;

public class Dijkstra {
	public Map<Integer, Double> Distance = new HashMap<Integer, Double>();
	public Map<Integer, Integer> Previous = new HashMap<Integer, Integer>();

	public Dijkstra() {

	}
	
	public static void Main(String [] args){
		
	}

	public Map<Integer, Double> getDistance() {
		return Distance;
	}

	public void setDistance(Map<Integer, Double> distance) {
		Distance = distance;
	}

	public Map<Integer, Integer> getPrevious() {
		return Previous;
	}

	public void setPrevious(Map<Integer, Integer> previous) {
		Previous = previous;
	}

	// If the distance between two node A and B is an unreachable distance,
	// there is no path from A to B
	public static final float UnreachableDistance = Float.MAX_VALUE;
	/* Holds queue for the nodes to be evaluated */
	private List<Integer> queue = new ArrayList<Integer>();

	/// <summary>
	/// Find the shortest paths from a given start node
	/// </summary>
	/// <param name="graph">The adjacency list (zero-based index) that is
	/// composed of tuples (start, end, weight)</param>
	/// <param name="start">The index of the start node</param>
	/// <returns>
	/// 1. The shortest distances from vertices to the start in property
	/// Distance
	/// 2. The shortest paths in property Previous
	/// </returns>
	public void FindShortestPathAndDistance(Map<Integer, HashMap<Integer, Double>> graph, int start) {
		/* Check graph format and that the graph actually contains something */
		if (graph.keySet().size() < 1) {
			return;
		}
		
		Initialize(graph, start);

		if (!graph.containsKey(start))
			return;

		while (queue.size() > 0) {
			int u = GetNextVertex();

			/* Find the nodes that u connects to and perform relax */
			// for (int v = 0; v < graph[u].Keys.Count; v++)
			for (int v : graph.get(u).keySet()) {
				/* Checks for edges with negative weight */
				if (graph.get(u).get(v) < 0) {
					throw new IllegalArgumentException("Graph contains negative edge(s)");
				}

				/* Check for an edge between u and v */
				if (graph.get(u).get(v) > 0) // graph[u][v] > 0
				{
					/* Edge exists, relax the edge */
					if (getDistance(v) > getDistance(u) + graph.get(u).get(v)) {
						Distance.put(v, (Distance.get(u) + graph.get(u).get(v)));
						Previous.put(v, u);
					}
				}
			}
		}
		
//		//test
//		for(Map.Entry<Integer, Double> node : Distance.entrySet()){
//			System.out.println("node: "+node.getKey()+" Distance: "+node.getValue());
//		}
	}

	private void Initialize(Map<Integer, HashMap<Integer, Double>> graph, int start) {
		Distance.clear();
		Previous.clear();
		queue.clear();

		/*
		 * Set distance to all nodes to infinity - alternatively use
		 * Int.MaxValue for use of Int type instead
		 */
		// for (int i = 0; i < len; i++)
		for (int i : graph.keySet()) {
			queue.add(i);
		}

		/*
		 * Set distance to 0 for starting point and the previous node to null
		 * (-1)
		 */
		Distance.put(start, 0.0d);
		Previous.put(start, -1);
	}

	/* Retrives next node to evaluate from the queue */
	private int GetNextVertex() {
		double min = Double.POSITIVE_INFINITY;
		int vertex = -1;

		/*
		 * Search through queue to find the next node having the smallest
		 * distance
		 */
		// System.out.println("queue size: "+ queue.size());
		for (int j : queue) {
			if (getDistance(j) <= min) {
				min = getDistance(j);
				vertex = j;

			}
		}

		queue.remove(new Integer(vertex));

		return vertex;
	}

	private double getDistance(int idx) {
		if (Distance.containsKey(idx))
			return Distance.get(idx);
		else
			return UnreachableDistance;
	}
	
	public static void main(String[] args){
		Map<Integer, HashMap<Integer, Double>> graph = new HashMap<Integer, HashMap<Integer, Double>>();
		if (!graph.containsKey(0)) {
			graph.put(0, new HashMap<Integer, Double>());
		}
		if (!graph.containsKey(2)) {
			graph.put(2, new HashMap<Integer, Double>());
		}

		graph.get(0).put(1, !graph.get(0).containsKey(1) ? 9.0d : graph.get(0).get(1) + 9.0d);
		graph.get(0).put(2, !graph.get(0).containsKey(2) ? 6.0d : graph.get(0).get(2) + 6.0d);
		graph.get(0).put(3, !graph.get(0).containsKey(3) ? 5.0d : graph.get(0).get(3) + 5.0d);
		graph.get(0).put(4, !graph.get(0).containsKey(4) ? 3.0d : graph.get(0).get(4) + 9.0d);
		graph.get(2).put(1, !graph.get(2).containsKey(1) ? 2.0d : graph.get(2).get(1) + 2.0d);
		graph.get(2).put(3, !graph.get(2).containsKey(3) ? 4.0d : graph.get(2).get(3) + 4.0d);
		
		Dijkstra ds = new Dijkstra();
		
		ds.FindShortestPathAndDistance(graph, 0);
		
		System.out.println("The shorted path from node :"); 
		for (Map.Entry<Integer, Double> entry : ds.getDistance().entrySet()) {
			System.out.println(0 + " to " + entry.getKey() + " is "
                    + entry.getValue());
		}
		
	}
}