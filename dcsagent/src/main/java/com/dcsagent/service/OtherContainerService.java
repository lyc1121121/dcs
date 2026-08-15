package com.dcsagent.service;

import com.dcsagent.web.DcsCommandResult;
import com.dcsagent.web.OtherContainerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * dcs{DCS_ID} 형태로 DCSAgent 가 자체 관리하는 컨테이너 이외의, 해당 서버에 떠 있는
 * 나머지 컨테이너 목록을 조회하고 start/stop/삭제 한다 (docker-compose 재구성은 하지 않음).
 */
@Service
public class OtherContainerService {

    private static final Logger log = LoggerFactory.getLogger(OtherContainerService.class);
    private static final Pattern DCS_MANAGED_NAME = Pattern.compile("^dcs\\d+$");

    private final long commandTimeoutSeconds;

    public OtherContainerService(@Value("${agent.command-timeout-seconds:120}") long commandTimeoutSeconds) {
        this.commandTimeoutSeconds = commandTimeoutSeconds;
    }

    public List<OtherContainerInfo> list() throws IOException {
        List<OtherContainerInfo> result = new ArrayList<>();
        ProcessBuilder pb = new ProcessBuilder(
                "docker", "ps", "-a", "--format", "{{.Names}}|||{{.Image}}|||{{.Ports}}|||{{.State}}");
        pb.redirectErrorStream(true);
        Process process = pb.start();
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        awaitProcess(process, "docker ps");

        for (String line : lines) {
            String[] parts = line.split("\\|\\|\\|", -1);
            if (parts.length < 4) {
                continue;
            }
            String name = parts[0];
            if (DCS_MANAGED_NAME.matcher(name).matches()) {
                continue;
            }
            result.add(new OtherContainerInfo(name, parts[1], parts[2], "running".equals(parts[3])));
        }
        return result;
    }

    public DcsCommandResult start(String name) throws IOException {
        guard(name);
        return runDocker("start", name);
    }

    public DcsCommandResult stop(String name) throws IOException {
        guard(name);
        return runDocker("stop", name);
    }

    public DcsCommandResult delete(String name) throws IOException {
        guard(name);
        return runDocker("rm", name);
    }

    private void guard(String name) {
        if (DCS_MANAGED_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("dcs 컨테이너는 이 API로 제어할 수 없습니다: " + name);
        }
    }

    private DcsCommandResult runDocker(String action, String name) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("docker", action, name);
        pb.redirectErrorStream(true);
        log.info("Running 'docker {} {}'", action, name);
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        }
        boolean finished = awaitProcess(process, "docker " + action);
        if (!finished) {
            return new DcsCommandResult(false, "명령 실행이 시간 초과되었습니다 (" + commandTimeoutSeconds + "s).", output.toString());
        }
        boolean success = process.exitValue() == 0;
        return new DcsCommandResult(success, success ? "성공" : ("실패 (exit=" + process.exitValue() + ")"), output.toString());
    }

    private boolean awaitProcess(Process process, String label) {
        try {
            boolean finished = process.waitFor(commandTimeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("{} timed out", label);
            }
            return finished;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
