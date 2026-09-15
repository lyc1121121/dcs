package com.dcsmanager.repository;

import com.dcsmanager.domain.BackupNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BackupNoteRepository extends JpaRepository<BackupNote, Long> {
    List<BackupNote> findAllByOrderByUpdatedAtDesc();

    // 316단계 추가: 메모화면 페이지네이션/검색
    Page<BackupNote> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    Page<BackupNote> findByContentContainingIgnoreCaseOrderByUpdatedAtDesc(String keyword, Pageable pageable);
}
