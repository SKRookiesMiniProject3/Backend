package com.rookies.log2doc.log;

import com.rookies.log2doc.log.LogSender;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoggingInterceptor implements HandlerInterceptor {

    private final LogBuilder logBuilder;
    private final LogSender logSender;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {

        // 예외가 있으면 GlobalExceptionHandler에서 처리하므로 스킵
        if (ex != null) {
            log.debug("Interceptor: 예외 발생 → GlobalExceptionHandler에서 처리");
            return;
        }

        // 응답 상태가 4xx, 5xx면 예외 처리로 간주하고 스킵
        if (response.getStatus() >= 400) {
            log.debug("Interceptor: 에러 응답 ({}초) → GlobalExceptionHandler에서 처리", response.getStatus());
            return;
        }

        // 인증 관련 엔드포인트는 제외
        String requestUrl = request.getRequestURI();
        if (shouldSkipLogging(requestUrl)) {
            log.debug("Interceptor: 로깅 제외 URL → {}", requestUrl);
            return;
        }

        // 성공 케이스만 통합 로그 생성 및 전송
        try {
            Map<String, Object> logData = buildUnifiedLog(request, response);
            logSender.sendLog(logData);

            // 전체 URL로 로그 출력
            String fullUrl = (String) logData.get("request_url");
            log.info("통합 로그 전송 완료: {} {} ({})",
                    request.getMethod(), fullUrl, response.getStatus());

        } catch (Exception e) {
            log.error("통합 로그 처리 실패: {}", e.getMessage());
            log.debug("통합 로그 처리 실패 상세:", e);
        }
    }

    /**
     * 로깅을 건너뛸 URL 패턴 정의
     */
    private boolean shouldSkipLogging(String requestUrl) {
        return requestUrl.startsWith("/api/v1/auth") ||     // 인증 API
                requestUrl.startsWith("/api/v1/error-reports") ||  // 에러 리포트 API 추가
                requestUrl.startsWith("/swagger-ui") ||       // Swagger UI
                requestUrl.startsWith("/v3/api-docs") ||      // API Docs
                requestUrl.startsWith("/actuator") ||         // Actuator
                requestUrl.startsWith("/test-") ||            // 테스트 API
                requestUrl.startsWith("/h2-console");         // H2 Console
    }

    /**
     * 통합 로그 데이터 생성 (전체 URL 포함)
     */
    private Map<String, Object> buildUnifiedLog(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 기본 로그 데이터 생성
        Map<String, Object> logData = new HashMap<>();

        // 기본 정보
        logData.put("timestamp", Instant.now().toString());
        logData.put("request_method", request.getMethod());

        // 전체 URL 생성 (경로 + 쿼리스트링) - 여기가 핵심!
        String fullUrl = buildFullUrl(request);
        logData.put("request_url", fullUrl);  // 전체 URL 사용

        // 세션 정보 안전하게 처리
        String sessionId = getSessionIdSafely(request);
        logData.put("session_id", sessionId);

        // 헤더 정보
        Map<String, String> headersMap = new HashMap<>();
        try {
            Collections.list(request.getHeaderNames()).forEach(headerName -> {
                String headerValue = request.getHeader(headerName);
                headersMap.put(headerName, headerValue);
            });
//            log.debug("헤더 맵 생성 완료: {}", headersMap.keySet());
        } catch (Exception e) {
//            log.error("헤더 정보 수집 실패: {}", e.getMessage());
            headersMap.put("User-Agent", "Unknown");
        }

        logData.put("request_headers", headersMap);
//        log.debug("request_headers 설정 완료: {}", logData.get("request_headers"));

        // 사용자 정보
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

        // 응답 상태 설정
        logData.put("response_status", response.getStatus());
        logData.put("access_result", response.getStatus() < 400 ? "SUCCESS" : "FAILED");

        // URL별 액션 타입 결정
        String actionType = determineActionType(request);
        logData.put("action_type", actionType);

        // 기본 보안 필드
        logData.put("security_events", Collections.emptyList());
        logData.put("threat_level", "LOW");
        logData.put("is_suspicious", false);
        logData.put("suspicious_patterns", Collections.emptyList());

        // 문서/에러 리포트 관련 정보 추출
        extractAttributeInfo(request, logData);

        // 최종 로그 데이터 확인
//        log.debug("최종 로그 데이터 - 전체 URL: {}", fullUrl);
//        log.debug("최종 로그 데이터 키들: {}", logData.keySet());

        return logData;
    }

    /**
     * 전체 URL 생성 (경로 + 쿼리스트링)
     */
    private String buildFullUrl(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String queryString = request.getQueryString();

        // 쿼리스트링이 있으면 결합, 없으면 경로만
        if (queryString != null && !queryString.trim().isEmpty()) {
            String fullUrl = requestURI + "?" + queryString;
//            log.debug("전체 URL 생성: {}", fullUrl);
            return fullUrl;
        } else {
//            log.debug("경로만 URL: {}", requestURI);
            return requestURI;
        }
    }

    /**
     * 세션 ID를 안전하게 가져오는 메서드
     */
    private String getSessionIdSafely(HttpServletRequest request) {
        try {
            // 기존 세션만 가져오기 (새로 생성하지 않음)
            HttpSession existingSession = request.getSession(false);
            if (existingSession != null) {
                return existingSession.getId();
            }

            // 세션이 없으면 요청 ID나 다른 식별자 사용
            String requestId = request.getHeader("X-Request-ID");
            if (requestId != null) {
                return "req_" + requestId;
            }

            // 마지막 수단: 현재 시간 기반 ID 생성
            return "temp_" + System.currentTimeMillis();

        } catch (IllegalStateException e) {
            // 세션 생성 불가 시 임시 ID 사용
            log.debug("세션 접근 불가, 임시 ID 사용: {}", e.getMessage());
            return "no_session_" + System.currentTimeMillis();
        }
    }

    /**
     * URL과 메서드에 따른 액션 타입 결정
     */
    private String determineActionType(HttpServletRequest request) {
        String method = request.getMethod();
        String url = request.getRequestURI();

        // 문서 관련
        if (url.startsWith("/documents")) {
            if ("POST".equals(method) && url.contains("/upload")) return "CREATE";
            if ("GET".equals(method) && url.contains("/download/")) return "DOWNLOAD";
            if ("GET".equals(method) && url.matches(".*/\\d+$")) return "READ";
            if ("GET".equals(method) && url.contains("/hash/")) return "READ";
            if ("GET".equals(method) && url.contains("/status")) return "STATUS_CHECK";
            if ("GET".equals(method)) return "LIST";
            if ("PUT".equals(method) || "PATCH".equals(method)) return "UPDATE";
            if ("DELETE".equals(method)) return "DELETE";
        }

        // 에러 리포트 관련
        if (url.startsWith("/errors")) {
            if ("POST".equals(method)) return "CREATE_ERROR_REPORT";
            if ("GET".equals(method) && url.contains("/daily-count")) return "DAILY_COUNT";
            if ("GET".equals(method) && url.contains("/latest")) return "LATEST_LIST";
            if ("GET".equals(method) && url.contains("/unresolved")) return "UNRESOLVED_LIST";
            if ("GET".equals(method) && url.matches(".*/\\d+$")) return "READ_ERROR_REPORT";
            if ("PATCH".equals(method) && url.contains("/resolve")) return "RESOLVE_ERROR";
        }

        // CEO 사용자 관리
        if (url.startsWith("/api/v1/ceo/users")) {
            return "CEO_USER_MANAGEMENT";
        }

        return "GENERAL";
    }

    /**
     * Request Attribute에서 정보 추출
     */
    private void extractAttributeInfo(HttpServletRequest request, Map<String, Object> logData) {
        // 문서 관련 정보
        Object docId = request.getAttribute("document_id");
        if (docId != null) {
            logData.put("document_id", docId);
        }

        Object docOwner = request.getAttribute("document_owner");
        if (docOwner != null) {
            logData.put("document_owner", docOwner);
        }

        Object docHash = request.getAttribute("document_hash");
        if (docHash != null) {
            logData.put("document_hash", docHash);
        }

        // 에러 리포트 관련 정보
        Object errorReportId = request.getAttribute("error_report_id");
        if (errorReportId != null) {
            logData.put("error_report_id", errorReportId);
        }

        Object errorSeverity = request.getAttribute("error_severity");
        if (errorSeverity != null) {
            logData.put("error_severity", errorSeverity);
        }

        Object errorAction = request.getAttribute("error_report_action");
        if (errorAction != null) {
            logData.put("error_report_action", errorAction);
        }

        Object errorMessage = request.getAttribute("error_message");
        if (errorMessage != null) {
            logData.put("error_message", errorMessage);
        }

        Object errorCode = request.getAttribute("error_code");
        if (errorCode != null) {
            logData.put("error_code", errorCode);
        }

        Object resultCount = request.getAttribute("result_count");
        if (resultCount != null) {
            logData.put("result_count", resultCount);
        }

        // 기본 필드 설정 (누락 방지)
        logData.putIfAbsent("document_classification", null);
        logData.putIfAbsent("document_id", null);
        logData.putIfAbsent("document_owner", null);
    }
}