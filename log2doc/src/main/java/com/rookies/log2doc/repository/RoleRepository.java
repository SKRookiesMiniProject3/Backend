package com.rookies.log2doc.repository;

import com.rookies.log2doc.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 권한 정보 관련 데이터베이스 접근 Repository
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    /**
     * 권한명으로 권한 조회
     * @param name 권한명
     * @return 권한 정보 Optional
     */
    Optional<Role> findByName(Role.RoleName name);
    
    /**
     * 권한명 존재 여부 확인
     * @param name 권한명
     * @return 존재하면 true, 아니면 false
     */
    Boolean existsByName(Role.RoleName name);
}