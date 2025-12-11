package festo.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ReconfigurationPlan implements Serializable {
    private String planId;
    private String scenarioId;
    private String strategyId;
    private List<String> actions;

    public ReconfigurationPlan(String planId, String scenarioId, String strategyId) {
        this.planId = planId;
        this.scenarioId = scenarioId;
        this.strategyId = strategyId;
        this.actions = new ArrayList<>();
    }

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public String getScenarioId() { return scenarioId; }
    public void setScenarioId(String scenarioId) { this.scenarioId = scenarioId; }

    public String getStrategyId() { return strategyId; }
    public void setStrategyId(String strategyId) { this.strategyId = strategyId; }

    public List<String> getActions() { return actions; }
    public void setActions(List<String> actions) { this.actions = actions; }
    public void addAction(String action) { this.actions.add(action); }

    @Override
    public String toString() {
        return "Plan[" + planId + "] - " + strategyId + " (" + actions.size() + " actions)";
    }
}