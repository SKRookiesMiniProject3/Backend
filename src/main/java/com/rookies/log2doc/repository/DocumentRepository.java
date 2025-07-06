package com.rookies.log2doc.repository;

import com.rookies.log2doc.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Document 엔티티 JPA Repository
 * - Soft Delete 문서 제외한 기본 조회 지원
 */
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * 카테고리별 문서 리스트 조회
     * (Soft Delete 제외)
     *
     * @param category 문서 카테고리
     * @return 삭제되지 않은 해당 카테고리 문서 목록
     */
    List<Document> findByCategoryAndIsDeletedFalse(String category);

    /**
     * 삭제되지 않은 모든 문서 리스트 조회
     *
     * @return Soft Delete 제외 전체 문서 목록
     */
    List<Document> findByIsDeletedFalse();

    /**
     * 파일 해시 경로 기준 단일 문서 조회
     *
     * @param filePath 파일 저장 해시 경로(UUID)
     * @return 존재하면 Optional<Document>
     */
    Optional<Document> findByFilePath(String filePath);
}
