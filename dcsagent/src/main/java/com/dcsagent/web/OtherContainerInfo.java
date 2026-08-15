package com.dcsagent.web;

public class OtherContainerInfo {

    private String name;
    private String image;
    private String ports;
    private boolean running;

    public OtherContainerInfo() {
    }

    public OtherContainerInfo(String name, String image, String ports, boolean running) {
        this.name = name;
        this.image = image;
        this.ports = ports;
        this.running = running;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getPorts() {
        return ports;
    }

    public void setPorts(String ports) {
        this.ports = ports;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}
