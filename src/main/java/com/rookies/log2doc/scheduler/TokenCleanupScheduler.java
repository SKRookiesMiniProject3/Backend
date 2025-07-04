package com.rookies.log2doc.scheduler;

import com.rookies.log2doc.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 만료된 토큰을 정리하는 스케줄러
 * 매일 자정에 실행되어 만료된 토큰들을 삭제
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupScheduler {
    
    private final RefreshTokenRepository refreshTokenRepository;
    
    /**
     * 매일 자정에 만료된 토큰 정리
     * 시간 복잡도: O(n) - 삭제해야 할 토큰 수에 비례
     * 공간 복잡도: O(1) - 고정된 메모리 사용
     */
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정 실행
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("만료된 토큰 정리 작업 시작...");
        
        try {
            int deletedCount = refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
            log.info("만료된 토큰 정리 완료: {}개 삭제", deletedCount);
        } catch (Exception e) {
            log.error("만료된 토큰 정리 중 오류 발생", e);
        }
    }
    
    /**
     * 매시간 실행되는 가벼운 정리 작업
     * 시스템 부하를 줄이기 위해 소량씩 처리
     */
    @Scheduled(fixedRate = 3600000) // 1시간마다 실행
    @Transactional
    public void lightweightCleanup() {
        log.debug("가벼운 토큰 정리 작업 실행");
        
        try {
            // 1시간 전에 만료된 토큰들만 삭제
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            int deletedCount = refreshTokenRepository.deleteExpiredTokens(oneHourAgo);
            
            if (deletedCount > 0) {
                log.info("가벼운 토큰 정리 완료: {}개 삭제", deletedCount);
            }
        } catch (Exception e) {
            log.error("가벼운 토큰 정리 중 오류 발생", e);
        }
    }
}