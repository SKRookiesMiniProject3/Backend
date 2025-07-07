package com.rookies.log2doc.exception;

import com.rookies.log2doc.dto.response.MessageResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 전역 예외 처리 핸들러
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private void sendLogToFlask(HttpServletRequest request, String errorMessage, String accessResult, String actionType) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("timestamp", Instant.now().toString());

        // ✅ JWT로 인증된 사용자 정보 꺼내기
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
        String userRole = (auth != null && auth.isAuthenticated()) ? auth.getAuthorities().toString() : "UNKNOWN";

        logData.put("user_id", userId);
        logData.put("session_id", request.getSession().getId());
        logData.put("request_method", request.getMethod());
        logData.put("request_url", request.getRequestURI());
        logData.put("request_headers", Collections.list(request.getHeaderNames()).stream()
                .collect(Collectors.toMap(h -> h, request::getHeader)));
        logData.put("user_role", userRole);
        logData.put("document_id", null);
        logData.put("document_classification", null);
        logData.put("document_owner", null);
        logData.put("action_type", actionType);
        logData.put("access_result", accessResult);
        logData.put("error_message", errorMessage);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForEntity("http://flask-server/logs", logData, String.class);
    }

    /**
     * 토큰 갱신 예외 처리
     */
    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<MessageResponse> handleTokenRefreshException(TokenRefreshException e) {
        log.error("토큰 갱신 오류: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new MessageResponse(e.getMessage(), false));
    }

    /**
     * 사용자 찾기 실패 예외 처리
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<MessageResponse> handleUsernameNotFoundException(UsernameNotFoundException e) {
        log.error("사용자 찾기 실패: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new MessageResponse("사용자를 찾을 수 없습니다.", false));
    }

    /**
     * 인증 실패 예외 처리 (BadCredentialsException)
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<MessageResponse> handleBadCredentialsException(
            BadCredentialsException e, HttpServletRequest request) {

        log.error("인증 실패: {}", e.getMessage());

        // ✅ 공통 전송 메서드만 호출!
        sendLogToFlask(request, e.getMessage(), "PERMISSION_DENIED", "LOGIN");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("사용자명 또는 비밀번호가 올바르지 않습니다.", false));
    }

    /**
     * 유효성 검증 실패 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.error("유효성 검증 실패: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
    
    /**
     * 일반 런타임 예외 처리
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<MessageResponse> handleRuntimeException(RuntimeException e) {
        log.error("런타임 오류: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse("서버 내부 오류가 발생했습니다.", false));
    }
    
    /**
     * 일반 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResponse> handleGenericException(Exception e) {
        log.error("예상치 못한 오류: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse("예상치 못한 오류가 발생했습니다.", false));
    }

    /**
     * PermissionDeniedException 처리 핸들러 추가
     */
    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<String> handlePermissionDenied(PermissionDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

}