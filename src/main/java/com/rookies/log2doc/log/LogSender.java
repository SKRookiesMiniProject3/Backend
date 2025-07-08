package com.rookies.log2doc.log;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class LogSender {

    private final RestClient restClient;

    public void sendLog(Map<String, Object> logData) {
        try {
            String response = restClient.post()
                    .uri("http://flask-server/logs")
                    .body(logData)
                    .retrieve()
                    .body(String.class);

            log.info("✅ Flask로 로그 전송 완료: {}", response);

        } catch (Exception e) {
            log.error("🚨 Flask 로그 전송 실패: {}", e.getMessage());
        }
    }
}
