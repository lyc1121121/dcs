package com.eaiserver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * eai_agent 연결 1개를 처리한다(연결 1개 = 요청 1건). 프로토콜은 EaiProtocol 참고.
 */
public class EaiConnectionHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(EaiConnectionHandler.class);

    private final Socket socket;
    private final String receiveBaseDir;
    private final OutboxService outboxService;

    public EaiConnectionHandler(Socket socket, String receiveBaseDir, OutboxService outboxService) {
        this.socket = socket;
        this.receiveBaseDir = receiveBaseDir;
        this.outboxService = outboxService;
    }

    @Override
    public void run() {
        try (Socket s = socket;
             DataInputStream in = new DataInputStream(s.getInputStream());
             DataOutputStream out = new DataOutputStream(s.getOutputStream())) {
            byte opType = in.readByte();
            if (opType == EaiProtocol.OP_SEND) {
                handleSend(in, out);
            } else if (opType == EaiProtocol.OP_FETCH_RCV) {
                handleFetchRcv(in, out);
            } else {
                log.warn("unknown opType {} from {}", opType, socket.getRemoteSocketAddress());
            }
        } catch (IOException e) {
            log.warn("connection from {} failed: {}", socket.getRemoteSocketAddress(), e.getMessage());
        }
    }

    // ---- OP_SEND: 에이전트 -> 서버로 파일 올림 (SND 방향) ----

    private void handleSend(DataInputStream in, DataOutputStream out) throws IOException {
        String dcsId = in.readUTF();
        String fileName = in.readUTF();
        long fileLength = in.readLong();

        Path dir = Paths.get(receiveBaseDir, dcsId);
        Files.createDirectories(dir);
        Path finalPath = dir.resolve(fileName);
        Path partPath = dir.resolve(fileName + ".part");

        try {
            writeFileFrom(in, partPath, fileLength);
            Files.move(partPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("received dcsId={} fileName={} ({} bytes) -> {}", dcsId, fileName, fileLength, finalPath);
            out.writeByte(1);
            out.writeUTF("OK");
        } catch (IOException e) {
            log.warn("failed to receive dcsId={} fileName={}: {}", dcsId, fileName, e.getMessage());
            Files.deleteIfExists(partPath);
            out.writeByte(0);
            out.writeUTF("실패: " + e.getMessage());
        }
        out.flush();
    }

    // ---- OP_FETCH_RCV: 에이전트가 자기 앞으로 온 파일이 있는지 물어봄 (RCV 방향) ----

    private void handleFetchRcv(DataInputStream in, DataOutputStream out) throws IOException {
        String dcsId = in.readUTF();
        Optional<Path> pending = outboxService.peekOldest(dcsId);
        if (!pending.isPresent()) {
            out.writeByte(0);
            out.flush();
            return;
        }

        Path file = pending.get();
        String fileName = file.getFileName().toString();
        long fileLength = Files.size(file);

        out.writeByte(1);
        out.writeUTF(fileName);
        out.writeLong(fileLength);
        try (java.io.InputStream fileIn = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fileIn.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        out.flush();

        byte ackStatus = in.readByte();
        if (ackStatus == 1) {
            Files.delete(file);
            log.info("delivered dcsId={} fileName={} ({} bytes), 에이전트 수신 확인 -> outbox 에서 제거", dcsId, fileName, fileLength);
        } else {
            log.warn("delivered dcsId={} fileName={} 했지만 에이전트가 수신 실패로 응답 - outbox 에 남겨서 다음에 재시도", dcsId, fileName);
        }
    }

    private void writeFileFrom(DataInputStream in, Path partPath, long fileLength) throws IOException {
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
