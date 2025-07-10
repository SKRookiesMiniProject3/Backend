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

        // ✅ JSON 응답 데이터
        final Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", "인증되지 않은 요청입니다.");
        body.put("path", request.getServletPath());

        // ✅ Flask 전송용 로그 데이터 (전체 URL 포함)
        Map<String, Object> logData = logBuilder.buildBaseLog(request, null);

        // ✅ 전체 URL 생성 및 덮어쓰기
        String fullUrl = buildFullUrl(request);
        logData.put("request_url", fullUrl);  // 기존 URL 덮어쓰기

        logData.put("error_message", authException.getMessage());
        logData.put("access_result", "PERMISSION_DENIED");
        logData.put("action_type", "AUTHENTICATION");
        logData.put("response_status", HttpServletResponse.SC_UNAUTHORIZED);

        logSender.sendLog(logData);

        log.info("📡 인증 실패 로그 전송 완료: {} {} (401)",
                request.getMethod(), fullUrl);

        // ✅ JSON 응답 반환
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

        // ✅ 쿼리스트링이 있으면 결합, 없으면 경로만
        if (queryString != null && !queryString.trim().isEmpty()) {
            String fullUrl = requestURI + "?" + queryString;
            log.debug("🔗 인증 실패 - 전체 URL 생성: {}", fullUrl);
            return fullUrl;
        } else {
            log.debug("🔗 인증 실패 - 경로만 URL: {}", requestURI);
            return requestURI;
        }
    }
}