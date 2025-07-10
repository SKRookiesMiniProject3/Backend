package com.rookies.log2doc.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        // ✅ 전체 URL 생성
        String fullUrl = buildFullUrl(request);

        log.warn("🔥 권한 부족: {} - URL: {}", accessDeniedException.getMessage(), fullUrl);

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("status", HttpServletResponse.SC_FORBIDDEN);
        responseBody.put("error", "Forbidden");
        responseBody.put("message", "권한이 부족합니다.");
        responseBody.put("path", fullUrl);  // ✅ 전체 URL 사용

        new ObjectMapper().writeValue(response.getOutputStream(), responseBody);
    }

    /**
     * 전체 URL 생성 (경로 + 쿼리스트링)
     */
    private String buildFullUrl(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String queryString = request.getQueryString();

        if (queryString != null && !queryString.trim().isEmpty()) {
            return requestURI + "?" + queryString;
        } else {
            return requestURI;
        }
    }
}