// src/main/java/com/rookies/log2doc/log/LogSender.java
package com.rookies.log2doc.log;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class LogSender {

    private final RestTemplate restTemplate;

    public void sendLog(Map<String, Object> logData) {
        try {
            restTemplate.postForEntity("http://flask-server/logs", logData, String.class);
            log.info("✅ Flask로 로그 전송 완료");
        } catch (Exception e) {
            log.error("🚨 Flask 로그 전송 실패: {}", e.getMessage());
        }
    }
}
