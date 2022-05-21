package kcore.plugin.rcore.sequence;

import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kcore.plugin.alg.param.KcoreParameters;
import kcore.plugin.hc.DirectionType;

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
import java.util.Spliterator;
import java.util.Stack;
import java.util.Vector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.swing.JOptionPane;

public class RCore extends AbstractTask {
	// path to input/output file
	private String inputFile;
	private String outputFile;

	private static final Logger logger = LoggerFactory.getLogger(RCore.class);
	private CyNetwork net;
	private CyTable cyTable;
	private List<CyEdge> listEdge;
	private CyTable cyTableNode;
	private List<CyNode> listNode;
	public KcoreParameters params;
	private boolean cancelled = false;

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

	public RCore() {
		init();
	}

	public RCore(KcoreParameters params, String path) {
		this.params = params;
		this.outputFile = path;
		// init();
	}

	public DirectionType getType(List<String> type) {
		DirectionType direction = DirectionType.UNDIRECTED;
		for (String temp : type) {
			if (temp.contains("activation") || temp.contains("expression") || temp.contains("inhibition")
					|| temp.contains("indirect_effect") || temp.contains("via_compound")
					|| temp.contains("missing_interaction") || temp.contains("phosphorylation") || temp.contains("1")) {
				direction = DirectionType.DIRECTED;
			} else if (temp.contains("dissociation")) {
				direction = DirectionType.UNDIRECTED;
			}
		}

		return direction;
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
//								int weight = 
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
				setAr(firstEdge, secondEdge, subName[0], 1);
				// set second array edge
				setAr(firstEdge, secondEdge, subName[subName.length - 1], 2);
				// Add edge
				swap(firstEdge, secondEdge, direction);
			}
		}
		
		logger.info("Init OK!");// alert when complete!
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
			// System.out.println(entry.getKey()+" "+entry.getValue());
			vertexQueue.add(new Vertex(entry.getKey(), entry.getValue()));
		}
	}

	// write result to output.txt
	public void writeTextFile(String start, String end) throws Exception {

		Path path = Paths.get(outputFile);
		List<String> lines = new ArrayList<>();
		// sort map by value
		sortedMap = MapComparator.sortByValue(rCore);
		lines.add("time start: " + start + " - " + "time end: " + end);
		lines.add("Node\tRCore");
		for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
			lines.add(String.format("%s\t%d", entry.getKey(), entry.getValue() + 1));
		}

		Files.write(path, lines);
		Runtime rt = Runtime.getRuntime();
		try {
			Process p = rt.exec("notepad " + path.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
						// System.out.print(count+" ");
					}
					count = count + 1;
					visited.add(vertex);
					pushMapS(reachableList, source, vertex);
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
		System.out.println("R-Core: " + r);
	}

	public String getInputFile() {
		return inputFile;
	}

	public void setInputFile(String inputFile) {
		this.inputFile = inputFile;
	}

	public String getOutputFile() {
		return outputFile;
	}

	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
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

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		taskMonitor.setProgress(0.0);
		taskMonitor.setStatusMessage("Searching modules ....");

		System.gc();

		taskMonitor.setProgress(0.1);
		taskMonitor.setStatusMessage("Initing parameters ....");
		init();

		taskMonitor.setProgress(0.3);
		taskMonitor.setStatusMessage("Load data ....");
		loadData();

		taskMonitor.setProgress(0.4);
		taskMonitor.setStatusMessage("Computing R-core ....");

		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");//dd/MM/yyyy
	    Date now = new Date();
	    timeStart = sdfDate.format(now);
	    System.out.println("time start: " + timeStart);
		compute();
	    Date now1 = new Date();
	    timeEnd = sdfDate.format(now1);
	    System.out.println("time end: " + timeEnd);

		taskMonitor.setProgress(0.9);
		taskMonitor.setStatusMessage("Write result....");
		writeTextFile(timeStart, timeEnd);
		// createColumn();

		taskMonitor.setProgress(1.0);
		taskMonitor.setStatusMessage("Compute success!");

	}

	@Override
	public void cancel() {
		super.cancel();
		cancelled = true;
//		if (cancelled == true) {
//			JOptionPane.showMessageDialog(null, "Compute R-core Success, open text file to see the result!", "Infor",
//					JOptionPane.INFORMATION_MESSAGE);
//		}
	}

}
