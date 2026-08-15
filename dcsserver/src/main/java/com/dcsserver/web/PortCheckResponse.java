package com.dcsserver.web;

public class PortCheckResponse {

    private final boolean inUse;
    private final String containerName;
    private final boolean checkFailed;

    public PortCheckResponse(boolean inUse, String containerName, boolean checkFailed) {
        this.inUse = inUse;
        this.containerName = containerName;
        this.checkFailed = checkFailed;
    }

    public boolean isInUse() {
        return inUse;
    }

    public String getContainerName() {
        return containerName;
    }

    public boolean isCheckFailed() {
        return checkFailed;
    }
}
