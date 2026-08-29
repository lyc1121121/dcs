package com.dcsmanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * 카카오톡 "나에게 보내기" API로 알림 메시지를 보낸다.
 * DCS 컨테이너가 올라가거나 내려갈 때 StatusPoller 가 이 서비스를 호출한다(109단계 후속 요청).
 */
@Service
public class KakaoNotifier {

    private static final Logger log = LoggerFactory.getLogger(KakaoNotifier.class);
    private static final String SEND_URL = "https://kapi.kakao.com/v2/api/talk/memo/default/send";

    private final RestTemplate restTemplate;
    private final KakaoTokenStore tokenStore;

    public KakaoNotifier(RestTemplate restTemplate, KakaoTokenStore tokenStore) {
        this.restTemplate = restTemplate;
        this.tokenStore = tokenStore;
    }

    public void notify(String message) {
        notify(message, "https://developers.kakao.com");
    }

    /** 159단계: 메시지를 탭했을 때 열릴 링크를 직접 지정할 수 있는 버전(파일탐색기 공유 링크용). */
    public void notify(String message, String linkUrl) {
        if (!tokenStore.isConfigured()) {
            return;
        }
        String accessToken = tokenStore.getAccessToken();
        if (accessToken == null) {
            accessToken = tokenStore.refresh();
        }
        if (accessToken == null) {
            log.warn("카카오 액세스 토큰이 없어 알림을 보내지 못했습니다: {}", message);
            return;
        }
        if (send(accessToken, message, linkUrl)) {
            return;
        }
        // 액세스 토큰이 만료됐을 수 있으니 한 번 갱신 후 재시도한다.
        String refreshed = tokenStore.refresh();
        if (refreshed != null) {
            send(refreshed, message, linkUrl);
        }
    }

    private boolean send(String accessToken, String message, String linkUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 159단계: 예전 button_title 필드만으로는 실제 카카오톡에서 버튼이 안 그려짐(테스트로 확인됨).
        // 지금 API가 쓰는 buttons 배열 형식으로 명시적인 링크 버튼을 넣어야 탭해서 바로 열림.
        String linkJson = "{\"web_url\":\"" + escapeJson(linkUrl) + "\",\"mobile_web_url\":\"" + escapeJson(linkUrl) + "\"}";
        String templateObject = "{\"object_type\":\"text\",\"text\":\"" + escapeJson(message)
                + "\",\"link\":" + linkJson
                + ",\"button_title\":\"바로 열기\""
                + ",\"buttons\":[{\"title\":\"바로 열기\",\"link\":" + linkJson + "}]}";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("template_object", templateObject);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(SEND_URL, new HttpEntity<>(body, headers), String.class);
            boolean success = response.getStatusCode().is2xxSuccessful();
            if (!success) {
                log.warn("카카오 알림 전송 실패(status={}): {}", response.getStatusCodeValue(), message);
            }
            return success;
        } catch (Exception e) {
            log.warn("카카오 알림 전송 실패: {}", e.getMessage());
            return false;
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
