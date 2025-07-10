package com.rookies.log2doc.config;

import com.rookies.log2doc.log.LoggingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 설정 클래스.
 * 공통 Interceptor(로깅 등) 설정을 관리함.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    /** 요청/응답 공통 로깅 Interceptor */
    private final LoggingInterceptor loggingInterceptor;

    /**
     * 애플리케이션 전역에 Interceptor 등록.
     * 모든 경로에 LoggingInterceptor를 적용함.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/**"); // 모든 요청 경로에 적용
    }
}
