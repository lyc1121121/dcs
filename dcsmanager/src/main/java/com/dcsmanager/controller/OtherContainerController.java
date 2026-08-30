package com.dcsmanager.controller;

import com.dcsmanager.service.DcsServerClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/containers")
public class OtherContainerController {

    private final DcsServerClient dcsServerClient;

    public OtherContainerController(DcsServerClient dcsServerClient) {
        this.dcsServerClient = dcsServerClient;
    }

    @PostMapping("/{name}/start")
    public String start(@PathVariable String name, @RequestParam String serverIp, RedirectAttributes redirectAttributes) {
        DcsServerClient.DcsServerResult result = dcsServerClient.startOtherContainer(serverIp, name);
        flash(redirectAttributes, result, "[" + name + "] 시작 요청 결과: ");
        return "redirect:/dcs#tab2";
    }

    @PostMapping("/{name}/stop")
    public String stop(@PathVariable String name, @RequestParam String serverIp, RedirectAttributes redirectAttributes) {
        DcsServerClient.DcsServerResult result = dcsServerClient.stopOtherContainer(serverIp, name);
        flash(redirectAttributes, result, "[" + name + "] 중지 요청 결과: ");
        return "redirect:/dcs#tab2";
    }

    @PostMapping("/{name}/delete")
    public String delete(@PathVariable String name, @RequestParam String serverIp, RedirectAttributes redirectAttributes) {
        boolean running = dcsServerClient.listOtherContainers().stream()
                .anyMatch(c -> c.getName().equals(name) && c.getServerIp().equals(serverIp) && c.isRunning());
        if (running) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "[" + name + "] 는 실행 중이라 삭제할 수 없습니다. 먼저 내려주세요.");
            return "redirect:/dcs#tab2";
        }
        DcsServerClient.DcsServerResult result = dcsServerClient.deleteOtherContainer(serverIp, name);
        flash(redirectAttributes, result, "[" + name + "] 삭제 요청 결과: ");
        return "redirect:/dcs#tab2";
    }

    private void flash(RedirectAttributes redirectAttributes, DcsServerClient.DcsServerResult result, String prefix) {
        redirectAttributes.addFlashAttribute(
                result.isSuccess() ? "message" : "errorMessage", prefix + result.getMessage());
    }
}
