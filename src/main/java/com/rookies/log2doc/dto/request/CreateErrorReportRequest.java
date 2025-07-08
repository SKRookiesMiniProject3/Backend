package com.rookies.log2doc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 에러 리포트 생성 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateErrorReportRequest {
    
    @NotBlank(message = "에러 메시지는 필수입니다.")
    @Size(max = 2000, message = "에러 메시지는 2000자를 초과할 수 없습니다.")
    private String message;
    
    @Size(max = 100, message = "에러 코드는 100자를 초과할 수 없습니다.")
    private String errorCode;
    
    private Boolean resolved = false;
    
    @Size(max = 500, message = "상세 설명은 500자를 초과할 수 없습니다.")
    private String description;
    
    @Size(max = 50, message = "심각도는 50자를 초과할 수 없습니다.")
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    
    @Size(max = 100, message = "발생 위치는 100자를 초과할 수 없습니다.")
    private String location; // 에러 발생 위치
}
