package com.dcsmanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 화면 접속 여부와 무관하게, 주기적으로(기본 3초) DCSServer 의 상태 스냅샷을 가져와 StatusCache 에 반영한다.
 * 이전 스냅샷과 비교해서 DCS_ID 가 올라가거나 내려간 게 감지되면 카카오톡으로 알림을 보낸다.
 */
@Component
public class StatusPoller {

    private static final Logger log = LoggerFactory.getLogger(StatusPoller.class);

    private final DcsServerClient dcsServerClient;
    private final StatusCache statusCache;
    private final KakaoNotifier kakaoNotifier;

    public StatusPoller(DcsServerClient dcsServerClient, StatusCache statusCache, KakaoNotifier kakaoNotifier) {
        this.dcsServerClient = dcsServerClient;
        this.statusCache = statusCache;
        this.kakaoNotifier = kakaoNotifier;
    }

    @Scheduled(fixedDelayString = "${dcsserver.poll-interval-ms:3000}", initialDelayString = "${dcsserver.poll-interval-ms:2000}")
    public void poll() {
        Map<String, DcsServerClient.DcsStatus> statuses = dcsServerClient.getStatuses();
        if (statuses == null) {
            log.warn("DCSServer 상태 조회 실패 - 이전 캐시 값을 유지합니다.");
            return;
        }
        Map<String, DcsServerClient.DcsStatus> previous = statusCache.get();
        statusCache.set(statuses);
        log.debug("Polled {} statuses from DCSServer", statuses.size());
        notifyTransitions(previous, statuses);
    }

    /**
     * 첫 폴링(previous 가 비어있음)에는 알림을 보내지 않는다 - 앱이 막 켜졌을 때 이미 떠 있는
     * 컨테이너들에 대해 전부 "up" 알림이 쏟아지는 것을 막기 위함이다. stale(상태 확인 불가)인
     * 항목도 비교 대상에서 제외한다(오탐 방지).
     */
    private void notifyTransitions(Map<String, DcsServerClient.DcsStatus> previous,
                                    Map<String, DcsServerClient.DcsStatus> current) {
        if (previous.isEmpty()) {
            return;
        }
        for (Map.Entry<String, DcsServerClient.DcsStatus> entry : current.entrySet()) {
            String dcsId = entry.getKey();
            DcsServerClient.DcsStatus currentStatus = entry.getValue();
            DcsServerClient.DcsStatus previousStatus = previous.get(dcsId);
            if (currentStatus == null || currentStatus.isStale()
                    || previousStatus == null || previousStatus.isStale()) {
                continue;
            }
            if (previousStatus.isRunning() == currentStatus.isRunning()) {
                continue;
            }
            if (currentStatus.isRunning()) {
                log.info("DCS_ID [{}] up 전환 감지 - 카카오 알림 발송 시도", dcsId);
                kakaoNotifier.notify("DCS_ID [" + dcsId + "] 컨테이너가 올라왔습니다. 🟢");
            } else {
                log.info("DCS_ID [{}] down 전환 감지 - 카카오 알림 발송 시도", dcsId);
                kakaoNotifier.notify("DCS_ID [" + dcsId + "] 컨테이너가 내려갔습니다. 🔴");
            }
        }
    }
}
