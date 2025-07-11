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

    // Flask 설정값 주입
    @Value("${flask.base.url}")
    private String flaskBaseUrl;

    @Value("${flask.endpoint.receive-report}")
    private String receiveReportEndpoint;

    @Value("${flask.endpoint.analyze}")
    private String analyzeEndpoint;

    @Value("${flask.endpoint.analyze-advanced}")
    private String analyzeAdvancedEndpoint;

    /**
     * 실제 에러 리포트 전송 메서드
     * - Map 형태의 에러 데이터(JSON) 전송
     * - Flask 측 엔드포인트: /api/error-reports
     *
     * @param errorData 전송할 에러 리포트 데이터
     */
    public void sendErrorReportToFlask(Map<String, Object> errorData) {
        try {
            // 에러 리포트 전용 엔드포인트 (아직 정의 안됨, 필요시 properties에 추가)
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
            log.error("Flask 에러 리포트 전송 실패 (URL: {}/api/error-reports): {}",
                    flaskBaseUrl, e.getMessage());
            throw new RuntimeException("Flask 전송 실패: " + e.getMessage());
        }
    }

    /**
     * 보안 로그 분석 요청 전송
     *
     * @param logData 분석할 로그 데이터
     * @return Flask 분석 결과
     */
    public String sendLogForAnalysis(Map<String, Object> logData) {
        try {
            String flaskUrl = flaskBaseUrl + analyzeEndpoint;

            String response = restClient.post()
                    .uri(flaskUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(logData)
                    .retrieve()
                    .body(String.class);

            log.info("Flask 로그 분석 요청 성공");
            return response;

        } catch (Exception e) {
            log.error("Flask 로그 분석 요청 실패 (URL: {}{}): {}",
                    flaskBaseUrl, analyzeEndpoint, e.getMessage());
            throw new RuntimeException("Flask 분석 요청 실패: " + e.getMessage());
        }
    }

    /**
     * 보안 로그 고급 분석 요청 전송
     *
     * @param logData 분석할 로그 데이터
     * @return Flask 고급 분석 결과
     */
    public String sendLogForAdvancedAnalysis(Map<String, Object> logData) {
        try {
            String flaskUrl = flaskBaseUrl + analyzeAdvancedEndpoint;

            String response = restClient.post()
                    .uri(flaskUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(logData)
                    .retrieve()
                    .body(String.class);

            log.info("Flask 고급 로그 분석 요청 성공");
            return response;

        } catch (Exception e) {
            log.error("Flask 고급 로그 분석 요청 실패 (URL: {}{}): {}",
                    flaskBaseUrl, analyzeAdvancedEndpoint, e.getMessage());
            throw new RuntimeException("Flask 고급 분석 요청 실패: " + e.getMessage());
        }
    }

    /**
     * 테스트용 전송 메서드
     * - 간단한 Map 데이터로 Flask와 통신 정상 여부 확인
     * - 실제 서비스 로직과는 별개로 유지
     */
    public void sendReportToFlask() {
        String flaskUrl = flaskBaseUrl + receiveReportEndpoint;

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

            log.info("Flask 테스트 응답: {}", response);

        } catch (Exception e) {
            log.error("Flask 연동 테스트 실패 (URL: {}{}): {}",
                    flaskBaseUrl, receiveReportEndpoint, e.getMessage());
        }
    }

    /**
     * Flask 서버 상태 확인 (헬스체크)
     */
    public boolean isFlaskServerHealthy() {
        try {
            String healthUrl = flaskBaseUrl + "/health";

            String response = restClient.get()
                    .uri(healthUrl)
                    .retrieve()
                    .body(String.class);

            log.info("Flask 서버 상태 확인 성공: {}", response);
            return true;

        } catch (Exception e) {
            log.error("Flask 서버 상태 확인 실패 (URL: {}/health): {}",
                    flaskBaseUrl, e.getMessage());
            return false;
        }
    }
}