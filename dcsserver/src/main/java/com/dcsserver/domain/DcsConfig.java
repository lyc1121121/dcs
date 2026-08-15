package com.dcsserver.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "dcs_config")
public class DcsConfig {

    @Id
    @Column(name = "dcs_id", length = 20, nullable = false)
    private String dcsId;

    @Column(name = "dcs_image_version", length = 50, nullable = false)
    private String dcsImageVersion;

    @Column(name = "dcs_dot_id", length = 30, nullable = false)
    private String dcsDotId;

    @Column(name = "dcs_mode", length = 10, nullable = false)
    private String dcsMode;

    @Column(name = "dcs_size", nullable = false)
    private Integer dcsSize;

    @Column(name = "port_dcs1", nullable = false)
    private Integer portDcs1;

    @Column(name = "port_dcs2", nullable = false)
    private Integer portDcs2;

    @Column(name = "dcs_server_ip", length = 45, nullable = false)
    private String dcsServerIp;

    protected DcsConfig() {
    }

    public String getDcsId() {
        return dcsId;
    }

    public String getDcsImageVersion() {
        return dcsImageVersion;
    }

    public String getDcsDotId() {
        return dcsDotId;
    }

    public String getDcsMode() {
        return dcsMode;
    }

    public Integer getDcsSize() {
        return dcsSize;
    }

    public Integer getPortDcs1() {
        return portDcs1;
    }

    public Integer getPortDcs2() {
        return portDcs2;
    }

    public String getDcsServerIp() {
        return dcsServerIp;
    }
}
