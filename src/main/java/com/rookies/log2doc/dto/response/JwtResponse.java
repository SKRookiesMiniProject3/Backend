package com.rookies.log2doc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JWT 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {

    private String token;
    @Builder.Default
    private String type = "Bearer";
    private String refreshToken;
    private Long id;
    private String username;
    private String email;
    private String role;  // List<String> roles에서 String role로 변경
    private long expiresIn;

    public JwtResponse(String accessToken, String refreshToken, Long id, String username, String email, String role, long expiresIn) {
        this.token = accessToken;
        this.refreshToken = refreshToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.expiresIn = expiresIn;
    }
}