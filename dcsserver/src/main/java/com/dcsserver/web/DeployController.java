package com.dcsserver.web;

import com.dcsserver.domain.DcsConfig;
import com.dcsserver.repository.DcsConfigRepository;
import com.dcsserver.service.AgentClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api")
public class DeployController {

    private final DcsConfigRepository dcsConfigRepository;
    private final AgentClient agentClient;

    public DeployController(DcsConfigRepository dcsConfigRepository, AgentClient agentClient) {
        this.dcsConfigRepository = dcsConfigRepository;
        this.agentClient = agentClient;
    }

    @PostMapping("/deploy/{dcsId}")
    public ResponseEntity<DeployResult> deploy(@PathVariable String dcsId) {
        DcsConfig config = findOrThrow(dcsId);
        DeployResult result = agentClient.up(config);
        return respond(result);
    }

    @PostMapping("/undeploy/{dcsId}")
    public ResponseEntity<DeployResult> undeploy(@PathVariable String dcsId) {
        DcsConfig config = findOrThrow(dcsId);
        DeployResult result = agentClient.down(config);
        return respond(result);
    }

    @PostMapping("/simulate/{dcsId}")
    public ResponseEntity<DeployResult> simulate(@PathVariable String dcsId,
                                                   @RequestParam String terminalId,
                                                   @RequestParam(defaultValue = "5") int fileCount,
                                                   @RequestParam(defaultValue = "1") int intervalSeconds) {
        DcsConfig config = findOrThrow(dcsId);
        DeployResult result = agentClient.simulateJaqt(config, terminalId, fileCount, intervalSeconds);
        return respond(result);
    }

    private DcsConfig findOrThrow(String dcsId) {
        return dcsConfigRepository.findById(dcsId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 DCS_ID 입니다: " + dcsId));
    }

    private ResponseEntity<DeployResult> respond(DeployResult result) {
        return result.isSuccess()
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(result);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<DeployResult> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new DeployResult(false, e.getMessage()));
    }
}
