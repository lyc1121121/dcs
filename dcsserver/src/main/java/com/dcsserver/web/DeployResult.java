package com.dcsserver.web;

public class DeployResult {

    private final boolean success;
    private final String message;

    public DeployResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
