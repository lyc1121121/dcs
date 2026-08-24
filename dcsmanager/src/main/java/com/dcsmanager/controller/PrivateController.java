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
import javax.servlet.http.HttpSession;

/**
 * 131단계: DCSManager "개요" 왼쪽 "PRIVATE" 탭 - dcs_client_ne 관련 작업이력을
 * 기록한다. 개요/이력 탭과 달리, 세션당 admin/admin 비밀번호를 입력해야 내용을
 * 볼 수 있다.
 */
@Controller
@RequestMapping("/private")
public class PrivateController {

    private static final String PAGE_KEY = "private";
    private static final String DEFAULT_RESOURCE = "private-default.md";
    private static final String SESSION_KEY = "privateAuthenticated";

    private final PageContentService pageContentService;

    public PrivateController(PageContentService pageContentService) {
        this.pageContentService = pageContentService;
    }

    @GetMapping
    public String view(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession();
        if (!Boolean.TRUE.equals(session.getAttribute(SESSION_KEY))) {
            return "private/login";
        }
        model.addAttribute("pageTitle", "PRIVATE");
        model.addAttribute("backUrl", "/dcs");
        model.addAttribute("formAction", "/private");
        model.addAttribute("logoutUrl", "/private/logout");
        model.addAttribute("contentHtml", pageContentService.getHtml(PAGE_KEY, DEFAULT_RESOURCE));
        model.addAttribute("contentMarkdown", pageContentService.getMarkdown(PAGE_KEY, DEFAULT_RESOURCE));
        return "page-content/view";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password,
                         HttpServletRequest request, RedirectAttributes redirectAttributes) {
        if ("admin".equals(username) && "admin".equals(password)) {
            request.getSession().setAttribute(SESSION_KEY, Boolean.TRUE);
        } else {
            redirectAttributes.addFlashAttribute("loginError", "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        return "redirect:/private";
    }

    /**
     * 132단계: 로그아웃하면 인증 상태만 지우고(전체 세션은 유지 - 다른 탭의 CSRF 토큰 등에
     * 영향 없도록) DCSManager 화면으로 이동한다.
     */
    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        request.getSession().removeAttribute(SESSION_KEY);
        return "redirect:/dcs";
    }

    @PostMapping
    public String save(@RequestParam String contentMarkdown, HttpServletRequest request,
                        RedirectAttributes redirectAttributes) {
        if (!Boolean.TRUE.equals(request.getSession().getAttribute(SESSION_KEY))) {
            return "redirect:/private";
        }
        pageContentService.save(PAGE_KEY, contentMarkdown);
        redirectAttributes.addFlashAttribute("message", "PRIVATE 내용이 저장되었습니다.");
        return "redirect:/private";
    }
}
