package com.eaiagent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 이 dcs${DCS_ID} 컨테이너의 SND 폴더(연계에이전트 관점에서 "송신폴더")를 주기적으로 훑어서,
 * 새로 생긴 파일을 eai_server 로 보내고, 전송 성공한 파일은 로컬에서 지운다(재전송 방지).
 * 전송에 실패한 파일은 다음 주기에 다시 시도한다.
 *
 * 기동 시점에 이미 폴더에 있던 파일(과거 누적분)은 baseline 으로 기록해두고 영구히 건드리지
 * 않는다 - 기동 "이후"에 새로 생긴 파일만 대상으로 한다.
 */
@Component
public class SndFolderWatcher {

    private static final Logger log = LoggerFactory.getLogger(SndFolderWatcher.class);
    private static final long MIN_AGE_MS = 1000; // 아직 쓰는 중인 파일을 건드리지 않기 위한 최소 대기

    private final String dcsId;
    private final Path sndDir;
    private final EaiFileSender sender;
    private final Set<String> baselineFileNames = ConcurrentHashMap.newKeySet();
    private volatile boolean baselineCaptured = false;

    public SndFolderWatcher(
            @Value("${eaiagent.dcs-id}") String dcsId,
            @Value("${eaiagent.snd-dir}") String sndDir,
            EaiFileSender sender) {
        this.dcsId = dcsId;
        this.sndDir = Paths.get(sndDir);
        this.sender = sender;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void captureBaseline() {
        Set<String> existing = listFileNames();
        baselineFileNames.addAll(existing);
        baselineCaptured = true;
        log.info("baseline captured: {} 개 기존 파일은 대상에서 제외됩니다 ({})", existing.size(), sndDir);
    }

    @Scheduled(fixedDelayString = "${eaiagent.poll-interval-ms:2000}")
    public void poll() {
        if (!baselineCaptured || !Files.isDirectory(sndDir)) {
            return;
        }
        try (Stream<Path> files = Files.list(sndDir)) {
            files.filter(Files::isRegularFile)
                    .filter(f -> !baselineFileNames.contains(f.getFileName().toString()))
                    .filter(this::isSettled)
                    .forEach(this::sendAndRemove);
        } catch (IOException e) {
            log.warn("failed to list {}: {}", sndDir, e.getMessage());
        }
    }

    private Set<String> listFileNames() {
        if (!Files.isDirectory(sndDir)) {
            return Collections.emptySet();
        }
        try (Stream<Path> files = Files.list(sndDir)) {
            Set<String> names = new HashSet<>();
            files.filter(Files::isRegularFile).forEach(f -> names.add(f.getFileName().toString()));
            return names;
        } catch (IOException e) {
            log.warn("failed to list {}: {}", sndDir, e.getMessage());
            return Collections.emptySet();
        }
    }

    private boolean isSettled(Path file) {
        try {
            long modifiedMs = Files.getLastModifiedTime(file).toMillis();
            return Instant.now().toEpochMilli() - modifiedMs >= MIN_AGE_MS;
        } catch (IOException e) {
            return false;
        }
    }

    private void sendAndRemove(Path file) {
        boolean ok = sender.send(dcsId, file);
        if (ok) {
            try {
                Files.delete(file);
            } catch (IOException e) {
                log.warn("sent but failed to delete local file {}: {}", file, e.getMessage());
            }
        }
    }
}
