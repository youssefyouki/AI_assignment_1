package code;

import java.util.ArrayList;

public abstract class GenericSearchProblem {

    public abstract boolean isGoalState(State state);

    public abstract double[] calculateHeuristic(State state);

}
