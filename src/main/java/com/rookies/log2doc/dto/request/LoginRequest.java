package com.rookies.log2doc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 로그인 요청 DTO
 */
@Data
public class LoginRequest {
    
    @NotBlank(message = "사용자명은 필수입니다.")
    @Size(min = 3, max = 50, message = "사용자명은 3~50자 사이여야 합니다.")
    private String username;
    
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 6, max = 40, message = "비밀번호는 6~40자 사이여야 합니다.")
    private String password;
    
    private String deviceInfo;
    private String ipAddress;
}