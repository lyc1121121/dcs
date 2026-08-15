package com.dcsserver.web;

public class DcsContainerStatus {

    private String dcsId;
    private boolean running;

    public String getDcsId() {
        return dcsId;
    }

    public void setDcsId(String dcsId) {
        this.dcsId = dcsId;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}
