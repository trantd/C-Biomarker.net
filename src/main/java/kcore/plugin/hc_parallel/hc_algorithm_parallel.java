package kcore.plugin.hc_parallel;

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
import java.util.Vector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JOptionPane;

public class hc_algorithm_parallel extends AbstractTask {
	private static final Logger logger = LoggerFactory.getLogger(hc_algorithm_parallel.class);
	private static String device;
	private boolean bio;
	private boolean isBio;
	
	public hc_algorithm_parallel(KcoreParameters params, String path, String device, boolean bio, boolean isBio) {
		this.params = params;
		this.outputFile = path;
		hc_algorithm_parallel.device = device;
		this.bio = bio;
		this.isBio = isBio;
	}
	static {
		System.setProperty("com.aparapi.dumpProfilesOnExit", "true");
		System.setProperty("com.aparapi.enableExecutionModeReporting", "false");
		System.setProperty("com.aparapi.enableShowGeneratedOpenCL", "false");
	}
	// path to input/output file
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
		// map to store r-core
		private Map<String, Double> hc;
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

		private Set<String> vertexList;
		private Set<String> visited;

		private static Map<String, Set<String>> reachableList;

		public Map<String, Double> sortedMap;	
		private String timeStart;
		private String timeEnd;

		public Map<String, Double> getHc() {
			return hc;
		}

		public void setHc(Map<String, Double> hc) {
			this.hc = hc;
		}

		public hc_algorithm_parallel() {
			init();
		}
		
		public DirectionType getType(List<String> type) {
			DirectionType direction = DirectionType.UNDIRECTED;
			
			if (type == null) {
				return direction;
			}
			
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
			hc = new HashMap<>();
			adjList = new HashMap<>();
			reachability = new HashMap<>();
			vertexQueue = new PriorityQueue<>();
			vertexList = new HashSet<>();
			visited = new HashSet<>();
			reachableList = new HashMap<>();
			vertList = new ArrayList<>(); 
			closeness = new HashMap<>();

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
				System.setProperty("com.aparapi.executionMode", "JTP");
			}
			else {
				System.setProperty("com.aparapi.executionMode", "GPU");
			}
//			else
//				rc.setExecutionModeWithoutFallback(Kernel.EXECUTION_MODE.GPU);
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
			if(device == "CPU") {
				hcRea.setExecutionMode(Kernel.EXECUTION_MODE.JTP);
			}
			else {
				hcRea.setExecutionMode(Kernel.EXECUTION_MODE.GPU);
			}
			hcRea.setAdjList(adjList);
			hcRea.setVertextList(vertexs);
			
			hcRea.execute(range);
			reachability = hcRea.getReachability();
			reachableList = hcRea.getReachableList();
			hcRea.dispose();
			
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
			
			//compute closeness
//			hcKernel_closeness hcClo = new hcKernel_closeness();
//			hcClo.setVertexList(vertexList);
//			hcClo.setAdjList(adjList);
//			hcClo.setReachableList(reachableList);
//			hcClo.setListVert(vertList);
//			hcClo.execute(range);
//			closeness = hcClo.getCloseness();
//			hcClo.dispose();

			for (Map.Entry<String, Integer> entry : reachability.entrySet()) {
				vertexQueue.add(new Vertex(entry.getKey(), entry.getValue()));
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
			Path path = Paths.get(outputFile);
			List<String> lines = new ArrayList<>();
			// sort map by value
			sortedMap = MapComparator.sortByValue(hc);
			lines.add("Node\tHC");
			for (Map.Entry<String, Double> entry : sortedMap.entrySet()) {
				lines.add(String.format("%s\t%f", entry.getKey(), entry.getValue() + 1));
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
//		public void writeText(String start, String end) {
//			try{
//		        File file =new File(outputFile);
//
//		        if(!file.exists()){
//		           file.createNewFile();
//		        }
//		        List<String> lines = new ArrayList<>();
//		        lines.add("\t\tHC");
//		        for (Map.Entry<String, Double> entry : sortedMap.entrySet()) {
//					lines.add(String.format("\t\t%f", entry.getValue() + 1));
//				}
//		        //Here true is to append the content to file
//		        FileWriter fw = new FileWriter(file.getAbsoluteFile(),true);
//		        //BufferedWriter writer give better performance
//		        BufferedWriter bw = new BufferedWriter(fw);
//		        for (String line : lines) {
//		        	bw.write(line);
//				}
//		        
//		        //Closing BufferedWriter Stream
//		        bw.close();
//		 
//		        System.out.println("Data successfully appended at the end of file");
//		 
//		      }catch(IOException ioe){
//		         System.out.println("Exception occurred:");
//		         ioe.printStackTrace();
//		       }
//		}

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

		// compute
		public void compute() {
//			System.out.println("reachability ss: " + reachability);
//			System.out.println("clossness: " + closeness);
			Set<String> nodeList = reachability.keySet();
			for (String node : nodeList) {
				hc.put(node, (reachability.get(node) + closeness.get(node)));
			}
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
			SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");//dd/MM/yyyy
		    Date now = new Date();
		    timeStart = sdfDate.format(now);
		    System.out.println("time start: " + timeStart);
			loadData();
		    Date now1 = new Date();
		    timeEnd = sdfDate.format(now1);
		    System.out.println("time end: " + timeEnd);
		    
			taskMonitor.setProgress(0.8);
			taskMonitor.setStatusMessage("Computing HC parallel on "+ this.device+ "....");

			compute();
			
			if(isBio == false) {
				taskMonitor.setProgress(0.9);
				taskMonitor.setStatusMessage("Write result....");
				writeTextFile(timeStart, timeEnd);
			}

			taskMonitor.setProgress(1.0);
			taskMonitor.setStatusMessage("Compute success!");

		}

		@Override
		public void cancel() {
			super.cancel();
			cancelled = true;
//			if (cancelled == true) {
//				JOptionPane.showMessageDialog(null, "Compute R-core Success, open text file to see the result!", "Infor",
//						JOptionPane.INFORMATION_MESSAGE);
//			}
		}
}
