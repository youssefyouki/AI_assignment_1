package code;

import java.util.ArrayList;

public class CaveExplorer extends GenericSearchProblem {

    // Shared static map data
    public static int width;
    public static int height;
    public static int[][] grid;          // grid[y][x] = difficulty (0 = wall)
    public static int[][] doorLocations; // [[x1, y1], [x2, y2], ...]
    public static int[][] keyLocations;  // [[x1, y1], [x2, y2], ...]


    public static String solve(String initString, String strategy) {
        State initialState = parseInput(initString);

        return "";

    }

    private static State parseInput(String caveSystem) {
        // Parse the input string and initialize the static map data
        // This is a placeholder for the actual parsing logic
        //saves values and creates initial state
    return null;
    }

    public boolean isGoalState(State current){
        // Check if the current state is a goal state
        // This is a placeholder for the actual goal-checking logic
        return false;
    }


    public static ArrayList<ArrayList<Object>> getNextState(State current) {
        ArrayList<ArrayList<Object>> successors = new ArrayList<>();

        // Example: Checking move LEFT[cite: 4]
        if (canMoveLeft(current)) {
            State nextState = applyLeft(current); // generates brand new State object
            
            ArrayList<Object> pair = new ArrayList<>();
            pair.add(Action.LEFT);
            pair.add(nextState);
            
            successors.add(pair);
        }

        // Example: Checking move CLIMBUP (requires rope > 0)[cite: 5]
        if (canClimbUp(current)) {
            State nextState = applyClimbUp(current);
            
            ArrayList<Object> pair = new ArrayList<>();
            pair.add(Action.CLIMBUP);
            pair.add(nextState);
            
            successors.add(pair);
        }

        // Repeat for RIGHT, CLIMBDOWN, JUMPDOWN, COLLECT, UNLOCK...

        return successors;
    }

    @Override
    public double[] calculateHeuristic(State state) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'calculateHeuristic'");
    }

}