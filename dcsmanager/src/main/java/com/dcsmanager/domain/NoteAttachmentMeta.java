package com.dcsmanager.domain;

import java.time.LocalDateTime;

/**
 * 334단계: 첨부파일 목록을 보여줄 때는 큰 LONGBLOB(data)까지 매번 불러올 필요가 없어서,
 * 메타데이터만 뽑는 JPQL 프로젝션 결과로 이 인터페이스를 쓴다(NoteAttachmentRepository 참고).
 */
public interface NoteAttachmentMeta {
    Long getId();
    String getOriginalName();
    String getContentType();
    Long getFileSize();
    LocalDateTime getCreatedAt();
}
