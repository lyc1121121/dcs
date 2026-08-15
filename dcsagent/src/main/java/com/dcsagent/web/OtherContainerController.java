package com.dcsagent.web;

import com.dcsagent.service.OtherContainerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/containers")
public class OtherContainerController {

    private final OtherContainerService otherContainerService;

    public OtherContainerController(OtherContainerService otherContainerService) {
        this.otherContainerService = otherContainerService;
    }

    @GetMapping
    public List<OtherContainerInfo> list() throws IOException {
        return otherContainerService.list();
    }

    @PostMapping("/{name}/start")
    public ResponseEntity<DcsCommandResult> start(@PathVariable String name) throws IOException {
        return respond(otherContainerService.start(name));
    }

    @PostMapping("/{name}/stop")
    public ResponseEntity<DcsCommandResult> stop(@PathVariable String name) throws IOException {
        return respond(otherContainerService.stop(name));
    }

    @PostMapping("/{name}/delete")
    public ResponseEntity<DcsCommandResult> delete(@PathVariable String name) throws IOException {
        return respond(otherContainerService.delete(name));
    }

    private ResponseEntity<DcsCommandResult> respond(DcsCommandResult result) {
        return result.isSuccess()
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<DcsCommandResult> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new DcsCommandResult(false, e.getMessage(), ""));
    }
}
