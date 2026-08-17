package com.eaiserver.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * DCSManager 가 "eai_agent 가 보낸 파일들이 실제로 다 도착했는지" 확인할 때 쓰는 조회 API.
 * 각 파일의 실제 도착 시각(eai_server 에 저장된 시각)도 함께 내려줘서, DCSManager 가
 * "시뮬레이션 시작 버튼을 누른 시점 이후 도착분"만 걸러낼 수 있게 한다(107/109단계).
 * 아직 별도 인증은 없다(1차 구현 범위 밖) - 필요해지면 DCSAgent 처럼 API 키 필터를 추가하면 된다.
 */
@RestController
public class ReceivedFilesController {

    private final String receiveBaseDir;

    public ReceivedFilesController(@Value("${eaiserver.receive-base-dir:/data001/eai_received}") String receiveBaseDir) {
        this.receiveBaseDir = receiveBaseDir;
    }

    @GetMapping("/api/received/{dcsId}")
    public List<ReceivedFile> list(@PathVariable String dcsId) throws IOException {
        Path dir = Paths.get(receiveBaseDir, dcsId);
        if (!Files.isDirectory(dir)) {
            return Collections.emptyList();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(p -> !p.getFileName().toString().endsWith(".part"))
                    .map(p -> {
                        try {
                            return new ReceivedFile(p.getFileName().toString(), Files.getLastModifiedTime(p).toMillis());
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    })
                    .collect(Collectors.toList());
        }
    }

    public static class ReceivedFile {
        private final String fileName;
        private final long receivedAt;

        public ReceivedFile(String fileName, long receivedAt) {
            this.fileName = fileName;
            this.receivedAt = receivedAt;
        }

        public String getFileName() {
            return fileName;
        }

        public long getReceivedAt() {
            return receivedAt;
        }
    }
}
