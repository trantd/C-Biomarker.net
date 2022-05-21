package kcore.plugin.hc;

import java.util.Comparator;

//Class to represent a node in the graph 
class Node implements Comparator<Node> { 
public String name; 
public double degree; 
public double TotalDegree;

public Node() 
{ 
} 

public Node(String node, double cost) 
{ 
   this.name = node; 
   this.degree = cost; 
} 

@Override
public int compare(Node node1, Node node2) 
{ 
   if (node1.degree < node2.degree) 
       return -1; 
   if (node1.degree > node2.degree) 
       return 1; 
   return 0; 
}

public String getName() {
	return name;
}

public void setName(String node) {
	this.name = node;
}

public double getDegree() {
	return degree;
}

public void setDegree(double degree) {
	this.degree = degree;
} 

public double getTotalDegree() {
	return TotalDegree;
}

public void setTotalDegree(double totalDegree) {
	TotalDegree = totalDegree;
}
} 