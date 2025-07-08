package com.rookies.log2doc.dto.request;

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

    private Long reportFileId; // 리포트 파일 ID (선택사항)

    private Long errorSourceMember; // 에러 원인 사용자 ID (선택사항)

    @Size(max = 1000, message = "리포트 코멘트는 1000자를 초과할 수 없습니다.")
    private String reportComment; // 리포트 코멘트

    private String reportStatus = "NOT_STARTED"; // 기본값: 시작 안함
}