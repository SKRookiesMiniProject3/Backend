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
 * - AI가 생성한 리포트를 읽고 대시보드, 통계, 필터링 등 다양한 조회 기능 제공
 * - CRUD 중 Create/Update/Delete는 서비스에서 처리하고 Repository는 주로 Select 전용
 */
public interface ErrorReportRepository extends JpaRepository<ErrorReport, Long> {

    // ========================================
    // 기본 조회 메서드들
    // ========================================

    /**
     * 일별 에러 카운트 통계
     * - 결과: 날짜별 에러 개수
     * - 대시보드 그래프에 사용
     */
    @Query("SELECT new com.rookies.log2doc.dto.ErrorCountPerDayDTO(" +
            "CAST(e.createdDt AS date), COUNT(e)) " +
            "FROM ErrorReport e " +
            "WHERE e.isDeleted = false " +
            "GROUP BY CAST(e.createdDt AS date) " +
            "ORDER BY CAST(e.createdDt AS date) DESC")
    List<ErrorCountPerDayDTO> findDailyErrorCounts();

    /** 최신순 전체 리스트 */
    List<ErrorReport> findByIsDeletedFalseOrderByCreatedDtDesc();

    /** ID로 단일 조회 (삭제된 것 제외) */
    Optional<ErrorReport> findByIdAndIsDeletedFalse(Long id);

    /** 리포트 파일 경로로 단일 조회 */
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportPath = :path")
    Optional<ErrorReport> findByReportPath(@Param("path") String path);

    /** 제목 키워드로 검색 */
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportTitle LIKE %:title% ORDER BY e.createdDt DESC")
    List<ErrorReport> findByReportTitleContaining(@Param("title") String title);

    // ========================================
    // 상태별 조회 메서드들
    // ========================================

    /** 진행중인 리포트 */
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportStatus = 'IN_PROGRESS' ORDER BY e.createdDt DESC")
    List<ErrorReport> findInProgressReports();

    /** 완료된 리포트 */
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportStatus = 'COMPLETED' ORDER BY e.createdDt DESC")
    List<ErrorReport> findCompletedReports();

    /** 시작되지 않은 리포트 */
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportStatus = 'NOT_STARTED' ORDER BY e.createdDt DESC")
    List<ErrorReport> findNotStartedReports();

    // ========================================
    // 카테고리별 조회 메서드들 (AI 분류)
    // ========================================

    /** AI가 탐지한 공격 리포트 (중요) */
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportCategory = 'ATTACK' ORDER BY e.createdDt DESC")
    List<ErrorReport> findAttackReports();

    /** 정상으로 분류된 리포트 */
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportCategory = 'VALID' ORDER BY e.createdDt DESC")
    List<ErrorReport> findValidReports();

    /** 비정상으로 분류된 리포트 */
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportCategory = 'INVALID' ORDER BY e.createdDt DESC")
    List<ErrorReport> findInvalidReports();

    /** 공격 카테고리 + 진행중/완료 상태 리포트 */
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportCategory = 'ATTACK' AND e.reportStatus IN ('IN_PROGRESS', 'COMPLETED') ORDER BY e.createdDt DESC")
    List<ErrorReport> findCriticalReports();

    // ========================================
    // 조합형 조회 메서드들
    // ========================================

    /** 상태 + 카테고리로 조회 */
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportStatus = :status AND e.reportCategory = :category ORDER BY e.createdDt DESC")
    List<ErrorReport> findByStatusAndCategory(@Param("status") ErrorReport.ReportStatus status,
                                              @Param("category") ErrorReport.ReportCategory category);

    /** 특정 기간의 리포트 */
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.createdDt BETWEEN :startDate AND :endDate ORDER BY e.createdDt DESC")
    List<ErrorReport> findByCreatedDtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // ========================================
    // 통계 메서드들
    // ========================================

    /** 상태별 개수 */
    @Query("SELECT e.reportStatus, COUNT(e) FROM ErrorReport e WHERE e.isDeleted = false GROUP BY e.reportStatus")
    List<Object[]> countByReportStatus();

    /** 카테고리별 개수 */
    @Query("SELECT e.reportCategory, COUNT(e) FROM ErrorReport e WHERE e.isDeleted = false GROUP BY e.reportCategory")
    List<Object[]> countByReportCategory();

    /** 전체 리포트 개수 */
    long countByIsDeletedFalse();

    /** 오늘 생성된 리포트 */
    @Query("SELECT COUNT(e) FROM ErrorReport e WHERE e.isDeleted = false AND DATE(e.createdDt) = CURRENT_DATE")
    long countTodayReports();

    /** 최근 N일간의 공격 탐지 */
    @Query("SELECT COUNT(e) FROM ErrorReport e WHERE e.isDeleted = false AND e.reportCategory = 'ATTACK' AND e.createdDt >= :since")
    long countAttackReportsSince(@Param("since") LocalDateTime since);

    // ========================================
    // 기타/특화 조회 메서드들
    // ========================================

    /** 특정 카테고리 리포트 */
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportCategory = :category ORDER BY e.createdDt DESC")
    List<ErrorReport> findByReportCategory(@Param("category") ErrorReport.ReportCategory category);

    /** 최신 공격 탐지 리포트 (LIMIT 지원) */
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportCategory = 'ATTACK' ORDER BY e.createdDt DESC LIMIT :limit")
    List<ErrorReport> findLatestAttackReports(@Param("limit") int limit);

    /** 특정 날짜 리포트 */
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND DATE(e.createdDt) = :date ORDER BY e.createdDt DESC")
    List<ErrorReport> findByCreatedDate(@Param("date") String date);

    /** 카테고리 + 상태로 최신 리포트 */
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportCategory = :category AND e.reportStatus = :status ORDER BY e.createdDt DESC LIMIT :limit")
    List<ErrorReport> findLatestByCategoryAndStatus(@Param("category") ErrorReport.ReportCategory category,
                                                    @Param("status") ErrorReport.ReportStatus status,
                                                    @Param("limit") int limit);

    /** 제목이 있는 리포트만 (AI 분석 완료된 것) */
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportTitle IS NOT NULL AND e.reportTitle != '' ORDER BY e.createdDt DESC")
    List<ErrorReport> findReportsWithTitle();

    /** 미리보기 있는 리포트만 */
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportPreview IS NOT NULL AND e.reportPreview != '' ORDER BY e.createdDt DESC")
    List<ErrorReport> findReportsWithPreview();

    /** 제목+미리보기 둘 다 있는 리포트 */
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportTitle IS NOT NULL AND e.reportTitle != '' AND e.reportPreview IS NOT NULL AND e.reportPreview != '' ORDER BY e.createdDt DESC")
    List<ErrorReport> findAiAnalyzedReports();
}
