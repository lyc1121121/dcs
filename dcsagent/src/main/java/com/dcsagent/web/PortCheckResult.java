package com.dcsagent.web;

public class PortCheckResult {

    private boolean inUse;
    private String containerName;

    public PortCheckResult() {
    }

    public PortCheckResult(boolean inUse, String containerName) {
        this.inUse = inUse;
        this.containerName = containerName;
    }

    public boolean isInUse() {
        return inUse;
    }

    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }
}
