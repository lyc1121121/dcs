package com.eaiagent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * eai_server 와 같은 프로토콜(eaiserver.service.FileReceiveHandler 참고)로 파일 1개를 전송한다.
 *   writeUTF  dcsId
 *   writeUTF  fileName
 *   writeLong fileLength
 *   write     fileLength 바이트
 * 응답: readByte status(1=성공), readUTF message
 */
@Service
public class EaiFileSender {

    private static final Logger log = LoggerFactory.getLogger(EaiFileSender.class);
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int SOCKET_TIMEOUT_MS = 30000;

    private final String serverHost;
    private final int serverPort;

    public EaiFileSender(
            @Value("${eaiagent.server-host}") String serverHost,
            @Value("${eaiagent.server-port:9500}") int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public boolean send(String dcsId, Path file) {
        String fileName = file.getFileName().toString();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(serverHost, serverPort), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);
            try (DataInputStream in = new DataInputStream(socket.getInputStream());
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                byte[] content = Files.readAllBytes(file);
                out.writeByte(EaiProtocol.OP_SEND);
                out.writeUTF(dcsId);
                out.writeUTF(fileName);
                out.writeLong(content.length);
                out.write(content);
                out.flush();

                int status = in.readByte();
                String message = in.readUTF();
                if (status == 1) {
                    log.info("sent {} ({} bytes) to {}:{} - {}", fileName, content.length, serverHost, serverPort, message);
                    return true;
                }
                log.warn("eai_server rejected {}: {}", fileName, message);
                return false;
            }
        } catch (IOException e) {
            log.warn("failed to send {} to {}:{} - {}", fileName, serverHost, serverPort, e.getMessage());
            return false;
        }
    }
}
