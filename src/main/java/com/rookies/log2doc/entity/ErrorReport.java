package com.rookies.log2doc.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

/**
 * 에러 리포트 정보 엔티티 클래스
 * Flask에서 생성된 리포트 파일의 메타데이터와 상태 관리용
 */
@Entity
@Table(name = "error_report")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorReport {

    /** 리포트 고유 ID (PK) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, columnDefinition = "BIGINT COMMENT '리포트 ID'")
    private Long id;

    /** 리포트 제목 (Flask What) */
    @Column(name = "report_title", columnDefinition = "VARCHAR(255) COMMENT '리포트 제목 (Flask What)'")
    private String reportTitle;

    /** 리포트 간략 설명 (Flask Why) */
    @Lob
    @Column(name = "report_preview", columnDefinition = "TEXT COMMENT '리포트 간략 설명 (Flask Why)'")
    private String reportPreview;

    /** 리포트 카테고리 (attack, valid, invalid) */
    @Enumerated(EnumType.STRING)
    @Column(name = "report_category", columnDefinition = "VARCHAR(255) COMMENT '카테고리 (attack, valid, invalid)'")
    @Builder.Default
    private ReportCategory reportCategory = ReportCategory.VALID;

    /** 리포트 실제 파일 경로 */
    @Column(name = "report_path", nullable = false, columnDefinition = "VARCHAR(255) NOT NULL COMMENT '리포트 파일 실제 경로'")
    private String reportPath;

    /** 리포트 진행 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "report_status", nullable = false, columnDefinition = "VARCHAR(255) NOT NULL DEFAULT 'NOT_STARTED' COMMENT '리포트 진행상황'")
    @Builder.Default
    private ReportStatus reportStatus = ReportStatus.NOT_STARTED;

    /** 리포트 작업 코멘트 */
    @Lob
    @Column(name = "report_comment", columnDefinition = "TEXT COMMENT '리포트 작업에 맞는 코멘트'")
    private String reportComment;

    /** 리포트 삭제 여부 */
    @Column(name = "is_deleted", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE COMMENT '삭제 여부'")
    @Builder.Default
    private Boolean isDeleted = false;

    /** 생성일 (자동 설정) */
    @CreatedDate
    @Column(name = "created_dt", nullable = false, updatable = false,
            columnDefinition = "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일 (레코드 생성 시 자동 설정)'")
    private LocalDateTime createdDt;

    /** 삭제일 (삭제 시 설정, null이면 미삭제) */
    @Column(name = "deleted_dt", columnDefinition = "DATETIME NULL COMMENT '삭제일 (삭제 시 설정, null: 미삭제)'")
    private LocalDateTime deletedDt;

    // ======================
    // Enum 정의
    // ======================

    /**
     * 리포트 카테고리 Enum
     * 공격 시도인지, 정상 동작인지, 비정상 예외인지 구분
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

        public String getDescription() { return description; }
        public String getDetail() { return detail; }
    }

    /**
     * 리포트 진행 상태 Enum
     */
    public enum ReportStatus {
        NOT_STARTED("시작 안함"),
        IN_PROGRESS("진행중"),
        COMPLETED("완료");

        private final String description;

        ReportStatus(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
    }

    // ======================
    // 엔티티 라이프사이클 및 비즈니스 로직
    // ======================

    /**
     * Persist 전에 필수 필드를 기본값으로 초기화
     */
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
     * 카테고리가 공격인지 여부
     */
    public boolean isAttackCategory() {
        return this.reportCategory == ReportCategory.ATTACK;
    }

    /**
     * 카테고리가 정상인지 여부
     */
    public boolean isValidCategory() {
        return this.reportCategory == ReportCategory.VALID;
    }

    /**
     * 상태가 완료인지 여부
     */
    public boolean isCompleted() {
        return this.reportStatus == ReportStatus.COMPLETED;
    }

    /**
     * 상태가 진행중인지 여부
     */
    public boolean isInProgress() {
        return this.reportStatus == ReportStatus.IN_PROGRESS;
    }
}
