package com.dcsmanager.controller;

import com.dcsmanager.service.PageContentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 162단계: TMSManager "JobRadar" 탭 오른쪽에 "메모연동" 탭을 신설 - JobRadar의 "메모"와
 * PRIVATE의 "private내역"을 한 화면에서 바로 볼 수 있게 모아준다.
 * 163단계: 여기서 바로 추가/수정/삭제(+이미지)까지 가능하게 확장.
 *
 * - JobRadar 메모는 별도 앱(Flask, 다른 DB)이라 서버가 JobRadar의 기존
 *   GET/POST /api/notes, PUT/DELETE /api/notes/{id} 를 그대로 호출해서 중계(proxy)한다
 *   (같은 도커 네트워크 안이라 컨테이너명 jobradar:8090 으로 바로 접근 가능 - 프론트엔드가
 *   직접 다른 오리진을 호출하지 않아도 되게 서버에서 대신 가져온다). 이미지는 JobRadar
 *   메모와 동일하게 클립보드 이미지를 <img> 태그(data: URI)로 그대로 담아서 JobRadar DB에
 *   저장하므로, 실제로는 JobRadar "메모" 탭에서 만든 것과 완전히 같은 데이터다.
 * - private내역은 이미 DCSManager 자체 DB에 있어서(PageContentService, PAGE_KEY="private")
 *   바로 읽고 쓸 수 있지만, 원래 PRIVATE 탭처럼 admin/admin 로그인(세션의
 *   privateAuthenticated)이 없으면 내용을 보여주지도, 저장하지도 않는다 - 여기서 그 보안
 *   경계를 우회하면 안 되기 때문에, 로그인 여부를 매번 서버에서 다시 확인한다.
 */
@Controller
@RequestMapping("/memo-link")
public class MemoLinkController {

    private static final String PRIVATE_SESSION_KEY = "privateAuthenticated";
    private static final String PRIVATE_PAGE_KEY = "private";
    private static final String PRIVATE_DEFAULT_RESOURCE = "private-default.md";

    private final PageContentService pageContentService;
    private final RestTemplate restTemplate;
    private final String jobradarNotesUrl;

    public MemoLinkController(PageContentService pageContentService,
                               RestTemplate restTemplate,
                               @Value("${jobradar.base-url:http://jobradar:8090}") String jobradarBaseUrl) {
        this.pageContentService = pageContentService;
        this.restTemplate = restTemplate;
        this.jobradarNotesUrl = jobradarBaseUrl + "/api/notes";
    }

    @GetMapping
    public String view(Model model, HttpServletRequest request) {
        model.addAttribute("privateUnlocked", isPrivateUnlocked(request));
        if (isPrivateUnlocked(request)) {
            model.addAttribute("privateHtml", pageContentService.getHtml(PRIVATE_PAGE_KEY, PRIVATE_DEFAULT_RESOURCE));
            model.addAttribute("privateMarkdown", pageContentService.getMarkdown(PRIVATE_PAGE_KEY, PRIVATE_DEFAULT_RESOURCE));
        }
        return "memo-link/view";
    }

    private boolean isPrivateUnlocked(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && Boolean.TRUE.equals(session.getAttribute(PRIVATE_SESSION_KEY));
    }

    // ---- JobRadar 메모 중계(조회/추가/수정/삭제 모두 JobRadar API를 그대로 호출) ----

    @GetMapping("/jobradar-notes")
    @ResponseBody
    public ResponseEntity<String> listJobradarNotes() {
        return proxy(HttpMethod.GET, jobradarNotesUrl, null);
    }

    @PostMapping("/jobradar-notes")
    @ResponseBody
    public ResponseEntity<String> createJobradarNote(@RequestBody String body) {
        return proxy(HttpMethod.POST, jobradarNotesUrl, body);
    }

    @PutMapping("/jobradar-notes/{id}")
    @ResponseBody
    public ResponseEntity<String> updateJobradarNote(@PathVariable long id, @RequestBody String body) {
        return proxy(HttpMethod.PUT, jobradarNotesUrl + "/" + id, body);
    }

    @DeleteMapping("/jobradar-notes/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteJobradarNote(@PathVariable long id) {
        return proxy(HttpMethod.DELETE, jobradarNotesUrl + "/" + id, null);
    }

    private ResponseEntity<String> proxy(HttpMethod method, String url, String jsonBody) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = jsonBody != null ? new HttpEntity<>(jsonBody, headers) : new HttpEntity<>(headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, method, entity, String.class);
            return ResponseEntity.status(resp.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(resp.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"ok\":false,\"error\":\"JobRadar 요청 실패\"}");
        }
    }

    // ---- private내역 저장(같은 화면에서 벗어나지 않고 바로 저장) ----

    @PostMapping("/private-content")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> savePrivateContent(@RequestParam String contentMarkdown,
                                                                     HttpServletRequest request) {
        if (!isPrivateUnlocked(request)) {
            Map<String, Object> body = new HashMap<>();
            body.put("ok", false);
            body.put("message", "PRIVATE 로그인이 필요합니다.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
        }
        pageContentService.save(PRIVATE_PAGE_KEY, contentMarkdown);
        Map<String, Object> body = new HashMap<>();
        body.put("ok", true);
        body.put("html", pageContentService.getHtml(PRIVATE_PAGE_KEY, PRIVATE_DEFAULT_RESOURCE));
        return ResponseEntity.ok(body);
    }
}
