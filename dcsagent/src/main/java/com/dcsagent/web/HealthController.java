package com.dcsagent.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * DCSServer 가 DCSManager 의 저장/확인 요청에 응답하기 위해 호출하는 부작용 없는 헬스체크.
 * /api/** 하위이므로 기존 ApiKeyAuthFilter 로 인증된다.
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Collections.singletonMap("status", "UP");
    }
}
