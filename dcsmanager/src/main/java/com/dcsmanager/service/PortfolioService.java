package com.dcsmanager.service;

import com.dcsmanager.domain.PortfolioContent;
import com.dcsmanager.repository.PortfolioContentRepository;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 122단계: DCSManager "개요" 탭 - 포트폴리오 내용을 마크다운으로 저장해두고, 화면에는
 * HTML로 렌더링해서 보여준다. 화면에서 마크다운 원문을 직접 수정/저장할 수 있다.
 */
@Service
public class PortfolioService {

    private static final List<Extension> EXTENSIONS = Collections.singletonList(TablesExtension.create());
    private static final Parser PARSER = Parser.builder().extensions(EXTENSIONS).build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().extensions(EXTENSIONS).build();

    private final PortfolioContentRepository repository;

    public PortfolioService(PortfolioContentRepository repository) {
        this.repository = repository;
    }

    public String getMarkdown() {
        return getOrCreate().getContentMarkdown();
    }

    public String getHtml() {
        Node document = PARSER.parse(getMarkdown());
        return RENDERER.render(document);
    }

    public void save(String markdown) {
        PortfolioContent content = getOrCreate();
        content.setContentMarkdown(markdown);
        content.setUpdatedAt(LocalDateTime.now());
        repository.save(content);
    }

    private PortfolioContent getOrCreate() {
        return repository.findById(PortfolioContent.SINGLETON_ID).orElseGet(() -> {
            PortfolioContent content = new PortfolioContent();
            content.setId(PortfolioContent.SINGLETON_ID);
            content.setContentMarkdown(loadDefaultMarkdown());
            content.setUpdatedAt(LocalDateTime.now());
            return repository.save(content);
        });
    }

    private String loadDefaultMarkdown() {
        try (InputStream in = new ClassPathResource("portfolio-default.md").getInputStream()) {
            return new java.io.BufferedReader(new java.io.InputStreamReader(in, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new IllegalStateException("기본 포트폴리오 내용을 불러오지 못했습니다.", e);
        }
    }
}
