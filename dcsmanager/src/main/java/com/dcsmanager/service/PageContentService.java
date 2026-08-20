package com.dcsmanager.service;

import com.dcsmanager.domain.PageContent;
import com.dcsmanager.repository.PageContentRepository;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 122/124단계: "개요", "이력" 탭처럼 마크다운으로 저장해두고 화면엔 HTML로 렌더링하는
 * 정적 콘텐츠 탭들이 공용으로 쓰는 서비스. pageKey 로 콘텐츠를 구분한다.
 */
@Service
public class PageContentService {

    private static final List<Extension> EXTENSIONS = Collections.singletonList(TablesExtension.create());
    private static final Parser PARSER = Parser.builder().extensions(EXTENSIONS).build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().extensions(EXTENSIONS).build();

    private final PageContentRepository repository;

    public PageContentService(PageContentRepository repository) {
        this.repository = repository;
    }

    public String getMarkdown(String pageKey, String defaultResourceName) {
        return getOrCreate(pageKey, defaultResourceName).getContentMarkdown();
    }

    public String getHtml(String pageKey, String defaultResourceName) {
        Node document = PARSER.parse(getMarkdown(pageKey, defaultResourceName));
        return RENDERER.render(document);
    }

    public void save(String pageKey, String markdown) {
        PageContent content = repository.findById(pageKey).orElseGet(() -> newContent(pageKey, ""));
        content.setContentMarkdown(markdown);
        content.setUpdatedAt(LocalDateTime.now());
        repository.save(content);
    }

    private PageContent getOrCreate(String pageKey, String defaultResourceName) {
        return repository.findById(pageKey).orElseGet(() ->
                repository.save(newContent(pageKey, loadDefaultMarkdown(defaultResourceName))));
    }

    private PageContent newContent(String pageKey, String markdown) {
        PageContent content = new PageContent();
        content.setPageKey(pageKey);
        content.setContentMarkdown(markdown);
        content.setUpdatedAt(LocalDateTime.now());
        return content;
    }

    private String loadDefaultMarkdown(String resourceName) {
        try (InputStream in = new ClassPathResource(resourceName).getInputStream()) {
            return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new IllegalStateException("기본 콘텐츠[" + resourceName + "]를 불러오지 못했습니다.", e);
        }
    }
}
