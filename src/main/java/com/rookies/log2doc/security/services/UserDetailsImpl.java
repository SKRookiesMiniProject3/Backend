package com.rookies.log2doc.security.services;

import com.rookies.log2doc.entity.Role;
import com.rookies.log2doc.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

/**
 * Spring Security UserDetails 구현체
 * 1:1 관계로 변경된 Role 시스템에 맞게 수정
 */
@Data
@AllArgsConstructor
public class UserDetailsImpl implements UserDetails {

    private Long id;
    private String username;
    private String email;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;
    private Role.RoleName currentRoleName;
    private String roleName;
    private int roleId;                     // 직급 ID 숫자!


    /**
     * User 엔티티로부터 UserDetails 객체를 생성하는 팩토리 메서드
     * 1:1 관계로 변경된 Role 시스템에 맞게 수정
     *
     * @param user User 엔티티
     * @return UserDetailsImpl 객체
     */
    public static UserDetailsImpl build(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // 현재 사용자의 직급을 Spring Security 권한으로 변환
        String currentRoleName = user.getCurrentRoleName().name();
        authorities.add(new SimpleGrantedAuthority(currentRoleName));

        // 계층적 권한 부여: 현재 직급 이하의 모든 직급 권한을 포함
        // 예: MANAGER는 STAFF, SENIOR_STAFF, ASSISTANT_MANAGER 권한도 함께 가짐
        for (com.rookies.log2doc.entity.Role.RoleName roleName : com.rookies.log2doc.entity.Role.RoleName.values()) {
            if (roleName.getLevel() <= user.getCurrentLevel()) {
                authorities.add(new SimpleGrantedAuthority(roleName.name()));
            }
        }

        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                authorities,
                user.getCurrentRoleName(),
                currentRoleName,
                user.getCurrentLevel()
        );
    }

    /**
     * 특정 직급 이상의 권한을 가지고 있는지 확인
     *
     * @param roleName 확인할 직급
     * @return 해당 직급 이상의 권한을 가지고 있으면 true
     */
    public boolean hasRole(String roleName) {
        return authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals(roleName));
    }

    /**
     * 관리자급 이상의 권한을 가지고 있는지 확인
     *
     * @return 과장 이상의 권한을 가지고 있으면 true
     */
    public boolean isManager() {
        return hasRole(com.rookies.log2doc.entity.Role.RoleName.MANAGER.name());
    }

    /**
     * 임원급 이상의 권한을 가지고 있는지 확인
     *
     * @return 이사 이상의 권한을 가지고 있으면 true
     */
    public boolean isExecutive() {
        return hasRole(com.rookies.log2doc.entity.Role.RoleName.VICE_PRESIDENT.name());
    }

    /**
     * 최고 경영진 권한을 가지고 있는지 확인
     *
     * @return CEO 권한을 가지고 있으면 true
     */
    public boolean isCEO() {
        return hasRole(com.rookies.log2doc.entity.Role.RoleName.CEO.name());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}