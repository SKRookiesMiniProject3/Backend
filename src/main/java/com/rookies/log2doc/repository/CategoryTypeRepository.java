package com.rookies.log2doc.repository;

import com.rookies.log2doc.entity.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * CategoryType Repository
 *
 * - 문서 분류(CategoryType) 엔티티 전용 Repository
 * - 카테고리 등록, 조회, 중복 여부 확인 등에 사용됨
 * - CommandLineRunner 등 초기 데이터 생성 시 existsByName()으로 중복 체크
 *
 * 예시 사용처:
 *   - DataInitializer: 기본 카테고리 타입 자동 생성
 *   - 문서 업로드 시 카테고리 FK 유효성 검사
 */
public interface CategoryTypeRepository extends JpaRepository<CategoryType, Long> {

    /**
     * 이름으로 중복 카테고리 타입이 존재하는지 여부 확인
     *
     * @param name 카테고리 이름
     * @return true(중복), false(없음)
     */
    boolean existsByName(String name);
}
