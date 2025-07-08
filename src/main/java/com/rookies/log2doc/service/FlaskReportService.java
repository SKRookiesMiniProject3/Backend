package com.rookies.log2doc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlaskReportService {

    private final RestClient restClient;

    @Value("${flask.url:http://localhost:5000}")
    private String flaskBaseUrl;

    // ✅ 에러 리포트를 Flask로 전송
    public void sendErrorReportToFlask(Map<String, Object> errorData) {
        try {
            String flaskUrl = flaskBaseUrl + "/api/error-reports";

            String response = restClient.post()
                    .uri(flaskUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorData)
                    .retrieve()
                    .body(String.class);

            log.info("📌 Flask 에러 리포트 전송 성공: {}", response);

        } catch (Exception e) {
            log.error("🚨 Flask 에러 리포트 전송 실패", e);
            throw new RuntimeException("Flask 전송 실패: " + e.getMessage());
        }
    }

    // ✅ 기존 테스트 메서드도 유지
    public void sendReportToFlask() {
        String flaskUrl = flaskBaseUrl + "/receive-report";

        Map<String, Object> requestBody = Map.of(
                "message", "스프링에서 RestClient로 보낸 테스트 에러!",
                "resolved", false
        );

        try {
            String response = restClient.post()
                    .uri(flaskUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            log.info("📌 Flask 응답: {}", response);
        } catch (Exception e) {
            log.error("🚨 Flask 연동 테스트 실패", e);
        }
    }
}