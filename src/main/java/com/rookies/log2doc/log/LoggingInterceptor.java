package com.rookies.log2doc.log;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoggingInterceptor implements HandlerInterceptor {

    private final RestClient restClient = RestClient.create();

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // ✅ 예외가 있으면 Interceptor에서는 보내지 마라!
        if (ex != null) {
            log.debug("❌ Interceptor: 예외 발생 → 성공 로그 전송 스킵");
            return;
        }

        // ✅ 성공 로그만 Flask로 전송
        Map<String, Object> logData = new HashMap<>();
        logData.put("timestamp", Instant.now().toString());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
        String userRole = (auth != null && auth.isAuthenticated()) ? auth.getAuthorities().toString() : "UNKNOWN";

        logData.put("user_id", userId);
        logData.put("session_id", request.getSession(false) != null ? request.getSession().getId() : null);
        logData.put("request_method", request.getMethod());
        logData.put("request_url", request.getRequestURI());
        logData.put("request_headers", Collections.list(request.getHeaderNames()).stream()
                .collect(Collectors.toMap(h -> h, request::getHeader)));
        logData.put("user_role", userRole);
        logData.put("document_id", null);
        logData.put("document_classification", null);
        logData.put("document_owner", null);
        logData.put("access_result", "SUCCESS");
        logData.put("action_type", "GENERAL");
        logData.put("error_message", null);

        log.info("📡 Flask로 보낼 성공 요청-응답 로그: {}", logData);

        try {
            restClient.post()
                    .uri("http://flask-server/logs")
                    .body(logData)
                    .retrieve()
                    .body(String.class);
            log.info("✅ Flask 성공 로그 전송 완료");
        } catch (Exception e) {
            log.error("🚨 Flask 성공 로그 전송 실패: {}", e.getMessage());
        }
    }

}
