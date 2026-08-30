package com.dcsmanager.controller;

import com.dcsmanager.domain.Dcs;
import com.dcsmanager.service.DcsServerClient;
import com.dcsmanager.service.DcsService;
import com.dcsmanager.service.EaiServerClient;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/dcs")
public class DcsController {

    private static final Pattern TERMINAL_ID_PATTERN = Pattern.compile("^[0-9]{9}$");

    private final DcsService dcsService;
    private final DcsServerClient dcsServerClient;
    private final EaiServerClient eaiServerClient;
    private final StatusCache statusCache;

    public DcsController(DcsService dcsService, DcsServerClient dcsServerClient,
                          EaiServerClient eaiServerClient, StatusCache statusCache) {
        this.dcsService = dcsService;
        this.dcsServerClient = dcsServerClient;
        this.eaiServerClient = eaiServerClient;
        this.statusCache = statusCache;
    }

    @GetMapping
    public String list(Model model) {
        populateListModel(model);
        if (!model.containsAttribute("dcsForm")) {
            model.addAttribute("dcsForm", new DcsForm());
        }
        return "dcs/list";
    }

    /**
     * dcs/list.html 이 매 요청마다 필요로 하는 목록/상태 모델 속성을 채운다.
     * "시뮬레이션 테스트" 표는 실행 중인 TMS만 보여주는데, 그 필터 조건과 무관하게
     * "실행중인 TMS가 없습니다" 빈 안내문을 dcsList 전체 개수로만 판단하고 있어서
     * (175-6단계) 등록된 TMS가 있지만 전부 중지 상태일 때 표가 완전히 빈 채로
     * 아무 안내도 없이 나타나는 버그가 있었다 - hasRunningDcs 로 필터 조건과
     * 동일한 기준을 공유해서 고침.
     */
    private void populateListModel(Model model) {
        List<Dcs> dcsList = dcsService.findAll();
        Map<String, DcsServerClient.DcsStatus> statuses = statusCache.get();
        boolean hasRunningDcs = dcsList.stream().anyMatch(dcs -> {
            DcsServerClient.DcsStatus status = statuses.get(dcs.getDcsId());
            return status != null && !status.isStale() && status.isRunning();
        });
        model.addAttribute("dcsList", dcsList);
        model.addAttribute("statuses", statuses);
        model.addAttribute("otherContainers", dcsServerClient.listOtherContainers());
        model.addAttribute("hasRunningDcs", hasRunningDcs);
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
                    "TMS_SERVER_IP [" + form.getDcsServerIp() + "] 와 통신할 수 없습니다. 주소를 확인하고 다시 입력해주세요.");
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
        redirectAttributes.addFlashAttribute("message", "TMS_ID [" + form.getDcsId() + "] 가 생성되었습니다.");
        return "redirect:/dcs";
    }

    private String reopenCreateModal(Model model) {
        populateListModel(model);
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
                    "TMS_SERVER_IP [" + form.getDcsServerIp() + "] 와 통신할 수 없습니다. 주소를 확인하고 다시 입력해주세요.");
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
        redirectAttributes.addFlashAttribute("message", "TMS_ID [" + dcsId + "] 가 수정되었습니다.");
        return "redirect:/dcs";
    }

    private String reopenEditModal(Model model) {
        populateListModel(model);
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
                "TMS_ID [" + dcsId + "] 올리기 요청 결과: " + result.getMessage());
        return "redirect:/dcs";
    }

    @PostMapping("/{dcsId}/down")
    public String down(@PathVariable String dcsId, RedirectAttributes redirectAttributes) {
        DcsServerClient.DcsServerResult result = dcsServerClient.down(dcsId);
        redirectAttributes.addFlashAttribute(
                result.isSuccess() ? "message" : "errorMessage",
                "TMS_ID [" + dcsId + "] 내리기 요청 결과: " + result.getMessage());
        return "redirect:/dcs";
    }

    /**
     * 105단계 시뮬레이션 테스트 섹션: SND 방향 - DCSAgent 가 JAQT 테스트 파일을
     * fileCount 개, intervalSeconds 간격으로 생성하도록 요청. (디폴트: 5개, 1초)
     */
    @PostMapping("/{dcsId}/simulate")
    public String simulate(@PathVariable String dcsId, @RequestParam String terminalId,
                            @RequestParam(defaultValue = "5") int fileCount,
                            @RequestParam(defaultValue = "1") int intervalSeconds,
                            RedirectAttributes redirectAttributes) {
        if (!TERMINAL_ID_PATTERN.matcher(terminalId).matches()) {
            redirectAttributes.addFlashAttribute("errorMessage", "단말기 ID는 숫자 9자리여야 합니다: " + terminalId);
            return "redirect:/dcs";
        }
        if (fileCount < 1 || intervalSeconds < 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "개수는 1 이상, 시간간격은 0 이상이어야 합니다.");
            return "redirect:/dcs";
        }
        DcsServerClient.DcsServerResult result = dcsServerClient.simulate(dcsId, terminalId, fileCount, intervalSeconds);
        redirectAttributes.addFlashAttribute(
                result.isSuccess() ? "message" : "errorMessage",
                "TMS_ID [" + dcsId + "] 시뮬 요청 결과: " + result.getMessage());
        if (result.isSuccess()) {
            // "마지막 시작" 표시 및 eai_server 도착 필터 기준 시각은, 클릭 시점이 아니라
            // 실제로 서버가 요청을 성공 처리한 시점(112단계)을 기준으로 화면에 전달한다.
            redirectAttributes.addFlashAttribute("simStartedDcsId", dcsId);
            redirectAttributes.addFlashAttribute("simStartedAt", System.currentTimeMillis());
        }
        return "redirect:/dcs";
    }

    /**
     * 105/109단계 시뮬레이션 테스트 섹션: eai_server 에 실제 도착한 파일 목록 조회(AJAX). 이 DCS_ID의
     * SERVER_IP 로 eai_server 주소를 계산해서 직접 호출한다(DCSServer 경유하지 않음).
     * since(epoch millis) 가 주어지면 그 시각 이후 도착한 파일만 걸러서 반환한다
     * (SND 시뮬 "시작" 버튼을 누른 시점 이후 도착분만 보여주기 위함).
     */
    @GetMapping("/{dcsId}/arrived")
    @ResponseBody
    public Map<String, Object> arrived(@PathVariable String dcsId,
                                        @RequestParam(required = false) Long since) {
        Dcs dcs = dcsService.get(dcsId);
        List<EaiServerClient.ReceivedFile> files = eaiServerClient.listReceived(dcs.getDcsServerIp(), dcsId);
        if (files == null) {
            return Collections.singletonMap("ok", false);
        }
        List<String> fileNames = files.stream()
                .filter(f -> since == null || f.getReceivedAt() >= since)
                .map(EaiServerClient.ReceivedFile::getFileName)
                .collect(java.util.stream.Collectors.toList());
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("ok", true);
        body.put("files", fileNames);
        return body;
    }

    /**
     * 105단계 시뮬레이션 테스트 섹션: RCV 방향 - 화면에서 드래그드롭한 파일을 eai_server 의
     * outbox 로 그대로 전달한다. eai_agent 가 다음 폴링 때 가져가서 RCV 폴더에 내려준다.
     */
    @PostMapping("/{dcsId}/rcv-upload")
    @ResponseBody
    public Map<String, Object> rcvUpload(@PathVariable String dcsId, @RequestParam("file") MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.startsWith("J")) {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("ok", false);
            body.put("message", "파일명이 대문자 J로 시작하는 파일만 업로드할 수 있습니다.");
            return body;
        }
        Dcs dcs = dcsService.get(dcsId);
        EaiServerClient.UploadResult result = eaiServerClient.uploadToOutbox(dcs.getDcsServerIp(), dcsId, fileName, file.getBytes());
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("ok", result.isOk());
        body.put("message", result.getMessage());
        return body;
    }

    @PostMapping("/{dcsId}/delete")
    public String delete(@PathVariable String dcsId, RedirectAttributes redirectAttributes) {
        DcsServerClient.DcsStatus status = statusCache.get().get(dcsId);
        boolean known = status != null && !status.isStale();
        if (known && status.isRunning()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "TMS_ID [" + dcsId + "] 는 실행 중이라 삭제할 수 없습니다. 먼저 내려주세요.");
            return "redirect:/dcs";
        }
        try {
            dcsService.delete(dcsId);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/dcs";
        }
        redirectAttributes.addFlashAttribute("message", "TMS_ID [" + dcsId + "] 가 삭제되었습니다.");
        return "redirect:/dcs";
    }
}
