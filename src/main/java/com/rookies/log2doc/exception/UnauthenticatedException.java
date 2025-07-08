package com.rookies.log2doc.exception;

/**
 * 인증 안 됐을 때 401 반환용
 */
public class UnauthenticatedException extends RuntimeException {

    public UnauthenticatedException(String message) {
        super(message);
    }

    public UnauthenticatedException(String message, Throwable cause) {
        super(message, cause);
    }
}
