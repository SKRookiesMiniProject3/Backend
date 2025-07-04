// RefreshToken Repository
package com.rookies.log2doc.repository;

import com.rookies.log2doc.entity.RefreshToken;
import com.rookies.log2doc.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Refresh Token 관련 데이터베이스 접근 Repository
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    /**
     * 토큰 문자열로 토큰 조회
     * @param token 토큰 문자열
     * @return 토큰 정보 Optional
     */
    Optional<RefreshToken> findByToken(String token);
    
    /**
     * 사용자의 모든 유효한 토큰 조회
     * @param user 사용자
     * @return 토큰 목록
     */
    List<RefreshToken> findByUserAndIsRevokedFalse(User user);
    
    /**
     * 만료된 토큰들 삭제
     * @param now 현재 시간
     * @return 삭제된 토큰 수
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);
    
    /**
     * 사용자의 모든 토큰 무효화
     * @param user 사용자
     * @return 무효화된 토큰 수
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true WHERE rt.user = :user")
    int revokeAllUserTokens(@Param("user") User user);
}