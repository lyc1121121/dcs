package com.dcsserver.web;

/**
 * DCSManager 에게 내려주는, serverIp 가 포함된 집계 뷰.
 */
public class OtherContainerView {

    private final String serverIp;
    private final String name;
    private final String image;
    private final String ports;
    private final boolean running;

    public OtherContainerView(String serverIp, String name, String image, String ports, boolean running) {
        this.serverIp = serverIp;
        this.name = name;
        this.image = image;
        this.ports = ports;
        this.running = running;
    }

    public String getServerIp() {
        return serverIp;
    }

    public String getName() {
        return name;
    }

    public String getImage() {
        return image;
    }

    public String getPorts() {
        return ports;
    }

    public boolean isRunning() {
        return running;
    }
}
