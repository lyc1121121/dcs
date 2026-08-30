package com.dcsmanager.repository;

import com.dcsmanager.domain.BackupNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BackupNoteRepository extends JpaRepository<BackupNote, Long> {
    List<BackupNote> findAllByOrderByUpdatedAtDesc();
}
