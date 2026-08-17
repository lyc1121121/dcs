package com.dcsagent.web;

import com.dcsagent.service.DcsFileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.IOException;

@RestController
@RequestMapping("/api/dcs")
public class DcsAgentController {

    private final DcsFileService dcsFileService;

    public DcsAgentController(DcsFileService dcsFileService) {
        this.dcsFileService = dcsFileService;
    }

    @PostMapping("/{dcsId}/up")
    public ResponseEntity<DcsCommandResult> up(@PathVariable String dcsId,
                                                @Valid @RequestBody DcsProvisionRequest request) throws IOException {
        DcsCommandResult result = dcsFileService.provision(dcsId, request);
        return result.isSuccess()
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    @PostMapping("/{dcsId}/down")
    public ResponseEntity<DcsCommandResult> down(@PathVariable String dcsId) throws IOException {
        DcsCommandResult result = dcsFileService.decommission(dcsId);
        return result.isSuccess()
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    @PostMapping("/{dcsId}/simulate")
    public ResponseEntity<DcsCommandResult> simulate(
            @PathVariable String dcsId,
            @RequestParam String terminalId,
            @RequestParam(defaultValue = "5") int fileCount,
            @RequestParam(defaultValue = "1") int intervalSeconds) throws IOException {
        DcsCommandResult result = dcsFileService.simulateJaqt(dcsId, terminalId, fileCount, intervalSeconds);
        return result.isSuccess()
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }
}
