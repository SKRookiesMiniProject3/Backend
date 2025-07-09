package com.rookies.log2doc.service;

import com.rookies.log2doc.dto.ErrorCountPerDayDTO;
import com.rookies.log2doc.dto.ErrorReportDTO;
import com.rookies.log2doc.entity.ErrorReport;
import com.rookies.log2doc.repository.ErrorReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true) // 읽기 전용 서비스
public class ErrorReportService {

    private final ErrorReportRepository errorReportRepository;

    // ========================================
    // 조회 메서드들 (AI가 생성한 데이터 읽기만)
    // ========================================

    // ✅ 일별 에러 카운트
    public List<ErrorCountPerDayDTO> getDailyCounts() {
        return errorReportRepository.findDailyErrorCounts();
    }

    // ✅ 최신순 리스트 (삭제되지 않은 것만)
    public List<ErrorReportDTO> getLatestReports() {
        return errorReportRepository.findByIsDeletedFalseOrderByCreatedDtDesc()
                .stream()
                .limit(50) // 최대 50개만 반환
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ✅ 진행중인 리포트 조회
    public List<ErrorReportDTO> getInProgressReports() {
        return errorReportRepository.findInProgressReports()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ✅ 완료된 리포트 조회
    public List<ErrorReportDTO> getCompletedReports() {
        return errorReportRepository.findCompletedReports()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ✅ 시작되지 않은 리포트 조회
    public List<ErrorReportDTO> getNotStartedReports() {
        return errorReportRepository.findNotStartedReports()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ========================================
    // 카테고리별 조회 (AI가 분류한 결과)
    // ========================================

    // ✅ 공격 탐지 리포트 조회 (AI가 분류한 중요 데이터!)
    public List<ErrorReportDTO> getAttackReports() {
        return errorReportRepository.findAttackReports()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ✅ 정상 리포트 조회
    public List<ErrorReportDTO> getValidReports() {
        return errorReportRepository.findValidReports()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ✅ 비정상 리포트 조회
    public List<ErrorReportDTO> getInvalidReports() {
        return errorReportRepository.findInvalidReports()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ========================================
    // 단일 조회
    // ========================================

    // ✅ 에러 리포트 상세 조회
    public ErrorReportDTO getReportById(Long id) {
        ErrorReport errorReport = errorReportRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("에러 리포트를 찾을 수 없습니다."));

        return toDTO(errorReport);
    }

    // ========================================
    // 통계 메서드들 (AI 분석 결과 통계)
    // ========================================

    // ✅ 리포트 상태별 통계
    public Map<String, Long> getReportStatistics() {
        List<Object[]> results = errorReportRepository.countByReportStatus();
        return results.stream()
                .collect(Collectors.toMap(
                        result -> ((ErrorReport.ReportStatus) result[0]).name(),
                        result -> (Long) result[1]
                ));
    }

    // ✅ 카테고리별 통계 (AI 분류 결과)
    public Map<String, Long> getCategoryStatistics() {
        List<Object[]> results = errorReportRepository.countByReportCategory();
        return results.stream()
                .collect(Collectors.toMap(
                        result -> ((ErrorReport.ReportCategory) result[0]).name(),
                        result -> (Long) result[1]
                ));
    }

    // ✅ 최근 공격 탐지 건수 (보안 대시보드용)
    public long getRecentAttackCount(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return errorReportRepository.countAttackReportsSince(since);
    }

    // ✅ 전체 리포트 개수
    public long getTotalReportCount() {
        return errorReportRepository.countByIsDeletedFalse();
    }

    // ✅ 오늘 생성된 리포트 개수
    public long getTodayReportCount() {
        return errorReportRepository.countTodayReports();
    }

    // ========================================
    // 필터링 조회 메서드
    // ========================================

    // ✅ 특정 기간의 리포트 조회
    public List<ErrorReportDTO> getReportsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return errorReportRepository.findByCreatedDtBetween(startDate, endDate)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ========================================
    // 수정 메서드들 (컨트롤러에서 사용)
    // ========================================

    // ✅ 리포트 코멘트 수정
    @Transactional
    public ErrorReportDTO updateComment(Long id, String comment) {
        ErrorReport errorReport = errorReportRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("에러 리포트를 찾을 수 없습니다."));

        errorReport.setReportComment(comment);
        ErrorReport saved = errorReportRepository.save(errorReport);
        log.info("에러 리포트 코멘트 수정 완료 - ID: {}", id);

        return toDTO(saved);
    }

    // ✅ 리포트 상태 수정 (컨트롤러에서 필요한 범용 메서드)
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

        // 상태별 로그 출력
        switch (reportStatus) {
            case NOT_STARTED:
                log.info("🔄 리포트 상태 리셋 - ID: {}", id);
                break;
            case IN_PROGRESS:
                if (saved.isAttackCategory()) {
                    log.warn("🚨 공격 리포트 처리 시작! - ID: {}", id);
                } else {
                    log.info("▶️ 리포트 처리 시작 - ID: {}", id);
                }
                break;
            case COMPLETED:
                if (saved.isAttackCategory()) {
                    log.warn("✅ 공격 리포트 처리 완료! - ID: {}", id);
                } else {
                    log.info("✅ 리포트 처리 완료 - ID: {}", id);
                }
                break;
        }

        return toDTO(saved);
    }

    /**
     * 상태를 NOT_STARTED로 변경 (리셋)
     */
    @Transactional
    public ErrorReportDTO setStatusNotStarted(Long id) {
        ErrorReport errorReport = errorReportRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("에러 리포트를 찾을 수 없습니다."));

        errorReport.setReportStatus(ErrorReport.ReportStatus.NOT_STARTED);
        ErrorReport saved = errorReportRepository.save(errorReport);

        log.info("🔄 리포트 상태 리셋 - ID: {}", id);
        return toDTO(saved);
    }

    /**
     * 상태를 IN_PROGRESS로 변경 (시작)
     */
    @Transactional
    public ErrorReportDTO setStatusInProgress(Long id) {
        ErrorReport errorReport = errorReportRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("에러 리포트를 찾을 수 없습니다."));

        errorReport.setReportStatus(ErrorReport.ReportStatus.IN_PROGRESS);
        ErrorReport saved = errorReportRepository.save(errorReport);

        // 공격 카테고리면 특별 로그
        if (saved.isAttackCategory()) {
            log.warn("🚨 공격 리포트 처리 시작! - ID: {}", id);
        } else {
            log.info("▶️ 리포트 처리 시작 - ID: {}", id);
        }

        return toDTO(saved);
    }

    /**
     * 상태를 COMPLETED로 변경 (완료)
     */
    @Transactional
    public ErrorReportDTO setStatusCompleted(Long id, String completionComment) {
        ErrorReport errorReport = errorReportRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("에러 리포트를 찾을 수 없습니다."));

        errorReport.setReportStatus(ErrorReport.ReportStatus.COMPLETED);

        // 완료 코멘트가 있으면 추가
        if (completionComment != null && !completionComment.trim().isEmpty()) {
            String existingComment = errorReport.getReportComment();
            String newComment = existingComment != null ?
                    existingComment + "\n[완료] " + completionComment :
                    "[완료] " + completionComment;
            errorReport.setReportComment(newComment);
        }

        ErrorReport saved = errorReportRepository.save(errorReport);

        // 공격 카테고리면 특별 로그
        if (saved.isAttackCategory()) {
            log.warn("✅ 공격 리포트 처리 완료! - ID: {}", id);
        } else {
            log.info("✅ 리포트 처리 완료 - ID: {}", id);
        }

        return toDTO(saved);
    }

    // ✅ 에러 리포트 소프트 삭제
    @Transactional
    public void deleteReport(Long id) {
        ErrorReport errorReport = errorReportRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("에러 리포트를 찾을 수 없습니다."));

        errorReport.setIsDeleted(true);
        errorReport.setDeletedDt(LocalDateTime.now());
        errorReportRepository.save(errorReport);
        log.info("에러 리포트 삭제 완료 - ID: {}", id);
    }

    // ========================================
    // DTO 변환 (읽기 전용)
    // ========================================

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