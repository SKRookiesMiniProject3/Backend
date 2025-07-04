package com.rookies.log2doc.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 권한 정보를 저장하는 Entity 클래스
 * 기업 직급 시스템을 반영한 수직 구조
 */
@Entity
@Table(name = "roles")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true, length = 20)
    private RoleName name;

    @Column(name = "description", length = 200)
    private String description;

    /**
     * 기업 직급 타입 열거형
     * 숫자가 높을수록 높은 직급 (CEO가 가장 높음)
     */
    public enum RoleName {
        INTERN(1, "인턴"),
        STAFF(2, "사원"),
        SENIOR_STAFF(3, "주임"),
        ASSISTANT_MANAGER(4, "대리"),
        MANAGER(5, "과장"),
        SENIOR_MANAGER(6, "차장"),
        DIRECTOR(7, "부장"),
        VICE_PRESIDENT(8, "이사"),
        PRESIDENT(9, "상무"),
        EXECUTIVE_VICE_PRESIDENT(10, "전무"),
        CEO(11, "대표이사");

        private final int level;
        private final String description;

        RoleName(int level, String description) {
            this.level = level;
            this.description = description;
        }

        public int getLevel() {
            return level;
        }

        public String getDescription() {
            return description;
        }

        /**
         * 직급 레벨 비교 메서드
         * @param other 비교할 다른 직급
         * @return 현재 직급이 더 높으면 양수, 같으면 0, 낮으면 음수
         */
        public int compareLevel(RoleName other) {
            return Integer.compare(this.level, other.level);
        }

        /**
         * 현재 직급이 다른 직급보다 높은지 확인
         * @param other 비교할 다른 직급
         * @return 현재 직급이 더 높으면 true
         */
        public boolean isHigherThan(RoleName other) {
            return this.level > other.level;
        }

        /**
         * 현재 직급이 다른 직급보다 낮은지 확인
         * @param other 비교할 다른 직급
         * @return 현재 직급이 더 낮으면 true
         */
        public boolean isLowerThan(RoleName other) {
            return this.level < other.level;
        }

        /**
         * 현재 직급이 다른 직급과 같은지 확인
         * @param other 비교할 다른 직급
         * @return 직급이 같으면 true
         */
        public boolean isEqualTo(RoleName other) {
            return this.level == other.level;
        }

        /**
         * 현재 직급이 특정 직급 이상인지 확인
         * @param minimumRole 최소 요구 직급
         * @return 현재 직급이 최소 요구 직급 이상이면 true
         */
        public boolean isAtLeast(RoleName minimumRole) {
            return this.level >= minimumRole.level;
        }
    }

    /**
     * 비즈니스 로직 메서드들
     */

    /**
     * 현재 Role의 직급이 다른 Role의 직급보다 높은지 확인
     */
    public boolean isHigherThan(Role other) {
        return this.name.isHigherThan(other.name);
    }

    /**
     * 현재 Role의 직급이 다른 Role의 직급보다 낮은지 확인
     */
    public boolean isLowerThan(Role other) {
        return this.name.isLowerThan(other.name);
    }

    /**
     * 현재 Role의 직급 레벨 반환
     */
    public int getLevel() {
        return this.name.getLevel();
    }

    /**
     * 관리자급 이상인지 확인 (과장 이상)
     */
    public boolean isManagerLevel() {
        return this.name.isAtLeast(RoleName.MANAGER);
    }

    /**
     * 임원급인지 확인 (이사 이상)
     */
    public boolean isExecutiveLevel() {
        return this.name.isAtLeast(RoleName.VICE_PRESIDENT);
    }
}