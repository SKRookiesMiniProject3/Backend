package com.rookies.log2doc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Spring Boot 애플리케이션의 메인 클래스
 * JPA Auditing 기능을 활성화하여 Entity의 생성/수정 시간을 자동으로 관리
 */
@SpringBootApplication
@EnableJpaAuditing  // JPA Auditing 활성화 (CreatedDate, LastModifiedDate 자동 설정)
public class Log2docApplication {
	public static void main(String[] args) {
		SpringApplication.run(Log2docApplication.class, args);
	}
}