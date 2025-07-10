package com.rookies.log2doc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 외부 HTTP 호출에 사용할 RestClient Bean 설정 클래스.
 * RestClient를 전역에서 주입해 재사용할 수 있음.
 */
@Configuration
public class RestClientConfig {

    /**
     * RestClient Bean 등록.
     * 필요한 경우 커스텀 설정을 추가할 수 있음.
     *
     * @return 기본 RestClient 인스턴스
     */
    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }
}
