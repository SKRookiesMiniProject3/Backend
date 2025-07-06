package com.rookies.log2doc.repository;

import com.rookies.log2doc.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    // 카테고리별 필터링
    List<Document> findByCategoryAndIsDeletedFalse(String category);

    // Soft Delete 제외하고 전체 가져오기
    List<Document> findByIsDeletedFalse();

    Optional<Document> findByFilePath(String filePath);
}
