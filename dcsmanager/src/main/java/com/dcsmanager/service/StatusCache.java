package com.dcsmanager.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * DCSServer 로부터 주기적으로 가져온 컨테이너 상태를 보관한다.
 * 화면 요청은 이 캐시를 즉시 읽기만 하고, 실제 DCSServer 호출은 StatusPoller 가 배경에서 수행한다.
 */
@Service
public class StatusCache {

    private final AtomicReference<Map<String, DcsServerClient.DcsStatus>> current =
            new AtomicReference<>(Collections.emptyMap());

    public Map<String, DcsServerClient.DcsStatus> get() {
        return current.get();
    }

    public void set(Map<String, DcsServerClient.DcsStatus> statuses) {
        current.set(statuses);
    }
}
