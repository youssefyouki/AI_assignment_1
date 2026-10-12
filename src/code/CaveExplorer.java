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

    // TEMPORARY: only for checking the parser. Delete before submitting.
    public static void main(String[] args) {
        State s = parseInput("3,4;0,1;100,2;0,3,0,0;4,12,0,0;0,7,23,8;3,2;1,0;");
        System.out.println("height=" + height + " width=" + width);
        System.out.println("start=(" + s.getAgentX() + "," + s.getAgentY() + ")"
                + " energy=" + s.getEnergy() + " rope=" + s.getRope() + " lives=" + s.getLives());
        for (int y = 0; y < height; y++) {
            System.out.println(java.util.Arrays.toString(grid[y]));
        }
        System.out.println("doors=" + java.util.Arrays.deepToString(doorLocations));
        System.out.println("keys=" + java.util.Arrays.deepToString(keyLocations));
        System.out.println("doors unlocked: " + java.util.Arrays.toString(s.getDoorsUnlocked()));
        System.out.println("keys collected: " + java.util.Arrays.toString(s.getKeysCollected()));
    }
}