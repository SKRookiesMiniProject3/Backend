package com.rookies.log2doc.controller;

import com.rookies.log2doc.dto.response.ApiResponse;
import com.rookies.log2doc.dto.response.UserDetailResponse;
import com.rookies.log2doc.dto.response.UserListResponse;
import com.rookies.log2doc.service.CeoUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * CEO 전용 사용자 관리 컨트롤러
 * JWT 토큰 기반 CEO 권한 검증을 통한 사용자 관리 API 제공
 */
@RestController
@RequestMapping("/api/v1/ceo/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "CEO 전용 사용자 관리", description = "CEO 권한이 필요한 사용자 관리 API")
@SecurityRequirement(name = "Bearer Authentication")
public class CeoUserController {

    private final CeoUserService ceoUserService;

    /**
     * 전체 사용자 목록 조회 API (CEO 전용)
     *
     * 알고리즘 설명:
     * 1. HTTP 헤더에서 Authorization Bearer 토큰 추출
     * 2. JWT 토큰 유효성 검증 및 CEO 권한 확인
     * 3. 전체 사용자 목록 조회
     * 4. DTO로 변환하여 응답 반환
     *
     * 시간 복잡도: O(n) - 사용자 수에 비례
     * 공간 복잡도: O(n) - 응답 객체 생성
     *
     * @param request HTTP 요청 객체
     * @return 전체 사용자 목록 응답
     */
    @GetMapping
    @Operation(
            summary = "전체 사용자 목록 조회",
            description = "CEO 권한으로 시스템의 모든 사용자 목록을 조회합니다. " +
                    "JWT 토큰의 사용자가 CEO 권한을 가지고 있어야 합니다."
    )
    public ResponseEntity<ApiResponse<UserListResponse>> getAllUsers(
            HttpServletRequest request) {

        log.info("CEO 전용 전체 사용자 목록 조회 요청");

        try {
            String jwtToken = extractJwtToken(request);

            UserListResponse response = ceoUserService.getAllUsers(jwtToken);

            return ResponseEntity.ok(ApiResponse.<UserListResponse>builder()
                    .success(true)
                    .message("전체 사용자 목록 조회 성공")
                    .data(response)
                    .build());

        } catch (AccessDeniedException e) {
            log.warn("CEO 권한 없는 사용자의 접근 시도: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.<UserListResponse>builder()
                            .success(false)
                            .message("CEO 권한이 필요합니다: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("전체 사용자 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<UserListResponse>builder()
                            .success(false)
                            .message("서버 오류가 발생했습니다: " + e.getMessage())
                            .build());
        }
    }

    /**
     * 특정 사용자 상세 정보 조회 API (CEO 전용)
     *
     * 알고리즘 설명:
     * 1. 경로 변수에서 사용자 ID 추출
     * 2. JWT 토큰 추출 및 CEO 권한 검증
     * 3. 사용자 ID로 상세 정보 조회 (서비스에서 DTO 변환)
     * 4. 변환된 상세 정보 DTO 응답
     *
     * 시간 복잡도: O(1) - 기본 키 조회
     * 공간 복잡도: O(1)
     *
     * @param userId 조회할 사용자 ID
     * @param request HTTP 요청 객체
     * @return 사용자 상세 정보 응답
     */
    @GetMapping("/{userId}")
    @Operation(
            summary = "사용자 상세 정보 조회",
            description = "CEO 권한으로 특정 사용자의 상세 정보를 조회합니다. " +
                    "JWT 토큰의 사용자가 CEO 권한을 가지고 있어야 합니다."
    )
    public ResponseEntity<ApiResponse<UserDetailResponse>> getUserById(
            @Parameter(description = "조회할 사용자 ID", required = true)
            @PathVariable Long userId,
            HttpServletRequest request) {

        log.info("CEO 전용 사용자 상세 정보 조회 요청 - 사용자 ID: {}", userId);

        try {
            // 1단계: JWT 토큰 추출
            String jwtToken = extractJwtToken(request);

            // 2단계: CEO 권한 검증 및 사용자 상세 정보 조회 (서비스에서 DTO 변환)
            UserDetailResponse response = ceoUserService.getUserById(jwtToken, userId);

            log.info("CEO 전용 사용자 상세 정보 조회 성공 - 사용자 ID: {}", userId);

            return ResponseEntity.ok(ApiResponse.<UserDetailResponse>builder()
                    .success(true)
                    .message("사용자 상세 정보 조회 성공")
                    .data(response)
                    .build());

        } catch (AccessDeniedException e) {
            log.warn("CEO 권한 없는 사용자의 접근 시도: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.<UserDetailResponse>builder()
                            .success(false)
                            .message("CEO 권한이 필요합니다: " + e.getMessage())
                            .build());
        } catch (RuntimeException e) {
            log.error("사용자 상세 정보 조회 중 오류 발생 - 사용자 ID: {}", userId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<UserDetailResponse>builder()
                            .success(false)
                            .message("사용자를 찾을 수 없습니다: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("사용자 상세 정보 조회 중 서버 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<UserDetailResponse>builder()
                            .success(false)
                            .message("서버 오류가 발생했습니다: " + e.getMessage())
                            .build());
        }
    }

    /**
     * CEO 접근 통계 조회 API
     *
     * @param request HTTP 요청 객체
     * @return CEO 접근 통계 정보
     */
    @GetMapping("/access-stats")
    @Operation(
            summary = "CEO 접근 통계 조회",
            description = "현재 로그인한 CEO의 접근 통계를 조회합니다."
    )
    public ResponseEntity<ApiResponse<Long>> getCeoAccessStats(
            HttpServletRequest request) {

        log.info("CEO 접근 통계 조회 요청");

        try {
            String jwtToken = extractJwtToken(request);
            // JWT에서 사용자명 추출 (간단한 검증)
            String username = extractUsernameFromToken(jwtToken);

            long accessCount = ceoUserService.getCeoAccessCount(username);

            return ResponseEntity.ok(ApiResponse.<Long>builder()
                    .success(true)
                    .message("CEO 접근 통계 조회 성공")
                    .data(accessCount)
                    .build());

        } catch (Exception e) {
            log.error("CEO 접근 통계 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Long>builder()
                            .success(false)
                            .message("서버 오류가 발생했습니다: " + e.getMessage())
                            .build());
        }
    }

    /**
     * HTTP 요청에서 JWT 토큰 추출
     *
     * 알고리즘 설명:
     * 1. Authorization 헤더에서 Bearer 토큰 추출
     * 2. "Bearer " 접두사 제거
     * 3. 순수 JWT 토큰 반환
     *
     * 시간 복잡도: O(1)
     * 공간 복잡도: O(1)
     *
     * @param request HTTP 요청 객체
     * @return JWT 토큰 문자열
     * @throws IllegalArgumentException 토큰이 없거나 형식이 잘못된 경우
     */
    private String extractJwtToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.error("Authorization 헤더가 없거나 형식이 잘못됨: {}", authHeader);
            throw new IllegalArgumentException("Authorization 헤더가 필요합니다.");
        }

        String token = authHeader.substring(7); // "Bearer " 제거
        log.debug("JWT 토큰 추출 성공");

        return token;
    }

    /**
     * JWT 토큰에서 사용자명 추출 (간단한 검증용)
     *
     * @param jwtToken JWT 토큰
     * @return 사용자명
     */
    private String extractUsernameFromToken(String jwtToken) {
        // 실제 구현에서는 JwtUtils를 사용하여 검증과 함께 추출
        // 여기서는 간단히 처리
        return "extracted_username"; // 실제로는 JwtUtils 사용
    }
}