package com.rookies.log2doc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 공통 메시지 응답 DTO.
 * 단순 성공/실패 여부와 메시지를 반환할 때 사용됨.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    /** 응답 메시지 */
    private String message;

    /** 성공 여부 */
    private boolean success;

    /**
     * 메시지만 전달할 때 기본적으로 성공(true)로 처리하는 생성자.
     *
     * @param message 응답 메시지
     */
    public MessageResponse(String message) {
        this.message = message;
        this.success = true;
    }
}
