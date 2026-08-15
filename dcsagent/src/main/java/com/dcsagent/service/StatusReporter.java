package com.dcsagent.service;

import com.dcsagent.web.DcsContainerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * 주기적으로(기본 3초) 이 서버가 관리하는 DCS 컨테이너들의 실행 상태를 DCSServer 로 보고한다.
 * DCSServer 는 dcs_server_ip 로 이 서버(에이전트)를 알고 있지만, 반대 방향(에이전트->서버)은
 * 도커 네트워크 이름에 의존할 수 없으므로(서로 다른 물리 서버일 수 있음) 항상 설정된
 * 실제 도달 가능한 DCSSERVER_URL 을 사용한다.
 */
@Component
public class StatusReporter {

    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final Logger log = LoggerFactory.getLogger(StatusReporter.class);

    private final DockerStatusChecker statusChecker;
    private final RestTemplate restTemplate;
    private final String dcsServerUrl;
    private final String apiKey;

    public StatusReporter(DockerStatusChecker statusChecker,
                           RestTemplate restTemplate,
                           @Value("${dcsserver.base-url}") String dcsServerUrl,
                           @Value("${agent.api-key}") String apiKey) {
        this.statusChecker = statusChecker;
        this.restTemplate = restTemplate;
        this.dcsServerUrl = dcsServerUrl;
        this.apiKey = apiKey;
    }

    @Scheduled(fixedDelayString = "${agent.status-interval-ms:3000}", initialDelayString = "${agent.status-interval-ms:3000}")
    public void reportStatus() {
        List<DcsContainerStatus> statuses = statusChecker.checkAll();
        if (statuses.isEmpty()) {
            return;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set(API_KEY_HEADER, apiKey);
        HttpEntity<List<DcsContainerStatus>> entity = new HttpEntity<>(statuses, headers);
        try {
            restTemplate.postForEntity(dcsServerUrl + "/api/agent-status", entity, Void.class);
            log.debug("Reported {} container statuses to {}", statuses.size(), dcsServerUrl);
        } catch (Exception e) {
            log.warn("Failed to report status to DCSServer ({}): {}", dcsServerUrl, e.getMessage());
        }
    }
}
