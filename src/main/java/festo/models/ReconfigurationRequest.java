package festo.models;

import java.io.Serializable;
import java.util.Date;

public class ReconfigurationRequest implements Serializable {
    private String requestId;
    private String requesterAgent;
    private String scenarioType;
    private String trigger;
    private Date requestTime;

    public ReconfigurationRequest(String requesterAgent, String scenarioType, String trigger) {
        this.requestId = "REQ_" + System.currentTimeMillis();
        this.requesterAgent = requesterAgent;
        this.scenarioType = scenarioType;
        this.trigger = trigger;
        this.requestTime = new Date();
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getRequesterAgent() { return requesterAgent; }
    public void setRequesterAgent(String requesterAgent) { this.requesterAgent = requesterAgent; }

    public String getScenarioType() { return scenarioType; }
    public void setScenarioType(String scenarioType) { this.scenarioType = scenarioType; }

    public String getTrigger() { return trigger; }
    public void setTrigger(String trigger) { this.trigger = trigger; }

    public Date getRequestTime() { return requestTime; }
    public void setRequestTime(Date requestTime) { this.requestTime = requestTime; }

    @Override
    public String toString() {
        return "Request[" + requestId + "] from " + requesterAgent + ": " + scenarioType;
    }
}