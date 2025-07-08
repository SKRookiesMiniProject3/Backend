package com.rookies.log2doc.exception;

import org.springframework.security.access.AccessDeniedException;

/**
 * 사용자가 필요한 권한(Role)이 부족할 때 발생하는 예외
 * ex) 문서 조회 시 권한 미달
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
