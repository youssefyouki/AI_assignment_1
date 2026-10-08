package code;

public enum Action {
    LEFT("left"),
    RIGHT("right"),
    CLIMBUP("climbup"),
    CLIMBDOWN("climbdown"),
    JUMPDOWN("jumpdown"),
    COLLECT("collect"),
    UNLOCK("unlock");

    private final String actionName;

    Action(String actionName) {
        this.actionName = actionName;
    }

    public String getActionName() {
        return actionName;
    }

    @Override
    public String toString() {
        return actionName;
    }

}


