// src/main/java/com/rookies/log2doc/log/LogBuilder.java
package com.rookies.log2doc.log;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LogBuilder {

    public Map<String, Object> buildBaseLog(HttpServletRequest request, Authentication auth) {
        Map<String, Object> logData = new HashMap<>();

        logData.put("timestamp", Instant.now().toString());
        logData.put("session_id", request.getSession().getId());
        logData.put("request_method", request.getMethod());
        logData.put("request_url", request.getRequestURI());

        Map<String, String> headersMap = Collections.list(request.getHeaderNames()).stream()
                .collect(HashMap::new, (m, k) -> m.put(k, request.getHeader(k)), HashMap::putAll);
        logData.put("request_headers", headersMap);

        if (auth != null && auth.isAuthenticated()) {
            logData.put("user_id", auth.getName());
            logData.put("user_role", auth.getAuthorities().toString());
        } else {
            logData.put("user_id", "anonymous");
            logData.put("user_role", "UNKNOWN");
        }

        // 기본값
        logData.put("security_events", Collections.emptyList());
        logData.put("threat_level", "LOW");
        logData.put("is_suspicious", false);
        logData.put("suspicious_patterns", Collections.emptyList());

        return logData;
    }
}
