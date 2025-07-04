package com.rookies.log2doc.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 토큰 갱신 요청 DTO
 */
@Data
public class TokenRefreshRequest {

    @NotBlank(message = "리프레시 토큰은 필수입니다.")
    private String refreshToken;
}