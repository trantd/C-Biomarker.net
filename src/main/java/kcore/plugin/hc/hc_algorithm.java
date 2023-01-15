package kcore.plugin.hc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;
//import xml read file
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import kcore.plugin.alg.param.KcoreParameters;
import kcore.plugin.hc_parallel.MapComparator;
import kcore.plugin.rcore.sequence.Edge;

public class hc_algorithm extends AbstractTask {
	private static final Logger logger = LoggerFactory.getLogger(hc_algorithm.class);
	private CyNetwork net;
	private CyTable cyTable;
	private List<CyEdge> listEdge;
	private CyTable cyTableNode;
	private List<CyNode> listNode;

	private KcoreParameters params;
	/// <summary>
	/// Median values of vertices on the network (The median value order is
	/// similar to one of centroid value)
	/// </summary>
	/// <returns>pairs of (node name (key), its median value)</returns>

	// path to input/output file
	private static String INPUT = "1680.txt";
	private static String XML_INPUT = "hsa05216.xml";
	private static String OUTPUT = "1680_hc.txt";
	// param
	public Set<Node> Nodes = new HashSet();
	public Set<String> setV = new HashSet();
	public List<Interaction> Links = new ArrayList<>();
	public Map<String, Double> hcEntropy = new HashMap<String, Double>();
	public int InDegree, OutDegree;
	public List<Interaction> UnLink;
	public int unCount = 0;
	public double hcE;
	// param xml
	public Map<Integer, String> geneDict = new HashMap<Integer, String>();
	public Map<Integer, String> mapDict = new HashMap<Integer, String>();
	public Map<Integer, List<Integer>> groupDict = new HashMap<Integer, List<Integer>>();
	// public double temp = 0.0d, connectivity = 0.0d, closeness = 0.0d;
	
	private String timeStart;
	private String timeEnd;
	private boolean isBio;

	public hc_algorithm(KcoreParameters params, String path, boolean isBio) {
		this.params = params;
		this.OUTPUT = path;
		this.isBio = isBio;
	}

	public hc_algorithm() {

	}

	public void execute() {
		OnComputingHcEntropyFolderCommand();
	}

	public DirectionType getType(List<String> type) {
		DirectionType direction = DirectionType.UNDIRECTED;
		
		if (type == null) {
			return direction;
		}
		
		for (String temp : type) {
			if (temp.contains("activation") || temp.contains("expression") || temp.contains("inhibition")
					|| temp.contains("indirect_effect") || temp.contains("via_compound")
					|| temp.contains("missing_interaction") || temp.contains("phosphorylation")  || temp.contains("1")) {
				direction = DirectionType.DIRECTED;
			} else if (temp.contains("dissociation")) {
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
				
			}else if (type.contains("compound")) {

			} else {
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

			logger.info("list Node: " + cyTableNode.getAllRows().size());
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

	// write file
	public void writeFile(String start, String end) throws Exception {

		Path path = Paths.get(OUTPUT);
		List<String> lines = new ArrayList<>();
		// sort map by value
		Map<String, Double> sortedMap = MapComparator.sortByValue(hcEntropy);
		for (Map.Entry<String, Double> entry : sortedMap.entrySet()) {
			lines.add(String.format("%s\t%.6f", entry.getKey(), entry.getValue()));
		}
//		lines.add(String.format("%s\t%.5f", "HC Entropy", hcE));
		lines.add("time start: " + start + " - " + "time end: " + end);

		Files.write(path, lines);

		Runtime rt = Runtime.getRuntime();
		try {
			Process p = rt.exec("notepad " + path.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

	public static String getOUTPUT() {
		return OUTPUT;
	}
	
	public Map<String, Double> getHcEntropy() {
		return hcEntropy;
	}

	public static void setOUTPUT(String oUTPUT) {
		OUTPUT = oUTPUT;
	}

	private void OnComputingHcEntropyFolderCommand() {
		ReadSignalingNetworkFile();

		// HC entropy
		Map<String, Triple<Double, Double, Double>> trancen = HierarchicalClosenessCentrality();
		Map<String, Double> hc = new HashMap<String, Double>();
		for (String s : trancen.keySet()) {
			hc.put(s, trancen.get(s).getFirst());
		}

		for (Map.Entry<String, Double> entry : hc.entrySet()) {
			hcEntropy.put(entry.getKey(), entry.getValue());
		}

		hcE = EntropyOfNodes(hc);
	}

	public static double EntropyOfNodes(Map<String, Double> pNodeList) {
		Map<Double, ArrayList<String>> group = new HashMap<>();
		Map<Double, Double> temp = new HashMap<Double, Double>();
		double sum = 0;

		for (Map.Entry<String, Double> entry : pNodeList.entrySet()) {
			if (!group.containsKey(entry.getValue())) {
				ArrayList<String> list = new ArrayList<String>();
				list.add(entry.getKey());

				group.put(entry.getValue(), list);
			} else {
				group.get(entry.getValue()).add(entry.getKey());
			}

		}
		int n = pNodeList.keySet().size();

		for (Map.Entry<Double, ArrayList<String>> entry : group.entrySet()) {
			double temps = entry.getValue().size();
			temp.put(entry.getKey(), (double) temps / n);
		}
		// get sum
		for (Map.Entry<Double, Double> entry : temp.entrySet()) {
			double x = log2(entry.getValue());
			// double entryValue = entry.getValue();
			// double temps = Math.log10(entry.getValue());
			// double l2 = Math.log10(2);
			sum = (sum + (entry.getValue() * x));
		}
		return -sum;
	}

	public static double log2(double n) {
		double temp = Math.log(n);
		double l2 = Math.log(2.0);
		return (temp / l2);
		// return (Math.log10(n) / Math.log10(2));
	}

	/**
	 * Compute X entropy of nodes, where X is maybe centrality values of the
	 * nodes
	 * 
	 * @param pNodeList
	 *            The node list with centrality value
	 * @return X entropy of nodes
	 */
	
	/**
	 * Arc degree = in-degree + out-degree + undirected-degree
	 */
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

	/**
	 * Create a adjacent matrix for big graph
	 * 
	 * @param nodeToIndex
	 *            Node index mapping between node name and zero-based index
	 * @param indexToNode
	 *            Node index mapping between node name and zero-based index
	 * @param isForwardLink
	 *            true if link's direction is considered from startNode to
	 *            endNode; false if link's direction is considered from endNode
	 *            to startNode
	 * @param WeightAs1
	 *            if true (default value), the weight =1; else the real weight
	 *            of interactions is used
	 * @return
	 */
	// Overloaded method(s) are created above:
	// ORIGINAL LINE: public Dictionary<int, Dictionary<int, double>>
	// CreateAdjacentList(out Dictionary<string, int> nodeToIndex, out
	// Dictionary<int, string> indexToNode, bool isForwardLink = true)
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

	/**
	 * Create the zero-based index of nodes in the graph
	 * 
	 * @return
	 */
	private final void CreateNodeIndex(Map<String, Integer> nodeToIndex, Map<Integer, String> indexToNode) {
		// nodeToIndex = new HashMap<String, Integer>();
		// indexToNode = new HashMap<Integer, String>();
		int i = 0;
		for (String temp : setV) {
			nodeToIndex.put(temp, i);
			indexToNode.put(i, temp);
			i++;
		}
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

	/**
	 * This novel status is defined by Tran Tien Dzung
	 * 
	 * @param x
	 *            The node index that needs to be calculated
	 * @param adjacentList
	 *            The adjacent list of the network
	 * @param nNode
	 *            The total of network nodes
	 * @return The status of a vertex, generally true with directed and
	 *         undirected network
	 */
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

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		taskMonitor.setProgress(0.0);
		taskMonitor.setStatusMessage("Searching modules ....");

		System.gc();

		taskMonitor.setProgress(0.1);
		taskMonitor.setStatusMessage("Initing parameters ....");

		// taskMonitor.setProgress(0.3);
		// taskMonitor.setStatusMessage("Load data ....");
		// loadData();

		taskMonitor.setProgress(0.4);
		taskMonitor.setStatusMessage("Computing HC ....");
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");//dd/MM/yyyy
	    Date now = new Date();
	    timeStart = sdfDate.format(now);
	    System.out.println("time start: " + timeStart);
		execute();
	    Date now1 = new Date();
	    timeEnd = sdfDate.format(now1);
	    System.out.println("time end: " + timeEnd);
	    
	    if(this.isBio == false) {
	    	taskMonitor.setProgress(0.9);
			taskMonitor.setStatusMessage("Write result....");
			writeFile(timeStart, timeEnd);
	    }
		
		taskMonitor.setProgress(1.0);
		taskMonitor.setStatusMessage("Compute success!");

	}

	@Override
	public void cancel() {
		super.cancel();
		cancelled = true;
//		if (cancelled == true) {
//			JOptionPane.showMessageDialog(null, "Compute HC Success, open text file to see the result!", "Infor",
//					JOptionPane.INFORMATION_MESSAGE);
//		}

	}

}
