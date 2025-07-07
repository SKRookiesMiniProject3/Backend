package com.rookies.log2doc.repository;

import com.rookies.log2doc.entity.DocumentCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentCategoryRepository extends JpaRepository<DocumentCategory, Long> {
    // 필요한 경우 문서 ID별, 카테고리별 매핑 조회 쿼리 추가 가능
}
