package com.rookies.log2doc.log;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoggingInterceptor implements HandlerInterceptor {

    private final LogBuilder logBuilder;
    private final LogSender logSender;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {

        // ✅ 예외가 있으면 별도 처리 (GlobalExceptionHandler에서 처리함)
        if (ex != null) {
            log.debug("❌ Interceptor: 예외 발생 → GlobalExceptionHandler에서 처리");
            return;
        }

        // ✅ 인증 관련 엔드포인트는 제외
        String requestUrl = request.getRequestURI();
        if (shouldSkipLogging(requestUrl)) {
            log.debug("❌ Interceptor: 로깅 제외 URL → {}", requestUrl);
            return;
        }

        // ✅ 통합 로그 생성 및 전송
        try {
            Map<String, Object> logData = buildUnifiedLog(request, response);
            logSender.sendLog(logData);
            log.info("✅ 통합 로그 전송 완료: {} {}", request.getMethod(), requestUrl);

        } catch (Exception e) {
            log.error("🚨 통합 로그 처리 실패", e);
        }
    }

    /**
     * 로깅을 건너뛸 URL 패턴 정의
     */
    private boolean shouldSkipLogging(String requestUrl) {
        return requestUrl.startsWith("/api/v1/auth") ||     // 인증 API
                requestUrl.startsWith("/swagger-ui") ||       // Swagger UI
                requestUrl.startsWith("/v3/api-docs") ||      // API Docs
                requestUrl.startsWith("/actuator") ||         // Actuator
                requestUrl.startsWith("/test-");              // 테스트 API
    }

    /**
     * 통합 로그 데이터 생성
     */
    private Map<String, Object> buildUnifiedLog(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 기본 로그 데이터 생성
        Map<String, Object> logData = logBuilder.buildBaseLog(request, auth);

        // 응답 상태 설정
        logData.put("response_status", response.getStatus());
        logData.put("access_result", response.getStatus() < 400 ? "SUCCESS" : "FAILED");

        // URL별 액션 타입 결정
        String actionType = determineActionType(request);
        logData.put("action_type", actionType);

        // 문서 관련 정보 추출 (필요한 경우)
        extractDocumentInfo(request, logData);

        return logData;
    }

    /**
     * URL과 메서드에 따른 액션 타입 결정
     */
    private String determineActionType(HttpServletRequest request) {
        String method = request.getMethod();
        String url = request.getRequestURI();

        // 문서 관련
        if (url.startsWith("/documents")) {
            if ("POST".equals(method)) return "CREATE";
            if ("GET".equals(method) && url.contains("/download/")) return "DOWNLOAD";
            if ("GET".equals(method)) return "READ";
            if ("PUT".equals(method) || "PATCH".equals(method)) return "UPDATE";
            if ("DELETE".equals(method)) return "DELETE";
        }

        // 에러 리포트 관련
        if (url.startsWith("/errors")) {
            if ("POST".equals(method)) return "CREATE_ERROR_REPORT";
            if ("GET".equals(method)) return "READ_ERROR_REPORT";
            if ("PATCH".equals(method) && url.contains("/resolve")) return "RESOLVE_ERROR";
        }

        // CEO 사용자 관리
        if (url.startsWith("/api/v1/ceo/users")) {
            return "CEO_USER_MANAGEMENT";
        }

        return "GENERAL";
    }

    /**
     * 문서 관련 정보 추출
     */
    private void extractDocumentInfo(HttpServletRequest request, Map<String, Object> logData) {
        String url = request.getRequestURI();

        // URL에서 문서 ID 추출
        if (url.startsWith("/documents/") && !url.equals("/documents")) {
            String[] parts = url.split("/");
            if (parts.length >= 3) {
                try {
                    String docIdOrHash = parts[2];
                    if (docIdOrHash.matches("\\d+")) {
                        logData.put("document_id", Long.parseLong(docIdOrHash));
                    } else {
                        logData.put("document_hash", docIdOrHash);
                    }
                } catch (NumberFormatException e) {
                    // 해시값인 경우 또는 기타 경우
                    logData.put("document_hash", parts[2]);
                }
            }
        }

        // Request attributes에서 추가 정보 추출
        Object docId = request.getAttribute("document_id");
        if (docId != null) {
            logData.put("document_id", docId);
        }

        Object docOwner = request.getAttribute("document_owner");
        if (docOwner != null) {
            logData.put("document_owner", docOwner);
        }
    }
}