package com.dcsserver.service;

import com.dcsserver.web.DcsContainerStatus;
import com.dcsserver.web.StatusView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DCSAgent 로부터 10초마다 수신하는 컨테이너 상태를 메모리에만 보관한다.
 * DCSServer 재시작 시 초기화되지만, 에이전트가 곧 다시 보고하므로 자연히 복구된다.
 */
@Service
public class StatusStore {

    private static class Entry {
        final boolean running;
        final Instant updatedAt;

        Entry(boolean running, Instant updatedAt) {
            this.running = running;
            this.updatedAt = updatedAt;
        }
    }

    private final Map<String, Entry> statuses = new ConcurrentHashMap<>();
    private final Duration staleAfter;

    public StatusStore(@Value("${status.stale-after-seconds:30}") long staleAfterSeconds) {
        this.staleAfter = Duration.ofSeconds(staleAfterSeconds);
    }

    public void update(List<DcsContainerStatus> incoming) {
        Instant now = Instant.now();
        for (DcsContainerStatus status : incoming) {
            statuses.put(status.getDcsId(), new Entry(status.isRunning(), now));
        }
    }

    public Map<String, StatusView> snapshot() {
        Instant now = Instant.now();
        Map<String, StatusView> result = new HashMap<>();
        for (Map.Entry<String, Entry> e : statuses.entrySet()) {
            Entry entry = e.getValue();
            boolean stale = Duration.between(entry.updatedAt, now).compareTo(staleAfter) > 0;
            result.put(e.getKey(), new StatusView(entry.running, stale));
        }
        return result;
    }
}
