package com.dcsmanager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class DcsServerClient {

    private static final String API_KEY_HEADER = "X-API-KEY";

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;

    public DcsServerClient(RestTemplate restTemplate,
                            @Value("${dcsserver.base-url}") String baseUrl,
                            @Value("${dcsserver.api-key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    /**
     * DCSServer 를 거쳐 해당 IP 의 DCSAgent 와 실제로(인증 포함) 통신되는지 확인한다.
     * 호출 자체가 실패하면(DCSServer 불통 등) 통신 불가로 간주한다.
     */
    public boolean checkConnectivity(String serverIp) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(API_KEY_HEADER, apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map<String, Boolean>> response = restTemplate.exchange(
                    baseUrl + "/api/check-connectivity?ip=" + serverIp, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<Map<String, Boolean>>() {
                    });
            Map<String, Boolean> body = response.getBody();
            return body != null && Boolean.TRUE.equals(body.get("reachable"));
        } catch (Exception e) {
            return false;
        }
    }

    public DcsServerResult up(String dcsId) {
        return call("/api/deploy/" + dcsId);
    }

    public DcsServerResult down(String dcsId) {
        return call("/api/undeploy/" + dcsId);
    }

    /**
     * DCSServer 호출에 실패하면 null 을 반환한다 (직전에 캐시된 값을 유지할지는 호출자가 판단).
     */
    public Map<String, DcsStatus> getStatuses() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(API_KEY_HEADER, apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map<String, DcsStatus>> response = restTemplate.exchange(
                    baseUrl + "/api/status", HttpMethod.GET, entity,
                    new ParameterizedTypeReference<Map<String, DcsStatus>>() {
                    });
            Map<String, DcsStatus> body = response.getBody();
            return body != null ? body : Collections.emptyMap();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * dcs_config 에 등록된 서버들의 DCSAgent 로부터 "dcs 컨테이너 이외의" 컨테이너 목록을 모아서 조회한다.
     * 호출에 실패하면 빈 목록을 반환한다.
     */
    public List<OtherContainer> listOtherContainers() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(API_KEY_HEADER, apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<OtherContainer[]> response = restTemplate.exchange(
                    baseUrl + "/api/other-containers", HttpMethod.GET, entity, OtherContainer[].class);
            OtherContainer[] body = response.getBody();
            return body != null ? Arrays.asList(body) : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public DcsServerResult startOtherContainer(String serverIp, String name) {
        return callContainerAction(name, "start", serverIp);
    }

    public DcsServerResult stopOtherContainer(String serverIp, String name) {
        return callContainerAction(name, "stop", serverIp);
    }

    public DcsServerResult deleteOtherContainer(String serverIp, String name) {
        return callContainerAction(name, "delete", serverIp);
    }

    private DcsServerResult callContainerAction(String name, String action, String serverIp) {
        return call("/api/other-containers/" + name + "/" + action + "?serverIp=" + serverIp);
    }

    /**
     * 해당 서버에서 포트가 실제로 사용 중인지 확인한다. 확인 자체가 실패하면(서버/DCSAgent 불통 등)
     * checkFailed=true 로 표시하며, 이 경우 호출자는 저장을 막지 않는다(과도한 차단 방지).
     */
    public PortCheckResult checkPortInUse(String serverIp, int port) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(API_KEY_HEADER, apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<PortCheckResult> response = restTemplate.exchange(
                    baseUrl + "/api/port-check?ip=" + serverIp + "&port=" + port,
                    HttpMethod.GET, entity, PortCheckResult.class);
            return response.getBody();
        } catch (Exception e) {
            return new PortCheckResult(false, null, true);
        }
    }

    public static class PortCheckResult {
        private boolean inUse;
        private String containerName;
        private boolean checkFailed;

        public PortCheckResult() {
        }

        public PortCheckResult(boolean inUse, String containerName, boolean checkFailed) {
            this.inUse = inUse;
            this.containerName = containerName;
            this.checkFailed = checkFailed;
        }

        public boolean isInUse() {
            return inUse;
        }

        public void setInUse(boolean inUse) {
            this.inUse = inUse;
        }

        public String getContainerName() {
            return containerName;
        }

        public void setContainerName(String containerName) {
            this.containerName = containerName;
        }

        public boolean isCheckFailed() {
            return checkFailed;
        }

        public void setCheckFailed(boolean checkFailed) {
            this.checkFailed = checkFailed;
        }
    }

    public static class DcsStatus {
        private boolean running;
        private boolean stale;

        public boolean isRunning() {
            return running;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }

        public boolean isStale() {
            return stale;
        }

        public void setStale(boolean stale) {
            this.stale = stale;
        }
    }

    private DcsServerResult call(String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(API_KEY_HEADER, apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + path, HttpMethod.POST, entity, String.class);
            return new DcsServerResult(response.getStatusCode().is2xxSuccessful(), response.getBody());
        } catch (Exception e) {
            return new DcsServerResult(false, "DCSServer 호출 실패: " + e.getMessage());
        }
    }

    public static class DcsServerResult {
        private final boolean success;
        private final String message;

        public DcsServerResult(boolean success, String message) {
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
}
