package com.rookies.log2doc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 작업 실행을 위한 Async 설정 클래스.
 * @EnableAsync 를 통해 @Async 어노테이션을 활성화하고
 * ThreadPoolTaskExecutor Bean 을 등록함.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 비동기 작업에 사용할 ThreadPoolTaskExecutor Bean 등록.
     * - corePoolSize: 기본 스레드 수
     * - maxPoolSize: 최대 스레드 수
     * - queueCapacity: 작업 대기열 크기
     * - threadNamePrefix: 스레드 이름 접두사
     *
     * @return Executor Bean
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);             // 기본 스레드 수
        executor.setMaxPoolSize(5);              // 최대 스레드 수
        executor.setQueueCapacity(100);          // 작업 대기열 용량
        executor.setThreadNamePrefix("Flask-Async-"); // 스레드 이름 접두사
        executor.initialize();
        return executor;
    }
}
