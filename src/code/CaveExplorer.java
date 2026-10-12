package code;

import java.util.ArrayList;

public class CaveExplorer extends GenericSearchProblem {

    // Shared static map data (filled in by parseInput on every solve call)
    public static int width;
    public static int height;
    public static int[][] grid;          // grid[y][x] = difficulty (0 = wall)
    public static int[][] doorLocations; // [[x1, y1], [x2, y2], ...]
    public static int[][] keyLocations;  // [[x1, y1], [x2, y2], ...]

    public static String solve(String initString, String strategy) {
        State initialState = parseInput(initString);
        CaveExplorer problem = new CaveExplorer();

        // TODO (later): hand problem + initialState to the SearchEngine
        return "";
    }

    /**
     * Reads the input string, saves the map into the static fields,
     * and returns the starting State.
     */
    private static State parseInput(String caveSystem) {
        // Cut the string into sections at every ';'
        String[] parts = caveSystem.split(";");

        // Section 0: "H,W" -> number of rows, then number of columns
        int[] size = toInts(parts[0]);
        height = size[0];
        width = size[1];

        // Section 1: "X,Y" -> where the explorer starts
        int[] start = toInts(parts[1]);
        int startX = start[0];
        int startY = start[1];

        // Section 2: "EN,RL" -> starting energy and rope length
        int[] resources = toInts(parts[2]);
        int startEnergy = resources[0];
        int startRope = resources[1];

        // Next H sections: one grid row each (0 = wall, otherwise energy cost)
        grid = new int[height][width];
        for (int y = 0; y < height; y++) {
            int[] row = toInts(parts[3 + y]);
            for (int x = 0; x < width; x++) {
                grid[y][x] = row[x];
            }
        }

        // Next section: the doors, then the keys (each is a list of x,y pairs).
        // The length check protects us if a section is missing.
        doorLocations = toPairs(parts.length > 3 + height ? parts[3 + height] : "");
        keyLocations = toPairs(parts.length > 4 + height ? parts[4 + height] : "");

        // Nothing is unlocked or collected yet (new boolean[] starts all false)
        boolean[] doorsUnlocked = new boolean[doorLocations.length];
        boolean[] keysCollected = new boolean[keyLocations.length];

        // The explorer always starts with 3 lives and no key in hand
        return new State(startX, startY, startEnergy, startRope, 3, false,
                         doorsUnlocked, keysCollected);
    }

    // Turns a text like "4,12,0,0" into the numbers {4, 12, 0, 0}
    private static int[] toInts(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return new int[0];
        }
        String[] pieces = trimmed.split(",");
        int[] numbers = new int[pieces.length];
        for (int i = 0; i < pieces.length; i++) {
            numbers[i] = Integer.parseInt(pieces[i].trim());
        }
        return numbers;
    }

    // Turns a text like "3,2,1,0" into the pairs {{3, 2}, {1, 0}}
    private static int[][] toPairs(String text) {
        int[] numbers = toInts(text);
        int[][] pairs = new int[numbers.length / 2][2];
        for (int i = 0; i < pairs.length; i++) {
            pairs[i][0] = numbers[2 * i];     // x
            pairs[i][1] = numbers[2 * i + 1]; // y
        }
        return pairs;
    }

    // ------------------------------------------------------------------
    // ACTIONS: each returns the new State, or null if the move is illegal
    // ------------------------------------------------------------------

    /**
     * Shared by all moves that change position (left, right, climbup,
     * climbdown, jumpdown). Moves one cell by (dx, dy), then checks:
     *   - the new cell is inside the grid
     *   - the new cell is not a wall
     *   - there is enough energy, rope and lives for this move
     * ropeCost and lifeCost are what the move uses up (0 for left/right).
     */
    private static State tryMove(State current, int dx, int dy, int ropeCost, int lifeCost) {
        int newX = current.getAgentX() + dx;
        int newY = current.getAgentY() + dy;

        // Must stay inside the grid
        if (newX < 0 || newX >= width || newY < 0 || newY >= height) {
            return null;
        }

        // Must not be a wall (difficulty 0)
        int difficulty = grid[newY][newX];
        if (difficulty == 0) {
            return null;
        }

        // Must have enough energy for the new cell
        if (current.getEnergy() < difficulty) {
            return null;
        }

        // Must have enough rope, and still keep at least 1 life afterwards
        if (current.getRope() < ropeCost) {
            return null;
        }
        if (current.getLives() - lifeCost < 1) {
            return null;
        }

        // Everything is fine: build the new state (position and resources change)
        return new State(newX, newY,
                         current.getEnergy() - difficulty,
                         current.getRope() - ropeCost,
                         current.getLives() - lifeCost,
                         current.isHasKey(),
                         current.getDoorsUnlocked(),
                         current.getKeysCollected());
    }

    // left: one cell to the left (x gets smaller), no rope, no life
    private static State applyLeft(State current) {
        return tryMove(current, -1, 0, 0, 0);
    }

    // right: one cell to the right (x gets bigger), no rope, no life
    private static State applyRight(State current) {
        return tryMove(current, 1, 0, 0, 0);
    }

    // climbup: one cell up (y gets smaller, row 0 is the top), uses 1 rope, no life
    private static State applyClimbUp(State current) {
        return tryMove(current, 0, -1, 1, 0);
    }

    // climbdown: one cell down (y gets bigger), uses 1 rope, no life
    private static State applyClimbDown(State current) {
        return tryMove(current, 0, 1, 1, 0);
    }

    // jumpdown: one cell down, no rope, costs 1 life (needs more than 1 life left).
    // It still pays the energy of the cell it lands on (see the PDF example).
    private static State applyJumpDown(State current) {
        return tryMove(current, 0, 1, 0, 1);
    }

    // Returns the index of the location (x, y) in the list, or -1 if it is not there.
    // Used to find "which door / which key am I standing on".
    private static int findIndex(int[][] locations, int x, int y) {
        for (int i = 0; i < locations.length; i++) {
            if (locations[i][0] == x && locations[i][1] == y) {
                return i;
            }
        }
        return -1;
    }

    /**
     * collect: pick up the key at the current cell.
     * Allowed only if we stand on a key that is still on the floor
     * and we are not already carrying a key (only one at a time).
     * Costs nothing.
     */
    private static State applyCollect(State current) {
        int keyIndex = findIndex(keyLocations, current.getAgentX(), current.getAgentY());

        // No key here
        if (keyIndex == -1) {
            return null;
        }
        // That key was already picked up
        if (current.getKeysCollected()[keyIndex]) {
            return null;
        }
        // Hands are full
        if (current.isHasKey()) {
            return null;
        }

        // Mark this key as collected and hold it
        boolean[] keysCollected = current.getKeysCollected();
        keysCollected[keyIndex] = true;

        return new State(current.getAgentX(), current.getAgentY(),
                         current.getEnergy(), current.getRope(), current.getLives(),
                         true,
                         current.getDoorsUnlocked(),
                         keysCollected);
    }

    /**
     * unlock: open the door at the current cell.
     * Allowed only if we stand on a door that is still locked
     * and we are carrying a key. The key is used up. Costs nothing else.
     */
    private static State applyUnlock(State current) {
        int doorIndex = findIndex(doorLocations, current.getAgentX(), current.getAgentY());

        // No door here
        if (doorIndex == -1) {
            return null;
        }
        // That door is already open
        if (current.getDoorsUnlocked()[doorIndex]) {
            return null;
        }
        // We have no key to open it with
        if (!current.isHasKey()) {
            return null;
        }

        // Mark this door as unlocked and use up the key
        boolean[] doorsUnlocked = current.getDoorsUnlocked();
        doorsUnlocked[doorIndex] = true;

        return new State(current.getAgentX(), current.getAgentY(),
                         current.getEnergy(), current.getRope(), current.getLives(),
                         false,
                         doorsUnlocked,
                         current.getKeysCollected());
    }

    // ------------------------------------------------------------------
    // THE CONTRACT WITH THE SEARCH (GenericSearchProblem)
    // ------------------------------------------------------------------

    /**
     * Goal: every door is unlocked and the explorer still has at least 1 life.
     */
    @Override
    public boolean isGoalState(State current) {
        return current.areAllDoorsUnlocked() && current.getLives() >= 1;
    }

    /**
     * Returns every legal move from this state as a list of [Action, State] pairs.
     * Each action is tried; illegal ones (null) are skipped.
     */
    @Override
    public ArrayList<ArrayList<Object>> getNextState(State current) {
        ArrayList<ArrayList<Object>> successors = new ArrayList<>();

        addIfLegal(successors, Action.LEFT, applyLeft(current));
        addIfLegal(successors, Action.RIGHT, applyRight(current));
        addIfLegal(successors, Action.CLIMBUP, applyClimbUp(current));
        addIfLegal(successors, Action.CLIMBDOWN, applyClimbDown(current));
        addIfLegal(successors, Action.JUMPDOWN, applyJumpDown(current));
        addIfLegal(successors, Action.COLLECT, applyCollect(current));
        addIfLegal(successors, Action.UNLOCK, applyUnlock(current));

        return successors;
    }

    // Adds [action, nextState] to the list, but only if the move was legal (not null)
    private static void addIfLegal(ArrayList<ArrayList<Object>> successors,
                                   Action action, State nextState) {
        if (nextState == null) {
            return;
        }
        ArrayList<Object> pair = new ArrayList<>();
        pair.add(action);
        pair.add(nextState);
        successors.add(pair);
    }

    @Override
    public double[] calculateHeuristic(State state) {
        // Part of A*, decided later with your teammate
        throw new UnsupportedOperationException("Unimplemented method 'calculateHeuristic'");
    }

    // TEMPORARY: only for checking our code. Delete before submitting.
    private static void showMoves(String label, CaveExplorer problem, State s) {
        ArrayList<String> moves = new ArrayList<>();
        for (ArrayList<Object> pair : problem.getNextState(s)) {
            Action action = (Action) pair.get(0);
            State next = (State) pair.get(1);
            moves.add(action + "->(" + next.getAgentX() + "," + next.getAgentY() + ")");
        }
        System.out.println(label + ": " + moves.size() + " moves " + moves);
    }

    public static void main(String[] args) {
        CaveExplorer problem = new CaveExplorer();
        State start = parseInput("3,4;0,1;100,2;0,3,0,0;4,12,0,0;0,7,23,8;3,2;1,0;");

        // Walk the PDF solution to get some interesting states
        State s1 = applyRight(start);
        State s2 = applyClimbUp(s1);
        State s3 = applyCollect(s2);
        State s4 = applyClimbDown(s3);
        State s5 = applyJumpDown(s4);
        State s6 = applyRight(s5);
        State s7 = applyRight(s6);
        State s8 = applyUnlock(s7);

        showMoves("from start", problem, start);
        showMoves("from (1,1)", problem, s1);
        showMoves("from (1,0) without key", problem, s2);
        showMoves("from (1,0) with key", problem, s3);
        showMoves("from door cell with key", problem, s7);

        System.out.println("goal at start: " + problem.isGoalState(start));
        System.out.println("goal before unlock: " + problem.isGoalState(s7));
        System.out.println("goal after unlock: " + problem.isGoalState(s8));
    }
}