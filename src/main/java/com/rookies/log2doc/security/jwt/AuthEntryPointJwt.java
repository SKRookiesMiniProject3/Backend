package com.rookies.log2doc.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rookies.log2doc.log.LogBuilder;
import com.rookies.log2doc.log.LogSender;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    private final LogBuilder logBuilder;
    private final LogSender logSender;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.error("인증되지 않은 요청: {}", authException.getMessage());

        // 에러 리포트 API는 Flask 전송 제외
        String requestUri = request.getRequestURI();
        if (shouldSkipFlaskLogging(requestUri)) {
            log.debug("Flask 전송 제외 URL: {}", requestUri);
            sendJsonResponse(request, response, authException);
            return;
        }

        // 나머지 URL은 Flask로 전송 + JSON 응답
        sendToFlaskAndRespond(request, response, authException);
    }

    /**
     * Flask 로깅을 건너뛸 URL 패턴 정의
     */
    private boolean shouldSkipFlaskLogging(String requestUri) {
        return requestUri.startsWith("/api/v1/error-reports") ||  // 에러 리포트 API
                requestUri.startsWith("/api/v1/auth") ||           // 인증 API
                requestUri.startsWith("/swagger-ui") ||            // Swagger UI
                requestUri.startsWith("/v3/api-docs") ||           // API Docs
                requestUri.startsWith("/actuator") ||              // Actuator
                requestUri.startsWith("/favicon.ico") ||           // 파비콘
                requestUri.startsWith("/static/");                 // 정적 리소스
    }

    /**
     * JSON 응답만 전송 (Flask 전송 없음)
     */
    private void sendJsonResponse(HttpServletRequest request, HttpServletResponse response,
                                  AuthenticationException authException) throws IOException {
        final Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", "인증되지 않은 요청입니다.");
        body.put("path", request.getServletPath());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), body);

        log.info("인증 실패 응답 완료 (Flask 전송 제외): {} {} (401)",
                request.getMethod(), request.getRequestURI());
    }

    /**
     * Flask로 로그 전송 + JSON 응답
     */
    private void sendToFlaskAndRespond(HttpServletRequest request, HttpServletResponse response,
                                       AuthenticationException authException) throws IOException {
        // Flask 전송용 로그 데이터 (전체 URL 포함)
        Map<String, Object> logData = logBuilder.buildBaseLog(request, null);

        // 전체 URL 생성 및 덮어쓰기
        String fullUrl = buildFullUrl(request);
        logData.put("request_url", fullUrl);  // 기존 URL 덮어쓰기

        logData.put("error_message", authException.getMessage());
        logData.put("access_result", "PERMISSION_DENIED");
        logData.put("action_type", "AUTHENTICATION");
        logData.put("response_status", HttpServletResponse.SC_UNAUTHORIZED);

        logSender.sendLog(logData);

        log.info("인증 실패 로그 전송 완료: {} {} (401)",
                request.getMethod(), fullUrl);

        // JSON 응답 반환
        final Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", "인증되지 않은 요청입니다.");
        body.put("path", request.getServletPath());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), body);
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
            log.debug("인증 실패 - 전체 URL 생성: {}", fullUrl);
            return fullUrl;
        } else {
            log.debug("인증 실패 - 경로만 URL: {}", requestURI);
            return requestURI;
        }
    }
}