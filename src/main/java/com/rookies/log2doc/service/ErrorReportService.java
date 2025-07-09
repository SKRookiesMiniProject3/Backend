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

    // ✅ 심각도 높은 리포트 조회 (공격 + 완료/진행중)
    public List<ErrorReportDTO> getCriticalReports() {
        return errorReportRepository.findCriticalReports()
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

    // ✅ 파일 경로로 리포트 조회
    public ErrorReportDTO getReportByPath(String reportPath) {
        ErrorReport errorReport = errorReportRepository.findByReportPath(reportPath)
                .orElseThrow(() -> new RuntimeException("해당 경로의 리포트를 찾을 수 없습니다."));

        return toDTO(errorReport);
    }

    // ✅ 제목으로 검색
    public List<ErrorReportDTO> searchReportsByTitle(String title) {
        return errorReportRepository.findByReportTitleContaining(title)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
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
    // 필터링 조회 메서드들
    // ========================================

    // ✅ 특정 기간의 리포트 조회
    public List<ErrorReportDTO> getReportsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return errorReportRepository.findByCreatedDtBetween(startDate, endDate)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ✅ 특정 상태와 카테고리 조합 조회
    public List<ErrorReportDTO> getReportsByStatusAndCategory(String status, String category) {
        try {
            ErrorReport.ReportStatus reportStatus = ErrorReport.ReportStatus.valueOf(status);
            ErrorReport.ReportCategory reportCategory = ErrorReport.ReportCategory.valueOf(category);

            return errorReportRepository.findByStatusAndCategory(reportStatus, reportCategory)
                    .stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("잘못된 상태 또는 카테고리입니다: " + status + ", " + category);
        }
    }

    // ========================================
    // 대시보드용 요약 메서드들
    // ========================================

    // ✅ 대시보드 요약 정보
    public Map<String, Object> getDashboardSummary() {
        return Map.of(
                "totalReports", getTotalReportCount(),
                "todayReports", getTodayReportCount(),
                "attackReports", getAttackReports().size(),
                "inProgressReports", getInProgressReports().size(),
                "completedReports", getCompletedReports().size(),
                "recentAttackCount", getRecentAttackCount(7), // 최근 7일
                "categoryStats", getCategoryStatistics(),
                "statusStats", getReportStatistics()
        );
    }

    // ✅ 보안 대시보드 요약 (공격 관련만)
    public Map<String, Object> getSecurityDashboardSummary() {
        List<ErrorReportDTO> attackReports = getAttackReports();
        List<ErrorReportDTO> criticalReports = getCriticalReports();

        return Map.of(
                "totalAttackReports", attackReports.size(),
                "criticalReports", criticalReports.size(),
                "recentAttacks7days", getRecentAttackCount(7),
                "recentAttacks24hours", getRecentAttackCount(1),
                "latestAttackReports", attackReports.stream().limit(10).collect(Collectors.toList())
        );
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