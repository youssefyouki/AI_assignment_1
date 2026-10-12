package code;

import java.util.ArrayList;

/**
 * The contract between the game rules (CaveExplorer) and the
 * search algorithms (SearchEngine). The search engine only talks
 * to the game through these three methods.
 */
public abstract class GenericSearchProblem {

    // True when the state is a win (all doors unlocked, at least 1 life).
    public abstract boolean isGoalState(State current);

    // Every legal move from this state. Each entry is [Action, State].
    public abstract ArrayList<ArrayList<Object>> getNextState(State current);

    // Heuristic for A*: {estimated lives still to lose, estimated energy still to spend}.
    public abstract double[] calculateHeuristic(State state);
}