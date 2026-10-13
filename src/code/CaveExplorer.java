package code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

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
    /*
     * ==========================================================================
     * STEP 1 — PRECOMPUTE MINIMUM ENERGY COSTS  (Backward Dijkstra)
     * ==========================================================================
     *
     * WHY DO WE PRECOMPUTE?
     * ----------------------
     * A* calls the heuristic for every single node it considers.  If we ran a
     * fresh shortest-path search each time, the cost per heuristic lookup would
     * be enormous.  Instead, we run ONE Dijkstra over the entire grid before the
     * search starts, and store the result in a 2-D table (minEnergyCost).
     * After that, any heuristic query is just a single array lookup: O(1).
     *
     * WHY RUN DIJKSTRA BACKWARDS (from goal → outward)?
     * ---------------------------------------------------
     * We need the cheapest energy cost from EVERY cell to the SINGLE goal cell.
     * If we ran Dijkstra forward from each cell separately we would need one full
     * Dijkstra run per cell — far too slow.  Running it once BACKWARDS from the
     * goal gives us the answer for every cell simultaneously in one pass, because
     * the cost from any cell A to the goal equals the cost from the goal to A on
     * a symmetric (undirected) grid.
     *
     * WHAT IS THE "RELAXED MAP"?
     * ---------------------------
     * The real problem has doors that block movement until unlocked.  We IGNORE
     * doors in this precomputation.  Removing a constraint can only make paths
     * cheaper, never more expensive, so:
     *
     *       relaxed cost  <=  real cost   (for every cell)
     *
     * This is the key property that makes our heuristic ADMISSIBLE — it never
     * overestimates the true remaining cost, which is the requirement A* needs
     * to guarantee finding the optimal solution.  Walls (grid difficulty == 0)
     * are still respected because the agent physically cannot pass through them.
     *
     * HOW IS ENERGY COST MEASURED?
     * ------------------------------
     * Moving into cell (x, y) costs grid[y][x] energy (the cell's difficulty).
     * The total energy for a path is the sum of difficulties of every cell
     * entered along that path.  Dijkstra naturally finds the minimum such sum.
     *
     * COMPLEXITY
     * -----------
     * O(W * H * log(W * H))  where W = grid width, H = grid height.
     * This runs exactly once per A* search invocation, not once per node.
     * ==========================================================================
     */
    private static void precomputeDijkstra() {
        minEnergyCost = new int[height][width];

        // Fill every cell with MAX_VALUE to represent "not yet reached".
        // Dijkstra will overwrite each cell when it finds the cheapest path to it.
        for (int[] row : minEnergyCost) Arrays.fill(row, Integer.MAX_VALUE);

        // The priority queue holds int[]{cost, x, y} and always pops the
        // lowest-cost entry first, driving Dijkstra's greedy expansion.
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));

        // The goal cell is the source of the backward search.
        // It costs 0 energy to reach the goal if you are already standing on it.
        minEnergyCost[goalY][goalX] = 0;
        pq.offer(new int[]{0, goalX, goalY});

        // The four cardinal directions the agent can move between cells
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int cost = curr[0], x = curr[1], y = curr[2];

            // LAZY DELETION: because Java's PriorityQueue does not support
            // decrease-key, a cell can appear multiple times in the queue with
            // different costs.  If the cost we just popped is higher than the
            // best cost already recorded, a cheaper route was processed earlier
            // — discard this stale entry and move on.
            if (cost > minEnergyCost[y][x]) continue;

            // Explore each of the four neighbours
            for (int[] d : dirs) {
                int nx = x + d[0];
                int ny = y + d[1];

                // Boundary check: stay within the grid
                // Wall check: difficulty == 0 means the cell is a wall — skip it
                if (nx >= 0 && nx < width && ny >= 0 && ny < height && grid[ny][nx] > 0) {

                    // Cost to reach neighbour (nx, ny) via the current cell:
                    // current best cost to (x,y)  +  difficulty of entering (nx,ny)
                    int newCost = cost + grid[ny][nx];

                    // Only update and re-enqueue if this is a strictly cheaper
                    // route than any previously found route to (nx, ny)
                    if (newCost < minEnergyCost[ny][nx]) {
                        minEnergyCost[ny][nx] = newCost;
                        pq.offer(new int[]{newCost, nx, ny});
                    }
                }
            }
        }
        // After this loop: minEnergyCost[y][x] = cheapest energy to travel from
        // cell (x,y) to the goal, ignoring doors but respecting walls.
    }

    /*
     * ==========================================================================
     * STEP 2 — CALCULATE THE HEURISTIC  h(n) = [ h_lives , h_energy ]
     * ==========================================================================
     *
     * WHAT IS A HEURISTIC?
     * ---------------------
     * A heuristic h(n) is an ESTIMATE of the cheapest cost to reach the goal
     * from node n.  A* uses it to prioritise nodes that are likely to lead to
     * the goal cheaply, so it expands far fewer nodes than UCS.
     *
     * For a heuristic to keep A* OPTIMAL it must be ADMISSIBLE, meaning:
     *
     *       h(n)  <=  true remaining cost from n   (for every node n)
     *
     * We return TWO components because our cost function is two-dimensional:
     *   result[0] = h_lives  — lower bound on lives still to be lost
     *   result[1] = h_energy — lower bound on energy still to be spent
     *
     * SearchNode.compareTo() uses both values to rank nodes as:
     *   f_lives  = g_lives  + h_lives
     *   f_energy = g_energy + h_energy  (used as tiebreaker)
     *
     * --------------------------------------------------------------------------
     * COMPONENT 1 — h_energy  (Dijkstra-based, non-trivial)
     * --------------------------------------------------------------------------
     * We look up minEnergyCost[agentY][agentX], which is the minimum energy the
     * agent would need to reach the goal on the RELAXED map (doors removed).
     *
     * Why is this non-trivial?
     *   - It uses the actual grid topology and cell difficulties.
     *   - It accounts for the fact that some paths are longer but cheaper (low
     *     difficulty detours), and some are shorter but expensive (high difficulty
     *     shortcuts).  A plain Manhattan distance completely ignores this.
     *   - Because it is the true optimal cost on a simpler problem (no doors),
     *     it is the TIGHTEST possible admissible energy heuristic — no other
     *     admissible heuristic based on grid difficulty can give a higher value.
     *
     * Admissibility proof:
     *   Real map = relaxed map + door constraints.
     *   Adding constraints can only increase cost, never decrease it.
     *   Therefore: real cost >= relaxed cost = h_energy.  ✓
     *
     * Edge case: if the cell is unreachable even on the relaxed map (e.g. fully
     * surrounded by walls), minEnergyCost equals Integer.MAX_VALUE.  We fall
     * back to 0 to keep the heuristic safe (0 is always admissible).
     *
     * --------------------------------------------------------------------------
     * COMPONENT 2 — h_lives  (descent-based)
     * --------------------------------------------------------------------------
     * The grid's y-axis INCREASES DOWNWARD (row 0 is the top of the cave).
     * So if goalY > agentY, the goal is below the agent and the agent must
     * descend some number of floors to reach it.
     *
     * Two actions exist for vertical descent:
     *   CLIMBDOWN — safe, but consumes 1 rope per floor descended
     *   JUMPDOWN  — costs 1 life per floor, but requires no rope
     *
     * The agent will naturally prefer CLIMBDOWN (safe) whenever rope is
     * available.  Once rope runs out it is forced to use JUMPDOWN and lose
     * lives.  Therefore the minimum lives that MUST be lost from descent alone:
     *
     *   descentNeeded = max(0, goalY - agentY)      floors the agent must go down
     *   floorsRopeCan = state.getRope()             floors rope can cover safely
     *   h_lives       = max(0, descentNeeded - floorsRopeCan)
     *
     * Example:
     *   Agent at y=2, goal at y=7  → descentNeeded = 5
     *   Agent has rope = 3         → rope covers 3 floors safely
     *   Remaining 2 floors MUST be jumped → h_lives = 2
     *
     * Admissibility proof:
     *   We are counting the absolute minimum forced jumps.  The real path may
     *   require the same number of jumps or MORE (e.g. if the path winds around
     *   and descends further), never fewer.  Therefore h_lives never
     *   overestimates the real lives cost.  ✓
     *
     * Note: if the goal is at the same level or above the agent (goalY <= agentY),
     * descentNeeded = 0 and h_lives = 0.  No forced jumps — sensible.
     * ==========================================================================
     */
    @Override
    public double[] calculateHeuristic(State state) {

        // Build the Dijkstra lookup table the first time A* needs a heuristic.
        // Subsequent calls skip this and go straight to the O(1) array lookup.
        if (minEnergyCost == null) precomputeDijkstra();

        int x = state.getAgentX();
        int y = state.getAgentY();

        // ------------------------------------------------------------------
        // h_energy: cheapest energy path to goal on the relaxed (door-free) map.
        // Falls back to 0 if the cell is walled off even on the relaxed map.
        // ------------------------------------------------------------------
        double hEnergy = (minEnergyCost[y][x] != Integer.MAX_VALUE)
                         ? minEnergyCost[y][x]
                         : 0;

        // ------------------------------------------------------------------
        // h_lives: minimum lives forced by descent when rope runs out.
        //
        // descentNeeded  = floors between agent and goal (0 if goal is above)
        // state.getRope() = rope charges remaining (each covers 1 safe floor)
        // forced jumps   = floors that exceed available rope → each costs 1 life
        // ------------------------------------------------------------------
        int descentNeeded = Math.max(0, goalY - y);
        double hLives     = Math.max(0, descentNeeded - state.getRope());

        // Return [h_lives, h_energy] — order matches SearchNode.setHeuristics(hLives, hEnergy)
        return new double[]{hLives, hEnergy};
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