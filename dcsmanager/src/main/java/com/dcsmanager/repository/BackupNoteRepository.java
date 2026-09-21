package com.dcsmanager.repository;

import com.dcsmanager.domain.BackupNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BackupNoteRepository extends JpaRepository<BackupNote, Long> {
    List<BackupNote> findAllByOrderByUpdatedAtDesc();

    // 316단계 추가: 메모화면 페이지네이션/검색
    Page<BackupNote> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    // 317단계: 원본 HTML 전체에 LIKE를 걸면 이미지(base64)의 우연한 문자열까지 걸려서,
    // 태그(속성 포함)를 제거한 "보이는 글자"에서만 검색한다.
    @Query(value = "SELECT * FROM backup_note WHERE REGEXP_REPLACE(content, '<[^>]*>', '') LIKE CONCAT('%', :kw, '%') ORDER BY updated_at DESC",
           countQuery = "SELECT COUNT(*) FROM backup_note WHERE REGEXP_REPLACE(content, '<[^>]*>', '') LIKE CONCAT('%', :kw, '%')",
           nativeQuery = true)
    Page<BackupNote> searchVisibleText(@Param("kw") String keyword, Pageable pageable);
}
