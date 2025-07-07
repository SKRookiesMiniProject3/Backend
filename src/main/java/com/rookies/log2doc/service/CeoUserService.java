package com.rookies.log2doc.service;

import com.rookies.log2doc.dto.response.UserDetailResponse;
import com.rookies.log2doc.dto.response.UserListResponse;
import com.rookies.log2doc.entity.User;
import com.rookies.log2doc.repository.UserRepository;
import com.rookies.log2doc.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * CEO 전용 사용자 관리 서비스
 * JWT 토큰과 Redis 캐싱을 통한 권한 검증 로직을 포함
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CeoUserService {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final RedisTemplate<String, String> redisTemplate;

    // Redis 키 패턴
    private static final String USER_ROLE_CACHE_KEY = "user:role:";
    private static final String CEO_ACCESS_LOG_KEY = "ceo:access:";
    private static final int CACHE_EXPIRATION_MINUTES = 30;

    /**
     * 모든 사용자 목록 조회 (CEO 전용)
     *
     * 알고리즘 설명:
     * 1. JWT 토큰에서 사용자명을 추출
     * 2. Redis에서 사용자의 직급 정보를 캐시로 확인
     * 3. 캐시가 없으면 DB에서 조회하여 Redis에 저장
     * 4. CEO 권한 검증 후 모든 사용자 목록 반환
     *
     * 시간 복잡도: O(1) - Redis 캐시 히트 시, O(n) - DB 조회 시
     * 공간 복잡도: O(1) - 캐시 저장 공간 제외
     *
     * @param jwtToken JWT 토큰
     * @return 모든 사용자 목록
     * @throws AccessDeniedException CEO가 아닌 경우
     */
    public UserListResponse getAllUsers(String jwtToken) {
        log.info("CEO 전용 전체 사용자 조회 요청");

        // 1단계: JWT 토큰 유효성 검증 및 사용자명 추출
        if (!jwtUtils.validateJwtToken(jwtToken)) {
            log.error("유효하지 않은 JWT 토큰");
            throw new AccessDeniedException("유효하지 않은 토큰입니다.");
        }

        String username = jwtUtils.getUserNameFromJwtToken(jwtToken);
        log.debug("JWT에서 추출한 사용자명: {}", username);

        // 2단계: CEO 권한 검증
        validateCeoAccess(username);

        // 3단계: CEO 접근 로그 기록
        logCeoAccess(username, "GET_ALL_USERS");

        // 4단계: 모든 사용자 목록 조회 (즉시 로딩으로 Role 정보 포함)
        List<User> users = userRepository.findAllWithRole(); // 페치 조인 사용
        log.info("총 {} 명의 사용자 조회 완료", users.size());

        UserListResponse response = UserListResponse.builder()
                .users(users.stream()
                        .map(this::convertToUserSummary)
                        .collect(Collectors.toList()))
                .totalCount(users.size())
                .build();

        return response;
    }

    private UserListResponse.UserSummary convertToUserSummary(User user) {
        return UserListResponse.UserSummary.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .roleName(user.getCurrentRoleName().name())
                .roleDescription(user.getCurrentRoleName().getDescription())
                .isActive(user.getIsActive())
                .isEmailVerified(user.getIsEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * 특정 사용자 상세 정보 조회 (CEO 전용)
     *
     * 알고리즘 설명:
     * 1. JWT 토큰 검증 및 사용자명 추출
     * 2. Redis 캐시를 통한 CEO 권한 검증
     * 3. 대상 사용자 ID로 사용자 정보 조회 (즉시 로딩)
     * 4. 트랜잭션 내에서 DTO 변환하여 반환
     *
     * 시간 복잡도: O(1) - 기본 키 조회
     * 공간 복잡도: O(1)
     *
     * @param jwtToken JWT 토큰
     * @param userId 조회할 사용자 ID
     * @return 사용자 상세 정보 (DTO)
     * @throws AccessDeniedException CEO가 아닌 경우
     * @throws RuntimeException 사용자를 찾을 수 없는 경우
     */
    public UserDetailResponse getUserById(String jwtToken, Long userId) {
        log.info("CEO 전용 사용자 상세 조회 요청 - 사용자 ID: {}", userId);

        // 1단계: JWT 토큰 유효성 검증
        if (!jwtUtils.validateJwtToken(jwtToken)) {
            log.error("유효하지 않은 JWT 토큰");
            throw new AccessDeniedException("유효하지 않은 토큰입니다.");
        }

        String username = jwtUtils.getUserNameFromJwtToken(jwtToken);
        log.debug("JWT에서 추출한 사용자명: {}", username);

        // 2단계: CEO 권한 검증
        validateCeoAccess(username);

        // 3단계: 사용자 ID 유효성 검증
        if (userId == null || userId <= 0) {
            log.error("잘못된 사용자 ID: {}", userId);
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }

        // 4단계: CEO 접근 로그 기록
        logCeoAccess(username, "GET_USER_BY_ID:" + userId);

        // 5단계: 사용자 정보 조회 (페치 조인으로 Role 정보 즉시 로딩)
        User user = userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> {
                    log.error("사용자를 찾을 수 없음 - ID: {}", userId);
                    return new RuntimeException("사용자를 찾을 수 없습니다. ID: " + userId);
                });

        // 6단계: 트랜잭션 내에서 DTO 변환 (지연 로딩 문제 해결)
        UserDetailResponse response = convertToUserDetail(user);

        log.info("사용자 상세 정보 조회 완료 - 사용자명: {}", user.getUsername());
        return response;
    }

    /**
     * User 엔티티를 사용자 상세 정보로 변환
     * 트랜잭션 내에서 실행되어 지연 로딩 문제 해결
     *
     * @param user User 엔티티
     * @return 사용자 상세 정보
     */
    private UserDetailResponse convertToUserDetail(User user) {
        return UserDetailResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .roleName(user.getCurrentRoleName().name())
                .roleDescription(user.getCurrentRoleName().getDescription())
                .roleLevel(user.getCurrentLevel())
                .isActive(user.getIsActive())
                .isEmailVerified(user.getIsEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .isManager(user.isManager())
                .isExecutive(user.isExecutive())
                .isCeo(user.isCEO())
                .build();
    }

    /**
     * CEO 권한 검증 메서드
     * Redis 캐시를 활용한 고성능 권한 검증
     *
     * 알고리즘 설명:
     * 1. Redis에서 사용자의 캐시된 직급 정보 조회
     * 2. 캐시 미스 시 DB에서 사용자 정보 조회
     * 3. 조회된 직급 정보를 Redis에 캐싱
     * 4. CEO 권한 여부 검증
     *
     * 시간 복잡도: O(1) - Redis 캐시 히트 시
     * 공간 복잡도: O(1)
     *
     * @param username 검증할 사용자명
     * @throws AccessDeniedException CEO가 아닌 경우
     */
    private void validateCeoAccess(String username) {
        // Redis 캐시에서 사용자 직급 조회
        String cacheKey = USER_ROLE_CACHE_KEY + username;
        String cachedRole = redisTemplate.opsForValue().get(cacheKey);

        String userRole;

        if (cachedRole != null) {
            // 캐시 히트: 캐시된 직급 정보 사용
            userRole = cachedRole;
            log.debug("Redis 캐시에서 사용자 직급 조회 성공: {} -> {}", username, userRole);
        } else {
            // 캐시 미스: DB에서 사용자 정보 조회 (페치 조인 사용)
            User user = userRepository.findByUsernameWithRole(username)
                    .orElseThrow(() -> {
                        log.error("사용자를 찾을 수 없음: {}", username);
                        return new AccessDeniedException("사용자를 찾을 수 없습니다.");
                    });

            userRole = user.getCurrentRoleName().name();

            // Redis에 캐싱 (TTL: 30분)
            redisTemplate.opsForValue().set(cacheKey, userRole,
                    CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES);

            log.debug("DB에서 사용자 직급 조회 후 Redis 캐싱 완료: {} -> {}", username, userRole);
        }

        // CEO 권한 검증
        if (!"CEO".equals(userRole)) {
            log.warn("CEO 권한이 아닌 사용자의 접근 시도: {} (직급: {})", username, userRole);
            throw new AccessDeniedException("CEO 권한이 필요합니다. 현재 권한: " + userRole);
        }

        log.info("CEO 권한 검증 성공: {}", username);
    }

    /**
     * CEO 접근 로그 기록
     * 보안 감사를 위한 접근 로그를 Redis에 기록
     *
     * 알고리즘 설명:
     * 1. 현재 시간과 함께 접근 로그 생성
     * 2. Redis에 접근 로그 저장 (TTL: 24시간)
     *
     * 시간 복잡도: O(1)
     * 공간 복잡도: O(1)
     *
     * @param username CEO 사용자명
     * @param action 수행한 작업
     */
    private void logCeoAccess(String username, String action) {
        String logKey = CEO_ACCESS_LOG_KEY + username + ":" + System.currentTimeMillis();
        String logValue = String.format("ACTION=%s,TIMESTAMP=%d", action, System.currentTimeMillis());

        // 24시간 TTL로 접근 로그 저장
        redisTemplate.opsForValue().set(logKey, logValue, 24, TimeUnit.HOURS);

        log.info("CEO 접근 로그 기록: {} -> {}", username, action);
    }

    /**
     * 사용자 직급 캐시 무효화
     * 사용자의 직급이 변경되었을 때 호출
     *
     * @param username 캐시를 무효화할 사용자명
     */
    public void invalidateUserRoleCache(String username) {
        String cacheKey = USER_ROLE_CACHE_KEY + username;
        redisTemplate.delete(cacheKey);
        log.info("사용자 직급 캐시 무효화: {}", username);
    }

    /**
     * CEO 접근 통계 조회
     *
     * @param username CEO 사용자명
     * @return 접근 횟수
     */
    public long getCeoAccessCount(String username) {
        String pattern = CEO_ACCESS_LOG_KEY + username + ":*";
        return redisTemplate.keys(pattern).size();
    }
}