package com.dcsmanager.repository;

import com.dcsmanager.domain.NoteAttachment;
import com.dcsmanager.domain.NoteAttachmentMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NoteAttachmentRepository extends JpaRepository<NoteAttachment, Long> {

    // 334단계: 목록 표시용 - LONGBLOB(data)는 빼고 메타데이터만 가져온다.
    @Query("SELECT a.id as id, a.originalName as originalName, a.contentType as contentType, "
            + "a.fileSize as fileSize, a.createdAt as createdAt "
            + "FROM NoteAttachment a WHERE a.noteKind = :kind AND a.noteId = :noteId ORDER BY a.id ASC")
    List<NoteAttachmentMeta> findMetaByNoteKindAndNoteId(@Param("kind") String noteKind, @Param("noteId") Long noteId);

    // 메모 자체가 삭제될 때 첨부파일도 같이 정리(고아 데이터 방지).
    // 335단계 수정: 파생 delete 쿼리 메서드는 (save/deleteById와 달리) SimpleJpaRepository가
    // 기본으로 트랜잭션을 걸어주지 않아서, 컨트롤러에서 바로 호출하면
    // "TransactionRequiredException: No EntityManager with actual transaction available"로
    // 500 에러가 났다(새 메모 삭제 테스트 중 발견). @Transactional을 직접 걸어서 해결.
    @Transactional
    void deleteByNoteKindAndNoteId(String noteKind, Long noteId);
}
