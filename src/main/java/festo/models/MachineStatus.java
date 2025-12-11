package festo.models;

import java.io.Serializable;

public class MachineStatus implements Serializable {
    private String machineId;
    private String state;
    private double load;
    private boolean hasError;
    private String errorCode;

    public MachineStatus(String machineId) {
        this.machineId = machineId;
        this.state = "IDLE";
        this.load = 0.0;
        this.hasError = false;
    }

    public String getMachineId() { return machineId; }
    public void setMachineId(String machineId) { this.machineId = machineId; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public double getLoad() { return load; }
    public void setLoad(double load) { this.load = load; }

    public boolean isHasError() { return hasError; }
    public void setHasError(boolean hasError) { this.hasError = hasError; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
        this.hasError = true;
    }

    @Override
    public String toString() {
        return "Machine " + machineId + ": " + state +
                " (Load: " + load + "%, Error: " + (hasError ? errorCode : "None") + ")";
    }
}