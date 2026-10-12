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

    // TODO next: applyClimbUp, applyClimbDown, applyJumpDown, applyCollect, applyUnlock

    @Override
    public boolean isGoalState(State current) {
        // TODO: true when all doors are unlocked and lives >= 1
        return false;
    }

    @Override
    public ArrayList<ArrayList<Object>> getNextState(State current) {
        // TODO: add [Action, State] pairs for each legal action
        return new ArrayList<>();
    }

    @Override
    public double[] calculateHeuristic(State state) {
        // Part of A*, decided later with your teammate
        throw new UnsupportedOperationException("Unimplemented method 'calculateHeuristic'");
    }

    // TEMPORARY: only for checking our code. Delete before submitting.
    private static void show(String label, State s) {
        if (s == null) {
            System.out.println(label + ": null (illegal move)");
        } else {
            System.out.println(label + ": pos=(" + s.getAgentX() + "," + s.getAgentY() + ")"
                    + " energy=" + s.getEnergy() + " rope=" + s.getRope() + " lives=" + s.getLives());
        }
    }

    public static void main(String[] args) {
        State start = parseInput("3,4;0,1;100,2;0,3,0,0;4,12,0,0;0,7,23,8;3,2;1,0;");
        show("start", start);

        State afterRight = applyRight(start);
        show("right from start", afterRight);                // (1,1), energy 100-12 = 88

        show("left from start", applyLeft(start));           // x would be -1: outside the grid
        show("right again", applyRight(afterRight));         // (2,1) is a wall

        show("left back", applyLeft(afterRight));            // (0,1), energy 88-4 = 84

        // Same position as start but only 5 energy: cell (1,1) needs 12
        State tired = new State(0, 1, 5, 2, 3, false, new boolean[1], new boolean[1]);
        show("right with 5 energy", applyRight(tired));
    }
}