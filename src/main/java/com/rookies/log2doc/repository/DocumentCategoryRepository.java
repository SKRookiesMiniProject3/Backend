package com.rookies.log2doc.repository;

import com.rookies.log2doc.entity.DocumentCategory;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * DocumentCategory Repository
 *
 * - 문서와 카테고리 간의 다대다 관계 매핑을 저장하는 테이블 전용
 * - 기본 CRUD(JpaRepository)만으로도 대부분 처리 가능
 * - 문서별 카테고리 조회, 카테고리별 문서 리스트 조회 등
 *   필요할 때 맞춤 JPQL 쿼리를 추가해서 확장할 수 있음
 *
 * 사용 예시:
 *   - 특정 문서에 연결된 카테고리 리스트 조회
 *   - 특정 카테고리에 포함된 문서 리스트 조회
 */
public interface DocumentCategoryRepository extends JpaRepository<DocumentCategory, Long> {

    // 현재는 기본 CRUD만 사용 중
}
