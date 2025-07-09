package com.rookies.log2doc.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * Flask 요구사항에 맞는 필드만 필터링해서 전송
     */
    public void sendLog(Map<String, Object> logData) {
        try {
            // ✅ Flask가 요구하는 필드만 필터링
            Map<String, Object> filteredLogData = filterLogData(logData);

//            String json = objectMapper.writeValueAsString(filteredLogData);
//            log.info("📤 [TEST] Flask 전송용 JSON: {}", json);
//
//            // ✅ 필터링된 로그 확인
//            log.debug("📤 Flask 전송 로그: {}", filteredLogData);

            String response = restClient.post()
                    .uri("http://flask-server/logs")
                    .body(filteredLogData)
                    .retrieve()
                    .body(String.class);

            log.info("✅ Flask로 로그 전송 완료: {}", response);

        } catch (Exception e) {
            log.error("🚨 Flask 로그 전송 실패: {}", e.getMessage());
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
     * - request_headers
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
        putIfExists(filteredData, originalLogData, "request_headers");

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
}