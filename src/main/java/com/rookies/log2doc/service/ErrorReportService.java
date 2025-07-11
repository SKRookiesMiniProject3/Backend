package com.rookies.log2doc.service;

import com.rookies.log2doc.dto.ErrorCountPerDayDTO;
import com.rookies.log2doc.dto.ErrorReportDTO;
import com.rookies.log2doc.entity.ErrorReport;
import com.rookies.log2doc.repository.ErrorReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 에러 리포트 조회/수정 서비스
 * - AI가 생성한 에러 리포트 데이터에 대한 조회/통계/상태 변경 처리
 * - 대부분 읽기 전용 트랜잭션으로 최적화
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true) // 기본적으로 읽기 전용
public class ErrorReportService {

    private final ErrorReportRepository errorReportRepository;

    // ===============================
    // 조회 메서드 (대시보드/리스트)
    // ===============================

    /**
     * 일별 에러 카운트 조회
     * @return 날짜별 에러 개수 리스트
     */
    public List<ErrorCountPerDayDTO> getDailyCounts() {
        return errorReportRepository.findDailyErrorCounts();
    }

    /**
     * 삭제되지 않은 최신 리포트 최대 50개 조회
     */
    public List<ErrorReportDTO> getLatestReports() {
        return errorReportRepository.findByIsDeletedFalseOrderByCreatedDtDesc()
                .stream()
                .limit(50)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 진행중 상태의 리포트 조회
     */
    public List<ErrorReportDTO> getInProgressReports() {
        return errorReportRepository.findInProgressReports()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 완료 상태의 리포트 조회
     */
    public List<ErrorReportDTO> getCompletedReports() {
        return errorReportRepository.findCompletedReports()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 시작되지 않은 리포트 조회
     */
    public List<ErrorReportDTO> getNotStartedReports() {
        return errorReportRepository.findNotStartedReports()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ========================================
    // 전체 조회 메소드
    // ========================================

    /**
     * 전체 에러 리포트 조회 (리스트 형태)
     */
    public List<ErrorReportDTO> getAllReportsList() {
        try {
            // 삭제되지 않은 리포트만 조회 (기존 패턴과 동일)
            List<ErrorReport> errorReports = errorReportRepository.findByIsDeletedFalseOrderByCreatedDtDesc();

            return errorReports.stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("전체 에러 리포트 리스트 조회 실패", e);
            throw new RuntimeException("전체 에러 리포트 리스트 조회 중 오류가 발생했습니다", e);
        }
    }

    // ===============================
    // 카테고리별 조회 (AI 분류 결과)
    // ===============================

    /**
     * AI가 공격으로 분류한 리포트만 조회
     */
    public List<ErrorReportDTO> getAttackReports() {
        return errorReportRepository.findAttackReports()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * AI가 정상으로 분류한 리포트 조회
     */
    public List<ErrorReportDTO> getValidReports() {
        return errorReportRepository.findValidReports()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * AI가 비정상으로 분류한 리포트 조회
     */
    public List<ErrorReportDTO> getInvalidReports() {
        return errorReportRepository.findInvalidReports()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ===============================
    // 단일 조회
    // ===============================

    /**
     * 리포트 상세 조회 (ID 기준)
     */
    public ErrorReportDTO getReportById(Long id) {
        ErrorReport errorReport = errorReportRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("에러 리포트를 찾을 수 없습니다."));
        return toDTO(errorReport);
    }

    // ===============================
    // 통계 메서드 (대시보드용)
    // ===============================

    /**
     * 상태별 리포트 개수 통계
     */
    public Map<String, Long> getReportStatistics() {
        List<Object[]> results = errorReportRepository.countByReportStatus();
        return results.stream()
                .collect(Collectors.toMap(
                        result -> ((ErrorReport.ReportStatus) result[0]).name(),
                        result -> (Long) result[1]
                ));
    }

    /**
     * 카테고리별 리포트 개수 통계
     */
    public Map<String, Long> getCategoryStatistics() {
        List<Object[]> results = errorReportRepository.countByReportCategory();
        return results.stream()
                .collect(Collectors.toMap(
                        result -> ((ErrorReport.ReportCategory) result[0]).name(),
                        result -> (Long) result[1]
                ));
    }

    /**
     * 최근 N일간 공격 탐지 건수
     */
    public long getRecentAttackCount(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return errorReportRepository.countAttackReportsSince(since);
    }

    /**
     * 전체 리포트 개수
     */
    public long getTotalReportCount() {
        return errorReportRepository.countByIsDeletedFalse();
    }

    /**
     * 오늘 생성된 리포트 개수
     */
    public long getTodayReportCount() {
        return errorReportRepository.countTodayReports();
    }

    // ===============================
    // 기간별 조회
    // ===============================

    /**
     * 시작일 ~ 종료일 범위 리포트 조회
     */
    public List<ErrorReportDTO> getReportsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return errorReportRepository.findByCreatedDtBetween(startDate, endDate)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ===============================
    // 상태/코멘트 수정 (관리자)
    // ===============================

    /**
     * 리포트 코멘트 수정
     */
    @Transactional
    public ErrorReportDTO updateComment(Long id, String comment) {
        ErrorReport errorReport = errorReportRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("에러 리포트를 찾을 수 없습니다."));

        errorReport.setReportComment(comment);
        ErrorReport saved = errorReportRepository.save(errorReport);

        log.info("에러 리포트 코멘트 수정 완료 - ID: {}", id);
        return toDTO(saved);
    }

    /**
     * 상태 업데이트 (문자열 입력받아 Enum 변환)
     */
    @Transactional
    public ErrorReportDTO updateReportStatus(Long id, String status) {
        ErrorReport errorReport = errorReportRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("에러 리포트를 찾을 수 없습니다."));

        ErrorReport.ReportStatus reportStatus;
        try {
            reportStatus = ErrorReport.ReportStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("유효하지 않은 상태값입니다: " + status);
        }

        errorReport.setReportStatus(reportStatus);
        ErrorReport saved = errorReportRepository.save(errorReport);

        switch (reportStatus) {
            case NOT_STARTED:
                log.info("리포트 상태 리셋 - ID: {}", id);
                break;
            case IN_PROGRESS:
                if (saved.isAttackCategory()) {
                    log.warn("공격 리포트 처리 시작 - ID: {}", id);
                } else {
                    log.info("리포트 처리 시작 - ID: {}", id);
                }
                break;
            case COMPLETED:
                if (saved.isAttackCategory()) {
                    log.warn("공격 리포트 처리 완료 - ID: {}", id);
                } else {
                    log.info("리포트 처리 완료 - ID: {}", id);
                }
                break;
        }

        return toDTO(saved);
    }

    /**
     * 상태를 NOT_STARTED로 변경
     */
    @Transactional
    public ErrorReportDTO setStatusNotStarted(Long id) {
        ErrorReport errorReport = errorReportRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("에러 리포트를 찾을 수 없습니다."));
        errorReport.setReportStatus(ErrorReport.ReportStatus.NOT_STARTED);

        ErrorReport saved = errorReportRepository.save(errorReport);
        log.info("리포트 상태 리셋 - ID: {}", id);
        return toDTO(saved);
    }

    /**
     * 상태를 IN_PROGRESS로 변경
     */
    @Transactional
    public ErrorReportDTO setStatusInProgress(Long id) {
        ErrorReport errorReport = errorReportRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("에러 리포트를 찾을 수 없습니다."));
        errorReport.setReportStatus(ErrorReport.ReportStatus.IN_PROGRESS);

        ErrorReport saved = errorReportRepository.save(errorReport);
        if (saved.isAttackCategory()) {
            log.warn("공격 리포트 처리 시작 - ID: {}", id);
        } else {
            log.info("리포트 처리 시작 - ID: {}", id);
        }
        return toDTO(saved);
    }

    /**
     * 상태를 COMPLETED로 변경 + 완료 코멘트 추가
     */
    @Transactional
    public ErrorReportDTO setStatusCompleted(Long id, String completionComment) {
        ErrorReport errorReport = errorReportRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("에러 리포트를 찾을 수 없습니다."));
        errorReport.setReportStatus(ErrorReport.ReportStatus.COMPLETED);

        if (completionComment != null && !completionComment.trim().isEmpty()) {
            String existing = errorReport.getReportComment();
            String newComment = existing != null ?
                    existing + "\n[완료] " + completionComment :
                    "[완료] " + completionComment;
            errorReport.setReportComment(newComment);
        }

        ErrorReport saved = errorReportRepository.save(errorReport);
        if (saved.isAttackCategory()) {
            log.warn("공격 리포트 처리 완료 - ID: {}", id);
        } else {
            log.info("리포트 처리 완료 - ID: {}", id);
        }
        return toDTO(saved);
    }

    /**
     * 에러 리포트 소프트 삭제 (isDeleted 플래그 true)
     */
    @Transactional
    public void deleteReport(Long id) {
        ErrorReport errorReport = errorReportRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("에러 리포트를 찾을 수 없습니다."));
        errorReport.setIsDeleted(true);
        errorReport.setDeletedDt(LocalDateTime.now());
        errorReportRepository.save(errorReport);
        log.info("에러 리포트 삭제 완료 - ID: {}", id);
    }

    // ===============================
    // 내부 유틸리티
    // ===============================

    /**
     * Entity → DTO 변환 메서드
     */
    private ErrorReportDTO toDTO(ErrorReport entity) {
        return ErrorReportDTO.builder()
                .id(entity.getId())
                .reportTitle(entity.getReportTitle())
                .reportPreview(entity.getReportPreview())
                .reportCategory(entity.getReportCategory().name())
                .reportCategoryDescription(entity.getReportCategory().getDescription())
                .reportPath(entity.getReportPath())
                .reportStatus(entity.getReportStatus().name())
                .reportStatusDescription(entity.getReportStatus().getDescription())
                .reportComment(entity.getReportComment())
                .isDeleted(entity.getIsDeleted())
                .createdDt(entity.getCreatedDt())
                .deletedDt(entity.getDeletedDt())
                .build();
    }
}
