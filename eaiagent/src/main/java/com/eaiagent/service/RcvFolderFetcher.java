package com.eaiagent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 주기적으로 eai_server 에 "내 dcsId 앞으로 온 파일 있어?" 라고 물어보고(OP_FETCH_RCV),
 * 있으면 받아서 RCV 폴더에 저장한다(연계에이전트 관점의 "수신폴더"). 서버가 에이전트에게
 * 직접 연결하는 게 아니라, 에이전트가 항상 먼저 연결하는 pull 방식이라 방화벽/NAT 환경에서도
 * 안전하다.
 */
@Component
public class RcvFolderFetcher {

    private static final Logger log = LoggerFactory.getLogger(RcvFolderFetcher.class);
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int SOCKET_TIMEOUT_MS = 30000;

    private final String dcsId;
    private final Path rcvDir;
    private final String serverHost;
    private final int serverPort;

    public RcvFolderFetcher(
            @Value("${eaiagent.dcs-id}") String dcsId,
            @Value("${eaiagent.rcv-dir}") String rcvDir,
            @Value("${eaiagent.server-host}") String serverHost,
            @Value("${eaiagent.server-port:9500}") int serverPort) {
        this.dcsId = dcsId;
        this.rcvDir = Paths.get(rcvDir);
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    @Scheduled(fixedDelayString = "${eaiagent.poll-interval-ms:2000}")
    public void poll() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(serverHost, serverPort), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);
            try (DataInputStream in = new DataInputStream(socket.getInputStream());
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                out.writeByte(EaiProtocol.OP_FETCH_RCV);
                out.writeUTF(dcsId);
                out.flush();

                boolean hasFile = in.readByte() == 1;
                if (!hasFile) {
                    return;
                }
                receiveOneFile(in, out);
            }
        } catch (IOException e) {
            log.debug("fetch-rcv failed (서버가 아직 안 떴거나 일시적 오류일 수 있음): {}", e.getMessage());
        }
    }

    private void receiveOneFile(DataInputStream in, DataOutputStream out) throws IOException {
        String fileName = in.readUTF();
        long fileLength = in.readLong();

        Files.createDirectories(rcvDir);
        Path finalPath = rcvDir.resolve(fileName);
        Path partPath = rcvDir.resolve(fileName + ".part");

        try {
            writeFile(in, partPath, fileLength);
            Files.move(partPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("received (RCV) fileName={} ({} bytes) -> {}", fileName, fileLength, finalPath);
            out.writeByte(1);
        } catch (IOException e) {
            log.warn("failed to receive (RCV) fileName={}: {}", fileName, e.getMessage());
            Files.deleteIfExists(partPath);
            out.writeByte(0);
        }
        out.flush();
    }

    private void writeFile(DataInputStream in, Path partPath, long fileLength) throws IOException {
        byte[] buffer = new byte[8192];
        long remaining = fileLength;
        try (BufferedOutputStream fileOut = new BufferedOutputStream(Files.newOutputStream(partPath))) {
            while (remaining > 0) {
                int toRead = (int) Math.min(buffer.length, remaining);
                int read = in.read(buffer, 0, toRead);
                if (read == -1) {
                    throw new IOException("연결이 끊겨서 파일을 끝까지 받지 못했습니다 (남은 바이트: " + remaining + ")");
                }
                fileOut.write(buffer, 0, read);
                remaining -= read;
            }
        }
    }
}
