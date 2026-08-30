package com.dcsmanager.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 169단계: "메모연동" 화면의 "백업화면" 탭 - 화면 디자인을 갈아엎기 전에 "이전 모습 +
 * 구조 설명"을 남겨두는 용도. 기능은 "기술메모"(167단계)와 완전히 동일(리치텍스트,
 * 이미지 붙여넣기, 추가/수정/삭제, 카카오톡 전송)이지만 데이터는 별개로 관리한다
 * (기술메모=업무 메모, 백업화면=화면 스냅샷 기록).
 */
@Entity
@Table(name = "backup_note")
public class BackupNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
