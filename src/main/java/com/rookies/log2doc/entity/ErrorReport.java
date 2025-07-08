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

    @Column(name = "report_file_id", columnDefinition = "BIGINT COMMENT '리포트 파일 ID (attach_file.id, category = ERROR_REPORT)'")
    private Long reportFileId;

    @Column(name = "error_source_member", columnDefinition = "BIGINT COMMENT '에러 원인 사용자 ID, 처음 생성 시 or unknown 계정 시 null'")
    private Long errorSourceMember;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_status", nullable = false, columnDefinition = "VARCHAR(255) NOT NULL DEFAULT 'NOT_STARTED' COMMENT '리포트 진행상황'")
    @Builder.Default
    private ReportStatus reportStatus = ReportStatus.NOT_STARTED;

    @Lob
    @Column(name = "report_comment", columnDefinition = "TEXT COMMENT '리포트에 맞는 코멘트'")
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

    // 리포트 상태 Enum
    public enum ReportStatus {
        NOT_STARTED("시작 안함"),
        IN_PROGRESS("진행중"),
        COMPLETED("완료"),
        CANCELLED("취소"),
        ON_HOLD("보류");

        private final String description;

        ReportStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

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
    }

    // 비즈니스 메서드들

    /**
     * 리포트 시작
     */
    public void startReport() {
        this.reportStatus = ReportStatus.IN_PROGRESS;
    }

    /**
     * 리포트 완료
     */
    public void completeReport() {
        this.reportStatus = ReportStatus.COMPLETED;
    }

    /**
     * 리포트 취소
     */
    public void cancelReport() {
        this.reportStatus = ReportStatus.CANCELLED;
    }

    /**
     * 리포트 보류
     */
    public void holdReport() {
        this.reportStatus = ReportStatus.ON_HOLD;
    }

    /**
     * 소프트 삭제
     */
    public void softDelete() {
        this.isDeleted = true;
        this.deletedDt = LocalDateTime.now();
    }

    /**
     * 삭제 복구
     */
    public void restore() {
        this.isDeleted = false;
        this.deletedDt = null;
    }

    /**
     * 삭제 여부 확인
     */
    public boolean isDeleted() {
        return Boolean.TRUE.equals(this.isDeleted);
    }

    /**
     * 진행중인지 확인
     */
    public boolean isInProgress() {
        return this.reportStatus == ReportStatus.IN_PROGRESS;
    }

    /**
     * 완료되었는지 확인
     */
    public boolean isCompleted() {
        return this.reportStatus == ReportStatus.COMPLETED;
    }

    /**
     * 댓글 추가/수정
     */
    public void updateComment(String comment) {
        this.reportComment = comment;
    }
}