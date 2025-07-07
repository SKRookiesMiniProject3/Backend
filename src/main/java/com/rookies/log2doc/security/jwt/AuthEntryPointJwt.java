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

    private final LogBuilder logBuilder;   // ✅ 주입해서 재사용
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

        // ✅ Flask 전송용 로그 데이터
        Map<String, Object> logData = logBuilder.buildBaseLog(request, null);
        logData.put("error_message", authException.getMessage());
        logData.put("access_result", "PERMISSION_DENIED");
        logData.put("action_type", "AUTHENTICATION");
        logData.put("response_status", HttpServletResponse.SC_UNAUTHORIZED);

        logSender.sendLog(logData);

        // ✅ JSON 응답 반환
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), body);
    }
}
