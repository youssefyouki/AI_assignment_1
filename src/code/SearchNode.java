package code;

public class SearchNode implements Comparable<SearchNode> {
    private final State state;
    private final SearchNode parent;
    private final Action action;
    
    // Cumulative costs required for goal evaluation and tie-breaking
    private final int pathCostLives;   // Cumulative lives lost
    private final int pathCostEnergy;  // Cumulative energy consumed
    private final int depth;           // Step count (used for Iterative Deepening limits)
    
    // Heuristic values for A* Search
    private double heuristicLivesCost;
    private double heuristicEnergyCost;

    public SearchNode(State state) {
        this.state = state;
        this.parent = null;
        this.action = null;
        this.pathCostLives = 0;
        this.pathCostEnergy = 0;
        this.depth = 0;
        this.heuristicLivesCost = 0;
        this.heuristicEnergyCost = 0;
    }

    public SearchNode(State state, SearchNode parent, Action action, int actionLifeCost, int actionEnergyCost) {
        this.state = state;
        this.parent = parent;
        this.action = action;
        this.pathCostLives = parent.pathCostLives + actionLifeCost;
        this.pathCostEnergy = parent.pathCostEnergy + actionEnergyCost;
        this.depth = parent.depth + 1;
        this.heuristicLivesCost = 0;
        this.heuristicEnergyCost = 0;
    }



    public State getState() { return state; }
    public SearchNode getParent() { return parent; }
    public Action getAction() { return action; }
    public int getPathCostLives() { return pathCostLives; }
    public int getPathCostEnergy() { return pathCostEnergy; }
    public int getDepth() { return depth; }

    public double getHeuristicLivesCost() { return heuristicLivesCost; }
    public double getHeuristicEnergyCost() { return heuristicEnergyCost; }

    public void setHeuristics(double hLives, double hEnergy) {
        this.heuristicLivesCost = hLives;
        this.heuristicEnergyCost = hEnergy;
    }



    @Override
    public int compareTo(SearchNode other) {
        double totalLives1 = this.pathCostLives + this.heuristicLivesCost;
        double totalLives2 = other.pathCostLives + other.heuristicLivesCost;

        if (Double.compare(totalLives1, totalLives2) != 0) {
            return Double.compare(totalLives1, totalLives2);
        }

        double totalEnergy1 = this.pathCostEnergy + this.heuristicEnergyCost;
        double totalEnergy2 = other.pathCostEnergy + other.heuristicEnergyCost;

        return Double.compare(totalEnergy1, totalEnergy2);
    }
}



}
