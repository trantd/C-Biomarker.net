package kcore.plugin.hc;

public class Interaction {
    private String startNode;
    private String endNode;
    private int weight;
    public DirectionType Direction; 

	public Interaction(String startNode, String endNode, int weight) {
        this.startNode = startNode;
        this.endNode = endNode;
        this.weight = weight;
    }
    
    public Interaction(String startNode, String endNode) {
        this.startNode = startNode;
        this.endNode = endNode;
        this.weight = 1;
    }
    public Interaction(){
    	
    }
    
    public DirectionType getDirection() {
		return Direction;
	}

	public void setDirection(DirectionType direction) {
		Direction = direction;
	}

    public String getStartNode() {
        return startNode;
    }

    public void setStartNode(String startNode) {
        this.startNode = startNode;
    }

    public String getEndNode() {
        return endNode;
    }

    public void setEndNode(String endNode) {
        this.endNode = endNode;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }
}

