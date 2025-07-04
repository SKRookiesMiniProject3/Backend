package com.rookies.log2doc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 암호화를 위한 설정 클래스
 * BCrypt 해싱 알고리즘을 사용하여 비밀번호 보안 강화
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // strength 값은 해싱 라운드 수를 결정 (기본값: 10)
        // 높을수록 보안성 증가하지만 처리 시간도 증가
        return new BCryptPasswordEncoder(12); // 2^12 = 4096 라운드
    }
}