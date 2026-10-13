package code;

import java.util.*;

public class SearchEngine {

    // -------------------------------------------------------------------------
    // UNIFORM COST SEARCH (UCS)
    // -------------------------------------------------------------------------
    // Explores nodes in order of their cumulative path cost, always expanding
    // the cheapest node seen so far. Guaranteed to find the optimal solution
    // (minimum lives lost, then minimum energy) as long as all action costs
    // are non-negative.
    //
    // Data structures:
    //   frontier  – min-heap (PriorityQueue) ordered by SearchNode.compareTo,
    //               which sorts first by total lives lost, then by total energy.
    //   explored  – HashSet of States already expanded; prevents re-processing
    //               the same state via a more expensive path that arrives later.
    // -------------------------------------------------------------------------
    public static String solveUC(GenericSearchProblem problem, State initialState) {

        PriorityQueue<SearchNode> frontier = new PriorityQueue<>();
        Set<State> explored = new HashSet<>();
        int expandedNodes = 0;

        // Wrap the starting state in a root node (no parent, no action, zero cost)
        frontier.add(new SearchNode(initialState));

        while (!frontier.isEmpty()) {

            // Always pick the lowest-cost node from the frontier
            SearchNode node = frontier.poll();

            // Goal check: if this state satisfies the goal, we found the optimal path
            if (problem.isGoalState(node.getState())) {
                return reconstructSolution(node, expandedNodes);
            }

            // Skip if we already expanded this state via a cheaper route
            if (explored.contains(node.getState())) {
                continue;
            }

            // Mark as expanded and count it
            explored.add(node.getState());
            expandedNodes++;

            // Generate all valid successor (action, nextState) pairs
            for (ArrayList<Object> successor : CaveExplorer.getNextState(node.getState())) {
                Action action    = (Action) successor.get(0);
                State  nextState = (State)  successor.get(1);

                if (!explored.contains(nextState)) {
                    // Derive incremental costs from the state delta:
                    // lives/energy only decrease, so parent - child gives cost >= 0
                    int lifeCost   = node.getState().getLives()  - nextState.getLives();
                    int energyCost = node.getState().getEnergy() - nextState.getEnergy();

                    frontier.add(new SearchNode(nextState, node, action, lifeCost, energyCost));
                }
            }
        }

        // Frontier exhausted with no goal found
        return "No Solution";
    }


    // -------------------------------------------------------------------------
    // ITERATIVE DEEPENING SEARCH (IDS)
    // -------------------------------------------------------------------------
    // Repeatedly runs a depth-limited DFS, increasing the depth limit by 1 each
    // iteration (0, 1, 2, …) until a solution is found. This combines:
    //   - DFS memory efficiency  (only one path in memory at a time)
    //   - BFS completeness       (the shallowest solution is found first)
    //
    // The outer loop never terminates on its own; it relies on the inner DLS
    // returning something other than "CUTOFF" to stop.
    // -------------------------------------------------------------------------
    public static String solveID(GenericSearchProblem problem, State initialState) {

        for (int depthLimit = 0; ; depthLimit++) {

            // Fresh counters and path-tracker for every new depth iteration
            int[]      expandedNodes = {0};
            Set<State> onPath        = new HashSet<>();

            String result = dls(problem, new SearchNode(initialState), depthLimit, expandedNodes, onPath);

            // "CUTOFF" means we hit the limit everywhere — try a deeper limit.
            // Anything else ("No Solution" or an actual plan) is a final answer.
            if (!result.equals("CUTOFF")) {
                return result;
            }
        }
    }

    // -------------------------------------------------------------------------
    // DEPTH-LIMITED SEARCH (DLS) — helper for IDS
    // -------------------------------------------------------------------------
    // A recursive DFS that stops expanding at depth == limit.
    //
    // Return values:
    //   solution string  – goal found; bubble it up immediately
    //   "CUTOFF"         – depth limit was the only reason we stopped;
    //                      the outer IDS loop should try a deeper limit
    //   "No Solution"    – subtree is fully explored with no goal and no cutoff;
    //                      going deeper will not help
    //
    // onPath tracks states on the current root-to-node path only.
    // It is populated on the way down and removed on backtrack, so the same
    // state can still be reached via a completely different branch (unlike a
    // global visited set which would wrongly prune valid deeper paths).
    // -------------------------------------------------------------------------
    private static String dls(GenericSearchProblem problem,
                               SearchNode node,
                               int limit,
                               int[] expanded,
                               Set<State> onPath) {

        // Goal check before anything else — do not count goal node as expanded
        if (problem.isGoalState(node.getState())) {
            return reconstructSolution(node, expanded[0]);
        }

        // Depth limit reached — signal the caller that we stopped due to depth,
        // not because the subtree is exhausted
        if (node.getDepth() >= limit) {
            return "CUTOFF";
        }

        // Add current state to the path and count the expansion
        onPath.add(node.getState());
        expanded[0]++;

        // Whether any child returned CUTOFF (meaning deeper nodes were cut off)
        boolean cutoff = false;

        for (ArrayList<Object> successor : CaveExplorer.getNextState(node.getState())) {
            Action action    = (Action) successor.get(0);
            State  nextState = (State)  successor.get(1);

            // Skip states already on the current path to prevent infinite loops
            if (!onPath.contains(nextState)) {
                int lifeCost   = node.getState().getLives()  - nextState.getLives();
                int energyCost = node.getState().getEnergy() - nextState.getEnergy();

                SearchNode child  = new SearchNode(nextState, node, action, lifeCost, energyCost);
                String     result = dls(problem, child, limit, expanded, onPath);

                if (result.equals("CUTOFF")) {
                    // At least one branch was cut; flag it but keep exploring siblings
                    cutoff = true;
                } else if (!result.equals("No Solution")) {
                    // Found a real solution — propagate it up immediately
                    return result;
                }
            }
        }

        // Backtrack: remove current state so other branches can visit it
        onPath.remove(node.getState());

        // If any branch was cut, report CUTOFF so IDS knows to go deeper.
        // Otherwise the subtree is truly exhausted.
        return cutoff ? "CUTOFF" : "No Solution";
    }


    // -------------------------------------------------------------------------
    // A* SEARCH
    // -------------------------------------------------------------------------
    // Like UCS, but each node's priority is f(n) = g(n) + h(n), where:
    //   g(n) – actual cumulative cost from start (lives lost, energy used)
    //   h(n) – admissible heuristic estimate of remaining cost to goal
    //
    // Because h is admissible (never overestimates), A* is guaranteed to find
    // the same optimal solution as UCS while expanding far fewer nodes.
    //
    // The heuristic is computed by the problem itself (calculateHeuristic),
    // keeping the search algorithm generic. SearchNode.compareTo already
    // ranks by (g + h) on lives first, then energy, so no extra comparator
    // is needed here.
    // -------------------------------------------------------------------------
    public static String solveAStar(GenericSearchProblem problem, State initialState) {

        PriorityQueue<SearchNode> frontier = new PriorityQueue<>();
        Set<State> explored = new HashSet<>();
        int expandedNodes = 0;

        // Root node: compute its heuristic and enqueue
        SearchNode root    = new SearchNode(initialState);
        double[]   hRoot   = problem.calculateHeuristic(initialState);
        root.setHeuristics(hRoot[0], hRoot[1]);   // h[0]=lives, h[1]=energy
        frontier.add(root);

        while (!frontier.isEmpty()) {

            // Pop the node with the lowest f = g + h
            SearchNode node = frontier.poll();

            // Goal check: since h is admissible, the first time we pop the goal
            // it is guaranteed to be via the optimal path
            if (problem.isGoalState(node.getState())) {
                return reconstructSolution(node, expandedNodes);
            }

            // Skip duplicate entries (a state may be inserted multiple times
            // with different costs before it is first expanded)
            if (explored.contains(node.getState())) {
                continue;
            }

            explored.add(node.getState());
            expandedNodes++;

            for (ArrayList<Object> successor : CaveExplorer.getNextState(node.getState())) {
                Action action    = (Action) successor.get(0);
                State  nextState = (State)  successor.get(1);

                if (!explored.contains(nextState)) {
                    int lifeCost   = node.getState().getLives()  - nextState.getLives();
                    int energyCost = node.getState().getEnergy() - nextState.getEnergy();

                    SearchNode child  = new SearchNode(nextState, node, action, lifeCost, energyCost);

                    // Attach heuristic estimates so the priority queue can rank correctly
                    double[] hChild = problem.calculateHeuristic(nextState);
                    child.setHeuristics(hChild[0], hChild[1]);

                    frontier.add(child);
                }
            }
        }

        return "No Solution";
    }


    // -------------------------------------------------------------------------
    // RECONSTRUCT SOLUTION
    // -------------------------------------------------------------------------
    // Walks the parent chain from the goal node back to the root to recover
    // the sequence of actions taken, then formats the result string.
    //
    // Output format:  plan;lives;energy;nodes
    //   plan   – comma-separated action names in order (e.g. "left,climbup,right")
    //   lives  – total lives lost along the path
    //   energy – total energy consumed along the path
    //   nodes  – total number of nodes expanded during the search
    // -------------------------------------------------------------------------
    public static String reconstructSolution(SearchNode goalNode, int expandedNodes) {
        if (goalNode == null) return "No Solution";

        // Walk from goal to root, prepending each action to preserve order
        LinkedList<String> actions = new LinkedList<>();
        SearchNode current = goalNode;

        while (current.getParent() != null) {
            actions.addFirst(current.getAction().toString());
            current = current.getParent();
        }

        String plan   = String.join(",", actions);
        int    lives  = goalNode.getPathCostLives();
        int    energy = goalNode.getPathCostEnergy();

        return plan + ";" + lives + ";" + energy + ";" + expandedNodes;
    }
}
