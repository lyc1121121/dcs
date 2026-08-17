package com.eaiserver.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * dcsId 별로 "앞으로 그 에이전트에 내려줘야 할 파일"이 쌓이는 발송함(outbox).
 * 지금은 사람이(또는 나중에 AP서버 연동이) {outboxBaseDir}/{dcsId}/ 아래에 파일을 두면,
 * eai_agent 가 폴링할 때 그중 가장 오래된 파일 1개를 내려준다.
 */
public class OutboxService {

    private final String outboxBaseDir;

    public OutboxService(String outboxBaseDir) {
        this.outboxBaseDir = outboxBaseDir;
    }

    public Optional<Path> peekOldest(String dcsId) throws IOException {
        Path dir = Paths.get(outboxBaseDir, dcsId);
        if (!Files.isDirectory(dir)) {
            return Optional.empty();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(Files::isRegularFile)
                    .filter(p -> !p.getFileName().toString().endsWith(".part"))
                    .min(Comparator.comparing(this::lastModifiedSafe));
        }
    }

    private long lastModifiedSafe(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return Long.MAX_VALUE;
        }
    }
}
