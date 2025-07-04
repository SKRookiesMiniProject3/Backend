package com.rookies.log2doc.repository;

import com.rookies.log2doc.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 정보 관련 데이터베이스 접근 Repository
 * 커스텀 쿼리 메서드들을 정의
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 사용자명으로 사용자 조회
     * @param username 사용자명
     * @return 사용자 정보 Optional
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 이메일로 사용자 조회
     * @param email 이메일
     * @return 사용자 정보 Optional
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 사용자명 존재 여부 확인
     * @param username 사용자명
     * @return 존재하면 true, 아니면 false
     */
    Boolean existsByUsername(String username);
    
    /**
     * 이메일 존재 여부 확인
     * @param email 이메일
     * @return 존재하면 true, 아니면 false
     */
    Boolean existsByEmail(String email);
    
    /**
     * 활성 사용자 조회 (권한 정보 포함)
     * @param username 사용자명
     * @return 권한 정보가 포함된 사용자 정보
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.username = :username AND u.isActive = true")
    Optional<User> findActiveUserWithRoles(@Param("username") String username);
    
    /**
     * 이메일 인증이 완료된 사용자 조회
     * @param email 이메일
     * @return 사용자 정보 Optional
     */
    Optional<User> findByEmailAndIsEmailVerifiedTrue(String email);
}