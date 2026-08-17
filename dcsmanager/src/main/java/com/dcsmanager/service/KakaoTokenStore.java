package com.dcsmanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 카카오톡 "나에게 보내기" 알림에 쓰는 액세스/리프레시 토큰을 관리한다.
 * - 리프레시 토큰은 재기동/회전에도 살아남도록 파일에 저장한다(최초 값은 사람이 직접 OAuth 인증해서 파일에 심어둔다).
 * - 액세스 토큰은 메모리에만 두고, 만료(약 6시간)되기 전에 주기적으로 새로 받는다.
 */
@Service
public class KakaoTokenStore {

    private static final Logger log = LoggerFactory.getLogger(KakaoTokenStore.class);
    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";

    private final RestTemplate restTemplate;
    private final String restApiKey;
    private final String clientSecret;
    private final Path tokenFile;

    private final AtomicReference<String> accessToken = new AtomicReference<>();
    private final AtomicReference<String> refreshToken = new AtomicReference<>();

    public KakaoTokenStore(RestTemplate restTemplate,
                            @Value("${kakao.rest-api-key:}") String restApiKey,
                            @Value("${kakao.client-secret:}") String clientSecret,
                            @Value("${kakao.token-file:/data001/kakao/refresh_token.txt}") String tokenFilePath) {
        this.restTemplate = restTemplate;
        this.restApiKey = restApiKey;
        this.clientSecret = clientSecret;
        this.tokenFile = Paths.get(tokenFilePath);
        loadRefreshToken();
    }

    public boolean isConfigured() {
        return !restApiKey.isEmpty() && refreshToken.get() != null;
    }

    public String getAccessToken() {
        return accessToken.get();
    }

    private void loadRefreshToken() {
        try {
            if (Files.isRegularFile(tokenFile)) {
                String token = new String(Files.readAllBytes(tokenFile), StandardCharsets.UTF_8).trim();
                if (!token.isEmpty()) {
                    refreshToken.set(token);
                    log.info("카카오 리프레시 토큰을 파일에서 불러왔습니다: {}", tokenFile);
                }
            } else {
                log.warn("카카오 리프레시 토큰 파일이 없습니다({}) - 카카오 알림이 비활성화됩니다.", tokenFile);
            }
        } catch (IOException e) {
            log.warn("카카오 리프레시 토큰 파일 읽기 실패: {}", e.getMessage());
        }
    }

    private void persistRefreshToken(String token) {
        try {
            Files.createDirectories(tokenFile.getParent());
            Path tmp = Files.createTempFile(tokenFile.getParent(), ".kakao-token", ".tmp");
            Files.write(tmp, token.getBytes(StandardCharsets.UTF_8));
            Files.move(tmp, tokenFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("카카오 리프레시 토큰 저장 실패", e);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (isConfigured()) {
            refresh();
        }
    }

    /** 6시간 만료 전에 미리 갱신한다(5시간마다). */
    @Scheduled(initialDelay = 5 * 60 * 60 * 1000L, fixedRate = 5 * 60 * 60 * 1000L)
    public void scheduledRefresh() {
        if (isConfigured()) {
            refresh();
        }
    }

    /** 리프레시 토큰으로 액세스 토큰을 새로 받는다. 실패하면 null. */
    public synchronized String refresh() {
        String rt = refreshToken.get();
        if (rt == null) {
            return null;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", restApiKey);
        if (!clientSecret.isEmpty()) {
            body.add("client_secret", clientSecret);
        }
        body.add("refresh_token", rt);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(TOKEN_URL, new HttpEntity<>(body, headers), Map.class);
            Map<?, ?> result = response.getBody();
            if (result == null || result.get("access_token") == null) {
                log.error("카카오 토큰 갱신 응답이 비정상입니다: {}", result);
                return null;
            }
            String newAccessToken = result.get("access_token").toString();
            accessToken.set(newAccessToken);

            Object newRefreshToken = result.get("refresh_token");
            if (newRefreshToken != null) {
                refreshToken.set(newRefreshToken.toString());
                persistRefreshToken(newRefreshToken.toString());
                log.info("카카오 리프레시 토큰이 회전되어 파일에 다시 저장했습니다.");
            }
            log.info("카카오 액세스 토큰을 갱신했습니다.");
            return newAccessToken;
        } catch (Exception e) {
            log.error("카카오 토큰 갱신 실패: {}", e.getMessage());
            return null;
        }
    }
}
