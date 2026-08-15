package com.dcsserver.web;

/**
 * DCSAgent 의 GET /api/containers 응답 하나를 그대로 역직렬화하기 위한 DTO.
 */
public class AgentOtherContainer {

    private String name;
    private String image;
    private String ports;
    private boolean running;

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
