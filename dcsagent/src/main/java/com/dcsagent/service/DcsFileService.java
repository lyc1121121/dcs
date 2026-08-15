package com.dcsagent.service;

import com.dcsagent.web.DcsCommandResult;
import com.dcsagent.web.DcsProvisionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

/**
 * DCSAgent 는 .env / docker-compose.yml 파일을 생성한 뒤, 실제로 docker-compose up -d / down 을 실행한다.
 */
@Service
public class DcsFileService {

    private static final Logger log = LoggerFactory.getLogger(DcsFileService.class);
    private static final String COMPOSE_TEMPLATE = "templates/docker-compose.yml.template";

    private final String baseDir;
    private final String composeCommand;
    private final long commandTimeoutSeconds;
    private final String dataBaseDir;
    private final String logsBaseDir;
    private final String runtimeOwner;

    public DcsFileService(
            @Value("${agent.base-dir:/docker/dcs}") String baseDir,
            @Value("${agent.compose-command:docker-compose}") String composeCommand,
            @Value("${agent.command-timeout-seconds:120}") long commandTimeoutSeconds,
            @Value("${agent.data-base-dir:/data001}") String dataBaseDir,
            @Value("${agent.logs-base-dir:/logs001}") String logsBaseDir,
            @Value("${agent.runtime-owner:5000:5000}") String runtimeOwner) {
        this.baseDir = baseDir;
        this.composeCommand = composeCommand;
        this.commandTimeoutSeconds = commandTimeoutSeconds;
        this.dataBaseDir = dataBaseDir;
        this.logsBaseDir = logsBaseDir;
        this.runtimeOwner = runtimeOwner;
    }

    public DcsCommandResult provision(String dcsId, DcsProvisionRequest req) throws IOException {
        Path dir = Paths.get(baseDir, dcsId);
        Files.createDirectories(dir);
        ensureRuntimeDirectories(dcsId);

        String envContent = "DCS_IMAGE_VERSION=" + req.getDcsImageVersion() + "\n"
                + "DCS_ID=" + dcsId + "\n"
                + "DCS_DOT_ID=" + req.getDcsDotId() + "\n"
                + "DCS_MODE=" + req.getDcsMode() + "\n"
                + "DCS_SIZE=" + req.getDcsSize() + "\n"
                + "PORT_DCS1=" + req.getPortDcs1() + "\n"
                + "PORT_DCS2=" + req.getPortDcs2() + "\n";
        Files.write(dir.resolve(".env"), envContent.getBytes(StandardCharsets.UTF_8));

        try (InputStream in = new ClassPathResource(COMPOSE_TEMPLATE).getInputStream()) {
            Files.copy(in, dir.resolve("docker-compose.yml"), StandardCopyOption.REPLACE_EXISTING);
        }
        log.info("Provisioned .env + docker-compose.yml at {}", dir);

        return runCompose(dir, "up", "-d");
    }

    /**
     * docker-compose.yml 이 마운트하는 /data001/dcs{DCS_ID}/data001, ssc, /logs001/dcs{DCS_ID} 를
     * 컨테이너가 실행되는 uid(runtime-owner)가 쓸 수 있도록 매 up 요청마다 미리 만들고 소유권을 맞춘다.
     * 그렇지 않으면 Docker 데몬이 root 소유로 자동 생성해서, 컨테이너 내부(uid 5000)가
     * 최초 skeleton 데이터를 옮기지 못해 기동에 실패한다.
     */
    private void ensureRuntimeDirectories(String dcsId) throws IOException {
        String containerDirName = "dcs" + dcsId;
        Path dataDir = Paths.get(dataBaseDir, containerDirName, "data001");
        Path sscDir = Paths.get(dataBaseDir, containerDirName, "ssc");
        Path logsDir = Paths.get(logsBaseDir, containerDirName);
        Files.createDirectories(dataDir);
        Files.createDirectories(sscDir);
        Files.createDirectories(logsDir);
        chown(dataDir, sscDir, logsDir);
    }

    private void chown(Path... paths) throws IOException {
        for (Path p : paths) {
            ProcessBuilder pb = new ProcessBuilder("chown", runtimeOwner, p.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }
            try {
                boolean finished = process.waitFor(10, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    log.warn("chown timed out for {}", p);
                } else if (process.exitValue() != 0) {
                    log.warn("chown {} {} failed: {}", runtimeOwner, p, output);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public DcsCommandResult decommission(String dcsId) throws IOException {
        Path dir = Paths.get(baseDir, dcsId);
        if (!Files.isDirectory(dir)) {
            return new DcsCommandResult(false, "폴더가 존재하지 않습니다: " + dir, "");
        }
        return runCompose(dir, "down");
    }

    private DcsCommandResult runCompose(Path workDir, String... args) throws IOException {
        String[] command = new String[args.length + 1];
        command[0] = composeCommand;
        System.arraycopy(args, 0, command, 1, args.length);

        log.info("Running '{}' in {}", String.join(" ", command), workDir);
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        }

        boolean finished;
        try {
            finished = process.waitFor(commandTimeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new DcsCommandResult(false, "명령 실행이 중단되었습니다.", output.toString());
        }

        if (!finished) {
            process.destroyForcibly();
            return new DcsCommandResult(false, "명령 실행이 시간 초과되었습니다 (" + commandTimeoutSeconds + "s).", output.toString());
        }

        int exitCode = process.exitValue();
        boolean success = exitCode == 0;
        String message = success ? "성공" : ("실패 (exit=" + exitCode + ")");
        return new DcsCommandResult(success, message, output.toString());
    }
}
