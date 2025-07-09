package com.rookies.log2doc.repository;

import com.rookies.log2doc.dto.ErrorCountPerDayDTO;
import com.rookies.log2doc.entity.ErrorReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ErrorReport Repository (조회 전용)
 * AI가 생성한 데이터를 읽기만 함
 */
public interface ErrorReportRepository extends JpaRepository<ErrorReport, Long> {

    // ========================================
    // 기본 조회 메서드들
    // ========================================

    // ✅ 일별 에러 카운트 (삭제되지 않은 것만)
    @Query("SELECT new com.rookies.log2doc.dto.ErrorCountPerDayDTO(" +
            "CAST(e.createdDt AS date), COUNT(e)) " +
            "FROM ErrorReport e " +
            "WHERE e.isDeleted = false " +
            "GROUP BY CAST(e.createdDt AS date) " +
            "ORDER BY CAST(e.createdDt AS date) DESC")
    List<ErrorCountPerDayDTO> findDailyErrorCounts();

    // ✅ 최신순 리스트 (삭제되지 않은 것만)
    List<ErrorReport> findByIsDeletedFalseOrderByCreatedDtDesc();

    // ✅ 삭제되지 않은 리포트 중 ID로 조회
    Optional<ErrorReport> findByIdAndIsDeletedFalse(Long id);

    // ✅ 리포트 파일 경로로 조회
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportPath = :path")
    Optional<ErrorReport> findByReportPath(@Param("path") String path);

    // ✅ 제목으로 검색 (부분 일치)
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportTitle LIKE %:title% ORDER BY e.createdDt DESC")
    List<ErrorReport> findByReportTitleContaining(@Param("title") String title);

    // ========================================
    // 상태별 조회 메서드들
    // ========================================

    // ✅ 진행중인 리포트 조회
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportStatus = 'IN_PROGRESS' ORDER BY e.createdDt DESC")
    List<ErrorReport> findInProgressReports();

    // ✅ 완료된 리포트 조회
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportStatus = 'COMPLETED' ORDER BY e.createdDt DESC")
    List<ErrorReport> findCompletedReports();

    // ✅ 시작되지 않은 리포트 조회
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportStatus = 'NOT_STARTED' ORDER BY e.createdDt DESC")
    List<ErrorReport> findNotStartedReports();

    // ========================================
    // 카테고리별 조회 메서드들 (AI 분류 결과)
    // ========================================

    // ✅ 공격 탐지 리포트만 조회 (중요!)
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportCategory = 'ATTACK' ORDER BY e.createdDt DESC")
    List<ErrorReport> findAttackReports();

    // ✅ 정상 카테고리 리포트 조회
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportCategory = 'VALID' ORDER BY e.createdDt DESC")
    List<ErrorReport> findValidReports();

    // ✅ 비정상 카테고리 리포트 조회
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportCategory = 'INVALID' ORDER BY e.createdDt DESC")
    List<ErrorReport> findInvalidReports();

    // ✅ 심각도 높은 리포트 조회 (공격 + 진행중/완료)
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportCategory = 'ATTACK' AND e.reportStatus IN ('IN_PROGRESS', 'COMPLETED') ORDER BY e.createdDt DESC")
    List<ErrorReport> findCriticalReports();

    // ========================================
    // 조합 조회 메서드들
    // ========================================

    // ✅ 특정 상태와 카테고리로 조회
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportStatus = :status AND e.reportCategory = :category ORDER BY e.createdDt DESC")
    List<ErrorReport> findByStatusAndCategory(@Param("status") ErrorReport.ReportStatus status,
                                              @Param("category") ErrorReport.ReportCategory category);

    // ✅ 특정 기간 내 에러 리포트 조회
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.createdDt BETWEEN :startDate AND :endDate ORDER BY e.createdDt DESC")
    List<ErrorReport> findByCreatedDtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // ========================================
    // 통계 조회 메서드들
    // ========================================

    // ✅ 리포트 상태별 개수 조회
    @Query("SELECT e.reportStatus, COUNT(e) FROM ErrorReport e WHERE e.isDeleted = false GROUP BY e.reportStatus")
    List<Object[]> countByReportStatus();

    // ✅ 카테고리별 개수 조회 (AI 분류 결과)
    @Query("SELECT e.reportCategory, COUNT(e) FROM ErrorReport e WHERE e.isDeleted = false GROUP BY e.reportCategory")
    List<Object[]> countByReportCategory();

    // ✅ 전체 리포트 개수 (삭제되지 않은 것만)
    long countByIsDeletedFalse();

    // ✅ 오늘 생성된 리포트 개수
    @Query("SELECT COUNT(e) FROM ErrorReport e WHERE e.isDeleted = false AND DATE(e.createdDt) = CURRENT_DATE")
    long countTodayReports();

    // ✅ 최근 N일간의 공격 탐지 건수 (보안 대시보드용)
    @Query("SELECT COUNT(e) FROM ErrorReport e WHERE e.isDeleted = false AND e.reportCategory = 'ATTACK' AND e.createdDt >= :since")
    long countAttackReportsSince(@Param("since") LocalDateTime since);

    // ========================================
    // 특별 조회 메서드들
    // ========================================

    // ✅ 특정 카테고리의 리포트 조회
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportCategory = :category ORDER BY e.createdDt DESC")
    List<ErrorReport> findByReportCategory(@Param("category") ErrorReport.ReportCategory category);

    // ✅ 최신 공격 탐지 리포트 (제한된 개수)
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportCategory = 'ATTACK' ORDER BY e.createdDt DESC LIMIT :limit")
    List<ErrorReport> findLatestAttackReports(@Param("limit") int limit);

    // ✅ 특정 날짜의 리포트 조회
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND DATE(e.createdDt) = :date ORDER BY e.createdDt DESC")
    List<ErrorReport> findByCreatedDate(@Param("date") String date);

    // ✅ 특정 카테고리와 상태의 최신 리포트
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportCategory = :category AND e.reportStatus = :status ORDER BY e.createdDt DESC LIMIT :limit")
    List<ErrorReport> findLatestByCategoryAndStatus(@Param("category") ErrorReport.ReportCategory category,
                                                    @Param("status") ErrorReport.ReportStatus status,
                                                    @Param("limit") int limit);

    // ✅ 제목이 있는 리포트만 조회 (AI가 분석을 완료한 것들)
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportTitle IS NOT NULL AND e.reportTitle != '' ORDER BY e.createdDt DESC")
    List<ErrorReport> findReportsWithTitle();

    // ✅ 미리보기가 있는 리포트만 조회 (AI 분석 완료)
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportPreview IS NOT NULL AND e.reportPreview != '' ORDER BY e.createdDt DESC")
    List<ErrorReport> findReportsWithPreview();

    // ✅ AI 분석이 완료된 리포트 (제목과 미리보기 모두 있음)
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportTitle IS NOT NULL AND e.reportTitle != '' AND e.reportPreview IS NOT NULL AND e.reportPreview != '' ORDER BY e.createdDt DESC")
    List<ErrorReport> findAiAnalyzedReports();
}