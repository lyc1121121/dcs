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
 * 334단계: 메모게시판(공통메모/기술메모/백업화면) 각 항목에 파일을 첨부할 수 있게 추가.
 * 메모 자체(jobradar는 다른 앱/DB, tech/backup은 DCSManager 자체 DB)와 무관하게 이 테이블
 * 하나로 모든 탭의 첨부파일을 관리한다 - noteKind("jobradar"|"tech"|"backup") + noteId로
 * 어느 메모의 첨부인지 느슨하게(FK 없이) 연결한다. jobradar는 별도 DB라 FK를 걸 수 없고,
 * tech/backup도 통일된 방식을 쓰려고 일관되게 FK 없이 둔다.
 *
 * 파일은 디스크가 아니라 DB(LONGBLOB)에 그대로 저장한다 - 메모 이미지(base64)도 이미 DB에
 * 저장하는 구조와 맞추고, dcsmanager-test-tomcat 컨테이너에 쓰기 가능한 볼륨이 새로 필요
 * 없게 하기 위함(호스트 /working 마운트는 읽기전용이라 디스크에 쓸 수 없음).
 */
@Entity
@Table(name = "note_attachment")
public class NoteAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "note_kind", nullable = false, length = 20)
    private String noteKind;

    @Column(name = "note_id", nullable = false)
    private Long noteId;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Lob
    @Column(name = "data", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] data;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNoteKind() {
        return noteKind;
    }

    public void setNoteKind(String noteKind) {
        this.noteKind = noteKind;
    }

    public Long getNoteId() {
        return noteId;
    }

    public void setNoteId(Long noteId) {
        this.noteId = noteId;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
