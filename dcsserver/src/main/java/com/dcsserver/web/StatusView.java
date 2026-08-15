package com.dcsserver.web;

public class StatusView {

    private final boolean running;
    private final boolean stale;

    public StatusView(boolean running, boolean stale) {
        this.running = running;
        this.stale = stale;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isStale() {
        return stale;
    }
}
