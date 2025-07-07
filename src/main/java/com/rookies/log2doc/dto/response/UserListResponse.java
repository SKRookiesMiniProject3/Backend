package com.rookies.log2doc.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 목록 조회 응답 DTO
 * 
 * 알고리즘 설명:
 * 1. 사용자 목록과 총 개수를 포함하는 응답 구조
 * 2. 중첩된 UserSummary 클래스로 사용자 요약 정보 표현
 * 3. 페이징 처리를 위한 확장 가능한 구조
 * 
 * 시간 복잡도: O(n) - 사용자 수에 비례
 * 공간 복잡도: O(n) - 사용자 수에 비례
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 목록 조회 응답")
public class UserListResponse {
    
    /**
     * 사용자 요약 정보 목록
     */
    @JsonProperty("users")
    @Schema(description = "사용자 요약 정보 목록")
    private List<UserSummary> users;
    
    /**
     * 총 사용자 수
     */
    @JsonProperty("totalCount")
    @Schema(description = "총 사용자 수", example = "150")
    private int totalCount;
    
    /**
     * 현재 페이지 (확장성을 위한 필드)
     */
    @JsonProperty("currentPage")
    @Schema(description = "현재 페이지", example = "1")
    @Builder.Default
    private int currentPage = 1;
    
    /**
     * 페이지 크기 (확장성을 위한 필드)
     */
    @JsonProperty("pageSize")
    @Schema(description = "페이지 크기", example = "10")
    @Builder.Default
    private int pageSize = 10;
    
    /**
     * 사용자 요약 정보 DTO
     * 
     * 알고리즘 설명:
     * 1. 사용자 목록에서 보여줄 핵심 정보만 포함
     * 2. 민감한 정보 제외 (패스워드 등)
     * 3. 역할 정보와 상태 정보 포함
     * 
     * 시간 복잡도: O(1) - 단순 객체 생성
     * 공간 복잡도: O(1) - 고정된 필드 수
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "사용자 요약 정보")
    public static class UserSummary {
        
        /**
         * 사용자 ID
         */
        @JsonProperty("id")
        @Schema(description = "사용자 ID", example = "1")
        private Long id;
        
        /**
         * 사용자명
         */
        @JsonProperty("username")
        @Schema(description = "사용자명", example = "john_doe")
        private String username;
        
        /**
         * 이메일
         */
        @JsonProperty("email")
        @Schema(description = "이메일", example = "john@example.com")
        private String email;
        
        /**
         * 전화번호
         */
        @JsonProperty("phone")
        @Schema(description = "전화번호", example = "010-1234-5678")
        private String phone;
        
        /**
         * 역할명
         */
        @JsonProperty("roleName")
        @Schema(description = "역할명", example = "USER")
        private String roleName;
        
        /**
         * 역할 설명
         */
        @JsonProperty("roleDescription")
        @Schema(description = "역할 설명", example = "일반 사용자")
        private String roleDescription;
        
        /**
         * 계정 활성화 상태
         */
        @JsonProperty("isActive")
        @Schema(description = "계정 활성화 상태", example = "true")
        private Boolean isActive;
        
        /**
         * 이메일 인증 상태
         */
        @JsonProperty("isEmailVerified")
        @Schema(description = "이메일 인증 상태", example = "true")
        private Boolean isEmailVerified;
        
        /**
         * 계정 생성일
         */
        @JsonProperty("createdAt")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "계정 생성일", example = "2024-01-01 12:00:00")
        private LocalDateTime createdAt;
        
        /**
         * 마지막 로그인 시간 (확장성을 위한 필드)
         */
        @JsonProperty("lastLoginAt")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "마지막 로그인 시간", example = "2024-01-15 14:30:00")
        private LocalDateTime lastLoginAt;
    }
}