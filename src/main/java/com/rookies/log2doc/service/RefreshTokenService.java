// RefreshTokenService.java
package com.rookies.log2doc.service;

import com.rookies.log2doc.entity.RefreshToken;
import com.rookies.log2doc.entity.User;
import com.rookies.log2doc.exception.TokenRefreshException;
import com.rookies.log2doc.repository.RefreshTokenRepository;
import com.rookies.log2doc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Refresh Token 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {
    
    @Value("${app.jwt.refresh.expiration}")
    private Long refreshTokenDurationMs;
    
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    
    /**
     * 새로운 리프레시 토큰 생성
     * @param userId 사용자 ID
     * @param deviceInfo 기기 정보
     * @param ipAddress IP 주소
     * @return 생성된 리프레시 토큰
     */
    @Transactional
    public RefreshToken createRefreshToken(Long userId, String deviceInfo, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusNanos(refreshTokenDurationMs * 1_000_000);
        
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiryDate(expiryDate)
                .createdAt(LocalDateTime.now())
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .build();
        
        // DB에 저장
        refreshToken = refreshTokenRepository.save(refreshToken);
        
        // Redis에도 저장 (캐시 및 이중화)
        String redisKey = REFRESH_TOKEN_PREFIX + token;
        redisTemplate.opsForValue().set(redisKey, userId.toString(), refreshTokenDurationMs, TimeUnit.MILLISECONDS);
        
        return refreshToken;
    }
    
    /**
     * 리프레시 토큰으로 토큰 정보 조회
     * @param token 토큰 문자열
     * @return 토큰 정보
     */
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
    
    /**
     * 리프레시 토큰 검증
     * @param token 리프레시 토큰
     * @return 검증된 토큰
     */
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            // Redis에서도 삭제
            redisTemplate.delete(REFRESH_TOKEN_PREFIX + token.getToken());
            throw new TokenRefreshException(token.getToken(), "리프레시 토큰이 만료되었습니다. 다시 로그인해주세요.");
        }
        return token;
    }
    
    /**
     * 사용자의 모든 리프레시 토큰 무효화
     * @param user 사용자
     */
    @Transactional
    public void deleteByUser(User user) {
        // 사용자의 모든 토큰 조회 후 Redis에서 삭제
        refreshTokenRepository.findByUserAndIsRevokedFalse(user)
                .forEach(token -> redisTemplate.delete(REFRESH_TOKEN_PREFIX + token.getToken()));
        
        // DB에서 모든 토큰 무효화
        refreshTokenRepository.revokeAllUserTokens(user);
    }
    
    /**
     * 특정 리프레시 토큰 삭제
     * @param refreshToken 삭제할 토큰
     */
    @Transactional
    public void deleteRefreshToken(RefreshToken refreshToken) {
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + refreshToken.getToken());
    }
    
    /**
     * 만료된 토큰 정리 (스케줄러)
     */
    @Scheduled(fixedRate = 86400000) // 24시간마다 실행
    @Transactional
    public void deleteExpiredTokens() {
        int deletedCount = refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("만료된 리프레시 토큰 {}개를 삭제했습니다.", deletedCount);
    }
    
    /**
     * Redis에서 토큰 유효성 확인
     * @param token 토큰 문자열
     * @return 유효하면 true
     */
    public boolean isTokenValidInRedis(String token) {
        String redisKey = REFRESH_TOKEN_PREFIX + token;
        return redisTemplate.hasKey(redisKey);
    }
}