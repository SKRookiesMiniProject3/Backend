package com.rookies.log2doc.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ErrorReportDTO {
    private Long id;
    private Long reportFileId;
    private Long errorSourceMember;
    private String reportStatus;
    private String reportStatusDescription;
    private String reportComment;
    private Boolean isDeleted;
    private LocalDateTime createdDt;
    private LocalDateTime deletedDt;

    // 추가 정보 (조인 등으로 가져올 수 있는 정보)
    private String errorSourceMemberName; // 에러 원인 사용자 이름
    private String reportFileName; // 리포트 파일명
}