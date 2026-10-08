package code;
import java.util.Arrays;
import java.util.Objects;

public class State {
    // Explorer Status
    private final int agentX;
    private final int agentY;
    private final int energy;
    private final int rope;
    private final int lives;
    private final boolean hasKey;

    // Environment Status
    private final boolean[] doorsUnlocked; 
    private final boolean[] keysCollected; 

    public State(int agentX, 
                 int agentY, 
                 int energy, 
                 int rope, 
                 int lives, 
                 boolean hasKey, 
                 boolean[] doorsUnlocked, 
                 boolean[] keysCollected) {
        this.agentX = agentX;
        this.agentY = agentY;
        this.energy = energy;
        this.rope = rope;
        this.lives = lives;
        this.hasKey = hasKey;
        // Clone arrays to enforce immutability across search tree expansions
        this.doorsUnlocked = doorsUnlocked != null ? doorsUnlocked.clone() : new boolean[0];
        this.keysCollected = keysCollected != null ? keysCollected.clone() : new boolean[0];
    }

    // Getters for the state attributes
    public int getAgentX() { return agentX; }
    public int getAgentY() { return agentY; }
    public int getEnergy() { return energy; }
    public int getRope() { return rope; }
    public int getLives() { return lives; }
    public boolean isHasKey() { return hasKey; }
    public boolean[] getDoorsUnlocked() { return doorsUnlocked.clone(); }
    public boolean[] getKeysCollected() { return keysCollected.clone(); }

    // Check if all doors are unlocked
    public boolean areAllDoorsUnlocked() {
        for (boolean unlocked : doorsUnlocked) {
            if (!unlocked) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        State state = (State) o;
        return agentX == state.agentX &&
               agentY == state.agentY &&
               energy == state.energy &&
               rope == state.rope &&
               lives == state.lives &&
               hasKey == state.hasKey &&
               Arrays.equals(doorsUnlocked, state.doorsUnlocked) &&
               Arrays.equals(keysCollected, state.keysCollected);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(agentX, agentY, energy, rope, lives, hasKey);
        result = 31 * result + Arrays.hashCode(doorsUnlocked);
        result = 31 * result + Arrays.hashCode(keysCollected);
        return result;
    }
}
