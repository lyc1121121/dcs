package com.dcsagent.web;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class DcsProvisionRequest {

    @NotBlank
    private String dcsImageVersion;

    @NotBlank
    private String dcsDotId;

    @NotBlank
    private String dcsMode;

    @NotNull
    private Integer dcsSize;

    @NotNull
    private Integer portDcs1;

    @NotNull
    private Integer portDcs2;

    public String getDcsImageVersion() {
        return dcsImageVersion;
    }

    public void setDcsImageVersion(String dcsImageVersion) {
        this.dcsImageVersion = dcsImageVersion;
    }

    public String getDcsDotId() {
        return dcsDotId;
    }

    public void setDcsDotId(String dcsDotId) {
        this.dcsDotId = dcsDotId;
    }

    public String getDcsMode() {
        return dcsMode;
    }

    public void setDcsMode(String dcsMode) {
        this.dcsMode = dcsMode;
    }

    public Integer getDcsSize() {
        return dcsSize;
    }

    public void setDcsSize(Integer dcsSize) {
        this.dcsSize = dcsSize;
    }

    public Integer getPortDcs1() {
        return portDcs1;
    }

    public void setPortDcs1(Integer portDcs1) {
        this.portDcs1 = portDcs1;
    }

    public Integer getPortDcs2() {
        return portDcs2;
    }

    public void setPortDcs2(Integer portDcs2) {
        this.portDcs2 = portDcs2;
    }
}
