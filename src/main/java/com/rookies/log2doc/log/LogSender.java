package com.rookies.log2doc.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class LogSender {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    // Flask 설정값 주입
    @Value("${flask.base.url}")
    private String flaskBaseUrl;

    @Value("${flask.endpoint.analyze}")
    private String analyzeEndpoint;

    /**
     * Flask 요구사항에 맞는 필드만 필터링해서 전송
     */
    public void sendLog(Map<String, Object> logData) {
        try {
//            // ✅ 입력 데이터 디버깅
//            log.debug("🔍 [DEBUG] 입력 로그 데이터 키들: {}", logData.keySet());
//            log.debug("🔍 [DEBUG] request_headers 원본: {}", logData.get("request_headers"));

            // ✅ Flask가 요구하는 필드만 필터링
            Map<String, Object> filteredLogData = filterLogData(logData);

//            String json = objectMapper.writeValueAsString(filteredLogData);
//            log.info("📤 [TEST] Flask 전송용 JSON: {}", json);
//
//            // ✅ 필터링된 로그 확인
//            log.debug("📤 Flask 전송 로그: {}", filteredLogData);

            // Flask URL 동적 구성
            String flaskUrl = flaskBaseUrl + analyzeEndpoint;

            String response = restClient.post()
                    .uri(flaskUrl)
                    .body(filteredLogData)
                    .retrieve()
                    .body(String.class);

            log.info("✅ Flask로 로그 전송 완료: {}", response);

        } catch (Exception e) {
            log.error("🚨 Flask 로그 전송 실패 (URL: {}{}): {}",
                    flaskBaseUrl, analyzeEndpoint, e.getMessage());
        }
    }

    /**
     * Flask 요구사항에 맞는 필드만 추출
     *
     * 전송 필드:
     * - timestamp
     * - user_id
     * - request_method
     * - request_url
     * - user_role
     * - action_type
     * - document_classification
     * - access_result
     * - request_headers (User-Agent만)
     * - document_id
     */
    private Map<String, Object> filterLogData(Map<String, Object> originalLogData) {
        Map<String, Object> filteredData = new HashMap<>();

        // ✅ 필수 필드들만 추출
        putIfExists(filteredData, originalLogData, "timestamp");
        putIfExists(filteredData, originalLogData, "user_id");
        putIfExists(filteredData, originalLogData, "request_method");
        putIfExists(filteredData, originalLogData, "request_url");
        putIfExists(filteredData, originalLogData, "user_role");
        putIfExists(filteredData, originalLogData, "action_type");
        putIfExists(filteredData, originalLogData, "document_id");
        putIfExists(filteredData, originalLogData, "document_classification");
        putIfExists(filteredData, originalLogData, "access_result");

        // ✅ request_headers에서 User-Agent만 추출
        extractUserAgent(filteredData, originalLogData);

        return filteredData;
    }

    /**
     * 원본 데이터에 키가 존재하면 필터링된 데이터에 추가
     */
    private void putIfExists(Map<String, Object> target, Map<String, Object> source, String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }

    /**
     * request_headers에서 User-Agent만 추출해서 담기
     */
    private void extractUserAgent(Map<String, Object> target, Map<String, Object> source) {
        Object requestHeaders = source.get("request_headers");

        log.debug("🔍 [DEBUG] extractUserAgent 호출됨");
        log.debug("🔍 [DEBUG] requestHeaders 타입: {}", requestHeaders != null ? requestHeaders.getClass() : "null");
        log.debug("🔍 [DEBUG] requestHeaders 값: {}", requestHeaders);

        if (requestHeaders instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> headersMap = (Map<String, Object>) requestHeaders;

            // ✅ User-Agent를 대소문자 구분 없이 찾기
            String userAgent = findUserAgentIgnoreCase(headersMap);
//            log.debug("🔍 [DEBUG] 추출된 User-Agent: {}", userAgent);

            if (userAgent != null && !userAgent.trim().isEmpty()) {
                target.put("request_headers", Map.of("User-Agent", userAgent));
//                log.debug("✅ [DEBUG] User-Agent 설정 완료");
            } else {
                // User-Agent가 없으면 기본값 설정
                target.put("request_headers", Map.of("User-Agent", "Unknown"));
//                log.debug("⚠️ [DEBUG] User-Agent 없음, 기본값 설정");
            }
        } else {
            // request_headers가 없거나 Map이 아니면 기본값 설정
            target.put("request_headers", Map.of("User-Agent", "Unknown"));
//            log.warn("❌ [DEBUG] request_headers가 Map이 아님 또는 null, 기본값 설정");
        }

//        log.debug("🔍 [DEBUG] 최종 target에 설정된 request_headers: {}", target.get("request_headers"));
    }

    /**
     * Map에서 User-Agent를 대소문자 구분 없이 찾기
     */
    private String findUserAgentIgnoreCase(Map<String, Object> headers) {
        // 일반적인 User-Agent 키 패턴들을 시도
        String[] userAgentKeys = {"User-Agent", "user-agent", "USER-AGENT", "User-agent"};

        for (String key : userAgentKeys) {
            Object value = headers.get(key);
            if (value instanceof String) {
                return (String) value;
            }
        }

        // 위의 패턴으로 찾지 못했다면 대소문자 구분 없이 검색
        for (Map.Entry<String, Object> entry : headers.entrySet()) {
            if (entry.getKey() != null &&
                    entry.getKey().toLowerCase().equals("user-agent") &&
                    entry.getValue() instanceof String) {
                return (String) entry.getValue();
            }
        }

        return null;
    }
}