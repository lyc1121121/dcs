package com.dcsmanager.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "dcs_config")
public class Dcs {

    @Id
    @Column(name = "dcs_id", length = 20, nullable = false, updatable = false)
    private String dcsId;

    @Column(name = "dcs_image_version", length = 50, nullable = false)
    private String dcsImageVersion = "latest";

    @Column(name = "dcs_dot_id", length = 30, nullable = false)
    private String dcsDotId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dcs_mode", length = 10, nullable = false)
    private DcsMode dcsMode;

    @Column(name = "dcs_size", nullable = false)
    private Integer dcsSize = 1;

    @Column(name = "port_dcs1", nullable = false)
    private Integer portDcs1;

    @Column(name = "port_dcs2", nullable = false)
    private Integer portDcs2;

    @Column(name = "dcs_server_ip", length = 45, nullable = false)
    private String dcsServerIp;

    public Dcs() {
    }

    public String getDcsId() {
        return dcsId;
    }

    public void setDcsId(String dcsId) {
        this.dcsId = dcsId;
    }

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

    public DcsMode getDcsMode() {
        return dcsMode;
    }

    public void setDcsMode(DcsMode dcsMode) {
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

    public String getDcsServerIp() {
        return dcsServerIp;
    }

    public void setDcsServerIp(String dcsServerIp) {
        this.dcsServerIp = dcsServerIp;
    }
}
