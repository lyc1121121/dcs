package com.dcsmanager.repository;

import com.dcsmanager.domain.TechNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TechNoteRepository extends JpaRepository<TechNote, Long> {
    List<TechNote> findAllByOrderByUpdatedAtDesc();
}
