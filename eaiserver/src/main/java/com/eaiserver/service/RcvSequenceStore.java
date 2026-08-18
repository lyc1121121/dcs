package com.eaiserver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 114/115단계: RCV 테스트로 업로드된 J 파일에 매핑되는 I 파일의 일련번호(00001~)를
 * DCS_ID + 날짜별로 관리한다. 날짜가 바뀌면 다시 00001부터 시작하고, eai_server 가
 * 재기동돼도 값이 유지되도록 디스크(파일)에 저장한다.
 */
@Service
public class RcvSequenceStore {

    private static final Logger log = LoggerFactory.getLogger(RcvSequenceStore.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final Path stateDir;
    private final Object lock = new Object();

    public RcvSequenceStore(@Value("${eaiserver.rcv-seq-dir:/data001/eai_outbox/.seq}") String stateDir) {
        this.stateDir = Paths.get(stateDir);
    }

    public String today() {
        return LocalDate.now(KST).format(DATE_FMT);
    }

    /** 오늘 날짜 기준 다음 일련번호(1부터 시작)를 반환한다. */
    public int nextSequence(String dcsId) {
        String today = today();
        synchronized (lock) {
            try {
                Files.createDirectories(stateDir);
                Path stateFile = stateDir.resolve(dcsId + ".seq");
                int seq = 1;
                if (Files.isRegularFile(stateFile)) {
                    String content = new String(Files.readAllBytes(stateFile), StandardCharsets.UTF_8).trim();
                    String[] parts = content.split(":", 2);
                    if (parts.length == 2 && parts[0].equals(today)) {
                        seq = Integer.parseInt(parts[1]) + 1;
                    }
                }
                Path tmp = Files.createTempFile(stateDir, ".seq", ".tmp");
                Files.write(tmp, (today + ":" + seq).getBytes(StandardCharsets.UTF_8));
                Files.move(tmp, stateFile, StandardCopyOption.REPLACE_EXISTING);
                return seq;
            } catch (IOException e) {
                log.error("RCV 일련번호 저장 실패 (dcsId={})", dcsId, e);
                return 1;
            }
        }
    }
}
