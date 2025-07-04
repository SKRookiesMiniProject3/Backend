package com.rookies.log2doc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 메시지 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private String message;
    private boolean success;
    
    public MessageResponse(String message) {
        this.message = message;
        this.success = true;
    }
}