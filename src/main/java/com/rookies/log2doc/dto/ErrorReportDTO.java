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
    private String message;
    private String errorCode;
    private Boolean resolved;
    private LocalDateTime createdAt;
}
