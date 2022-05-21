package kcore.plugin.biomarker_parallel;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aparapi.Kernel;
import com.aparapi.Range;

import kcore.plugin.alg.param.KcoreParameters;
import kcore.plugin.hc.DirectionType;
import kcore.plugin.hc_parallel.Edge1;
import kcore.plugin.hc_parallel.Vert;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Vector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JOptionPane;

public class Biomarker_algorithm_parallel extends AbstractTask {
	private static final Logger logger = LoggerFactory.getLogger(Biomarker_algorithm_parallel.class);
	static {
		System.setProperty("com.aparapi.executionMode", "JTP");
		System.setProperty("com.aparapi.dumpProfilesOnExit", "true");
		System.setProperty("com.aparapi.enableExecutionModeReporting", "false");
		System.setProperty("com.aparapi.enableShowGeneratedOpenCL", "false");
	}
	private static String device;
	
	public Biomarker_algorithm_parallel(KcoreParameters params, String path, String device) {
		this.params = params;
		this.outputFile = path;
		Biomarker_algorithm_parallel.device = device;
	}
	private String inputFile;
	private String outputFile;

	private CyNetwork net;
	private CyTable cyTable;
	private List<CyEdge> listEdge;
	private CyTable cyTableNode;
	private List<CyNode> listNode;
	public KcoreParameters params;
	private boolean cancelled = false;

	// list to store edges
	private List<Edge> edgeList;
	// map to store hc
	private Map<String, Double> hc;
	// map to store r-core
	private Map<String, Integer> rCore;
	// arraylist to store biomarker
	private ArrayList<BiomarkerResult> biomarkerGene;
	// map to store adjacency list
	private Map<String, Vector<String>> adjList;
	// map to store reachability
	private Map<String, Integer> reachability;
	// array to store vert list
	private ArrayList<Vert> vertList;
	// map to store closeness
	private Map<String, Double> closeness;
	// vertex queue
	private PriorityQueue<Vertex> vertexQueue;
	private ArrayList<String> vertexBuff;

	private Set<String> vertexList;
	private Set<String> visited;

	private static Map<String, Set<String>> reachableList;
	
	private Map<String, Vector<String>> adjListRcore;

	public Map<String, Double> sortedMap;	
	private String timeStart;
	private String timeEnd;
	
	public DirectionType getType(List<String> type) {
		DirectionType direction = DirectionType.UNDIRECTED;
		for (String temp : type) {
			if (temp.contains("activation") || temp.contains("expression") || temp.contains("inhibition")
					|| temp.contains("indirect_effect") || temp.contains("via_compound")
					|| temp.contains("missing_interaction") || temp.contains("phosphorylation") || temp.contains("1")) {
				direction = DirectionType.DIRECTED;
			} 
			else if (temp.contains("dissociation") || temp.contains("binding/association")) {
				direction = DirectionType.UNDIRECTED;
			}
		}

		return direction;
	}
	
	// initialize
	public void init() {
		edgeList = new ArrayList<>();
		hc = new HashMap<>();
		adjList = new HashMap<>();
		reachability = new HashMap<>();
		vertexQueue = new PriorityQueue<>();
		vertexList = new HashSet<>();
		visited = new HashSet<>();
		reachableList = new HashMap<>();
		vertList = new ArrayList<>(); 
		closeness = new HashMap<>();
		rCore = new HashMap<>();
		biomarkerGene = new ArrayList<>();
		vertexBuff = new ArrayList<>();
		adjListRcore = new HashMap<>();

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

	// load data
	@SuppressWarnings("deprecation")
	public void loadData() {
		if(device == "CPU") {
//			hcRea.setExecutionMode(Kernel.EXECUTION_MODE.JTP);
			System.setProperty("com.aparapi.executionMode", "CPU");
		}
		else {
//			hcRea.setExecutionMode(Kernel.EXECUTION_MODE.GPU);
			System.setProperty("com.aparapi.executionMode", "GPU");
		}
		for (Edge edge : edgeList) {
			pushMapV(adjList, edge.getStartNode(), edge.getEndNode(), edge.getDirection());
			vertexList.add(edge.getStartNode());
			vertexList.add(edge.getEndNode());
		}
		
		Range range = Range.create(vertexList.size());
		String[] vertexs = new String[vertexList.size()];
		vertexList.toArray(vertexs);
		
		//compute reachability
		hcKernel_reachability hcRea = new hcKernel_reachability();
		hcRea.setAdjList(adjList);
		hcRea.setVertextList(vertexs);
		
		hcRea.execute(range);
		reachability = hcRea.getReachability();
		reachableList = hcRea.getReachableList();
		hcRea.dispose();
		for (Map.Entry<String, Integer> entry : reachability.entrySet()) {
			vertexQueue.add(new Vertex(entry.getKey(), entry.getValue()));
		}		
//		compute closeness
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
		for (Vert vert : vertList) {
			double path = 0;
			ShortestP(vert);
			Set<String> rea = reachableList.get(vert.getName());
				for (Vert v : vertList) {
					if(rea != null && rea.contains(v.getName())) {
						path += 1/v.getDist();
					}
					v.setDist(Double.MAX_VALUE);
					v.setVisited(false);
				}
			closeness.put(vert.getName(), path/(vertexList.size()-1));
		}
//		compute hc
		Set<String> nodeList = reachability.keySet();
		for (String node : nodeList) {
			hc.put(node, (reachability.get(node) + closeness.get(node)));
		}
		for (String vertex : vertexList) {
			adjListRcore.put(vertex, new Vector<>());
			adjListRcore.get(vertex).add(vertex);
			for (String vert : vertexList) {
				if(reachableList.get(vert) != null && reachableList.get(vert).contains(vertex)) {
					adjListRcore.get(vertex).add(vert);
				}
			}
		} 
		
		//compute r-core
		int i = 0;
		int l = 0;
		while (i < vertexList.size()) {
			for (Vertex vert : vertexQueue) {
				String vertName = vert.getVertex();
				if(reachability.get(vertName) == l) {
					vertexBuff.add(vertName);
				}
			}
			if(vertexBuff.size() > 0) {
				range = Range.create(vertexList.size());
				RCoreKernel rc = new RCoreKernel();
				rc.setL(l);
				rc.setAdjList(adjListRcore);
				rc.setReachableList(reachableList);
				rc.setReachability(reachability);
				rc.setVertexBuff(vertexBuff);
				rc.setVertexList(vertexList);
				
				rc.execute(range);
				reachability = rc.getReachability();
				i += rc.getVisitedVertex();
				rc.dispose();
				vertexBuff.clear();
			}
			l++;
		}
	}
	public void addNeighbour(Vert vert) {
		Vector<String> neighbour  = adjList.get(vert.getName());
		if(neighbour != null) {
			for (Vert v : vertList) {
				if(neighbour.contains(v.getName())) {
					vert.addNeighbour(new Edge1(1, vert, v));
				}
			}
		}
	}

	// write result to output.txt
	public void writeTextFile(String start, String end) throws Exception {
		System.out.println("bio ss");
		Path path = Paths.get(outputFile);
		List<String> lines = new ArrayList<>();
		
		lines.add("Node\tRcore\tHC");
//		for (String vertex : vertexList) {
//			lines.add(String.format("%s\t%d\t%f", vertex, reachability.get(vertex) + 1, hc.get(vertex) + 1));
//		}
		for (BiomarkerResult result : biomarkerGene) {
			lines.add(String.format("%s\t%d\t%f", result.getName(), result.getrCore(), result.getHc()));
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
		System.out.println(path.toString());
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
		for (String vertex : vertexList) {
			BiomarkerResult result = new BiomarkerResult(vertex, reachability.get(vertex) + 1, hc.get(vertex) + 1);
			biomarkerGene.add(result);
		}
		Collections.sort(biomarkerGene, new Comparator<BiomarkerResult>() {
	        @Override
	        public int compare(BiomarkerResult bio1, BiomarkerResult bio2) {
	        	if(bio1.getrCore() < bio2.getrCore()) {
	        		return 1;
	        	}
	        	else if(bio1.getrCore() > bio2.getrCore()) {
	        		return -1;
	        	}
	        	else {
	        		if(bio1.getHc() <= bio2.getHc()) {
	        			return 1;
	        		}
	        		else {
	        			return -1;
	        		}
	        	}
	        }
	    });
//		biomarkerGene.sort((Comparator.comparingInt(bio -> ((BiomarkerResult)bio).getrCore())
//                .thenComparingDouble(bio -> ((BiomarkerResult)bio).getHc())));
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
		taskMonitor.setStatusMessage("Computing Biomarker gene ....");
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");//dd/MM/yyyy
	    Date now = new Date();
	    timeStart = sdfDate.format(now);
	    System.out.println("time start: " + timeStart);
		loadData();
	    Date now1 = new Date();
	    timeEnd = sdfDate.format(now1);
	    System.out.println("time end: " + timeEnd);
	    
		taskMonitor.setProgress(0.8);
		taskMonitor.setStatusMessage("Computing biomarker ....");

		compute();

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
