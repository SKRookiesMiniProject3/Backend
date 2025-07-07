package com.rookies.log2doc.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API 응답 표준 래퍼 클래스
 * 
 * 알고리즘 설명:
 * 1. 모든 API 응답에 대한 일관된 형태 제공
 * 2. 성공/실패 여부, 메시지, 데이터를 포함
 * 3. 제네릭을 사용하여 다양한 데이터 타입 지원
 * 
 * 시간 복잡도: O(1) - 단순 객체 생성
 * 공간 복잡도: O(1) - 고정된 필드 수
 * 
 * @param <T> 응답 데이터 타입
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "API 응답 표준 형태")
public class ApiResponse<T> {
    
    /**
     * 응답 성공 여부
     */
    @JsonProperty("success")
    @Schema(description = "응답 성공 여부", example = "true")
    private boolean success;
    
    /**
     * 응답 메시지
     */
    @JsonProperty("message")
    @Schema(description = "응답 메시지", example = "요청이 성공적으로 처리되었습니다.")
    private String message;
    
    /**
     * 응답 데이터
     */
    @JsonProperty("data")
    @Schema(description = "응답 데이터")
    private T data;
    
    /**
     * 오류 코드 (선택적)
     */
    @JsonProperty("errorCode")
    @Schema(description = "오류 코드", example = "USER_NOT_FOUND")
    private String errorCode;
    
    /**
     * 타임스탬프
     */
    @JsonProperty("timestamp")
    @Schema(description = "응답 생성 시간", example = "2024-01-01T12:00:00Z")
    @Builder.Default
    private String timestamp = java.time.Instant.now().toString();
    
    /**
     * 성공 응답 생성 메소드
     * 
     * @param <T> 데이터 타입
     * @param message 성공 메시지
     * @param data 응답 데이터
     * @return 성공 응답 객체
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .message(message)
            .data(data)
            .build();
    }
    
    /**
     * 실패 응답 생성 메소드
     * 
     * @param <T> 데이터 타입
     * @param message 실패 메시지
     * @param errorCode 오류 코드
     * @return 실패 응답 객체
     */
    public static <T> ApiResponse<T> failure(String message, String errorCode) {
        return ApiResponse.<T>builder()
            .success(false)
            .message(message)
            .errorCode(errorCode)
            .build();
    }
    
    /**
     * 단순 성공 응답 생성 메소드
     * 
     * @param message 성공 메시지
     * @return 성공 응답 객체
     */
    public static ApiResponse<Void> success(String message) {
        return success(message, null);
    }
}