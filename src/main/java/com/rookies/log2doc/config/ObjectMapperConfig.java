package com.rookies.log2doc.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 전역 ObjectMapper 설정 클래스.
 * LocalDateTime 등 Java 8 Date/Time API 직렬화 설정을 포함함.
 */
@Configuration
public class ObjectMapperConfig {

    /**
     * 커스텀 ObjectMapper Bean 등록.
     * - JavaTimeModule 등록으로 LocalDateTime 직렬화 지원
     * - 날짜를 timestamp 대신 ISO-8601 형식으로 출력
     *
     * @return 커스텀 ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Java 8 Time 모듈 등록 (LocalDateTime 등)
        mapper.registerModule(new JavaTimeModule());

        // 날짜/시간을 timestamp 대신 문자열로 직렬화
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }
}
