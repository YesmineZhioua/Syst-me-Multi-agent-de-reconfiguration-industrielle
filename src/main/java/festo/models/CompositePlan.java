package festo.models;

public class CompositePlan extends ReconfigurationPlan {
    private String strategy;

    public CompositePlan() {
        super("COMP_" + System.currentTimeMillis(), "COMPOSITE", "ADAPTIVE");
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getStrategy() {
        return strategy;
    }

    @Override
    public String toString() {
        return "CompositePlan[" + getPlanId() + "] Strategy: " + strategy +
                " (" + getActions().size() + " phases)";
    }
}