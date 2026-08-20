package com.dcsmanager.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 122/124단계: "개요", "이력" 등 화면에서 직접 수정 가능한 정적 콘텐츠 탭들이 공용으로
 * 쓰는 저장소. pageKey 별로 마크다운 원문 1건씩만 유지한다.
 */
@Entity
@Table(name = "page_content")
public class PageContent {

    @Id
    @Column(name = "page_key", length = 50, nullable = false, updatable = false)
    private String pageKey;

    @Lob
    @Column(name = "content_markdown", columnDefinition = "LONGTEXT")
    private String contentMarkdown;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public String getPageKey() {
        return pageKey;
    }

    public void setPageKey(String pageKey) {
        this.pageKey = pageKey;
    }

    public String getContentMarkdown() {
        return contentMarkdown;
    }

    public void setContentMarkdown(String contentMarkdown) {
        this.contentMarkdown = contentMarkdown;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
