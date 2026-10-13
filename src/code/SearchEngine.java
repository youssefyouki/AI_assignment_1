package code;

import java.util.*;

public class SearchEngine {

    public static String solveUC(GenericSearchProblem problem, State initialState) {
        PriorityQueue<SearchNode> frontier = new PriorityQueue<>();
        Set<State> explored = new HashSet<>();
        int expandedNodes = 0;

        frontier.add(new SearchNode(initialState));

        while (!frontier.isEmpty()) {
            SearchNode node = frontier.poll();

            if (problem.isGoalState(node.getState())) {
                return reconstructSolution(node, expandedNodes);
            }

            if (explored.contains(node.getState())) {
                continue;
            }
            explored.add(node.getState());
            expandedNodes++;

            for (ArrayList<Object> successor : CaveExplorer.getNextState(node.getState())) {
                Action action = (Action) successor.get(0);
                State nextState = (State) successor.get(1);

                if (!explored.contains(nextState)) {
                    int lifeCost = node.getState().getLives() - nextState.getLives();
                    int energyCost = node.getState().getEnergy() - nextState.getEnergy();
                    frontier.add(new SearchNode(nextState, node, action, lifeCost, energyCost));
                }
            }
        }

        return "No Solution";
    }

    public static String solveID(GenericSearchProblem problem, State initialState) {
        for (int depthLimit = 0; ; depthLimit++) {
            int[] expandedNodes = {0};
            String result = dls(problem, new SearchNode(initialState), depthLimit, expandedNodes, new HashSet<>());
            if (!result.equals("CUTOFF")) {
                return result;
            }
        }
    }

    private static String dls(GenericSearchProblem problem, SearchNode node, int limit, int[] expanded, Set<State> onPath) {
        if (problem.isGoalState(node.getState())) {
            return reconstructSolution(node, expanded[0]);
        }
        if (node.getDepth() >= limit) {
            return "CUTOFF";
        }

        onPath.add(node.getState());
        expanded[0]++;
        boolean cutoff = false;

        for (ArrayList<Object> successor : CaveExplorer.getNextState(node.getState())) {
            Action action = (Action) successor.get(0);
            State nextState = (State) successor.get(1);

            if (!onPath.contains(nextState)) {
                int lifeCost = node.getState().getLives() - nextState.getLives();
                int energyCost = node.getState().getEnergy() - nextState.getEnergy();
                SearchNode child = new SearchNode(nextState, node, action, lifeCost, energyCost);
                String result = dls(problem, child, limit, expanded, onPath);
                if (result.equals("CUTOFF")) {
                    cutoff = true;
                } else if (!result.equals("No Solution")) {
                    return result;
                }
            }
        }

        onPath.remove(node.getState());
        return cutoff ? "CUTOFF" : "No Solution";
    }

    public static String solveAStar(GenericSearchProblem problem, State initialState) {
        // Implement A* Search algorithm
        // This is a placeholder for the actual A* implementation
        return "";
    }

    public static String reconstructSolution(SearchNode goalNode, int expandedNodes) {
        if (goalNode == null) return "No Solution";

        LinkedList<String> actions = new LinkedList<>();
        SearchNode current = goalNode;

        while (current.getParent() != null) {
            actions.addFirst(current.getAction().toString());
            current = current.getParent();
        }

        String plan = String.join(",", actions);
        int lives = goalNode.getPathCostLives();
        int energy = goalNode.getPathCostEnergy();

        return plan + ";" + lives + ";" + energy + ";" + expandedNodes;
    }


}
