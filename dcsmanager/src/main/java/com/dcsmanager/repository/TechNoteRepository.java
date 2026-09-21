package com.dcsmanager.repository;

import com.dcsmanager.domain.TechNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TechNoteRepository extends JpaRepository<TechNote, Long> {
    List<TechNote> findAllByOrderByUpdatedAtDesc();

    // 316단계 추가: 메모화면 페이지네이션/검색 - 검색어가 없을 때와 있을 때를 나눠서
    // 컨트롤러에서 선택 호출한다(둘을 하나로 합치면 "검색어 없음"을 표현하기
    // 애매해서 - null/빈 문자열 처리를 LIKE 쿼리 안에서 하는 것보다 명확함).
    Page<TechNote> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    // 317단계: 원본 HTML 전체에 LIKE를 걸면 이미지(base64)의 우연한 문자열까지 걸려서,
    // 태그(속성 포함)를 제거한 "보이는 글자"에서만 검색한다.
    @Query(value = "SELECT * FROM tech_note WHERE REGEXP_REPLACE(content, '<[^>]*>', '') LIKE CONCAT('%', :kw, '%') ORDER BY updated_at DESC",
           countQuery = "SELECT COUNT(*) FROM tech_note WHERE REGEXP_REPLACE(content, '<[^>]*>', '') LIKE CONCAT('%', :kw, '%')",
           nativeQuery = true)
    Page<TechNote> searchVisibleText(@Param("kw") String keyword, Pageable pageable);
}
