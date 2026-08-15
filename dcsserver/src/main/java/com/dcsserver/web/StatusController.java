package com.dcsserver.web;

import com.dcsserver.service.StatusStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatusController {

    private final StatusStore statusStore;

    public StatusController(StatusStore statusStore) {
        this.statusStore = statusStore;
    }

    @PostMapping("/agent-status")
    public void receiveAgentStatus(@RequestBody List<DcsContainerStatus> statuses) {
        statusStore.update(statuses);
    }

    @GetMapping("/status")
    public Map<String, StatusView> getStatuses() {
        return statusStore.snapshot();
    }
}
