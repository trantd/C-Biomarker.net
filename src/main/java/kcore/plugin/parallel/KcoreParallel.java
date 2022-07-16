package kcore.plugin.parallel;

//import com.aparapi.device.Device;
//import com.aparapi.internal.kernel.KernelManager;
//import com.aparapi.internal.kernel.KernelPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import javax.swing.JOptionPane;

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
import kcore.plugin.sequence.Kcore;
import kcore.plugin.service.ServicesUtil;

import com.aparapi.Kernel;
import com.aparapi.Range;

import EDU.oswego.cs.dl.util.concurrent.Barrier;

public class KcoreParallel extends AbstractTask {
	private static final Logger logger = LoggerFactory.getLogger(KcoreParallel.class);
	static {
		System.setProperty("com.aparapi.dumpProfilesOnExit", "true");
		System.setProperty("com.aparapi.enableExecutionModeReporting", "false");
		System.setProperty("com.aparapi.enableShowGeneratedOpenCL", "false");
	}
	private static String device;
	public KcoreParallel(KcoreParameters params, String path, String device) {
		this.params = params;
		this.OUTPUT = path;
		KcoreParallel.device = device;
	}
	private String OUTPUT; 
	private CyNetwork net;
	private CyTable cyTable;
	private CyTable cyTableNode;
	List<CyNode> listNode;
	List<CyEdge> listEdge;

	private KcoreParameters params;
	// list to store edges
	private List<Edge> edgeList;
	// map to store k-core
//	private Map<String, Integer> kCore;
	// map to store adjacency list
	private Map<String, Vector<String>> adjList;
	// map to store degree
	private Map<String, Integer> degrees;
	// vertex queue
	private ArrayList<Vertex> vertexList;
	private ArrayList<String> vertexBuff;
	private int kCore[];
	private AtomicIntegerArray atomickCore;

	private boolean cancelled = false;
	
	private String timeStart;
	private String timeEnd;

	// initialize
	@SuppressWarnings("unchecked")
	public void init() {
		edgeList = new ArrayList<Edge>();
//		kCore = new HashMap<String, Integer>();
		adjList = new HashMap<String, Vector<String>>();
		degrees = new HashMap<String, Integer>();
		vertexList = new ArrayList<Vertex>();
		vertexBuff = new ArrayList<>();
//		kCore = new ArrayList<>();

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
//			kCore.add(entry.getValue().size());
			logger.info("list adj: " + entry.getKey() + " " + entry.getValue().size());
		}

		for (Map.Entry<String, Integer> entry : degrees.entrySet()) {
			vertexList.add(new Vertex(entry.getKey(), entry.getValue()));
		}
		kCore = new int[degrees.size()];
		int i = 0;
		for (Map.Entry<String, Vector<String>> entry : adjList.entrySet()) {
			kCore[i] = entry.getValue().size();
			i++;
		}
		atomickCore = new AtomicIntegerArray(kCore);
	}

	// write result to output.txt
	public void writeFile(String start, String end) throws Exception {
		for(int i = 0; i< vertexList.size(); i++) {
			degrees.replace(vertexList.get(i).getVertex(), atomickCore.get(i));
		}
		// save result	
			Path path = Paths.get(OUTPUT);
			List<String> lines = new ArrayList<>();
			// sort map by value
			Map<String, Integer> sortedMap = MapComparator.sortByValue(degrees);

			lines.add("Node\tKCore");
			for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
				lines.add(String.format("%s\t%d", entry.getKey().toString(), entry.getValue()));
			}
			lines.add("time start: " + start + " - " + "time end: " + end);
			Files.write(path, lines);

//			Files.write(path, lines);

			Runtime rt = Runtime.getRuntime();
			try {
				Process p = rt.exec("notepad " + path.toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	
	}

	// push value to map
	public void pushMap(Map<String, Vector<String>> adjList, String cyNode, String cyNode2) {
		if (!adjList.containsKey(cyNode)) {
			adjList.put(cyNode, new Vector<>());
		}
		adjList.get(cyNode).add(cyNode2);
	}

	@SuppressWarnings("deprecation")
	public void compute() {
		if(device == "CPU") {
			System.setProperty("com.aparapi.executionMode", "JTP");
		}
		else if(device == "GPU") {
			System.setProperty("com.aparapi.executionMode", "GPU");
		}
			
		int i = 0;
		int l = 0;
		Set<String> visitedList = new HashSet<>();
		while (i < vertexList.size()) {
//			System.out.println("core: " + l);
			Range range = Range.create(vertexList.size());
			KCoreKernel kc = new KCoreKernel(degrees);
			kc.setL(l);
			kc.setAdjList(adjList);
//			kc.setDegrees(degrees);
//			kc.setVertexBuff(vertexBuff);
			kc.setAtomickCore(atomickCore);
			kc.setVisitedList(visitedList);
			
			kc.execute(range);
//			degrees = kc.getDegrees();
//			kCore = kc.getkCore();
			atomickCore = kc.getAtomickCore();
			i += kc.getVisitedVertex();
			visitedList = kc.getVisitedList();
			kc.dispose();
			vertexBuff.clear();
			l++;
		}
//		System.out.println("vertex: " + i);
//		while (i < vertexList.size()) {
//			for (int j = 0; j < vertexList.size(); j++) {
//				String vertName = vertexList.get(j).getVertex();
//				if(atomickCore.get(j) == l) {
//					vertexBuff.add(vertName);
//				}
//			}
//			if(vertexBuff.size() > 0) {
//				Range range = Range.create(vertexList.size());
//				KCoreKernel kc = new KCoreKernel(degrees);
//				kc.setL(l);
//				kc.setAdjList(adjList);
////				kc.setDegrees(degrees);
//				kc.setVertexBuff(vertexBuff);
//				kc.setAtomickCore(atomickCore);
//				
//				kc.execute(range);
////				degrees = kc.getDegrees();
////				kCore = kc.getkCore();
//				atomickCore = kc.getAtomickCore();
//				i += kc.getVisitedVertex();
//				kc.dispose();
//				vertexBuff.clear();
//			}
//			l++;
//		}
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
	    timeEnd = sdfDate.format(now1);
	    System.out.println("time end: " + timeEnd);

		taskMonitor.setProgress(0.9);
		taskMonitor.setStatusMessage("Write result....");
		writeFile(timeStart, timeEnd);

		taskMonitor.setProgress(1.0);
		taskMonitor.setStatusMessage("Compute success!");

	}

//	@Override
//	public void cancel() {
//		super.cancel();
//		cancelled = true;
////		if (cancelled == true) {
////		JOptionPane.showMessageDialog(null, "Compute K-core Error, open text file to see the result!",
////				"Infor", JOptionPane.INFORMATION_MESSAGE);
////	}
//	}
}
