package com.rookies.log2doc.exception;

/**
 * 애플리케이션 전역에서 사용하는 사용자 정의 예외 클래스.
 * 에러 코드와 에러 메시지를 함께 포함해 상세한 예외 정보 제공.
 */
public class CustomException extends RuntimeException {

    // 에러 코드 (예: USER_NOT_FOUND, INVALID_REQUEST 등)
    private final String errorCode;

    // 상세 에러 메시지
    private final String errorMessage;

    /**
     * 에러 코드와 메시지를 받아 예외 객체 생성.
     * @param errorCode 에러 코드 식별자
     * @param errorMessage 상세 에러 메시지
     */
    public CustomException(String errorCode, String errorMessage) {
        super(errorMessage); // 부모 클래스 RuntimeException에 메시지 전달
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    /**
     * 에러 코드 반환.
     * @return 에러 코드
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 에러 메시지 반환.
     * @return 에러 메시지
     */
    public String getErrorMessage() {
        return errorMessage;
    }
}
