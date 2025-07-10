package com.rookies.log2doc.exception;

import com.rookies.log2doc.dto.response.MessageResponse;
import com.rookies.log2doc.log.LogBuilder;
import com.rookies.log2doc.log.LogSender;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final LogBuilder logBuilder;
    private final LogSender logSender;

    public GlobalExceptionHandler(LogBuilder logBuilder, LogSender logSender) {
        this.logBuilder = logBuilder;
        this.logSender = logSender;
    }

    // 1순위: 권한 부족 예외 (403) - 가장 구체적인 예외부터 처리
    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<MessageResponse> handlePermissionDenied(
            PermissionDeniedException ex, HttpServletRequest request) {

        log.warn("권한 거부: {}", ex.getMessage());

        // 권한 거부 전용 로그 전송
        sendExceptionLog(request, ex.getMessage(), "PERMISSION_DENIED", "ACCESS_DENIED", 403);

        return ResponseEntity.status(HttpStatus.FORBIDDEN)  // 403 반환
                .body(new MessageResponse(ex.getMessage(), false));
    }

    // 2순위: 인증되지 않음 예외 (401)
    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthenticatedException(
            UnauthenticatedException ex, HttpServletRequest request) {

        log.warn("인증 오류: {}", ex.getMessage());

        sendExceptionLog(request, ex.getMessage(), "PERMISSION_DENIED", "AUTHENTICATION", 401);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 401);
        response.put("error", "Unauthorized");
        response.put("message", ex.getMessage());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // 3순위: 접근 거부 예외 (403)
    @ExceptionHandler(AccessForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleAccessForbiddenException(
            AccessForbiddenException ex, HttpServletRequest request) {

        log.warn("접근 거부: {}", ex.getMessage());

        sendExceptionLog(request, ex.getMessage(), "PERMISSION_DENIED", "ACCESS_FORBIDDEN", 403);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 403);
        response.put("error", "Forbidden");
        response.put("message", ex.getMessage());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // 4순위: 토큰 갱신 예외 (403)
    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<MessageResponse> handleTokenRefreshException(
            TokenRefreshException e, HttpServletRequest request) {

        log.error("토큰 갱신 오류: {}", e.getMessage());
        sendExceptionLog(request, e.getMessage(), "PERMISSION_DENIED", "TOKEN_REFRESH", 403);

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new MessageResponse(e.getMessage(), false));
    }

    // 5순위: 사용자 없음 예외 (404)
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<MessageResponse> handleUsernameNotFoundException(
            UsernameNotFoundException e, HttpServletRequest request) {

        log.error("사용자 찾기 실패: {}", e.getMessage());
        sendExceptionLog(request, e.getMessage(), "PERMISSION_DENIED", "LOGIN", 404);

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new MessageResponse("사용자를 찾을 수 없습니다.", false));
    }

    // 6순위: 인증 실패 예외 (401)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<MessageResponse> handleBadCredentialsException(
            BadCredentialsException e, HttpServletRequest request) {

        log.error("인증 실패: {}", e.getMessage());
        sendExceptionLog(request, e.getMessage(), "PERMISSION_DENIED", "LOGIN", 401);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("사용자명 또는 비밀번호가 올바르지 않습니다.", false));
    }

    // 7순위: 유효성 검증 실패 (400)
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
        sendExceptionLog(request, combinedErrors, "VALIDATION_ERROR", "VALIDATE", 400);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse(combinedErrors, false));
    }

    // 8순위: 파라미터 누락 (400)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<MessageResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        String errorMessage = String.format("필수 파라미터 '%s' 누락됨", ex.getParameterName());
        log.error("파라미터 누락: {}", errorMessage);

        sendExceptionLog(request, errorMessage, "VALIDATION_ERROR", "VALIDATE", 400);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse(errorMessage, false));
    }

    // 9순위: JSON 파싱 오류 (400)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<MessageResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.error("요청 바디 파싱 실패: {}", ex.getMessage());

        sendExceptionLog(request, "요청 본문을 읽을 수 없습니다.", "VALIDATION_ERROR", "VALIDATE", 400);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse("요청 본문이 올바르지 않습니다.", false));
    }

    // 10순위: 제약 조건 위반 (400)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<MessageResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        String combinedErrors = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));

        log.error("유효성 검증 실패(ConstraintViolation): {}", combinedErrors);

        sendExceptionLog(request, combinedErrors, "VALIDATION_ERROR", "VALIDATE", 400);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse(combinedErrors, false));
    }

    // 마지막 순위: 런타임 예외 (500) - 이제 PermissionDeniedException 제외됨
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<MessageResponse> handleRuntimeException(
            RuntimeException e, HttpServletRequest request) {

        // 이미 위에서 처리된 예외들은 제외
        if (e instanceof PermissionDeniedException ||
                e instanceof AccessForbiddenException ||
                e instanceof UnauthenticatedException) {
            // 이미 위에서 처리됨 - 이 코드에 도달하면 안 됨
            log.warn("런타임 핸들러에서 권한 예외 감지: {}", e.getClass().getSimpleName());
            return handlePermissionDenied((PermissionDeniedException) e, request);
        }

        log.error("런타임 오류: {}", e.getMessage());
        sendExceptionLog(request, e.getMessage(), "ERROR", "RUNTIME", 500);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse("서버 내부 오류가 발생했습니다.", false));
    }

    // 최후 순위: 일반 예외 (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResponse> handleGenericException(
            Exception e, HttpServletRequest request) {

        log.error("예상치 못한 오류: {}", e.getMessage());
        sendExceptionLog(request, e.getMessage(), "ERROR", "UNKNOWN", 500);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse("예상치 못한 오류가 발생했습니다.", false));
    }

    // 통합 예외 로그 전송 메서드
    private void sendExceptionLog(HttpServletRequest request, String errorMessage,
                                  String accessResult, String actionType, int statusCode) {
        try {
            Map<String, Object> logData = logBuilder.buildBaseLog(request,
                    SecurityContextHolder.getContext().getAuthentication());

            // 전체 URL 생성 (경로 + 쿼리스트링)
            String fullUrl = buildFullUrlForException(request);
            logData.put("request_url", fullUrl);  // 기존 request_url 덮어쓰기

            logData.put("access_result", accessResult);
            logData.put("error_message", errorMessage);
            logData.put("action_type", actionType);
            logData.put("response_status", statusCode);

            // 문서 관련 정보 추출 (URL에서)
            extractDocumentInfoFromUrl(request, logData);

            logSender.sendLog(logData);
            log.info("예외 로그 전송 완료: {} {} ({})",
                    request.getMethod(), fullUrl, statusCode);

        } catch (Exception ex) {
            log.error("예외 로그 전송 실패: {}", ex.getMessage());
        }
    }

    // URL에서 문서 정보 추출
    private void extractDocumentInfoFromUrl(HttpServletRequest request, Map<String, Object> logData) {
        String url = request.getRequestURI();

        // /documents/11 같은 패턴에서 문서 ID 추출
        if (url.startsWith("/documents/") && !url.equals("/documents")) {
            String[] parts = url.split("/");
            if (parts.length >= 3) {
                try {
                    String docIdOrHash = parts[2];
                    if (docIdOrHash.matches("\\d+")) {
                        logData.put("document_id", Long.parseLong(docIdOrHash));
                    } else if (!docIdOrHash.equals("upload") && !docIdOrHash.equals("hash")) {
                        logData.put("document_hash", docIdOrHash);
                    }
                } catch (NumberFormatException e) {
                    logData.put("document_hash", parts[2]);
                }
            }
        }

        // Request attributes에서 추가 정보 (있다면)
        Object docId = request.getAttribute("document_id");
        if (docId != null) {
            logData.put("document_id", docId);
        }
    }

    /**
     * 예외 처리용 전체 URL 생성 (경로 + 쿼리스트링)
     */
    private String buildFullUrlForException(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String queryString = request.getQueryString();

        // 쿼리스트링이 있으면 결합, 없으면 경로만
        if (queryString != null && !queryString.trim().isEmpty()) {
            String fullUrl = requestURI + "?" + queryString;
//            log.debug("예외 처리 - 전체 URL 생성: {}", fullUrl);
            return fullUrl;
        } else {
//            log.debug("예외 처리 - 경로만 URL: {}", requestURI);
            return requestURI;
        }
    }
}