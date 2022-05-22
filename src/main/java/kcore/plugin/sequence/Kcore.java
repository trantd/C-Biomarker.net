package kcore.plugin.sequence;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
//import java.util.Spliterator;
import java.util.Vector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

//import org.apache.poi.hssf.usermodel.HSSFCell;
//import org.apache.poi.hssf.usermodel.HSSFRow;
//import org.apache.poi.hssf.usermodel.HSSFSheet;
//import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import org.apache.commons.math3.stat.inference.TestUtils;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kcore.plugin.alg.param.KcoreParameters;
import kcore.plugin.alg.param.NetFilteringMethod;
import kcore.plugin.rcore.sequence.Edge;
import kcore.plugin.service.ServicesUtil;

public class Kcore extends AbstractTask {
	// path to input/output file
	// private static final String INPUT = "wikivote.txt";
	private String OUTPUT; // = "D:/Project/docx/cytoscape_data/kcore.txt";
	private static final Logger logger = LoggerFactory.getLogger(Kcore.class);
	private int finalCore;
	private CyNetwork net;
	private CyTable cyTable;
	private CyTable cyTableNode;
	List<CyNode> listNode;
	List<CyEdge> listEdge;

	private KcoreParameters params;
	// list to store edges
	private List<Edge> edgeList;
	// map to store k-core
	private Map<String, Integer> kCore;
	// map to store adjacency list
	private Map<String, Vector<String>> adjList;
	// map to store degree
	private Map<String, Integer> degrees;
	// vertex queue
	private PriorityQueue<Vertex> vertexQueue;

	private boolean cancelled = false;	
	
	private String timeStart;
	private String timeEnd;

	public Kcore(KcoreParameters params, String path) {
		this.params = params;
		this.OUTPUT = path;
	}

	// initialize
	@SuppressWarnings("unchecked")
	public void init() {
		edgeList = new ArrayList<Edge>();
		kCore = new HashMap<String, Integer>();
		adjList = new HashMap<String, Vector<String>>();
		degrees = new HashMap<String, Integer>();
		vertexQueue = new PriorityQueue<Vertex>();

		// init cytoscape network

		this.net = params.getCyNetwork();
		cyTable = this.net.getDefaultEdgeTable();
		cyTableNode = this.net.getDefaultNodeTable();
		listEdge = net.getEdgeList();
		listNode = net.getNodeList();

		// neu trong suid hien tai co nhieu hon 1 nut
		for (int i = 0; i < listNode.size(); i++) {
			String subName = cyTableNode.getRow(listNode.get(i).getSUID()).get("name", String.class).trim();
			if (subName.contains("container")) {
				// continue;
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
								Edge edge = new Edge(subNameNode.get(j), subNameNode.get(k), 1, 1);
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
				String name = cyTable.getRow(listEdge.get(i).getSUID()).get("name", String.class).trim();
				String[] subName = name.split(" ");
				Edge edge = new Edge(subName[0],subName[subName.length - 1], 1, 1);
				edgeList.add(edge);
			
			}
			else if (type.contains("compound")) {

			} 
			else {
				String name = cyTable.getRow(listEdge.get(i).getSUID()).get("name", String.class).trim();
				String[] subName = name.split(" ");
				ArrayList<String> firstEdge = new ArrayList<>();
				ArrayList<String> secondEdge = new ArrayList<>();
				// set first array edge
				setAr(firstEdge, secondEdge, subName[0], 1);
				// set second array edge
				setAr(firstEdge, secondEdge, subName[subName.length - 1], 2);
				// Add edge
				swap(firstEdge, secondEdge);
			}
		}
		logger.info("Init OK!");
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
	public void swap(ArrayList<String> ar1, ArrayList<String> ar2) {
		for (int i = 0; i < ar1.size(); i++) {
			for (int j = 0; j < ar2.size(); j++) {
				Edge edge = new Edge(ar1.get(i), ar2.get(j), 1, 1);
				edgeList.add(edge);
			}
		}
	}

	// load data
	public void loadData() {
		for (Edge edge : edgeList) {
			pushMap(adjList, edge.getStartNode(), edge.getEndNode());
			pushMap(adjList, edge.getEndNode(), edge.getStartNode());

		}

		for (Map.Entry<String, Vector<String>> entry : adjList.entrySet()) {
			degrees.put(entry.getKey(), entry.getValue().size());
			logger.info("list adj: " + entry.getKey() + " " + entry.getValue().size());
		}

		for (Map.Entry<String, Integer> entry : degrees.entrySet()) {
			vertexQueue.add(new Vertex(entry.getKey(), entry.getValue()));
		}

	}

	// write result to output.txt
	public void writeFile(String start, String end) throws Exception {
		// save result	
			Path path = Paths.get(OUTPUT);
			List<String> lines = new ArrayList<>();
			// sort map by value
			Map<String, Integer> sortedMap = MapComparator.sortByValue(kCore);

			lines.add("Node\tKCore");
			for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
				lines.add(String.format("%s\t%d", entry.getKey().toString(), entry.getValue()));
			}
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

	public void createColumn() {
		this.net.getDefaultNodeTable().createColumn("K-core", String.class, false, 0.0);

		List<String> lines = new ArrayList<>();
		// sort map by value
		Map<String, Integer> sortedMap = MapComparator.sortByValue(kCore);

		for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
			lines.add(String.format("%s\t%d", entry.getKey(), entry.getValue()));
		}
		int i = 0;
		for (CyRow row : net.getDefaultNodeTable().getAllRows()) {
			// Long nodeId = row.get(primaryKeyColname, Long.class);
			row.set("K-core", lines.get(i));
			logger.info("list kcore: " + lines.get(i));
			i++;
		}
	}

	public void showResult(TaskMonitor taskMonitor) {
		// TaskIterator layoutingTaskIterator = new TaskIterator();
		taskMonitor.setStatusMessage("Get result... ");
		taskMonitor.setProgress(0.8);

		System.gc();

		taskMonitor.setStatusMessage("Get K-core... ");

		Map<String, Integer> sortedMap = MapComparator.sortByValue(kCore);
		HashMap<String, Integer> hashMap = new HashMap<>();
		for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
			hashMap.put(entry.getKey(), entry.getValue());

		}

		// JOptionPane.showMessageDialog(null, "K-core: " + this.finalCore,
		// "Infor", JOptionPane.INFORMATION_MESSAGE);
		// insertTasksAfterCurrentTask(layoutingTaskIterator);
	}

	// push value to map
	public void pushMap(Map<String, Vector<String>> adjList, String cyNode, String cyNode2) {
		if (!adjList.containsKey(cyNode)) {
			adjList.put(cyNode, new Vector<>());
		}
		adjList.get(cyNode).add(cyNode2);
	}

	public void compute() {
		int k = 0;

		while (vertexQueue.size() != 0) {
			// tries++;
			logger.info("load queue...");
			Vertex current = vertexQueue.poll();

			if (degrees.get(current.getVertex()) < current.getDegree()) {
				continue;
			}
			k = Math.max(k, degrees.get(current.getVertex()));
			kCore.put(current.getVertex(), Integer.valueOf(k));

			for (String vertex : adjList.get(current.getVertex())) {

				if (!kCore.containsKey(vertex)) {
					degrees.put(vertex, degrees.get(vertex) - 1);
					vertexQueue.add(new Vertex(vertex, degrees.get(vertex)));
				}

			}
		}

		this.finalCore = k;
		// createdNets++;
		// insertTasksAfterCurrentTask(layoutingTaskIterator);
		logger.error("K-core: " + k);
		// System.out.println("K-Core: " + k);
		// JOptionPane.showMessageDialog(null, "K-core: " + k, "Infor",
		// JOptionPane.INFORMATION_MESSAGE);
	}

	void popup() {
		cancelled = true;
		// if(cancelled == true){
		JOptionPane.showMessageDialog(null, "Compute K-core Success, open text file to see the result!", "Infor",
				JOptionPane.INFORMATION_MESSAGE);
		// }
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
		taskMonitor.setStatusMessage("Computing K-core ....");

		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");//dd/MM/yyyy
	    Date now = new Date();
	    timeStart = sdfDate.format(now);
	    System.out.println("time start: " + timeStart);
		compute();
	    Date now1 = new Date();
	    timeEnd  = sdfDate.format(now1);
	    System.out.println("time end: " + timeEnd);
//		taskMonitor, 0.4, 0.8

		taskMonitor.setProgress(0.9);
		taskMonitor.setStatusMessage("Write result....");
		writeFile(timeStart, timeEnd);

		// createColumn();
		taskMonitor.setProgress(1.0);
		taskMonitor.setStatusMessage("Compute success!");

	}

	@Override
	public void cancel() {
		super.cancel();
		cancelled = true;
//		if (cancelled == true) {
//			JOptionPane.showMessageDialog(null, "Compute K-core Success, open text file to see the result!", "Infor",
//					JOptionPane.INFORMATION_MESSAGE);
//		}

	}

}
