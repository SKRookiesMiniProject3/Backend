// src/main/java/com/rookies/log2doc/log/LogSender.java
package com.rookies.log2doc.log;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class LogSender {

    private final RestTemplate restTemplate;

    public void sendLog(Map<String, Object> logData) {
        try {
            ResponseEntity<String> response = restTemplate.postForEntity("http://flask-server/logs", logData, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Flask로 로그 전송 완료: {}", response.getBody());
            } else {
                log.warn("⚠️ Flask 응답 상태 이상: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("🚨 Flask 로그 전송 실패: {}", e.getMessage());
        }
    }
}
