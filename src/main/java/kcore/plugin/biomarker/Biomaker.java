package kcore.plugin.biomarker;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import javax.swing.JOptionPane;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kcore.plugin.alg.param.KcoreParameters;
import kcore.plugin.hc.DirectionType;
import kcore.plugin.hc.Interaction;
import kcore.plugin.rcore.sequence.Edge;
import kcore.plugin.service.ServicesUtil;
import kcore.plugin.service.TaskFactory;

public class Biomaker extends AbstractTask {
	public static String OUTPUT;
	// private static final Logger logger =
	// LoggerFactory.getLogger(Biomaker.class);
	private CyNetwork net;
	private CyTable cyTable;
	private List<CyEdge> listEdge;
	private CyTable cyTableNode;
	private List<CyNode> listNode;
	public KcoreParameters params;
	private boolean cancelled = false;
	// param hc
	public Set<Node> Nodes = new HashSet();
	public Set<String> setV = new HashSet();
	public List<Interaction> Links = new ArrayList<>();
	public Map<String, Double> hcEntropy = new HashMap<String, Double>();
	public int InDegree, OutDegree;
	public List<Interaction> UnLink;
	public int unCount = 0;
	// param rcore
	// list to store edges
	private List<Edge> edgeList;
	// map to store r-core
	private Map<String, Integer> rCore;
	// map to store adjacency list
	private Map<String, Vector<String>> adjList;
	// map to store degree
	private Map<String, Integer> reachability;
	// vertex queue
	private PriorityQueue<Vertex> vertexQueue;

	private Set<String> vertexList;
	private Set<String> visited;

	private static Map<String, Set<String>> reachableList;

	public Map<String, Integer> sortedMap;
	
	private String timeStart;
	private String timeEnd;

	// --------------------------------------HC-----------------------------------------
	public void executeHC() {
		OnComputingHcEntropyFolderCommand();

	}

	public DirectionType getType(List<String> type) {
		DirectionType direction = DirectionType.UNDIRECTED;
		for (String temp : type) {
			if (temp.contains("activation") || temp.contains("expression") || temp.contains("inhibition")
					|| temp.contains("indirect_effect") || temp.contains("via_compound")
					|| temp.contains("missing_interaction") || temp.contains("phosphorylation") || temp.contains("1")) {
				direction = DirectionType.DIRECTED;
			} 
			else if (temp.contains("dissociation")) {
				direction = DirectionType.UNDIRECTED;
			}
		}

		return direction;
	}

	public void readFile() throws IOException {
		this.net = params.getCyNetwork();
		cyTable = this.net.getDefaultEdgeTable();
		listEdge = net.getEdgeList();
		cyTableNode = this.net.getDefaultNodeTable();
		listNode = net.getNodeList();

		// neu trong suid hien tai co nhieu hon 1 nut
		for (int i = 0; i < listNode.size(); i++) {
			String subName = cyTableNode.getRow(listNode.get(i).getSUID()).get("name", String.class).trim();
			if (subName.contains("container")) {

			} else {
				List<String> subNameNode = new ArrayList<String>();
				try {
					subNameNode = cyTableNode.getRow(listNode.get(i).getSUID()).get("KEGG_ID", List.class);
				} catch (Exception e) {
					JOptionPane.showMessageDialog(null, "Lỗi convert!", "Error", JOptionPane.ERROR_MESSAGE);
				}

				if(subNameNode != null) {
					for (int j = 0; j < subNameNode.size(); j++) {
						if (j == subNameNode.size() - 1) {

						} else {
							for (int k = j + 1; k < subNameNode.size(); k++) {
								Interaction edge = new Interaction(subNameNode.get(j), subNameNode.get(k), 1);
								edge.setDirection(DirectionType.UNDIRECTED);
								Links.add(edge);

								setV.add(subNameNode.get(j));
								setV.add(subNameNode.get(k));
							}
						}
					}
				}
			}

		}

		for (int i = 0; i < listEdge.size(); i++) {
			// get first edge of edge table
			List<String> type = cyTable.getRow(listEdge.get(i).getSUID()).get("KEGG_EDGE_SUBTYPES", List.class);
			if(type == null) {
				List<String> txtType = cyTable.getRow(listEdge.get(i).getSUID()).get("direction", List.class);
				DirectionType direction = getType(txtType);
				String name = cyTable.getRow(listEdge.get(i).getSUID()).get("name", String.class).trim();
				String[] subName = name.split(" ");
				Interaction edge = new Interaction(subName[0], subName[subName.length - 1], 1);
				edge.setDirection(direction);
				Links.add(edge);

				setV.add(subName[0]);
				setV.add(subName[subName.length - 1]);
				if (direction == DirectionType.UNDIRECTED) {
					unCount++;
				}
				
			}
			else if (type.contains("compound")) {

			} 
			else {
				DirectionType direction = getType(type);

				String name = cyTable.getRow(listEdge.get(i).getSUID()).get("name", String.class).trim();
				String[] subName = name.split(" ");

				ArrayList<String> firstEdge = new ArrayList<>();
				ArrayList<String> secondEdge = new ArrayList<>();
				// set first array edge
				setArHC(firstEdge, secondEdge, subName[0], 1);
				// set second array edge
				setArHC(firstEdge, secondEdge, subName[subName.length - 1], 2);
				// Add edge
				swapHC(firstEdge, secondEdge, direction);

			}

			// logger.info("list Node: " + cyTableNode.getAllRows().size());
		}
	}

	// set Source array node of edge
	@SuppressWarnings("unchecked")
	public void setArHC(ArrayList<String> ar1, ArrayList<String> ar2, String key, int type) {
		for (int k = 0; k < listNode.size(); k++) {
			String subName = cyTableNode.getRow(listNode.get(k).getSUID()).get("name", String.class).trim();
			if (subName.contains("container")) {

			} else {
				String[] subNameArray = subName.split(":");
				// lay ra entryId
				String subItem = subNameArray[subNameArray.length - 1];
				if (subItem.equals(key)) {
					// Neu entryId = edge key thi lay ra danh sach kegg id
					List<String> subNameMain = cyTableNode.getRow(listNode.get(k).getSUID()).get("KEGG_ID", List.class);
					// String nameMain = convertString(temp);
					// get node list of first edge
					// String[] subNameMain = nameMain.split(",");
					if (type == 1) {
						for (int l = 0; l < subNameMain.size(); l++) {
							ar1.add(subNameMain.get(l));
						}
					} else if (type == 2) {
						for (int l = 0; l < subNameMain.size(); l++) {
							ar2.add(subNameMain.get(l));
						}
					}

				}
			}
		}
	}

	// Compare and swap
	public void swapHC(ArrayList<String> ar1, ArrayList<String> ar2, DirectionType direction) {
		for (int i = 0; i < ar1.size(); i++) {
			for (int j = 0; j < ar2.size(); j++) {
				Interaction edge = new Interaction(ar1.get(i), ar2.get(j), 1);
				edge.setDirection(direction);
				Links.add(edge);

				setV.add(ar1.get(i));
				setV.add(ar2.get(j));
				if (direction == DirectionType.UNDIRECTED) {
					unCount++;
					// UnLink.add(edge);
				}
			}
		}
	}

	public void ReadSignalingNetworkFile() {
		try {
			readFile();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		for (Interaction e : Links) {
			// System.out.println(e.getStartNode()+"-"+e.getEndNode());
			Node n1 = new Node();
			n1.setName(e.getStartNode());
			Nodes.add(n1);
			Node n2 = new Node();
			n2.setName(e.getEndNode());
			Nodes.add(n2);

		}

	}

	private void OnComputingHcEntropyFolderCommand() {
		ReadSignalingNetworkFile();

		// HC entropy
		Map<String, Triple<Double, Double, Double>> trancen = HierarchicalClosenessCentrality();
		Map<String, Double> hc = new HashMap<String, Double>();
		for (String s : trancen.keySet()) {
			// hc.put(s, trancen.get(s).getFirst());
			// System.out.println("First: " + trancen.get(s).getFirst() + "
			// Second: " + trancen.get(s).getSecond()
			// + " Third: " + trancen.get(s).getThird());
			hc.put(s, trancen.get(s).getFirst());
		}

		for (Map.Entry<String, Double> entry : hc.entrySet()) {
			hcEntropy.put(entry.getKey(), entry.getValue());
			// System.out.println("Node: "+entry.getKey()+" hc: "+
			// entry.getValue());
		}

		// double hcEntropy = EntropyOfNodes(hc);
		// System.out.println("HC Entropy: " + hcEntropy);
		//
		// // Degree entropy
		// HashMap<String, Double> Totaldegree = new HashMap<String, Double>();
		// for (Node n : Nodes) {
		// Totaldegree.put(n.name, n.TotalDegree);
		//
		// }
		// double degreeEntropy = EntropyOfNodes(Totaldegree);
		// System.out.println("Degree Entropy: " + degreeEntropy);
	}

	public final int getTotalDegree() {
		// return Arcs.Count();
		return InDegree + OutDegree + unCount; // do not use arcs.Count()
												// for Total degree due
												// to avoid self-loop
												// nodes
	}

	public final Map<String, Triple<Double, Double, Double>> HierarchicalClosenessCentrality() {
		Map<String, Integer> nodeToIndex = new HashMap<String, Integer>();
		Map<Integer, String> indexToNode = new HashMap<Integer, String>();

		Map<Integer, HashMap<Integer, Double>> adjacentList = CreateAdjacentList(nodeToIndex, indexToNode, true);
		// for(Map.Entry<Integer, HashMap<Integer, Double>> node :
		// adjacentList.entrySet()){
		// System.out.println("Key: "+ node.getKey()+ " value: ");
		// for(Map.Entry<Integer, Double> temp : node.getValue().entrySet()){
		// System.out.println("Value Key: "+ temp.getKey()+ " sub values: "+
		// temp.getValue());
		// }
		// }
		Map<String, Triple<Double, Double, Double>> dxs = new HashMap<String, Triple<Double, Double, Double>>(),
				infinityList = new HashMap<String, Triple<Double, Double, Double>>();

		double temp = 0.0d, connectivity = 0.0d, closeness = 0.0d;

		for (Map.Entry<String, Integer> node : nodeToIndex.entrySet()) {
			// System.out.println("key: "+node.getKey()+" value:
			// "+node.getValue());
			temp = DirectedStatus(node.getValue(), adjacentList, setV.size(), connectivity, closeness);
			if (Double.isInfinite(temp)) {
				// infinityList.Add(node.Key, float.PositiveInfinity);
				infinityList.put(node.getKey(), new Triple<Double, Double, Double>(0.0d, connectivity, 0.0d));
			} else {
				dxs.put(node.getKey(), new Triple<Double, Double, Double>(temp, connectivity, closeness));
			}

		}

		// Netutil.standardizeMedianValues(dxs);
		for (Map.Entry<String, Triple<Double, Double, Double>> e : infinityList.entrySet()) {
			dxs.put(e.getKey(), e.getValue());
		}

		for (Map.Entry<String, Triple<Double, Double, Double>> s : dxs.entrySet()) {
			// hc.put(s, trancen.get(s).getFirst());
			// System.out.println("hc_key: "+s.getKey()+" hc_value:
			// "+s.getValue().getFirst());
			// hc.put(s, trancen.get(s).getFirst());
		}
		return dxs;

	}

	public final Map<Integer, HashMap<Integer, Double>> CreateAdjacentList(Map<String, Integer> nodeToIndex,
			Map<Integer, String> indexToNode, boolean isForwardLink) {
		// Arcs<Interaction> _arcs = new Arcs<Interaction>();
		List<Interaction> links = new ArrayList<>();
		for (Interaction inter : Links) {
			links.add(inter);
		}

		// get item map
		CreateNodeIndex(nodeToIndex, indexToNode);
		Map<Integer, HashMap<Integer, Double>> Ma = new HashMap<Integer, HashMap<Integer, Double>>();

		if (isForwardLink) {
			for (Interaction inter : links) {
				FillInteractionToAdjacentList(Ma, inter, nodeToIndex.get(inter.getStartNode()),
						nodeToIndex.get(inter.getEndNode()), inter.getWeight());
			}

		} else {
			for (Interaction inter : links) {
				FillInteractionToAdjacentList(Ma, inter, nodeToIndex.get(inter.getEndNode()),
						nodeToIndex.get(inter.getStartNode()), inter.getWeight());
			}
		}

		return Ma;
	}

	private final void CreateNodeIndex(Map<String, Integer> nodeToIndex, Map<Integer, String> indexToNode) {
		// nodeToIndex = new HashMap<String, Integer>();
		// indexToNode = new HashMap<Integer, String>();
		int i = 0;
		for (String temp : setV) {
			nodeToIndex.put(temp, i);
			indexToNode.put(i, temp);
			i++;
		}
		// System.out.print("------------------------------");
		// System.out.print("Node to index: ");
		// for (Map.Entry<String, Integer> entry : nodeToIndex.entrySet()) {
		// System.out.println("Node: "+entry.getKey()+" index:
		// "+entry.getValue()); //nodeToIndex.put(Nodes.get(i).getName(), i);
		// }
		// System.out.print("index to Node: ");
		// for (Map.Entry<Integer, String> entry : indexToNode.entrySet()) {
		// System.out.println("Index: "+entry.getKey()+" node:
		// "+entry.getValue()); //nodeToIndex.put(Nodes.get(i).getName(), i);
		// }
	}

	private void FillInteractionToAdjacentList(Map<Integer, HashMap<Integer, Double>> Ma, Interaction inter, int row,
			int col, double value) {
		if (!Ma.containsKey(row)) {
			Ma.put(row, new HashMap<Integer, Double>());
		}

		Ma.get(row).put(col, !Ma.get(row).containsKey(col) ? value : Ma.get(row).get(col) + value);
		if (inter.getDirection() == DirectionType.UNDIRECTED) {
			if (!Ma.containsKey(col)) {
				Ma.put(col, new HashMap<Integer, Double>());
			}

			Ma.get(col).put(row, !Ma.get(col).containsKey(row) ? value : Ma.get(col).get(row) + value);
		}
	}

	private double DirectedStatus(int x, Map<Integer, HashMap<Integer, Double>> adjacentList, int nNode,
			double connectivity, double closeness) {
		double sumDist = 0;

		Dijkstra dijk = new Dijkstra();

		dijk.FindShortestPathAndDistance(adjacentList, x);
		connectivity = dijk.getDistance().size();
		// step 1
		for (Map.Entry<Integer, Double> entry : dijk.getDistance().entrySet()) {
			// System.out.println("Key: "+entry.getKey()+" value:
			// "+entry.getValue());
			if (entry.getValue() > 0) {
				// System.out.println("value: "+1/entry.getValue());
				sumDist = (sumDist + (1 / entry.getValue()));
				// System.out.println("sum: "+sumDist);
			}
		}

		closeness = sumDist / (nNode - 1);
		// System.out.println("Closeness: "+closeness);
		// The unconnected nodes and the shortest distance between connected
		// nodes have to minimize

		// <------- minumum reaching is 0
		// return (dijk.Distance.Count - 1) + (from p in dijk.Distance where
		// p.Value > 0 select p).Sum(t => 1 / t.Value)/(this.Nodes.Count() - 1);

		// <------- minumum reaching is 1
		return (connectivity + closeness); // dijk.dist.length + sumDist /
											// (this.Nodes.size() - 1);
	}

	// -------------------------------------Rcore----------------------------------------------

	public void executeRcore() {
		init();
		loadData();
		compute();
		sortedMap = MapComparator.sortByValue(rCore);
		// return sortedMap;
	}

	// initialize
	public void init() {
		edgeList = new ArrayList<>();
		rCore = new HashMap<>();
		adjList = new HashMap<>();
		reachability = new HashMap<>();
		vertexQueue = new PriorityQueue<>();
		vertexList = new HashSet<>();
		visited = new HashSet<>();
		reachableList = new HashMap<>();

		this.net = params.getCyNetwork();
		cyTable = this.net.getDefaultEdgeTable();
		listEdge = net.getEdgeList();
		cyTableNode = this.net.getDefaultNodeTable();
		listNode = net.getNodeList();

		// neu trong suid hien tai co nhieu hon 1 nut
//		for (int i = 0; i < listNode.size(); i++) {
//			String subName = cyTableNode.getRow(listNode.get(i).getSUID()).get("name", String.class).trim();
//			if (subName.contains("container")) {
//
//			} else {
//				List<String> subNameNode = new ArrayList<String>();
//				try {
//					subNameNode = cyTableNode.getRow(listNode.get(i).getSUID()).get("KEGG_ID", List.class);
//				} catch (Exception e) {
//					JOptionPane.showMessageDialog(null, "Lỗi convert!", "Error", JOptionPane.ERROR_MESSAGE);
//				}
//
//				for (int j = 0; j < subNameNode.size(); j++) {
//					if (j == subNameNode.size() - 1) {
//
//					} else {
//						for (int k = j + 1; k < subNameNode.size(); k++) {
//							Edge edge = new Edge(subNameNode.get(j), subNameNode.get(k), 0, 1);
//							edgeList.add(edge);
//						}
//					}
//				}
//			}
//
//		}
//
//		for (int i = 0; i < listEdge.size(); i++) {
//			// get first edge of edge table
//			List<String> type = cyTable.getRow(listEdge.get(i).getSUID()).get("KEGG_EDGE_SUBTYPES", List.class);
//			if (type.contains("compound")) {
//
//			} else {
//				DirectionType direction = getType(type);
//				String name = cyTable.getRow(listEdge.get(i).getSUID()).get("name", String.class).trim();
//				String[] subName = name.split(" ");
//
//				ArrayList<String> firstEdge = new ArrayList<>();
//				ArrayList<String> secondEdge = new ArrayList<>();
//				// set first array edge
//				setAr(firstEdge, secondEdge, subName[0], 1);
//				// set second array edge
//				setAr(firstEdge, secondEdge, subName[subName.length - 1], 2);
//				// Add edge
//				swap(firstEdge, secondEdge, direction);
//
//			}
//		}
		for (int i = 0; i < listNode.size(); i++) {
			String subName = cyTableNode.getRow(listNode.get(i).getSUID()).get("name", String.class).trim();
			if (subName.contains("container")) {
				
			} else {
				List<String> subNameNode = new ArrayList<String>();
				try {
					subNameNode = cyTableNode.getRow(listNode.get(i).getSUID()).get("KEGG_ID", List.class);
				} catch (Exception e) {
					JOptionPane.showMessageDialog(null, "Lỗi convert!", "Error", JOptionPane.ERROR_MESSAGE);
				}
				if(subNameNode != null) {
					for (int j = 0; j < subNameNode.size(); j++) {
						if (j == subNameNode.size() - 1) {

						} else {
							for (int k = j + 1; k < subNameNode.size(); k++) {
								Edge edge = new Edge(subNameNode.get(j), subNameNode.get(k), 0, 1);
								edgeList.add(edge);
							}
						}
					}
				}
			}
		}

		for (int i = 0; i < listEdge.size(); i++) {
			// get first edge of edge table
			List<String> type = cyTable.getRow(listEdge.get(i).getSUID()).get("KEGG_EDGE_SUBTYPES", List.class);
			if(type == null) {
				List<String> txtType =  cyTable.getRow(listEdge.get(i).getSUID()).get("direction", List.class);
				DirectionType direction = getType(txtType);
				String name = cyTable.getRow(listEdge.get(i).getSUID()).get("name", String.class).trim();
				String[] subName = name.split(" ");
				
				Edge edge = new Edge(subName[0], subName[subName.length - 1], direction == DirectionType.DIRECTED ? 1:0, 1);
				edgeList.add(edge);
			}else if (type.contains("compound")) {

			} else {
				DirectionType direction = getType(type);
				String name = cyTable.getRow(listEdge.get(i).getSUID()).get("name", String.class).trim();
				String[] subName = name.split(" ");

				ArrayList<String> firstEdge = new ArrayList<>();
				ArrayList<String> secondEdge = new ArrayList<>();
				// set first array edge
				setAr(firstEdge, secondEdge, subName[0], 1);
				// set second array edge
				setAr(firstEdge, secondEdge, subName[subName.length - 1], 2);
				// Add edge
				swap(firstEdge, secondEdge, direction);
			}
		}
		// logger.info("Init OK!");//alert when complete!
	}

	// set Source array node of edge
	@SuppressWarnings("unchecked")
	public void setAr(ArrayList<String> ar1, ArrayList<String> ar2, String key, int type) {
		for (int k = 0; k < listNode.size(); k++) {
			String subName = cyTableNode.getRow(listNode.get(k).getSUID()).get("name", String.class).trim();
			if (subName.contains("container")) {

			} else {
				String[] subNameArray = subName.split(":");
				// lay ra entryId
				String subItem = subNameArray[subNameArray.length - 1];
				if (subItem.equals(key)) {
					// Neu entryId = edge key thi lay ra danh sach kegg id
					List<String> subNameMain = cyTableNode.getRow(listNode.get(k).getSUID()).get("KEGG_ID", List.class);
					// String nameMain = convertString(temp);
					// get node list of first edge
					// String[] subNameMain = nameMain.split(",");
					if (type == 1) {
						for (int l = 0; l < subNameMain.size(); l++) {
							ar1.add(subNameMain.get(l));
						}
					} else if (type == 2) {
						for (int l = 0; l < subNameMain.size(); l++) {
							ar2.add(subNameMain.get(l));
						}
					}

				}
			}
		}
	}

	// Compare and swap
	public void swap(ArrayList<String> ar1, ArrayList<String> ar2, DirectionType direction) {
		for (int i = 0; i < ar1.size(); i++) {
			for (int j = 0; j < ar2.size(); j++) {
				Edge edge = new Edge(ar1.get(i), ar2.get(j), direction == DirectionType.DIRECTED ? 1:0, 1);
				edgeList.add(edge);
			}
		}
	}

	// load data
	public void loadData() {
		for (Edge edge : edgeList) {
			pushMapV(adjList, edge.getStartNode(), edge.getEndNode(), edge.getDirection());
			vertexList.add(edge.getStartNode());
			vertexList.add(edge.getEndNode());
		}

		for (String vertex : vertexList) {
			visited.clear();
			int n = countChildNode(vertex, vertex);
			reachability.put(vertex, n);
		}

		for (Map.Entry<String, Integer> entry : reachability.entrySet()) {
			vertexQueue.add(new Vertex(entry.getKey(), entry.getValue()));
		}
	}

	// push value to map
	public void pushMapV(Map<String, Vector<String>> adjList, String start, String end, int weight) {
		if (!adjList.containsKey(start)) {
			adjList.put(start, new Vector<>());
		}
		adjList.get(start).add(end);
		if (weight == 0) {
			if (!adjList.containsKey(end)) {
				adjList.put(end, new Vector<>());
			}
			adjList.get(end).add(start);
		}
	}

	public void pushMapS(Map<String, Set<String>> adjList, String start, String end) {
		if (!adjList.containsKey(start)) {
			adjList.put(start, new HashSet<>());
		}
		adjList.get(start).add(end);
	}

	public int countChildNode(String node, String source) {
		int count = 0;
		visited.add(node);
		if (adjList.get(node) != null) {
			for (String vertex : adjList.get(node)) {
				if (!visited.contains(vertex)) {
					if (adjList.get(vertex) != null && adjList.get(vertex).size() > 0) {
						count = count + countChildNode(vertex, source);
					}
					count = count + 1;
					visited.add(vertex);
					pushMapS(reachableList, source, vertex);
				}
			}
		}
		return count;
	}

	public int countChildNodeSe(String node) {
		Stack<String> s = new Stack<>();

		int count = 0;

		s.push(node);

		while (!s.isEmpty()) {
			String current = s.pop();

			if (visited.contains(current)) {
				continue;
			}

			visited.add(current);

			if (adjList.get(current) != null) {
				for (String vertex : adjList.get(current)) {
					s.push(vertex);
					if (!visited.contains(vertex)) {
						count = count + 1;
						pushMapS(reachableList, node, vertex);
					}
				}
			}
		}

		return count;
	}

	// compute
	public void compute() {
		int r = 0;
		// BFS traverse
		while (!vertexQueue.isEmpty()) {
			Vertex current = vertexQueue.poll();
			if (reachability.get(current.getVertex()) < current.getDegree()) {
				continue;
			}

			r = Math.max(r, reachability.get(current.getVertex()));

			rCore.put(current.getVertex(), Integer.valueOf(r));
			// sequentially
			if (adjList.get(current.getVertex()) != null && reachability.get(current.getVertex()) > 0) {

				for (String vertex : adjList.get(current.getVertex())) {
					if (!rCore.containsKey(vertex) && reachability.get(vertex) != null) {
						reachability.put(vertex, reachability.get(vertex) - 1);

						for (Map.Entry<String, Set<String>> entry : reachableList.entrySet()) {
							if (entry.getValue().contains(vertex)) {
								reachability.put(entry.getKey(), reachability.get(entry.getKey()) - 1);

								vertexQueue.add(new Vertex(entry.getKey(), reachability.get(entry.getKey())));
							}

						}
						vertexQueue.add(new Vertex(vertex, reachability.get(vertex)));
					}
				}

			} else if (reachability.get(current.getVertex()) == 0) {
				for (Map.Entry<String, Set<String>> entry : reachableList.entrySet()) {
					if (entry.getValue().contains(current.getVertex())) {
						reachability.put(entry.getKey(), reachability.get(entry.getKey()) - 1);
						vertexQueue.add(new Vertex(entry.getKey(), reachability.get(entry.getKey())));
					}
				}
			}

		}
		// for (Map.Entry<String, Integer> entry : reachability.entrySet()) {
		// System.out.println(entry.getKey() + " > " + entry.getValue());
		// }
		System.out.println("R-Core: " + r + 1);
	}

	public List<Edge> getEdgeList() {
		return edgeList;
	}

	public void setEdgeList(List<Edge> edgeList) {
		this.edgeList = edgeList;
	}

	public Set<String> getVertexList() {
		return vertexList;
	}

	public void setVertexList(Set<String> vertexList) {
		this.vertexList = vertexList;
	}

	public Map<String, Integer> getSortedMap() {
		return sortedMap;
	}

	public void setSortedMap(Map<String, Integer> sortedMap) {
		this.sortedMap = sortedMap;
	}

	public Biomaker(KcoreParameters params, String path) {
		this.params = params;
		this.OUTPUT = path;

	}

	// public void execute() {
	// // execute
	// rc_temp rc = new rc_temp(params, OUTPUT);
	//
	// hc_temp hc = new hc_temp(params, OUTPUT);
	// rc.execute();
	// hc.execute();
	//
	// }

	public void writeFile(String start, String end) {
		// write file
		Path path = Paths.get(OUTPUT);
		ArrayList<String> lines = new ArrayList<>();

		lines.add("time start: " + start + " - " + "time end: " + end);
		lines.add("Node\tRCore\tHC");
//		for (String vertex : vertexList) {
//			lines.add(String.format("%s\t%d\t%f", vertex, rCore.get(vertex) + 1, hcEntropy.get(vertex)));
//		}
		int i = 0;
		for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
			if (i >= vertexList.size())
				break;		
			for (Map.Entry<String, Double> entry1 : hcEntropy.entrySet()) {
				if (entry1.getKey().equals(entry.getKey())) {	
					lines.add(String.format("%s\t%d\t%9.8f", entry.getKey(), entry.getValue() + 1, entry1.getValue()));	
				}
			}
			i++;
		}
		try {
			Files.write(path, lines);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Runtime rt = Runtime.getRuntime();
		try {
			Process p = rt.exec("notepad " + path.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(path.toString());
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		taskMonitor.setProgress(0.0);
		taskMonitor.setStatusMessage("Searching modules ....");

		System.gc();

		taskMonitor.setProgress(0.3);
		taskMonitor.setStatusMessage("Computing Rcore ....");
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");//dd/MM/yyyy
	    Date now = new Date();
	    timeStart = sdfDate.format(now);
	    System.out.println("time start: " + timeStart);
		executeRcore();

		taskMonitor.setProgress(0.6);
		taskMonitor.setStatusMessage("Computing HC ....");
		executeHC();
	    Date now1 = new Date();
	    timeEnd = sdfDate.format(now1);
	    System.out.println("time end: " + timeEnd);

		taskMonitor.setProgress(0.8);
		taskMonitor.setStatusMessage("Write result....");
		writeFile(timeStart, timeEnd);
		
		taskMonitor.setProgress(1.0);
		taskMonitor.setStatusMessage("Compute success!");

	}

	@Override
	public void cancel() {
		super.cancel();
		cancelled = true;
//		if (cancelled == true) {
//			JOptionPane.showMessageDialog(null, "Compute Biomarker Success, open text file to see the result!", "Infor",
//					JOptionPane.INFORMATION_MESSAGE);
//		}

	}
}
