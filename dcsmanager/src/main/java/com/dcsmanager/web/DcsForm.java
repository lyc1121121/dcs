package com.dcsmanager.web;

import com.dcsmanager.domain.Dcs;
import com.dcsmanager.domain.DcsMode;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class DcsForm {

    @NotBlank(message = "DCS_ID는 필수입니다.")
    @Pattern(regexp = "\\d{10}", message = "DCS_ID는 10자리 숫자여야 합니다.")
    private String dcsId;

    @NotBlank(message = "DCS_IMAGE_VERSION은 필수입니다.")
    private String dcsImageVersion = "latest";

    /** 화면 표시용. 실제 저장값은 서버에서 DCS_ID로부터 재계산한다. */
    private String dcsDotId;

    @NotNull(message = "DCS_MODE를 선택하세요.")
    private DcsMode dcsMode;

    @NotNull(message = "DCS_SIZE는 필수입니다.")
    @Min(value = 1, message = "DCS_SIZE는 1 이상이어야 합니다.")
    private Integer dcsSize = 1;

    @NotNull(message = "PORT_DCS1은 필수입니다.")
    @Min(value = 1) @Max(value = 65535)
    private Integer portDcs1;

    @NotNull(message = "PORT_DCS2는 필수입니다.")
    @Min(value = 1) @Max(value = 65535)
    private Integer portDcs2;

    @NotBlank(message = "DCS_SERVER_IP는 필수입니다.")
    @Pattern(
            regexp = "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$",
            message = "DCS_SERVER_IP는 올바른 IPv4 형식이어야 합니다. (예: 100.125.13.91)")
    private String dcsServerIp;

    public static DcsForm from(Dcs dcs) {
        DcsForm form = new DcsForm();
        form.dcsId = dcs.getDcsId();
        form.dcsImageVersion = dcs.getDcsImageVersion();
        form.dcsDotId = dcs.getDcsDotId();
        form.dcsMode = dcs.getDcsMode();
        form.dcsSize = dcs.getDcsSize();
        form.portDcs1 = dcs.getPortDcs1();
        form.portDcs2 = dcs.getPortDcs2();
        form.dcsServerIp = dcs.getDcsServerIp();
        return form;
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
