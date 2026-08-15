package com.dcsagent.web;

import com.dcsagent.service.PortCheckService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/ports")
public class PortCheckController {

    private final PortCheckService portCheckService;

    public PortCheckController(PortCheckService portCheckService) {
        this.portCheckService = portCheckService;
    }

    @GetMapping("/{port}/in-use")
    public PortCheckResult check(@PathVariable int port) throws IOException {
        String containerName = portCheckService.findContainerUsingPort(port);
        return new PortCheckResult(containerName != null, containerName);
    }
}
