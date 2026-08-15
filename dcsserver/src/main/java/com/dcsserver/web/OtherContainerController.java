package com.dcsserver.web;

import com.dcsserver.repository.DcsConfigRepository;
import com.dcsserver.service.AgentClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * dcs_config 에 등록된 서버 IP 들의 DCSAgent 로부터 "dcs 컨테이너 이외의" 컨테이너 목록을
 * 모아서 보여주고, 개별 컨테이너의 start/stop/삭제 요청을 해당 서버의 DCSAgent 로 중계한다.
 */
@RestController
@RequestMapping("/api/other-containers")
public class OtherContainerController {

    private final DcsConfigRepository dcsConfigRepository;
    private final AgentClient agentClient;

    public OtherContainerController(DcsConfigRepository dcsConfigRepository, AgentClient agentClient) {
        this.dcsConfigRepository = dcsConfigRepository;
        this.agentClient = agentClient;
    }

    @GetMapping
    public List<OtherContainerView> list() {
        List<OtherContainerView> result = new ArrayList<>();
        for (String serverIp : dcsConfigRepository.findDistinctServerIps()) {
            List<AgentOtherContainer> containers = agentClient.listOtherContainers(serverIp);
            if (containers == null) {
                continue;
            }
            for (AgentOtherContainer c : containers) {
                result.add(new OtherContainerView(serverIp, c.getName(), c.getImage(), c.getPorts(), c.isRunning()));
            }
        }
        return result;
    }

    @PostMapping("/{name}/start")
    public ResponseEntity<DeployResult> start(@PathVariable String name, @RequestParam String serverIp) {
        return respond(agentClient.startContainer(serverIp, name));
    }

    @PostMapping("/{name}/stop")
    public ResponseEntity<DeployResult> stop(@PathVariable String name, @RequestParam String serverIp) {
        return respond(agentClient.stopContainer(serverIp, name));
    }

    @PostMapping("/{name}/delete")
    public ResponseEntity<DeployResult> delete(@PathVariable String name, @RequestParam String serverIp) {
        return respond(agentClient.deleteContainer(serverIp, name));
    }

    private ResponseEntity<DeployResult> respond(DeployResult result) {
        return result.isSuccess()
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(result);
    }
}
