package com.dcsserver.web;

import com.dcsserver.service.AgentClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PortCheckController {

    private final AgentClient agentClient;

    public PortCheckController(AgentClient agentClient) {
        this.agentClient = agentClient;
    }

    @GetMapping("/port-check")
    public PortCheckResponse check(@RequestParam String ip, @RequestParam int port) {
        AgentPortCheckResult result = agentClient.checkPortInUse(ip, port);
        if (result == null) {
            return new PortCheckResponse(false, null, true);
        }
        return new PortCheckResponse(result.isInUse(), result.getContainerName(), false);
    }
}
