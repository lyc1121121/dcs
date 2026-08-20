package com.dcsmanager.controller;

import com.dcsmanager.service.PageContentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 124단계: DCSManager "개요" 옆 "이력" 탭 - 지금까지의 변경 이력을 기록해두고, 화면에서
 * 직접 수정도 가능하게 한다.
 */
@Controller
@RequestMapping("/changelog")
public class ChangelogController {

    private static final String PAGE_KEY = "changelog";
    private static final String DEFAULT_RESOURCE = "changelog-default.md";

    private final PageContentService pageContentService;

    public ChangelogController(PageContentService pageContentService) {
        this.pageContentService = pageContentService;
    }

    @GetMapping
    public String view(Model model, HttpServletRequest request) {
        request.getSession();
        model.addAttribute("pageTitle", "이력");
        model.addAttribute("backUrl", "/dcs");
        model.addAttribute("formAction", "/changelog");
        model.addAttribute("contentHtml", pageContentService.getHtml(PAGE_KEY, DEFAULT_RESOURCE));
        model.addAttribute("contentMarkdown", pageContentService.getMarkdown(PAGE_KEY, DEFAULT_RESOURCE));
        return "page-content/view";
    }

    @PostMapping
    public String save(@RequestParam String contentMarkdown, RedirectAttributes redirectAttributes) {
        pageContentService.save(PAGE_KEY, contentMarkdown);
        redirectAttributes.addFlashAttribute("message", "이력 내용이 저장되었습니다.");
        return "redirect:/changelog";
    }
}
