package com.dcsmanager.controller;

import com.dcsmanager.service.DcsServerClient;
import com.dcsmanager.service.DcsService;
import com.dcsmanager.service.StatusCache;
import com.dcsmanager.web.DcsForm;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

@Controller
@RequestMapping("/dcs")
public class DcsController {

    private final DcsService dcsService;
    private final DcsServerClient dcsServerClient;
    private final StatusCache statusCache;

    public DcsController(DcsService dcsService, DcsServerClient dcsServerClient, StatusCache statusCache) {
        this.dcsService = dcsService;
        this.dcsServerClient = dcsServerClient;
        this.statusCache = statusCache;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("dcsList", dcsService.findAll());
        model.addAttribute("statuses", statusCache.get());
        model.addAttribute("otherContainers", dcsServerClient.listOtherContainers());
        if (!model.containsAttribute("dcsForm")) {
            model.addAttribute("dcsForm", new DcsForm());
        }
        return "dcs/list";
    }

    @GetMapping("/new")
    public String createForm() {
        return "redirect:/dcs";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("dcsForm") DcsForm form,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return reopenCreateModal(model);
        }
        if (!dcsServerClient.checkConnectivity(form.getDcsServerIp())) {
            model.addAttribute("popupError",
                    "DCS_SERVER_IP [" + form.getDcsServerIp() + "] 와 통신할 수 없습니다. 주소를 확인하고 다시 입력해주세요.");
            return reopenCreateModal(model);
        }
        String portError = findPortInUseError(form, null);
        if (portError != null) {
            model.addAttribute("popupError", portError);
            return reopenCreateModal(model);
        }
        try {
            dcsService.create(form);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("global", ex.getMessage());
            return reopenCreateModal(model);
        }
        redirectAttributes.addFlashAttribute("message", "DCS_ID [" + form.getDcsId() + "] 가 생성되었습니다.");
        return "redirect:/dcs";
    }

    private String reopenCreateModal(Model model) {
        model.addAttribute("dcsList", dcsService.findAll());
        model.addAttribute("statuses", statusCache.get());
        model.addAttribute("otherContainers", dcsServerClient.listOtherContainers());
        model.addAttribute("openCreateModal", true);
        return "dcs/list";
    }

    @GetMapping("/{dcsId}/edit")
    public String editForm() {
        return "redirect:/dcs";
    }

    @PostMapping("/{dcsId}")
    public String update(@PathVariable String dcsId,
                          @Valid @ModelAttribute("dcsForm") DcsForm form,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        form.setDcsId(dcsId);
        if (bindingResult.hasErrors()) {
            return reopenEditModal(model);
        }
        if (!dcsServerClient.checkConnectivity(form.getDcsServerIp())) {
            model.addAttribute("popupError",
                    "DCS_SERVER_IP [" + form.getDcsServerIp() + "] 와 통신할 수 없습니다. 주소를 확인하고 다시 입력해주세요.");
            return reopenEditModal(model);
        }
        String portError = findPortInUseError(form, "dcs" + dcsId);
        if (portError != null) {
            model.addAttribute("popupError", portError);
            return reopenEditModal(model);
        }
        try {
            dcsService.update(dcsId, form);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("global", ex.getMessage());
            return reopenEditModal(model);
        }
        redirectAttributes.addFlashAttribute("message", "DCS_ID [" + dcsId + "] 가 수정되었습니다.");
        return "redirect:/dcs";
    }

    private String reopenEditModal(Model model) {
        model.addAttribute("dcsList", dcsService.findAll());
        model.addAttribute("statuses", statusCache.get());
        model.addAttribute("otherContainers", dcsServerClient.listOtherContainers());
        model.addAttribute("openEditModal", true);
        return "dcs/list";
    }

    /**
     * PORT_DCS1/PORT_DCS2 가 대상 서버에서 실제로 사용 중인지 확인한다.
     * excludeContainerName 은 수정 시 자기 자신이 이미 점유 중인 포트를 오탐하지 않도록 제외한다.
     * 확인 자체가 실패한 경우(checkFailed)는 저장을 막지 않는다.
     */
    private String findPortInUseError(DcsForm form, String excludeContainerName) {
        for (Integer port : new Integer[]{form.getPortDcs1(), form.getPortDcs2()}) {
            if (port == null) {
                continue;
            }
            DcsServerClient.PortCheckResult result = dcsServerClient.checkPortInUse(form.getDcsServerIp(), port);
            if (result == null || result.isCheckFailed() || !result.isInUse()) {
                continue;
            }
            if (excludeContainerName != null && excludeContainerName.equals(result.getContainerName())) {
                continue;
            }
            String owner = result.getContainerName() != null ? " (사용중: " + result.getContainerName() + ")" : "";
            return "PORT [" + port + "] 는 서버 [" + form.getDcsServerIp() + "] 에서 이미 사용중입니다" + owner + ".";
        }
        return null;
    }

    @PostMapping("/{dcsId}/up")
    public String up(@PathVariable String dcsId, RedirectAttributes redirectAttributes) {
        DcsServerClient.DcsServerResult result = dcsServerClient.up(dcsId);
        redirectAttributes.addFlashAttribute(
                result.isSuccess() ? "message" : "errorMessage",
                "DCS_ID [" + dcsId + "] 올리기 요청 결과: " + result.getMessage());
        return "redirect:/dcs";
    }

    @PostMapping("/{dcsId}/down")
    public String down(@PathVariable String dcsId, RedirectAttributes redirectAttributes) {
        DcsServerClient.DcsServerResult result = dcsServerClient.down(dcsId);
        redirectAttributes.addFlashAttribute(
                result.isSuccess() ? "message" : "errorMessage",
                "DCS_ID [" + dcsId + "] 내리기 요청 결과: " + result.getMessage());
        return "redirect:/dcs";
    }

    @PostMapping("/{dcsId}/delete")
    public String delete(@PathVariable String dcsId, RedirectAttributes redirectAttributes) {
        DcsServerClient.DcsStatus status = statusCache.get().get(dcsId);
        boolean known = status != null && !status.isStale();
        if (known && status.isRunning()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "DCS_ID [" + dcsId + "] 는 실행 중이라 삭제할 수 없습니다. 먼저 내려주세요.");
            return "redirect:/dcs";
        }
        try {
            dcsService.delete(dcsId);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/dcs";
        }
        redirectAttributes.addFlashAttribute("message", "DCS_ID [" + dcsId + "] 가 삭제되었습니다.");
        return "redirect:/dcs";
    }
}
