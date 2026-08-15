package com.dcsserver.web;

import com.dcsserver.service.AgentClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ConnectivityController {

    private final AgentClient agentClient;

    public ConnectivityController(AgentClient agentClient) {
        this.agentClient = agentClient;
    }

    @GetMapping("/check-connectivity")
    public Map<String, Boolean> checkConnectivity(@RequestParam String ip) {
        return Collections.singletonMap("reachable", agentClient.checkConnectivity(ip));
    }
}
