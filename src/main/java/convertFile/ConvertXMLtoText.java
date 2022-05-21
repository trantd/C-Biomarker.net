package convertFile;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import org.w3c.dom.Node;
import org.w3c.dom.Element;

public class ConvertXMLtoText{
	private static List<Edge> edgeList = new ArrayList<Edge>();
	private static File inputFile = new File("D:\\Thuc Tap\\Tài Liệu\\hsa05219.xml");
	private static String outputFile;
	
	public ConvertXMLtoText(String outputFile) {
		this.outputFile = outputFile;
	}

	public ConvertXMLtoText() {
		super();
	}

	public List<Edge> getEdgeList() {
		return edgeList;
	}

	public void setEdgeList(List<Edge> edgeList) {
		this.edgeList = edgeList;
	}

	public File getInputFile() {
		return inputFile;
	}

	public void setInputFile(File inputFile) {
		this.inputFile = inputFile;
	}

	public String getOutputFile() {
		return outputFile;
	}

	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}

	public static DirectionType getType(String type) {
		DirectionType direction = DirectionType.UNDIRECTED;
			if (type.equals("-->") || type.equals("--|") || type.equals("..>")
					|| type.equals("-o->") || type.equals("-/-")
					|| type.equals("+p") || type.equals("phosphorylation")) {
				direction = DirectionType.DIRECTED;
			} 
			else if (type.equals("---") || type.equals("-+-")) {
				direction = DirectionType.UNDIRECTED;
			}

		return direction;
	}
	
	public static void ConvertKeggXML() {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		    Document doc = dBuilder.parse(inputFile);
		    doc.getDocumentElement().normalize();
            NodeList entryLink = doc.getElementsByTagName("entry link");
            System.out.println("----------------------------");
            for (int temp = 0; temp < entryLink.getLength(); temp++) {
                Node nNode = entryLink.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    String[] subNameNode = eElement.getAttribute("name").split(" ");
                    if(subNameNode != null) {
    					for (int j = 0; j < subNameNode.length; j++) {
    						if (j == subNameNode.length - 1) {

    						} else {
    							for (int k = j + 1; k < subNameNode.length; k++) {
    								Edge edge = new Edge(subNameNode[j], subNameNode[k], 0, 1);
    								edgeList.add(edge);
    							}
    						}
    					}
    				}
                }
            }
            
            NodeList relation = doc.getElementsByTagName("relation");
            for (int temp = 0; temp < relation.getLength(); temp++) {
				Node nNode = relation.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					Node subType = eElement.getElementsByTagName("subtype").item(0);
					Element eSubType = (Element) subType;
					String type = eSubType.getAttribute("value");
					DirectionType direction = getType(type);
					String entry2 = eElement.getAttribute("entry2");
					String entry1 = eElement.getAttribute("entry1");
					ArrayList<String> firstEdge = new ArrayList<>();
	 				ArrayList<String> secondEdge = new ArrayList<>();
	 				// set first array edge
	 				setAr(firstEdge, secondEdge, entry2, 1, doc);
	 				// set second array edge
	 				setAr(firstEdge, secondEdge, entry1, 2, doc);
	 				// Add edge
	 				swap(firstEdge, secondEdge, direction);
            	}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	public static void setAr(ArrayList<String> ar1, ArrayList<String> ar2, String key, int type, Document doc) {
		NodeList entryName = doc.getElementsByTagName("entry link");
		for (int temp = 0; temp < entryName.getLength(); temp++) {
			Node nNode = entryName.item(temp);
			 if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				 Element eElement = (Element) nNode;
				 String entryId = eElement.getAttribute("id");
				 if (entryId.equals(key)) {
						// Neu entryId = edge key thi lay ra danh sach kegg id
						String[] subNameMain = eElement.getAttribute("name").split(" ");
						// String nameMain = convertString(temp);
						// get node list of first edge
						// String[] subNameMain = nameMain.split(",");
						if (type == 1) {
							for (int l = 0; l < subNameMain.length; l++) {
								ar1.add(subNameMain[l]);
							}
						} else if (type == 2) {
							for (int l = 0; l < subNameMain.length; l++) {
								ar2.add(subNameMain[l]);
							}
						}

					}
			 }
		}
	}

	// Compare and swap
	public static void swap(ArrayList<String> ar1, ArrayList<String> ar2, DirectionType direction) {
		for (int i = 0; i < ar1.size(); i++) {
			for (int j = 0; j < ar2.size(); j++) {
				Edge edge = new Edge(ar1.get(i), ar2.get(j), direction == DirectionType.DIRECTED ? 1:0, 1);
				edgeList.add(edge);
			}
		}
	}
	
	public static void writeFile(String output) {
		Path path = Paths.get(output);
		List<String> lines = new ArrayList<>();
		for (Edge edge : edgeList) {
			lines.add(String.format("%s\t%s\t%d", edge.getStartNode(), edge.getEndNode(), edge.getDirection()));
		}
		try {
			Files.write(path, lines);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public static void main(String[] args) {
		ConvertKeggXML();
	}
}
