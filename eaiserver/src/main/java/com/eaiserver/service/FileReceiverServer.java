package com.eaiserver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 이 에이전트서버에 붙은 eai_agent(dcs 컨테이너별 1개)들의 TCP 연결을 받는 소켓 서버.
 * 서버당 eai_server 는 1개만 뜨고, 여러 eai_agent 의 연결을 동시에(스레드풀) 받는다.
 */
@Service
public class FileReceiverServer implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(FileReceiverServer.class);

    private final int port;
    private final String receiveBaseDir;
    private final OutboxService outboxService;
    private final ExecutorService workers = Executors.newCachedThreadPool();

    private ServerSocket serverSocket;
    private volatile boolean running = true;

    public FileReceiverServer(
            @Value("${eaiserver.port:9500}") int port,
            @Value("${eaiserver.receive-base-dir:/data001/eai_received}") String receiveBaseDir,
            @Value("${eaiserver.outbox-base-dir:/data001/eai_outbox}") String outboxBaseDir) {
        this.port = port;
        this.receiveBaseDir = receiveBaseDir;
        this.outboxService = new OutboxService(outboxBaseDir);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        Thread acceptThread = new Thread(this::acceptLoop, "eai-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    private void acceptLoop() {
        try {
            serverSocket = new ServerSocket(port);
            log.info("EAI file receiver listening on port {}, receiveBaseDir={}", port, receiveBaseDir);
        } catch (IOException e) {
            log.error("failed to bind port {}: {}", port, e.getMessage());
            return;
        }
        while (running) {
            try {
                Socket client = serverSocket.accept();
                workers.submit(new EaiConnectionHandler(client, receiveBaseDir, outboxService));
            } catch (IOException e) {
                if (running) {
                    log.warn("accept failed: {}", e.getMessage());
                }
            }
        }
    }

    @Override
    public void destroy() {
        running = false;
        workers.shutdownNow();
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                log.warn("failed to close server socket: {}", e.getMessage());
            }
        }
    }
}
