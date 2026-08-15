package com.dcsagent.web;

public class DcsCommandResult {

    private final boolean success;
    private final String message;
    private final String output;

    public DcsCommandResult(boolean success, String message, String output) {
        this.success = success;
        this.message = message;
        this.output = output;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getOutput() {
        return output;
    }
}
