package kcore.plugin.alg;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import org.w3c.dom.Node;
import org.w3c.dom.Element;

public class ConvertKegg {
	Map<Integer, String> geneDict = new HashMap<>();
    Map<Integer, String> mapDict = new HashMap<>();
    Map<Integer, List<Integer>> groupDict = new HashMap<>();
    private static List<Edge> edgeList = new ArrayList<Edge>();
	private static File inputFile;
	
	public static List<Edge> getEdgeList() {
		return edgeList;
	}

	public static void setEdgeList(List<Edge> edgeList) {
		ConvertKegg.edgeList = edgeList;
	}

	public boolean hasEdge(String start, String end) {
		for (Edge edge : edgeList) {
			if(edge.getStartNode().equals(start) && edge.getEndNode().equals(end)) {
				return true;
			}
		}
		return false;
	}
	
    public void ConvertXMLtoText(String input) {
    	try {
    		System.out.println(edgeList.size());
    		inputFile = new File(input);
    		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    	    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    	    Document doc = dBuilder.parse(inputFile);
    	    doc.getDocumentElement().normalize();
    	    Node root = doc.getElementsByTagName("pathway").item(0);
    	    Element rootElement = (Element) root;
    	    NodeList nodeList = rootElement.getChildNodes();
    	    
    	    for(int i = 0; i < nodeList.getLength(); i++) {
    	    	Node node = nodeList.item(i);
    	    	if (node.getNodeType() == Node.ELEMENT_NODE) {
    	    		Element eElement = (Element) node;
//        	    	String text = node.getTextContent();
        	    	switch(eElement.getNodeName()) {
        	    		case "entry":
    	    	    		switch(eElement.getAttribute("type")) {
    	    	    		case "gene":// the node as a single gene or multiple genes (homologous genes) in the name
    	                        geneDict.put(Integer.parseInt(eElement.getAttribute("id")), eElement.getAttribute("name"));
    	                        break;
    	                    case "map":// the node as a pathway
    	                        mapDict.put(Integer.parseInt(eElement.getAttribute("id")), eElement.getAttribute("name"));
    	                        break;
    	                    case "group":// the node as a group of nodes indicated in this children with Name = component
    	                        int id = Integer.parseInt(eElement.getAttribute("id"));
    	                        groupDict.put(id, new ArrayList<Integer>());
    	                        NodeList childNodes = node.getChildNodes();
    	                        for (int j = 0; j < childNodes.getLength(); j++)
    	                            if (childNodes.item(j).getNodeName() == "component") {
    	                            	Element child = (Element) childNodes.item(j);
    	                                groupDict.get(id).add(Integer.parseInt(child.getAttribute("id")));
    	                            }
    	                        break;
    	    	    		}
        	    		case "relation":
        	    			int iStart=0, iEnd=0;
        	    			try{
            	    			 iStart = Integer.parseInt(eElement.getAttribute("entry1"));
            	    			 iEnd = Integer.parseInt(eElement.getAttribute("entry2"));
        	    			} catch(NumberFormatException ex){ // handle your exception
        	    			    
        	    			}
        	    			if(node.hasChildNodes()) {
        	    				NodeList childNodes = node.getChildNodes();
        	    				for(int j = 0; j < childNodes.getLength(); j++) {
        	    					int type = 0;
        	    					DirectionType direction = DirectionType.UNDIRECTED;
        	    					if (childNodes.item(j).getNodeType() == Node.ELEMENT_NODE) {
        	    						Element child = (Element) childNodes.item(j);
            	    					switch(child.getAttribute("value")) {
        	    	    					case "-->": //activation, expression
        	                                    type = 1;
        	                                    direction = DirectionType.DIRECTED;
        	                                    break;
        	                                case "--|": //inhibition
        	                                    type = -1;
        	                                    direction = DirectionType.DIRECTED;
        	                                    break;
        	                                case "..>": //indirect_effect
        	                                    type = 2;
        	                                    direction = DirectionType.DIRECTED;
        	                                    break;
        	                                case "-o->": //via_compound
        	                                    type = 3;
        	                                    direction = DirectionType.DIRECTED;
        	                                    break;
        	                                case "-/-"://missing_interaction
        	                                    type = 4;
        	                                    direction = DirectionType.DIRECTED;
        	                                    break;
        	                                case "+p": //phosphorylation
        	                                    type = 5;
        	                                    direction = DirectionType.DIRECTED;
        	                                    break;
        	                                case "---": //PPIs_in_complex, binding/association
        	                                case "-+-"://dissociation
        	                                    type = 0;
        	                                    direction = DirectionType.UNDIRECTED;
        	                                    break;
        	                                default:
        	                                    //User.One.SendErrorToUser(new Exception("Network work type " + subtype.Attributes["value"].Value + ";" + subtype.Attributes["name"].Value + " is new"));
        	                                    break;
            	    					}
            	    					String[] nStarts = null, nEnds = null;
            	    					if(geneDict.containsKey(iStart)) {
            	    						String[] geneids = geneDict.get(iStart).split(" ");
            	    						nStarts = new String[geneids.length];
            	    						for (int k = 0; k < geneids.length; k++) {
            	    							nStarts[k] = geneids[k];
            	    						}
            	    					}
            	    					else if (groupDict.containsKey(iStart))
                                        {
                                            nStarts = new String[groupDict.get(iStart).size()];
                                            for (int k = 0; k < groupDict.get(iStart).size(); k++)
                                                nStarts[k] = geneDict.get(groupDict.get(iStart).get(k));
                                            //Each group is a multiple geneid

                                        }
                                        else
                                            continue;

                                        if (geneDict.containsKey(iEnd))
                                        {
                                            String[] geneids = geneDict.get(iEnd).split(" ");
                                            nEnds = new String[geneids.length];
                                            for (int k = 0; k < geneids.length; k++)
                                                nEnds[k] = geneids[k];
                                        }
                                        else if (groupDict.containsKey(iEnd))
                                        {
                                        	nEnds = new String[groupDict.get(iEnd).size()];
                                            for (int k = 0; k < groupDict.get(iEnd).size(); k++)
                                            	nEnds[k] = geneDict.get(groupDict.get(iEnd).get(k));
                                        }
                                        else
                                            continue;
                                        
                                      //PPi conneections between a multiple gene node (homologous genes)
                                        for (int k = 0; k < nStarts.length - 1; k++)
                                            for (int l = k + 1; l < nStarts.length; l++)
                                                if (!hasEdge(nStarts[k], nStarts[l]))
                                                {
                                                	edgeList.add(new Edge(nStarts[k], nStarts[l], 0, 1));
                                                }
                                        //PPi conneections between a multiple gene node (homologous genes)
                                        for (int k = 0; k < nEnds.length - 1; k++)
                                            for (int l = k + 1; l < nEnds.length; l++)
                                                if (!hasEdge(nEnds[k], nEnds[l]))
                                                {
                                                	edgeList.add(new Edge(nEnds[k], nEnds[l], 0, 1));
                                                }
                                         
                                        //Conneections between {s1, s2, ... sn} -> {e1, e2, ... en} 
                                        // decomposed into s1 -> e1, s1 -> e2, ... s1 -> en,
                                        //                  s2 -> e1, s2 -> e2, ... s2 -> en,
                                        //                  sn -> e1, sn -> e2, ... sn -> en,
                                        for (int k = 0; k < nStarts.length; k++)
                                            for (int l = 0; l < nEnds.length; l++)
                                        	 if (!hasEdge(nStarts[k], nEnds[l]))
                                            {
                                            	int dir = direction == DirectionType.DIRECTED ? 1:0; 
                                            	edgeList.add(new Edge(nStarts[k], nEnds[l], dir, 1));
                                            }
        	    					}
        	    				}
        	    			}//end if
        	    		break;
        	    	}
    	    	}
    	    }
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }
    public void writeFile(String output) {
		Path path = Paths.get(output);
		List<String> lines = new ArrayList<>();
		lines.add("Start\tEnd\tDirection");
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
}
