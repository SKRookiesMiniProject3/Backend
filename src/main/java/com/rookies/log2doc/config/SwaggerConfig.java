package com.rookies.log2doc.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 설정 클래스
 * 
 * 알고리즘 설명:
 * 1. OpenAPI 3.0 기반 API 문서 설정
 * 2. JWT Bearer 토큰 인증 스킴 설정
 * 3. API 정보 및 서버 정보 구성
 * 
 * 시간 복잡도: O(1) - 설정 객체 생성
 * 공간 복잡도: O(1) - 고정된 설정 크기
 */
@Configuration
public class SwaggerConfig {
    
    /**
     * OpenAPI 설정 Bean
     * 
     * 알고리즘 설명:
     * 1. API 기본 정보 설정 (제목, 설명, 버전)
     * 2. JWT Bearer 토큰 인증 스킴 추가
     * 3. 서버 정보 설정
     * 4. 연락처 및 라이선스 정보 포함
     * 
     * @return OpenAPI 설정 객체
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Log2Doc API")
                .description("로그 문서화 시스템 API 문서")
                .version("v1.0.0")
                .contact(new Contact()
                    .name("Rookies Team")
                    .email("contact@rookies.com")
                    .url("https://github.com/rookies-team"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("개발 서버"),
                new Server()
                    .url("https://api.log2doc.com")
                    .description("운영 서버")))
            .components(new Components()
                .addSecuritySchemes("Bearer Authentication", 
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT 토큰을 사용한 인증. 형식: Bearer {token}")))
            .addSecurityItem(new SecurityRequirement()
                .addList("Bearer Authentication"));
    }
}