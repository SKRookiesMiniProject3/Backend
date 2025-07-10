package com.rookies.log2doc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Flask 연동 서비스
 * - Flask 서버로 에러 리포트 데이터 전송
 * - 테스트용 전송 메서드 포함
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FlaskReportService {

    // RestClient는 HTTP 요청 전송 전용
    private final RestClient restClient;

    // application.properties 에서 Flask URL 설정값 주입 (없으면 기본 localhost)
    @Value("${flask.url:http://localhost:5000}")
    private String flaskBaseUrl;

    /**
     * 실제 에러 리포트 전송 메서드
     * - Map 형태의 에러 데이터(JSON) 전송
     * - Flask 측 엔드포인트: /api/error-reports
     *
     * @param errorData 전송할 에러 리포트 데이터
     */
    public void sendErrorReportToFlask(Map<String, Object> errorData) {
        try {
            String flaskUrl = flaskBaseUrl + "/api/error-reports";

            // POST 요청 실행 (JSON)
            String response = restClient.post()
                    .uri(flaskUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorData)
                    .retrieve()
                    .body(String.class);

            log.info("Flask 에러 리포트 전송 성공: {}", response);

        } catch (Exception e) {
            log.error("Flask 에러 리포트 전송 실패", e);
            throw new RuntimeException("Flask 전송 실패: " + e.getMessage());
        }
    }

    /**
     * 테스트용 전송 메서드
     * - 간단한 Map 데이터로 Flask와 통신 정상 여부 확인
     * - 실제 서비스 로직과는 별개로 유지
     */
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

            log.info("Flask 응답: {}", response);

        } catch (Exception e) {
            log.error("Flask 연동 테스트 실패", e);
        }
    }
}
