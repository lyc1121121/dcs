package com.dcsmanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 화면 접속 여부와 무관하게, 주기적으로(기본 3초) DCSServer 의 상태 스냅샷을 가져와 StatusCache 에 반영한다.
 */
@Component
public class StatusPoller {

    private static final Logger log = LoggerFactory.getLogger(StatusPoller.class);

    private final DcsServerClient dcsServerClient;
    private final StatusCache statusCache;

    public StatusPoller(DcsServerClient dcsServerClient, StatusCache statusCache) {
        this.dcsServerClient = dcsServerClient;
        this.statusCache = statusCache;
    }

    @Scheduled(fixedDelayString = "${dcsserver.poll-interval-ms:3000}", initialDelayString = "${dcsserver.poll-interval-ms:2000}")
    public void poll() {
        Map<String, DcsServerClient.DcsStatus> statuses = dcsServerClient.getStatuses();
        if (statuses == null) {
            log.warn("DCSServer 상태 조회 실패 - 이전 캐시 값을 유지합니다.");
            return;
        }
        statusCache.set(statuses);
        log.debug("Polled {} statuses from DCSServer", statuses.size());
    }
}
