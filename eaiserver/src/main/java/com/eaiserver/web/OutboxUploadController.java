package com.eaiserver.web;

import com.eaiserver.service.JToIFileMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * DCSManager 화면(드래그드롭 등)에서 특정 dcsId 앞으로 RCV 테스트 파일을 올릴 때 쓰는 API.
 * 여기 저장된 파일은 eai_agent 가 다음 폴링 때 가져가서 그 dcs 컨테이너의 RCV 폴더에 내려준다.
 * 114/115단계: "J" 로 시작하는 파일만 받고, 파일명 안의 날짜를 오늘 기준으로 치환한 뒤,
 * 짝이 되는 "I" 파일(XML)도 같이 만들어서 outbox 에 저장한다.
 * 아직 별도 인증은 없다(1차 구현 범위 밖).
 */
@RestController
public class OutboxUploadController {

    private static final Logger log = LoggerFactory.getLogger(OutboxUploadController.class);

    private final String outboxBaseDir;
    private final JToIFileMapper jToIFileMapper;

    public OutboxUploadController(@Value("${eaiserver.outbox-base-dir:/data001/eai_outbox}") String outboxBaseDir,
                                   JToIFileMapper jToIFileMapper) {
        this.outboxBaseDir = outboxBaseDir;
        this.jToIFileMapper = jToIFileMapper;
    }

    @PostMapping("/api/outbox/{dcsId}")
    public Map<String, Object> upload(@PathVariable String dcsId, @RequestParam("file") MultipartFile file) throws IOException {
        String originalFileName = file.getOriginalFilename();
        if (!JToIFileMapper.isJFileName(originalFileName)) {
            Map<String, Object> body = new HashMap<>();
            body.put("ok", false);
            body.put("message", "\"J{4자리코드}.타임스탬프.날짜\" 형식의 파일만 업로드할 수 있습니다: " + originalFileName);
            return body;
        }

        Path dir = Paths.get(outboxBaseDir, dcsId);
        Files.createDirectories(dir);

        JToIFileMapper.Mapped mapped;
        try {
            mapped = jToIFileMapper.map(originalFileName, dcsId);
        } catch (IllegalArgumentException e) {
            Map<String, Object> body = new HashMap<>();
            body.put("ok", false);
            body.put("message", e.getMessage());
            return body;
        }

        Path jDest = dir.resolve(mapped.jFileName);
        file.transferTo(jDest);

        Path iDest = dir.resolve(mapped.iFileName);
        Files.write(iDest, mapped.iFileContent);
        log.info("RCV 업로드: {} -> J={}, I={}", originalFileName, mapped.jFileName, mapped.iFileName);

        Map<String, Object> body = new HashMap<>();
        body.put("ok", true);
        body.put("jFileName", mapped.jFileName);
        body.put("iFileName", mapped.iFileName);
        return body;
    }
}
