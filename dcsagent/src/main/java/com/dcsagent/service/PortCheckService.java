package com.dcsagent.service;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 실행 중인 모든 컨테이너(docker ps, dcs 여부 무관)의 호스트 포트 바인딩을 조회해서,
 * 주어진 포트가 실제로 이 서버에서 사용 중인지 확인한다. 중지된 컨테이너는 포트 바인딩이
 * 해제되므로 (docker ps -a 가 아니라) 실행 중인 것만 대상으로 한다.
 */
@Service
public class PortCheckService {

    private static final Logger log = LoggerFactory.getLogger(PortCheckService.class);
    private static final Pattern HOST_PORT = Pattern.compile(":(\\d+)->");

    private final long commandTimeoutSeconds;

    public PortCheckService(@Value("${agent.command-timeout-seconds:120}") long commandTimeoutSeconds) {
        this.commandTimeoutSeconds = commandTimeoutSeconds;
    }

    public String findContainerUsingPort(int port) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("docker", "ps", "--format", "{{.Names}}|||{{.Ports}}");
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
        awaitProcess(process);

        for (String line : lines) {
            String[] parts = line.split("\\|\\|\\|", -1);
            if (parts.length < 2) {
                continue;
            }
            Matcher matcher = HOST_PORT.matcher(parts[1]);
            while (matcher.find()) {
                if (Integer.parseInt(matcher.group(1)) == port) {
                    return parts[0];
                }
            }
        }
        return null;
    }

    private void awaitProcess(Process process) {
        try {
            boolean finished = process.waitFor(commandTimeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("docker ps timed out during port check");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
