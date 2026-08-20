package com.dcsmanager.controller;

import com.dcsmanager.service.PortfolioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 122단계: DCSManager 좌측 "개요" 탭 - 작업이력/포트폴리오 내용을 보여주고, 화면에서
 * 직접 수정도 가능하게 한다.
 */
@Controller
@RequestMapping("/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public String view(Model model, HttpServletRequest request) {
        // 페이지 본문이 커서 렌더링 도중 응답이 커밋될 수 있는데, CSRF 토큰은 세션에 지연 저장되므로
        // 폼(th:action)을 만나는 시점에 세션이 아직 없으면 "커밋 후 세션 생성 불가" 예외가 난다.
        // 스트리밍이 시작되기 전에 세션을 미리 만들어 둔다.
        request.getSession();
        model.addAttribute("contentHtml", portfolioService.getHtml());
        model.addAttribute("contentMarkdown", portfolioService.getMarkdown());
        return "portfolio/view";
    }

    @PostMapping
    public String save(@RequestParam String contentMarkdown, RedirectAttributes redirectAttributes) {
        portfolioService.save(contentMarkdown);
        redirectAttributes.addFlashAttribute("message", "개요 내용이 저장되었습니다.");
        return "redirect:/portfolio";
    }
}
