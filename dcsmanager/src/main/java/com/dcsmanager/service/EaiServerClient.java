package com.dcsmanager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * eai_server 는 DCSAgent 처럼 DCS_ID 별로 갈라지는 게 아니라, 그 DCS_ID 가 떠 있는
 * 에이전트서버당 1개만 존재한다(103단계 설계). 그래서 DCSServer 를 거치지 않고,
 * dcs_config 에 이미 있는 그 DCS_ID 의 SERVER_IP + 고정 포트로 eai_server 주소를 직접
 * 계산해서 호출한다(eai_server 가 나중에 별도 서버로 분리돼도 이 계산 방식은 그대로 유효).
 */
@Service
public class EaiServerClient {

    private final RestTemplate restTemplate;
    private final int eaiServerPort;

    public EaiServerClient(RestTemplate restTemplate,
                            @Value("${eaiserver.http-port:8445}") int eaiServerPort) {
        this.restTemplate = restTemplate;
        this.eaiServerPort = eaiServerPort;
    }

    /**
     * 해당 dcsId 앞으로 eai_server 에 도착한(SND 로 전송되어 수신된) 파일 목록(도착 시각 포함).
     * 실패하면 null (호출자가 "확인 불가"로 표시하도록).
     */
    public List<ReceivedFile> listReceived(String serverIp, String dcsId) {
        String url = baseUrl(serverIp) + "/api/received/" + dcsId;
        try {
            ResponseEntity<List<ReceivedFile>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<List<ReceivedFile>>() {
                    });
            List<ReceivedFile> body = response.getBody();
            return body != null ? body : Collections.emptyList();
        } catch (Exception e) {
            return null;
        }
    }

    public static class ReceivedFile {
        private String fileName;
        private long receivedAt;

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public long getReceivedAt() {
            return receivedAt;
        }

        public void setReceivedAt(long receivedAt) {
            this.receivedAt = receivedAt;
        }
    }

    /**
     * RCV 테스트용 업로드. DCSManager 가 받은 파일을 그대로 eai_server 의 outbox 로 전달한다.
     * eai_agent 가 다음 폴링 때 가져가서 그 dcs 컨테이너의 RCV 폴더에 내려준다.
     * eai_server 는 J 파일명 형식이 아니면 HTTP 200 + {"ok":false,...} 로 응답하므로,
     * HTTP 상태코드만으로 판단하면 안 되고 응답 본문의 ok 값을 봐야 한다(114/115단계).
     */
    public UploadResult uploadToOutbox(String serverIp, String dcsId, String fileName, byte[] content) {
        String url = baseUrl(serverIp) + "/api/outbox/" + dcsId;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource fileResource = new ByteArrayResource(content) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            };
            body.add("file", fileResource);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<?, ?> responseBody = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || responseBody == null) {
                return new UploadResult(false, "eai_server 업로드 실패");
            }
            boolean ok = Boolean.TRUE.equals(responseBody.get("ok"));
            Object message = responseBody.get("message");
            return new UploadResult(ok, message != null ? message.toString() : null);
        } catch (Exception e) {
            return new UploadResult(false, "eai_server 호출 실패: " + e.getMessage());
        }
    }

    public static class UploadResult {
        private final boolean ok;
        private final String message;

        public UploadResult(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }

        public boolean isOk() {
            return ok;
        }

        public String getMessage() {
            return message;
        }
    }

    private String baseUrl(String serverIp) {
        return "http://" + serverIp + ":" + eaiServerPort;
    }
}
