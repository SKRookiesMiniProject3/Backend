package com.rookies.log2doc.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 권한 정보를 저장하는 Entity 클래스
 * Spring Security와 연동하여 사용자 권한 관리
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
     * 권한 타입 열거형
     */
    public enum RoleName {
        ROLE_USER("일반 사용자"),
        ROLE_ADMIN("관리자"),
        ROLE_MODERATOR("운영자");

        private final String description;

        RoleName(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}