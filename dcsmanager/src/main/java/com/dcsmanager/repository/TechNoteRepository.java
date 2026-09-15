package com.dcsmanager.repository;

import com.dcsmanager.domain.TechNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TechNoteRepository extends JpaRepository<TechNote, Long> {
    List<TechNote> findAllByOrderByUpdatedAtDesc();

    // 316단계 추가: 메모화면 페이지네이션/검색 - 검색어가 없을 때와 있을 때를 나눠서
    // 컨트롤러에서 선택 호출한다(둘을 하나로 합치면 "검색어 없음"을 표현하기
    // 애매해서 - null/빈 문자열 처리를 LIKE 쿼리 안에서 하는 것보다 명확함).
    Page<TechNote> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    Page<TechNote> findByContentContainingIgnoreCaseOrderByUpdatedAtDesc(String keyword, Pageable pageable);
}
