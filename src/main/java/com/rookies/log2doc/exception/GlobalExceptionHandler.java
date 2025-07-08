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
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClient;
import jakarta.validation.ConstraintViolationException;


import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final RestClient restClient = RestClient.create();

    private void sendLogToFlask(HttpServletRequest request, String errorMessage, String accessResult, String actionType) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("timestamp", Instant.now().toString());

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

        log.info("📡 Flask로 보낼 로그 데이터: {}", logData);

        // ✅ RestClient 사용
        restClient.post()
                .uri("http://flask-server/logs")
                .body(logData)
                .retrieve()
                .body(String.class);
    }

    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<MessageResponse> handleTokenRefreshException(TokenRefreshException e, HttpServletRequest request) {
        log.error("토큰 갱신 오류: {}", e.getMessage());
        sendLogToFlask(request, e.getMessage(), "PERMISSION_DENIED", "TOKEN_REFRESH");
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new MessageResponse(e.getMessage(), false));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<MessageResponse> handleUsernameNotFoundException(UsernameNotFoundException e, HttpServletRequest request) {
        log.error("사용자 찾기 실패: {}", e.getMessage());
        sendLogToFlask(request, e.getMessage(), "PERMISSION_DENIED", "LOGIN");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new MessageResponse("사용자를 찾을 수 없습니다.", false));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<MessageResponse> handleBadCredentialsException(
            BadCredentialsException e, HttpServletRequest request) {

        log.error("인증 실패: {}", e.getMessage());
        sendLogToFlask(request, e.getMessage(), "PERMISSION_DENIED", "LOGIN");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("사용자명 또는 비밀번호가 올바르지 않습니다.", false));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MessageResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        String combinedErrors = errors.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", "));

        log.error("유효성 검증 실패: {}", combinedErrors);
        sendLogToFlask(request, combinedErrors, "VALIDATION_ERROR", "VALIDATE");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse(combinedErrors, false));
    }

    public ResponseEntity<MessageResponse> handlePermissionDenied(
            PermissionDeniedException ex, HttpServletRequest request) {

        log.warn("권한 거부: {}", ex.getMessage());
        sendLogToFlask(request, ex.getMessage(), "PERMISSION_DENIED", "ACCESS");

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new MessageResponse(ex.getMessage(), false));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<MessageResponse> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("런타임 오류: {}", e.getMessage());
        sendLogToFlask(request, e.getMessage(), "ERROR", "RUNTIME");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse("서버 내부 오류가 발생했습니다.", false));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResponse> handleGenericException(Exception e, HttpServletRequest request) {
        log.error("예상치 못한 오류: {}", e.getMessage());
        sendLogToFlask(request, e.getMessage(), "ERROR", "UNKNOWN");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse("예상치 못한 오류가 발생했습니다.", false));
    }

    // 인증되지 않음 (401)
    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthenticatedException(
            UnauthenticatedException ex, HttpServletRequest request) {

        log.warn("🔒 인증 오류: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("status", 401);
        response.put("error", "Unauthorized");
        response.put("message", ex.getMessage());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // 권한 부족 (403)
    @ExceptionHandler(AccessForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleAccessForbiddenException(
            AccessForbiddenException ex, HttpServletRequest request) {

        log.warn("🚫 접근 거부: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("status", 403);
        response.put("error", "Forbidden");
        response.put("message", ex.getMessage());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // ✔️ @RequestParam 누락
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<MessageResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        String errorMessage = String.format("필수 파라미터 '%s' 누락됨", ex.getParameterName());
        log.error("파라미터 누락: {}", errorMessage);

        sendLogToFlask(request, errorMessage, "VALIDATION_ERROR", "VALIDATE");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse(errorMessage, false));
    }

    // ✔️ JSON 바디 누락 or 파싱 오류
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<MessageResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.error("요청 바디 파싱 실패: {}", ex.getMessage());

        sendLogToFlask(request, "요청 본문을 읽을 수 없습니다.", "VALIDATION_ERROR", "VALIDATE");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse("요청 본문이 올바르지 않습니다.", false));
    }

    // ✔️ @RequestParam/@PathVariable 제약 조건 위반
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<MessageResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        String combinedErrors = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));

        log.error("유효성 검증 실패(ConstraintViolation): {}", combinedErrors);

        sendLogToFlask(request, combinedErrors, "VALIDATION_ERROR", "VALIDATE");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse(combinedErrors, false));
    }

}
