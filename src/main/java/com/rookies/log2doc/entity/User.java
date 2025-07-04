package com.rookies.log2doc.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 사용자 정보를 저장하는 Entity 클래스
 * JWT 토큰 기반 인증을 위한 필드들을 포함
 * Role과 1:1 관계로 구성
 */
@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "username"),
                @UniqueConstraint(columnNames = "email")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_email_verified", nullable = false)
    @Builder.Default
    private Boolean isEmailVerified = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 권한 관리를 위한 일대일 관계
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "role_id", referencedColumnName = "role_id")
    private Role role;

    // 기본 직급 설정을 위한 필드 (Role이 null일 때 사용)
    @Enumerated(EnumType.STRING)
    @Column(name = "default_role")
    @Builder.Default
    private Role.RoleName defaultRole = Role.RoleName.STAFF;

    // 비즈니스 로직 메서드
    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void verifyEmail() {
        this.isEmailVerified = true;
    }

    /**
     * 사용자의 역할 설정
     * @param role 설정할 역할
     */
    public void setRole(Role role) {
        this.role = role;
    }

    /**
     * 사용자의 역할 제거
     */
    public void removeRole() {
        this.role = null;
    }

    /**
     * 현재 사용자의 직급 반환 (Role이 없으면 기본 직급 반환)
     * @return 현재 직급
     */
    public Role.RoleName getCurrentRoleName() {
        return role != null ? role.getName() : defaultRole;
    }

    /**
     * 현재 사용자의 직급 레벨 반환
     * @return 직급 레벨
     */
    public int getCurrentLevel() {
        return getCurrentRoleName().getLevel();
    }

    /**
     * 특정 직급 이상인지 확인
     * @param minimumRole 최소 요구 직급
     * @return 현재 직급이 최소 요구 직급 이상이면 true
     */
    public boolean hasMinimumRole(Role.RoleName minimumRole) {
        return getCurrentRoleName().isAtLeast(minimumRole);
    }

    /**
     * 다른 사용자보다 높은 직급인지 확인
     * @param otherUser 비교할 다른 사용자
     * @return 현재 사용자가 더 높은 직급이면 true
     */
    public boolean hasHigherRoleThan(User otherUser) {
        return getCurrentRoleName().isHigherThan(otherUser.getCurrentRoleName());
    }

    /**
     * 관리자급 이상인지 확인
     * @return 과장 이상이면 true
     */
    public boolean isManager() {
        return getCurrentRoleName().isAtLeast(Role.RoleName.MANAGER);
    }

    /**
     * 임원급인지 확인
     * @return 이사 이상이면 true
     */
    public boolean isExecutive() {
        return getCurrentRoleName().isAtLeast(Role.RoleName.VICE_PRESIDENT);
    }

    /**
     * 최고 경영진인지 확인
     * @return 대표이사면 true
     */
    public boolean isCEO() {
        return getCurrentRoleName() == Role.RoleName.CEO;
    }
}