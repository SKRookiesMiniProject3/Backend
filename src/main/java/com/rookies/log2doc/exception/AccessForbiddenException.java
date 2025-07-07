package com.rookies.log2doc.exception;

/**
 * 인증은 됐는데 권한이 없을 때 403 반환용
 */
public class AccessForbiddenException extends RuntimeException {

    public AccessForbiddenException(String message) {
        super(message);
    }

    public AccessForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
