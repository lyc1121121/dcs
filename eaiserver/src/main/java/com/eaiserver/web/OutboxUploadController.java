package com.eaiserver.web;

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
import java.util.Collections;
import java.util.Map;

/**
 * DCSManager 화면(드래그드롭 등)에서 특정 dcsId 앞으로 RCV 테스트 파일을 올릴 때 쓰는 API.
 * 여기 저장된 파일은 eai_agent 가 다음 폴링 때 가져가서 그 dcs 컨테이너의 RCV 폴더에 내려준다.
 * 아직 별도 인증은 없다(1차 구현 범위 밖).
 */
@RestController
public class OutboxUploadController {

    private final String outboxBaseDir;

    public OutboxUploadController(@Value("${eaiserver.outbox-base-dir:/data001/eai_outbox}") String outboxBaseDir) {
        this.outboxBaseDir = outboxBaseDir;
    }

    @PostMapping("/api/outbox/{dcsId}")
    public Map<String, Object> upload(@PathVariable String dcsId, @RequestParam("file") MultipartFile file) throws IOException {
        Path dir = Paths.get(outboxBaseDir, dcsId);
        Files.createDirectories(dir);
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = "upload_" + System.currentTimeMillis();
        }
        Path dest = dir.resolve(Paths.get(fileName).getFileName().toString());
        file.transferTo(dest);
        return Collections.singletonMap("ok", true);
    }
}
