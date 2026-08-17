package com.dcsserver.service;

import com.dcsserver.domain.DcsConfig;
import com.dcsserver.web.AgentOtherContainer;
import com.dcsserver.web.AgentPortCheckResult;
import com.dcsserver.web.DeployResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
public class AgentClient {

    private static final String API_KEY_HEADER = "X-API-KEY";

    private final RestTemplate restTemplate;
    private final int agentPort;
    private final String agentApiKey;

    public AgentClient(RestTemplate restTemplate,
                        @Value("${agent-client.port:8443}") int agentPort,
                        @Value("${agent-client.api-key}") String agentApiKey) {
        this.restTemplate = restTemplate;
        this.agentPort = agentPort;
        this.agentApiKey = agentApiKey;
    }

    public DeployResult up(DcsConfig config) {
        DeployResult ipCheck = checkServerIp(config);
        if (ipCheck != null) {
            return ipCheck;
        }
        AgentProvisionRequest body = new AgentProvisionRequest();
        body.setDcsImageVersion(config.getDcsImageVersion());
        body.setDcsDotId(config.getDcsDotId());
        body.setDcsMode(config.getDcsMode());
        body.setDcsSize(config.getDcsSize());
        body.setPortDcs1(config.getPortDcs1());
        body.setPortDcs2(config.getPortDcs2());

        String url = buildUrl(config.getDcsServerIp(), config.getDcsId(), "up");
        return call(url, HttpMethod.POST, body);
    }

    public DeployResult down(DcsConfig config) {
        DeployResult ipCheck = checkServerIp(config);
        if (ipCheck != null) {
            return ipCheck;
        }
        String url = buildUrl(config.getDcsServerIp(), config.getDcsId(), "down");
        return call(url, HttpMethod.POST, null);
    }

    public DeployResult simulateJaqt(DcsConfig config, String terminalId, int fileCount, int intervalSeconds) {
        DeployResult ipCheck = checkServerIp(config);
        if (ipCheck != null) {
            return ipCheck;
        }
        String url = buildUrl(config.getDcsServerIp(), config.getDcsId(), "simulate")
                + "?terminalId=" + terminalId
                + "&fileCount=" + fileCount
                + "&intervalSeconds=" + intervalSeconds;
        return call(url, HttpMethod.POST, null);
    }

    public boolean checkConnectivity(String serverIp) {
        String url = "https://" + serverIp + ":" + agentPort + "/api/health";
        HttpHeaders headers = new HttpHeaders();
        headers.set(API_KEY_HEADER, agentApiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 해당 서버의 DCSAgent 가 관리하지 않는(dcs{ID} 가 아닌) 컨테이너 목록을 조회한다.
     * 서버가 응답하지 않으면 null 을 반환한다 (호출자가 해당 서버를 건너뛰도록).
     */
    public List<AgentOtherContainer> listOtherContainers(String serverIp) {
        String url = "https://" + serverIp + ":" + agentPort + "/api/containers";
        HttpHeaders headers = new HttpHeaders();
        headers.set(API_KEY_HEADER, agentApiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<AgentOtherContainer[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, AgentOtherContainer[].class);
            AgentOtherContainer[] body = response.getBody();
            return body != null ? Arrays.asList(body) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public DeployResult startContainer(String serverIp, String name) {
        return call(buildContainerActionUrl(serverIp, name, "start"), HttpMethod.POST, null);
    }

    public DeployResult stopContainer(String serverIp, String name) {
        return call(buildContainerActionUrl(serverIp, name, "stop"), HttpMethod.POST, null);
    }

    public DeployResult deleteContainer(String serverIp, String name) {
        return call(buildContainerActionUrl(serverIp, name, "delete"), HttpMethod.POST, null);
    }

    /**
     * 해당 서버에서 지정한 포트가 실제로 사용 중인지 확인한다.
     * 서버가 응답하지 않으면 null 을 반환한다 (호출자가 "확인 불가"로 처리하도록).
     */
    public AgentPortCheckResult checkPortInUse(String serverIp, int port) {
        String url = "https://" + serverIp + ":" + agentPort + "/api/ports/" + port + "/in-use";
        HttpHeaders headers = new HttpHeaders();
        headers.set(API_KEY_HEADER, agentApiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<AgentPortCheckResult> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, AgentPortCheckResult.class);
            return response.getBody();
        } catch (Exception e) {
            return null;
        }
    }

    private String buildContainerActionUrl(String serverIp, String name, String action) {
        return "https://" + serverIp + ":" + agentPort + "/api/containers/" + name + "/" + action;
    }

    private DeployResult checkServerIp(DcsConfig config) {
        String ip = config.getDcsServerIp();
        if (ip == null || ip.trim().isEmpty()) {
            return new DeployResult(false,
                    "DCS_ID [" + config.getDcsId() + "] 의 DCS_SERVER_IP 가 비어 있습니다. DCSManager에서 값을 먼저 입력하세요.");
        }
        return null;
    }

    private String buildUrl(String serverIp, String dcsId, String action) {
        return "https://" + serverIp + ":" + agentPort + "/api/dcs/" + dcsId + "/" + action;
    }

    private DeployResult call(String url, HttpMethod method, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(API_KEY_HEADER, agentApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, method, entity, String.class);
            boolean success = response.getStatusCode().is2xxSuccessful();
            return new DeployResult(success, "DCSAgent 응답(" + response.getStatusCodeValue() + "): " + response.getBody());
        } catch (Exception e) {
            return new DeployResult(false, "DCSAgent 호출 실패: " + e.getMessage());
        }
    }
}
