package com.dcsagent.service;

import com.dcsagent.web.DcsContainerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * /docker/dcs/* 하위 각 DCS_ID 폴더를 스캔하여, 대응하는 dcs{DCS_ID} 컨테이너가
 * 실행 중인지 docker inspect 로 조회한다. (컨테이너를 생성/시작/중지하지는 않음 - 조회 전용)
 */
@Service
public class DockerStatusChecker {

    private static final Logger log = LoggerFactory.getLogger(DockerStatusChecker.class);
    private static final String AGENT_DIR_NAME = "agent";

    private final File baseDir;

    public DockerStatusChecker(@Value("${agent.base-dir:/docker/dcs}") String baseDir) {
        this.baseDir = new File(baseDir);
    }

    public List<DcsContainerStatus> checkAll() {
        List<DcsContainerStatus> result = new ArrayList<>();
        File[] dirs = baseDir.listFiles(File::isDirectory);
        if (dirs == null) {
            return result;
        }
        for (File dir : dirs) {
            String dcsId = dir.getName();
            if (AGENT_DIR_NAME.equals(dcsId)) {
                continue;
            }
            result.add(new DcsContainerStatus(dcsId, isRunning(dcsId)));
        }
        return result;
    }

    private boolean isRunning(String dcsId) {
        String containerName = "dcs" + dcsId;
        try {
            Process process = new ProcessBuilder(
                    "docker", "inspect", "-f", "{{.State.Running}}", containerName)
                    .redirectErrorStream(true)
                    .start();
            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                output = reader.readLine();
            }
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("docker inspect timed out for {}", containerName);
                return false;
            }
            return "true".equals(output);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.debug("docker inspect failed for {}: {}", containerName, e.getMessage());
            return false;
        }
    }
}
