package com.rookies.log2doc.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * ErrorReportDTO - 새 Entity 구조에 맞춤
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ErrorReportDTO {

    // 기본 정보
    private Long id;

    private String reportTitle;              // 리포트 제목 (Flask What)
    private String reportPreview;            // 리포트 간략 설명 (Flask Why)
    private String reportCategory;           // 카테고리 (ATTACK, VALID, INVALID)
    private String reportCategoryDescription; // 카테고리 설명
    private String reportPath;               // 리포트 파일 실제 경로
    private String reportStatus;             // 리포트 상태 (NOT_STARTED, IN_PROGRESS, COMPLETED)
    private String reportStatusDescription;  // 상태 설명
    private String reportComment;            // 리포트 코멘트
    private Boolean isDeleted;               // 삭제 여부
    private LocalDateTime createdDt;         // 생성일
    private LocalDateTime deletedDt;         // 삭제일
}