package com.rookies.log2doc.log;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LogBuilder {

    /**
     * 모든 요청의 공통 로그 정보 + 기본 보안 필드 세팅
     * @param request  HttpServletRequest
     * @param auth     현재 사용자 인증 정보 (없으면 null)
     * @return         기본 로그 데이터
     */
    public Map<String, Object> buildBaseLog(HttpServletRequest request, Authentication auth) {
        Map<String, Object> logData = new HashMap<>();

        logData.put("timestamp", Instant.now().toString());
        logData.put("session_id", request.getSession().getId());
        logData.put("request_method", request.getMethod());
        logData.put("request_url", request.getRequestURI());

        // 헤더 수집
        Map<String, String> headersMap = Collections.list(request.getHeaderNames()).stream()
                .collect(HashMap::new, (m, k) -> m.put(k, request.getHeader(k)), HashMap::putAll);
        logData.put("request_headers", headersMap);

        // 사용자 정보 - user_role을 배열로 설정
        if (auth != null && auth.isAuthenticated()) {
            logData.put("user_id", auth.getName());
            List<String> roles = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
            logData.put("user_role", roles);
        } else {
            logData.put("user_id", "anonymous");
            logData.put("user_role", Collections.singletonList("UNKNOWN"));
        }

        // 기본 보안 판별 값
        logData.put("security_events", Collections.emptyList());
        logData.put("threat_level", "LOW");
        logData.put("is_suspicious", false);
        logData.put("suspicious_patterns", Collections.emptyList());

        // 판별 기준 기본값 (상황별로 덮어써야 함)
        logData.put("access_result", "SUCCESS");
        logData.put("action_type", "READ");
        logData.put("response_status", 200);

        // 문서 관련 필드 기본값
        logData.put("document_id", null);
        logData.put("document_classification", null);
        logData.put("document_owner", null);

        return logData;
    }
}