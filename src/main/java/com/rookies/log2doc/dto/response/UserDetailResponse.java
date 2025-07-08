package com.rookies.log2doc.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 상세 정보 조회 응답 DTO
 * 
 * 알고리즘 설명:
 * 1. 사용자의 모든 상세 정보를 포함하는 응답 구조
 * 2. 보안을 위해 민감한 정보는 제외 (패스워드 등)
 * 3. 역할 정보와 권한 정보를 상세히 포함
 * 
 * 시간 복잡도: O(1) - 단순 객체 생성
 * 공간 복잡도: O(1) - 고정된 필드 수
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 상세 정보 조회 응답")
public class UserDetailResponse {
    
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
     * 역할 레벨
     */
    @JsonProperty("roleLevel")
    @Schema(description = "역할 레벨", example = "1")
    private Integer roleLevel;
    
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
     * 계정 수정일
     */
    @JsonProperty("updatedAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "계정 수정일", example = "2024-01-15 10:30:00")
    private LocalDateTime updatedAt;
    
    /**
     * 매니저 권한 여부
     */
    @JsonProperty("isManager")
    @Schema(description = "매니저 권한 여부", example = "false")
    private Boolean isManager;
    
    /**
     * 임원 권한 여부
     */
    @JsonProperty("isExecutive")
    @Schema(description = "임원 권한 여부", example = "false")
    private Boolean isExecutive;
    
    /**
     * CEO 권한 여부
     */
    @JsonProperty("isCeo")
    @Schema(description = "CEO 권한 여부", example = "false")
    private Boolean isCeo;
    
    /**
     * 마지막 로그인 시간 (확장성을 위한 필드)
     */
    @JsonProperty("lastLoginAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "마지막 로그인 시간", example = "2024-01-15 14:30:00")
    private LocalDateTime lastLoginAt;
    
    /**
     * 로그인 횟수 (확장성을 위한 필드)
     */
    @JsonProperty("loginCount")
    @Schema(description = "로그인 횟수", example = "25")
    private Long loginCount;
    
    /**
     * 프로필 이미지 URL (확장성을 위한 필드)
     */
    @JsonProperty("profileImageUrl")
    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
    private String profileImageUrl;
    
    /**
     * 부서 정보 (확장성을 위한 필드)
     */
    @JsonProperty("department")
    @Schema(description = "부서 정보", example = "개발팀")
    private String department;
    
    /**
     * 직책 정보 (확장성을 위한 필드)
     */
    @JsonProperty("position")
    @Schema(description = "직책 정보", example = "시니어 개발자")
    private String position;
}