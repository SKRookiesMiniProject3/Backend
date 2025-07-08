package com.rookies.log2doc.exception;

import org.springframework.security.access.AccessDeniedException;

/**
 * 사용자가 필요한 권한(Role)이 부족할 때 발생하는 예외
 * Spring Security의 AccessDeniedException을 상속받아
 * 403 Forbidden 응답을 자동으로 처리
 */
public class PermissionDeniedException extends AccessDeniedException {

    private static final long serialVersionUID = 1L;

    public PermissionDeniedException(String message) {
        super(message);
    }

    public PermissionDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}