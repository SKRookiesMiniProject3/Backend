package com.rookies.log2doc.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "error_report")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, columnDefinition = "BIGINT COMMENT '리포트 ID'")
    private Long id;

    // ========================================
    // 새로 추가된 컬럼들
    // ========================================

    @Column(name = "report_title", columnDefinition = "VARCHAR(255) COMMENT '리포트 제목 (Flask What)'")
    private String reportTitle;

    @Lob
    @Column(name = "report_preview", columnDefinition = "TEXT COMMENT '리포트 간략 설명 (Flask Why)'")
    private String reportPreview;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_category", columnDefinition = "VARCHAR(255) COMMENT '카테고리 (attack, valid, invalid)'")
    @Builder.Default
    private ReportCategory reportCategory = ReportCategory.VALID;

    @Column(name = "report_path", nullable = false, columnDefinition = "VARCHAR(255) NOT NULL COMMENT '리포트 파일 실제 경로'")
    private String reportPath;

    // ========================================
    // 기존 컬럼들 (수정됨)
    // ========================================

    @Enumerated(EnumType.STRING)
    @Column(name = "report_status", nullable = false, columnDefinition = "VARCHAR(255) NOT NULL DEFAULT 'NOT_STARTED' COMMENT '리포트 진행상황'")
    @Builder.Default
    private ReportStatus reportStatus = ReportStatus.NOT_STARTED;

    @Lob
    @Column(name = "report_comment", columnDefinition = "TEXT COMMENT '리포트 작업에 맞는 코멘트'")
    private String reportComment;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE COMMENT '삭제 여부'")
    @Builder.Default
    private Boolean isDeleted = false;

    @CreatedDate
    @Column(name = "created_dt", nullable = false, updatable = false,
            columnDefinition = "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일 (레코드 생성 시 자동 설정)'")
    private LocalDateTime createdDt;

    @Column(name = "deleted_dt", columnDefinition = "DATETIME NULL COMMENT '삭제일 (삭제 시 설정, null: 미삭제)'")
    private LocalDateTime deletedDt;

    // ========================================
    // Enum 정의 (필수!)
    // ========================================

    /**
     * 보고서 카테고리 Enum (attack, valid, invalid)
     */
    public enum ReportCategory {
        ATTACK("공격", "보안 위협 또는 공격 시도"),
        VALID("정상", "정상적인 시스템 동작"),
        INVALID("비정상", "시스템 오류 또는 예외 상황");

        private final String description;
        private final String detail;

        ReportCategory(String description, String detail) {
            this.description = description;
            this.detail = detail;
        }

        public String getDescription() {
            return description;
        }

        public String getDetail() {
            return detail;
        }
    }

    /**
     * 리포트 상태 Enum (3가지만)
     */
    public enum ReportStatus {
        NOT_STARTED("시작 안함"),
        IN_PROGRESS("진행중"),
        COMPLETED("완료");

        private final String description;

        ReportStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // ========================================
    // 비즈니스 메서드들
    // ========================================

    @PrePersist
    public void prePersist() {
        if (this.createdDt == null) {
            this.createdDt = LocalDateTime.now();
        }
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
        if (this.reportStatus == null) {
            this.reportStatus = ReportStatus.NOT_STARTED;
        }
        if (this.reportCategory == null) {
            this.reportCategory = ReportCategory.VALID;
        }
    }

    /**
     * 공격으로 분류되었는지 확인
     */
    public boolean isAttackCategory() {
        return this.reportCategory == ReportCategory.ATTACK;
    }

    /**
     * 정상으로 분류되었는지 확인
     */
    public boolean isValidCategory() {
        return this.reportCategory == ReportCategory.VALID;
    }

    /**
     * 완료되었는지 확인
     */
    public boolean isCompleted() {
        return this.reportStatus == ReportStatus.COMPLETED;
    }

    /**
     * 진행중인지 확인
     */
    public boolean isInProgress() {
        return this.reportStatus == ReportStatus.IN_PROGRESS;
    }
}