package com.rookies.log2doc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 로그인 성공 시 클라이언트에 전달되는 JWT 응답 DTO.
 * 액세스 토큰, 리프레시 토큰, 사용자 정보, 만료 시간 등을 포함함.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {

    /** 액세스 토큰 (JWT) */
    private String token;

    /** 토큰 타입 (기본: Bearer) */
    @Builder.Default
    private String type = "Bearer";

    /** 리프레시 토큰 */
    private String refreshToken;

    /** 사용자 ID */
    private Long id;

    /** 사용자 이름 (username) */
    private String username;

    /** 사용자 이메일 */
    private String email;

    /** 사용자 권한 (역할) */
    private String role; // roles를 List에서 단일 String으로 변경

    /** 액세스 토큰 만료까지 남은 시간(초) */
    private long expiresIn;

    /**
     * 필드별 값을 직접 설정할 수 있는 커스텀 생성자.
     *
     * @param accessToken  액세스 토큰
     * @param refreshToken 리프레시 토큰
     * @param id           사용자 ID
     * @param username     사용자 이름
     * @param email        사용자 이메일
     * @param role         사용자 역할
     * @param expiresIn    만료 시간(초)
     */
    public JwtResponse(String accessToken, String refreshToken, Long id,
                       String username, String email, String role, long expiresIn) {
        this.token = accessToken;
        this.refreshToken = refreshToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.expiresIn = expiresIn;
    }
}
